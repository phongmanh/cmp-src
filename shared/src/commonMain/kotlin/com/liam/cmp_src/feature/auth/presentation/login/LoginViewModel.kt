package com.liam.cmp_src.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.cmp_src.core.SUCCESS_HOLD_MILLIS
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.CredentialErrors
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
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
 * Depends only on use cases, never on `AuthRepository` directly, and never touches a
 * Compose or platform type — which is what makes it testable on every target.
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

    fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.ScreenEntered -> _uiState.update { it.clearedAfterSignIn() }

            is LoginAction.EmailChanged -> _uiState.update {
                it.copy(
                    email = action.value,
                    fieldErrors = it.fieldErrors.copy(email = null),
                    status = it.status.clearedOnEdit(),
                )
            }

            is LoginAction.PasswordChanged -> _uiState.update {
                it.copy(
                    password = action.value,
                    fieldErrors = it.fieldErrors.copy(password = null),
                    status = it.status.clearedOnEdit(),
                )
            }

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
        val current = _uiState.value
        if (current.isBusy) return

        val errors = validateCredentials(current.email, current.password)
        if (errors.hasErrors) {
            _uiState.update { it.copy(fieldErrors = errors, status = LoginStatus.Idle) }
            return
        }

        _uiState.update {
            it.copy(fieldErrors = CredentialErrors.NONE, status = LoginStatus.Submitting(null))
        }
        viewModelScope.launch {
            handle(signInWithEmail(current.email, current.password))
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

    /** Editing a field dismisses a previous failure, but must not interrupt one in flight. */
    private fun LoginStatus.clearedOnEdit(): LoginStatus =
        if (this is LoginStatus.Failed) LoginStatus.Idle else this

    /**
     * A blank form for a screen that is being entered after a completed sign-in.
     *
     * This ViewModel outlives the screen — it is resolved from the app's single
     * `ViewModelStoreOwner` — so signing out and landing back on login would otherwise inherit
     * [LoginStatus.Succeeded], which keeps every field and button disabled and leaves the form
     * unusable. Only that terminal state is cleared: re-entering mid-typing or mid-submit
     * (a configuration change, say) keeps what the user had.
     */
    private fun LoginUiState.clearedAfterSignIn(): LoginUiState =
        if (status is LoginStatus.Succeeded) LoginUiState() else this
}
