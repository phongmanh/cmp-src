package com.liam.cmp_src.feature.auth.data.social

import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/**
 * What a provider's sign-in sheet handed back.
 *
 * Deliberately not an `AuthResult`: a provider credential is not a session. It is spent against
 * `POST /auth/social`, and the server decides who the user is — a client that minted its own
 * session from a provider response would be trusting an unverified token.
 */
sealed interface SocialCredential {

    /**
     * [token] is the credential the provider's SDK produced — an OpenID Connect id_token for
     * Google, an access token for Facebook. It is sent once and never stored.
     */
    data class Granted(val token: String) : SocialCredential

    /** The sheet was dismissed, or the SDK failed before producing a credential. */
    data class Denied(val error: AuthError) : SocialCredential
}

/**
 * Drives a third-party sign-in sheet and reports the credential it produced.
 *
 * This is the single seam between shared code and each platform's vendor SDK. Everything
 * above it — repository, use cases, ViewModel, UI — is fully platform-independent and needs
 * no change when the real SDKs are wired in.
 *
 * Implementations must not throw: a failed or dismissed sign-in comes back as
 * [SocialCredential.Denied] with the appropriate `AuthError`.
 */
interface SocialAuthClient {
    suspend fun requestCredential(provider: SocialProvider): SocialCredential
}

/**
 * Builds the platform's client. Actuals live in `SocialAuthClient.<target>.kt` alongside the
 * other per-target files, matching the `Platform.<target>.kt` convention.
 *
 * Every actual currently delegates to [DemoSocialAuthClient] so the flow is wired end to end on
 * all five targets today; each one carries a KDoc block naming the SDK call that replaces
 * that delegation.
 */
expect fun createSocialAuthClient(): SocialAuthClient
