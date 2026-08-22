package com.liam.cmp_src.feature.auth.data.social

import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/**
 * Desktop (JVM) social sign-in.
 *
 * Neither Google nor Facebook ships a desktop SDK, so this one is a hand-rolled OAuth 2.0
 * authorization-code flow with PKCE:
 * 1. Start a loopback `HttpServer` on an ephemeral `127.0.0.1` port.
 * 2. Open the provider's authorize URL in the system browser via `java.awt.Desktop.browse`,
 *    with `redirect_uri` pointing at that loopback port and a freshly generated `state`.
 * 3. Await the redirect, reject any mismatched `state`, then exchange the code for tokens.
 *
 * Use the loopback redirect, not an embedded WebView — Google rejects embedded user-agents
 * for OAuth. Map the user closing the browser tab (server times out with no callback) to
 * `SocialCredential.Denied(AuthError.Cancelled)`, and treat the client secret as public:
 * desktop apps cannot keep one, which is exactly why PKCE is required here.
 */
internal class JvmSocialAuthClient(
    private val delegate: SocialAuthClient = DemoSocialAuthClient(),
) : SocialAuthClient {

    override suspend fun requestCredential(provider: SocialProvider): SocialCredential =
        delegate.requestCredential(provider)
}

actual fun createSocialAuthClient(): SocialAuthClient = JvmSocialAuthClient()
