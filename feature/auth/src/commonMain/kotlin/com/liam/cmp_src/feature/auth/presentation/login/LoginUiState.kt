package com.liam.cmp_src.feature.auth.presentation.login

import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.CredentialErrors
import com.liam.cmp_src.core.domain.model.SocialProvider

/**
 * Where the sign-in attempt currently stands.
 *
 * A login form has to keep rendering its fields in every state, so the screen state is a
 * data class carrying this sealed status rather than being a sealed hierarchy itself — the
 * status is still a closed set, but the password visibility and field errors survive across it.
 */
sealed interface LoginStatus {

    data object Idle : LoginStatus

    /** In flight. [provider] is `null` for the email/password form. */
    data class Submitting(val provider: SocialProvider?) : LoginStatus

    /**
     * The attempt failed. [nonce] increments per failure so the UI can re-run the shake
     * animation when the same error happens twice in a row.
     */
    data class Failed(val error: AuthError) : LoginStatus

    data object Succeeded : LoginStatus
}

/**
 * Everything the login screen renders apart from the typed text, which is in
 * [LoginViewModel.email] and [LoginViewModel.password].
 */
data class LoginUiState(
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
}
