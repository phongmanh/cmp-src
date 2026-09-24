package com.liam.cmp_src.feature.profile.changepassword

import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.cmp_src.core.ui.SUCCESS_HOLD_MILLIS
import com.liam.cmp_src.core.ui.input.FormField
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.profile.domain.model.ChangePasswordErrors
import com.liam.cmp_src.feature.profile.domain.usecase.ChangePasswordUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.ValidateChangePasswordUseCase
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
 * Holds the change-password dialog's state and runs the attempt.
 *
 * Notably it does *not* sign the user out afterwards. The server ends every other session and
 * hands this device a fresh token pair for exactly that purpose, and `AuthApi.changePassword`
 * has already stored it — so the dialog closes onto a still-signed-in profile.
 *
 * Depends only on use cases. Its one Compose type is the `TextFieldState` inside each
 * [FormField] — plain state with no UI behind it — so it still runs on every target.
 */
class ChangePasswordViewModel(
    private val changePassword: ChangePasswordUseCase,
    private val validateChangePassword: ValidateChangePasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChangePasswordEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ChangePasswordEvent> = _events.asSharedFlow()

    val currentPassword = FormField(viewModelScope) {
        _uiState.update { it.clearedOnEdit(it.fieldErrors.copy(currentPassword = null)) }
    }

    val newPassword = FormField(viewModelScope) {
        _uiState.update { it.clearedOnEdit(it.fieldErrors.copy(newPassword = null)) }
    }

    val confirmPassword = FormField(viewModelScope) {
        _uiState.update { it.clearedOnEdit(it.fieldErrors.copy(confirmPassword = null)) }
    }

    fun onAction(action: ChangePasswordAction) {
        when (action) {
            // A reopened dialog starts blank rather than inheriting the last attempt's input.
            ChangePasswordAction.Opened -> {
                currentPassword.state.clearText()
                newPassword.state.clearText()
                confirmPassword.state.clearText()
                _uiState.value = ChangePasswordUiState()
            }

            ChangePasswordAction.ToggleCurrentVisibility -> _uiState.update {
                it.copy(isCurrentVisible = !it.isCurrentVisible)
            }

            ChangePasswordAction.ToggleNewVisibility -> _uiState.update {
                it.copy(isNewVisible = !it.isNewVisible)
            }

            ChangePasswordAction.Submit -> submit()

            ChangePasswordAction.Cancel ->
                viewModelScope.launch { _events.emit(ChangePasswordEvent.Dismissed) }
        }
    }

    private fun submit() {
        if (_uiState.value.isBusy) return
        val typedCurrent = currentPassword.submit()
        val typedNew = newPassword.submit()

        val errors = validateChangePassword(
            currentPassword = typedCurrent,
            newPassword = typedNew,
            confirmPassword = confirmPassword.submit(),
        )
        if (errors.hasErrors) {
            _uiState.update { it.copy(fieldErrors = errors, status = ChangePasswordStatus.Idle) }
            return
        }

        _uiState.update {
            it.copy(
                fieldErrors = ChangePasswordErrors.NONE,
                status = ChangePasswordStatus.Submitting,
            )
        }

        viewModelScope.launch {
            val result = changePassword(
                currentPassword = typedCurrent,
                newPassword = typedNew,
            )
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(status = ChangePasswordStatus.Succeeded) }
                    // Let the button's success animation land before the dialog closes.
                    delay(SUCCESS_HOLD_MILLIS.milliseconds)
                    _events.emit(ChangePasswordEvent.Changed)
                }

                is AuthResult.Failure -> _uiState.update {
                    it.copy(status = ChangePasswordStatus.Failed(result.error))
                }
            }
        }
    }

    /**
     * Editing a field clears its own error, and dismisses a previous failure — but must not
     * interrupt one in flight.
     */
    private fun ChangePasswordUiState.clearedOnEdit(
        fieldErrors: ChangePasswordErrors,
    ): ChangePasswordUiState = copy(
        fieldErrors = fieldErrors,
        status = if (status is ChangePasswordStatus.Failed) ChangePasswordStatus.Idle else status,
    )
}
