package com.liam.cmp_src.feature.profile.changepassword

import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.feature.profile.domain.model.ChangePasswordErrors

/**
 * Where the change-password attempt currently stands.
 *
 * Same shape as `LoginStatus`, and for the same reason: the form has to keep rendering its three
 * fields in every state, so the screen state is a data class carrying a sealed status rather than
 * being a sealed hierarchy itself.
 */
sealed interface ChangePasswordStatus {
    data object Idle : ChangePasswordStatus
    data object Submitting : ChangePasswordStatus
    data object Succeeded : ChangePasswordStatus
    data class Failed(val error: AuthError) : ChangePasswordStatus
}

/**
 * Everything the change-password dialog renders.
 *
 * The confirmation field has no visibility flag of its own — it follows [isNewVisible], because
 * revealing one half of a pair the user is asked to match and not the other helps nobody.
 */
data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isCurrentVisible: Boolean = false,
    val isNewVisible: Boolean = false,
    val fieldErrors: ChangePasswordErrors = ChangePasswordErrors.NONE,
    val status: ChangePasswordStatus = ChangePasswordStatus.Idle,
) {
    /** True from the moment the call starts until the dialog is on its way out. */
    val isBusy: Boolean
        get() = status is ChangePasswordStatus.Submitting || status is ChangePasswordStatus.Succeeded

    val error: AuthError?
        get() = (status as? ChangePasswordStatus.Failed)?.error
}
