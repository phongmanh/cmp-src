package com.liam.cmp_src.feature.profile.domain.model

import com.liam.cmp_src.core.domain.model.PasswordError

/**
 * Per-field validation outcome for the change-password form, which has three password fields and
 * no email. [hasErrors] is what the caller gates submission on.
 *
 * Separate from `CredentialErrors` rather than reusing its single `password` slot: one slot for
 * three inputs would render the same message under all of them, and the user could not tell which
 * field it was about.
 */
data class ChangePasswordErrors(
    val currentPassword: PasswordError? = null,
    val newPassword: PasswordError? = null,
    val confirmPassword: PasswordError? = null,
) {
    val hasErrors: Boolean
        get() = currentPassword != null || newPassword != null || confirmPassword != null

    companion object {
        val NONE = ChangePasswordErrors()
    }
}
