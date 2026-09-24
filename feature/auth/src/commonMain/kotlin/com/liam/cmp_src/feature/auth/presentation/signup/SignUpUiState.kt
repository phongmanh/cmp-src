package com.liam.cmp_src.feature.auth.presentation.signup

import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.CredentialErrors

sealed interface SignUpUiStatus {
    data object Idle : SignUpUiStatus
    data object Submitted : SignUpUiStatus
    data object Succeeded : SignUpUiStatus
    data class Failed(val authError: AuthError) : SignUpUiStatus
}

/**
 * Everything the sign-up screen renders apart from the typed text, which is in
 * [SignUpViewModel.email] and [SignUpViewModel.password].
 */
data class SignUpUiState(
    val status: SignUpUiStatus = SignUpUiStatus.Idle,
    val isPasswordVisible: Boolean = false,
    val fieldErrors: CredentialErrors = CredentialErrors.NONE,
) {
    val error get() = (status as? SignUpUiStatus.Failed)?.authError
    val isBusy get() = status is SignUpUiStatus.Succeeded || status is SignUpUiStatus.Submitted
}