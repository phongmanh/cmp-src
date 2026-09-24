package com.liam.cmp_src.feature.auth.presentation.login

import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.cmp_src.core.ui.SUCCESS_HOLD_MILLIS
import com.liam.cmp_src.core.ui.input.FormField
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.CredentialErrors
import com.liam.cmp_src.core.domain.model.SocialProvider
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithEmailUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithSocialUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateCredentialsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Holds the login screen's state and runs sign-in attempts.
 *
 * Depends only on use cases, never on `AuthRepository` directly. Its one Compose type is the
 * `TextFieldState` inside each [FormField] — plain state with no UI behind it, so this still
 * runs and is tested on every target.
 */
class LoginViewModel(
    private val signInWithEmail: SignInWithEmailUseCase,
    private val signInWithSocial: SignInWithSocialUseCase,
    private val validateCredentials: ValidateCredentialsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    val email = FormField(viewModelScope) {
        _uiState.update { it.clearedOnEdit(it.fieldErrors.copy(email = null)) }
    }

    val password = FormField(viewModelScope) {
        _uiState.update { it.clearedOnEdit(it.fieldErrors.copy(password = null)) }
    }

    fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.ScreenEntered -> clearAfterSignIn()

            LoginAction.TogglePasswordVisibility -> _uiState.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }

            LoginAction.Submit -> submitEmailAndPassword()

            is LoginAction.SocialSignInClicked -> submitSocial(action.provider)

            LoginAction.ForgotPasswordClicked
                -> viewModelScope.launch { _events.emit(LoginEvent.ShowNotImplemented) }

            LoginAction.SignUpClicked,
                -> viewModelScope.launch { _events.emit(LoginEvent.SignUpClicked) }
        }
    }

    private fun submitEmailAndPassword() {
        if (_uiState.value.isBusy) return
        val typedEmail = email.submit()
        val typedPassword = password.submit()

        val errors = validateCredentials(typedEmail, typedPassword)
        if (errors.hasErrors) {
            _uiState.update { it.copy(fieldErrors = errors, status = LoginStatus.Idle) }
            return
        }

        _uiState.update {
            it.copy(fieldErrors = CredentialErrors.NONE, status = LoginStatus.Submitting(null))
        }
        viewModelScope.launch {
            handle(signInWithEmail(typedEmail, typedPassword))
        }
    }

    private fun submitSocial(provider: SocialProvider) {
        if (_uiState.value.isBusy) return

        _uiState.update {
            it.copy(fieldErrors = CredentialErrors.NONE, status = LoginStatus.Submitting(provider))
        }
        viewModelScope.launch {
            handle(signInWithSocial(provider))
        }
    }

    private suspend fun handle(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                _uiState.update { it.copy(status = LoginStatus.Succeeded) }
                // Let the button's success animation land before the screen changes.
                delay(SUCCESS_HOLD_MILLIS.milliseconds)
                _events.emit(LoginEvent.NavigateToHome(result.user))
            }

            is AuthResult.Failure -> {
                _uiState.update {
                    it.copy(status = LoginStatus.Failed(result.error))
                }
            }
        }
    }

    /**
     * Editing a field clears its own error, and dismisses a previous failure — but must not
     * interrupt one in flight.
     */
    private fun LoginUiState.clearedOnEdit(fieldErrors: CredentialErrors): LoginUiState = copy(
        fieldErrors = fieldErrors,
        status = if (status is LoginStatus.Failed) LoginStatus.Idle else status,
    )

    /**
     * Blanks the form for a screen that is being entered after a completed sign-in.
     *
     * This ViewModel outlives the screen — it is resolved from the app's single
     * `ViewModelStoreOwner` — so signing out and landing back on login would otherwise inherit
     * [LoginStatus.Succeeded], which keeps every field and button disabled and leaves the form
     * unusable. Only that terminal state is cleared: re-entering mid-typing or mid-submit
     * (a configuration change, say) keeps what the user had.
     */
    private fun clearAfterSignIn() {
        if (_uiState.value.status !is LoginStatus.Succeeded) return
        email.state.clearText()
        password.state.clearText()
        _uiState.value = LoginUiState()
    }
}
