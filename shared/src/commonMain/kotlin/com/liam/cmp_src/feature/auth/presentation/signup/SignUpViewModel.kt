package com.liam.cmp_src.feature.auth.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.cmp_src.core.SUCCESS_HOLD_MILLIS
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
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
import kotlin.time.Duration.Companion.microseconds

class SignUpViewModel(
    private val signUpUseCase: SignUpUseCase,
    private val validateCredentials: ValidateCredentialsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _effect = Channel<SignUpEvent>()
    val effect = _effect.receiveAsFlow()

    fun onAction(action: SignUpAction) {
        when (action) {
            SignUpAction.NavigateBack -> viewModelScope.launch { _effect.send(SignUpEvent.NavigateBackToLogin) }
            is SignUpAction.Submit -> onSubmit()
            is SignUpAction.EmailChanged -> {
                _state.update { it.copy(email = action.value, fieldErrors = it.fieldErrors.copy(email = null)) }
            }

            is SignUpAction.PasswordChanged -> {
                _state.update { it.copy(password = action.value, fieldErrors = it.fieldErrors.copy(password = null)) }
            }
        }
    }

    private fun onSubmit() {
        val current = _state.value
        if (current.isBusy) return

        val credentialErrors = validateCredentials(current.email, current.password)
        if (credentialErrors.hasErrors) {
            _state.update { it.copy(status = SignUpUiStatus.Idle, fieldErrors = credentialErrors) }
            return
        }
        _state.update {
            it.copy(fieldErrors = CredentialErrors.NONE, status = SignUpUiStatus.Submitted)
        }

        viewModelScope.launch {
            when (val result = signUpUseCase(current.email, current.password)) {
                is AuthResult.Failure -> _state.update { it.copy(status = SignUpUiStatus.Failed(result.error)) }
                is AuthResult.Success -> {
                    _state.update { it.copy(status = SignUpUiStatus.Succeeded) }
                    delay(SUCCESS_HOLD_MILLIS.microseconds)
                    _effect.send(SignUpEvent.NavigateToHome(result.user))
                }
            }
        }
    }

}