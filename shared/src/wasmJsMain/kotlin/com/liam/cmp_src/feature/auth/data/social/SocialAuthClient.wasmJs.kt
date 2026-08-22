package com.liam.cmp_src.feature.auth.data.social

import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/**
 * Web (Wasm) social sign-in.
 *
 * Same two SDKs as the JS target — Google Identity Services and the Facebook JS SDK, both
 * loaded from `webApp`'s `index.html`. The difference is the interop layer: Wasm `external`
 * declarations only allow types implementing `JsAny`, so results come back as `JsString` /
 * `JsAny` and are converted with `toString()` / `.toKotlinString()` rather than being used
 * as `dynamic`, which does not exist here.
 *
 * The popup-blocker constraint from the JS target applies identically: invoke the SDK inside
 * the user-gesture callback, never after a suspend point that yields to the event loop.
 */
internal class WasmJsSocialAuthClient(
    private val delegate: SocialAuthClient = DemoSocialAuthClient(),
) : SocialAuthClient {

    override suspend fun requestCredential(provider: SocialProvider): SocialCredential =
        delegate.requestCredential(provider)
}

actual fun createSocialAuthClient(): SocialAuthClient = WasmJsSocialAuthClient()
