package com.liam.cmp_src.feature.profile.domain.usecase

import com.example.api.common.FieldLimits
import com.liam.cmp_src.feature.profile.domain.model.ChangePasswordErrors
import com.liam.cmp_src.core.domain.model.PasswordError

/**
 * Checks a change-password form against the bounds the server enforces, before anything is sent.
 *
 * Every limit comes from the contract's [FieldLimits] rather than being restated here, so the two
 * sides cannot disagree about where the line is.
 *
 * Each field is judged on its own: a blank current password must not hide a too-short new one, or
 * the user fixes one problem only to be told about the next.
 */
class ValidateChangePasswordUseCase {

    operator fun invoke(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
    ): ChangePasswordErrors = ChangePasswordErrors(
        currentPassword = validateCurrent(currentPassword),
        newPassword = validateNew(currentPassword, newPassword),
        confirmPassword = validateConfirm(newPassword, confirmPassword),
    )

    private fun validateCurrent(currentPassword: String): PasswordError? =
        if (currentPassword.isEmpty()) PasswordError.Blank else null

    /**
     * The byte ceiling is checked as well as the character one: BCrypt silently ignores everything
     * past 72 bytes, so the server rejects a password that is short enough in characters but long
     * enough in multi-byte ones to cross it.
     */
    private fun validateNew(currentPassword: String, newPassword: String): PasswordError? = when {
        newPassword.isEmpty() -> PasswordError.Blank
        newPassword.length < FieldLimits.MIN_PASSWORD_LENGTH ->
            PasswordError.TooShort(FieldLimits.MIN_PASSWORD_LENGTH)

        newPassword.length > FieldLimits.MAX_PASSWORD_LENGTH ->
            PasswordError.TooLong(FieldLimits.MAX_PASSWORD_LENGTH)

        newPassword.encodeToByteArray().size > FieldLimits.MAX_PASSWORD_BYTES ->
            PasswordError.TooLong(FieldLimits.MAX_PASSWORD_BYTES)

        newPassword == currentPassword -> PasswordError.SameAsCurrent
        else -> null
    }

    private fun validateConfirm(newPassword: String, confirmPassword: String): PasswordError? =
        if (confirmPassword != newPassword) PasswordError.Mismatch else null
}
