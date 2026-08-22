package com.liam.cmp_src.feature.auth.data.social

import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/**
 * iOS social sign-in.
 *
 * To make this real:
 * - **Google** — add the `GoogleSignIn` pod (or SPM package) to `iosApp/iosApp.xcodeproj`,
 *   put your `GIDClientID` and the reversed-client-id URL scheme in `Info.plist`, then call
 *   `GIDSignIn.sharedInstance.signInWithPresentingViewController(...)`.
 * - **Facebook** — add `FBSDKLoginKit`, configure `FacebookAppID` / `FacebookClientToken` and
 *   the `fbauth2` URL scheme in `Info.plist`, then call
 *   `FBSDKLoginManager.logInWithPermissions(...)`.
 *
 * Both callbacks are completion-handler based; bridge them with `suspendCancellableCoroutine`
 * and map a user dismissal to `SocialCredential.Denied(AuthError.Cancelled)`.
 *
 * Both also need the presenting `UIViewController`. Resolve it lazily at call time (e.g.
 * from the key window's `rootViewController`) rather than capturing one here — this client
 * outlives any single controller.
 */
internal class IosSocialAuthClient(
    private val delegate: SocialAuthClient = DemoSocialAuthClient(),
) : SocialAuthClient {

    override suspend fun requestCredential(provider: SocialProvider): SocialCredential =
        delegate.requestCredential(provider)
}

actual fun createSocialAuthClient(): SocialAuthClient = IosSocialAuthClient()
