package com.liam.cmp_src.feature.auth.data.social

import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/**
 * Web (JS) social sign-in.
 *
 * To make this real:
 * - **Google** — load `https://accounts.google.com/gsi/client` from `webApp`'s `index.html`,
 *   then call `google.accounts.id.initialize({ client_id, callback })` followed by
 *   `google.accounts.id.prompt()`. The callback hands back a JWT credential to verify
 *   server-side.
 * - **Facebook** — load `https://connect.facebook.net/en_US/sdk.js`, call `FB.init({ appId })`
 *   and then `FB.login(callback, { scope: "email" })`.
 *
 * Declare both as `external` JS interfaces and bridge their callbacks with
 * `suspendCancellableCoroutine`. Trigger them straight from the click handler — browsers
 * block popups that are not tied to a user gesture, so any `delay` before the call will
 * cause the sign-in window to be suppressed. A dismissed popup maps to
 * `SocialCredential.Denied(AuthError.Cancelled)`.
 */
internal class JsSocialAuthClient(
    private val delegate: SocialAuthClient = DemoSocialAuthClient(),
) : SocialAuthClient {

    override suspend fun requestCredential(provider: SocialProvider): SocialCredential =
        delegate.requestCredential(provider)
}

actual fun createSocialAuthClient(): SocialAuthClient = JsSocialAuthClient()
