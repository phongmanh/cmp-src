package com.liam.cmp_src.feature.auth.domain.model

/** Why an email failed validation, or `null` slots in [CredentialErrors] when it passed. */
sealed interface EmailError {
    data object Blank : EmailError
    data object Malformed : EmailError
}

/** Why a password failed validation. */
sealed interface PasswordError {
    data object Blank : PasswordError
    data class TooShort(val minLength: Int) : PasswordError

    /**
     * Longer than the server will accept. [maxLength] is in characters; the byte ceiling BCrypt
     * imposes is reported the same way, because "shorten it" is the only remedy either way.
     */
    data class TooLong(val maxLength: Int) : PasswordError

    /** A new password identical to the one it was meant to replace. */
    data object SameAsCurrent : PasswordError

    /** A confirmation that does not match the new password above it. */
    data object Mismatch : PasswordError
}

/**
 * Per-field validation outcome. [hasErrors] is what the caller gates submission on; the
 * individual fields drive the inline messages under each input.
 */
data class CredentialErrors(
    val email: EmailError? = null,
    val password: PasswordError? = null,
) {
    val hasErrors: Boolean get() = email != null || password != null

    companion object {
        val NONE = CredentialErrors()
    }
}

/**
 * The same idea for the change-password form, which has three password fields and no email.
 *
 * Separate from [CredentialErrors] rather than reusing its single `password` slot: one slot for
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
