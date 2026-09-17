package com.liam.cmp_src.core.domain.model

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
