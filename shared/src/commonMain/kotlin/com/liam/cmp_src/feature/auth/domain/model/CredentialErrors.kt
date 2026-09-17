package com.liam.cmp_src.feature.auth.domain.model

import com.liam.cmp_src.core.domain.model.PasswordError

/** Why an email failed validation, or `null` slots in [CredentialErrors] when it passed. */
sealed interface EmailError {
    data object Blank : EmailError
    data object Malformed : EmailError
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
