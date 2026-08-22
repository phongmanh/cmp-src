package com.liam.cmp_src.feature.auth.presentation

import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.CredentialErrors
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/**
 * Where the sign-in attempt currently stands.
 *
 * A login form has to keep rendering its fields in every state, so the screen state is a
 * data class carrying this sealed status rather than being a sealed hierarchy itself — the
 * status is still a closed set, but the typed-in email and password survive across it.
 */
sealed interface LoginStatus {

    data object Idle : LoginStatus

    /** In flight. [provider] is `null` for the email/password form. */
    data class Submitting(val provider: SocialProvider?) : LoginStatus

    /**
     * The attempt failed. [nonce] increments per failure so the UI can re-run the shake
     * animation when the same error happens twice in a row.
     */
    data class Failed(val error: AuthError, val nonce: Int) : LoginStatus

    data object Succeeded : LoginStatus
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val fieldErrors: CredentialErrors = CredentialErrors.NONE,
    val status: LoginStatus = LoginStatus.Idle,
) {
    val isBusy: Boolean
        get() = status is LoginStatus.Submitting || status is LoginStatus.Succeeded

    /** True while the email/password form specifically is submitting. */
    val isSubmittingEmail: Boolean
        get() = status is LoginStatus.Submitting && status.provider == null

    /** Non-null while a social provider specifically is submitting. */
    val submittingProvider: SocialProvider?
        get() = (status as? LoginStatus.Submitting)?.provider

    val error: AuthError?
        get() = (status as? LoginStatus.Failed)?.error

    val errorNonce: Int
        get() = (status as? LoginStatus.Failed)?.nonce ?: 0
}
