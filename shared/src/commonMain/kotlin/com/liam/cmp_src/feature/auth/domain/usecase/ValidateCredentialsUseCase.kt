package com.liam.cmp_src.feature.auth.domain.usecase

import com.liam.cmp_src.feature.auth.domain.model.CredentialErrors
import com.liam.cmp_src.feature.auth.domain.model.EmailError
import com.liam.cmp_src.feature.auth.domain.model.PasswordError

/**
 * Checks an email/password pair against the app's own input rules, before anything is sent
 * anywhere. Deliberately client-side only: it rejects input that could not possibly be
 * valid, and leaves "is this actually an account" to the auth backend.
 */
class ValidateCredentialsUseCase {

    operator fun invoke(email: String, password: String): CredentialErrors =
        CredentialErrors(
            email = validateEmail(email),
            password = validatePassword(password),
        )

    private fun validateEmail(email: String): EmailError? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> EmailError.Blank
            !EMAIL_PATTERN.matches(trimmed) -> EmailError.Malformed
            else -> null
        }
    }

    private fun validatePassword(password: String): PasswordError? = when {
        password.isEmpty() -> PasswordError.Blank
        password.length < MIN_PASSWORD_LENGTH -> PasswordError.TooShort(MIN_PASSWORD_LENGTH)
        else -> null
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8

        /**
         * Intentionally permissive: a local part, an `@`, and a dotted domain with a
         * two-plus-letter TLD. Fully validating an address is a job for the server.
         */
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
