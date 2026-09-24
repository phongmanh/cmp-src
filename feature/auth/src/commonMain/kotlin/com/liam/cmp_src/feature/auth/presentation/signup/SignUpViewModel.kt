package com.liam.cmp_src.feature.auth.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.cmp_src.core.ui.SUCCESS_HOLD_MILLIS
import com.liam.cmp_src.core.ui.input.FormField
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.CredentialErrors
import com.liam.cmp_src.feature.auth.domain.usecase.SignUpUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateCredentialsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SignUpViewModel(
    private val signUpUseCase: SignUpUseCase,
    private val validateCredentials: ValidateCredentialsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _effect = Channel<SignUpEvent>()
    val effect = _effect.receiveAsFlow()

    val email = FormField(viewModelScope) {
        _state.update { it.copy(fieldErrors = it.fieldErrors.copy(email = null)) }
    }

    val password = FormField(viewModelScope) {
        _state.update { it.copy(fieldErrors = it.fieldErrors.copy(password = null)) }
    }

    fun onAction(action: SignUpAction) {
        when (action) {
            SignUpAction.NavigateBack -> viewModelScope.launch { _effect.send(SignUpEvent.NavigateBackToLogin) }
            SignUpAction.Submit -> onSubmit()
            SignUpAction.TogglePasswordVisibility ->
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
        }
    }

    private fun onSubmit() {
        if (_state.value.isBusy) return
        val typedEmail = email.submit()
        val typedPassword = password.submit()

        val credentialErrors = validateCredentials(typedEmail, typedPassword)
        if (credentialErrors.hasErrors) {
            _state.update { it.copy(status = SignUpUiStatus.Idle, fieldErrors = credentialErrors) }
            return
        }
        _state.update {
            it.copy(fieldErrors = CredentialErrors.NONE, status = SignUpUiStatus.Submitted)
        }

        viewModelScope.launch {
            when (val result = signUpUseCase(typedEmail, typedPassword)) {
                is AuthResult.Failure -> _state.update { it.copy(status = SignUpUiStatus.Failed(result.error)) }
                is AuthResult.Success -> {
                    _state.update { it.copy(status = SignUpUiStatus.Succeeded) }
                    delay(SUCCESS_HOLD_MILLIS.milliseconds)
                    _effect.send(SignUpEvent.NavigateToHome(result.user))
                }
            }
        }
    }

}