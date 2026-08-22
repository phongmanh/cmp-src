package com.liam.cmp_src.feature.auth.data.social

import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/**
 * Android social sign-in.
 *
 * To make this real:
 * - **Google** — add `androidx.credentials:credentials`,
 *   `androidx.credentials:credentials-play-services-auth` and
 *   `com.google.android.libraries.identity.googleid:googleid`, then build a
 *   `GetGoogleIdOption` with your web client ID and call
 *   `CredentialManager.create(context).getCredential(activityContext, request)`. Map
 *   `GetCredentialCancellationException` to `SocialCredential.Denied(AuthError.Cancelled)`,
 *   and any other `GetCredentialException` to `SocialCredential.Denied(AuthError.Unknown)`.
 * - **Facebook** — add `com.facebook.android:facebook-login`, declare the app id and client
 *   token in the manifest, then drive `LoginManager.getInstance().logInWithReadPermissions(...)`
 *   through a `CallbackManager`, bridging its callback with `suspendCancellableCoroutine`.
 *
 * Both need an `Activity` context, which this factory does not have. When wiring them up,
 * give [createSocialAuthClient] an `ActivityProvider` (or resolve one from Koin's
 * `androidContext()`) rather than holding an `Activity` reference in a long-lived object —
 * that leaks.
 */
internal class AndroidSocialAuthClient(
    private val delegate: SocialAuthClient = DemoSocialAuthClient(),
) : SocialAuthClient {

    override suspend fun requestCredential(provider: SocialProvider): SocialCredential =
        delegate.requestCredential(provider)
}

actual fun createSocialAuthClient(): SocialAuthClient = AndroidSocialAuthClient()
