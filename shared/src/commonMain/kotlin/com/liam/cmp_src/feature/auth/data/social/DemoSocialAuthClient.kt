package com.liam.cmp_src.feature.auth.data.social

import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Stand-in for a real provider SDK.
 *
 * It hands back a placeholder credential after a delay long enough to stand in for the time a
 * user spends in the provider's sheet. The exchange that follows is real: the token goes to
 * `POST /auth/social` and the server decides, which means sign-in succeeds here only against a
 * deployment that accepts these placeholder tokens. Every
 * `SocialAuthClient.<target>.kt` actual must replace this with its vendor SDK before shipping.
 */
class DemoSocialAuthClient(
    private val signInDelayMillis: Long = DEFAULT_SIGN_IN_DELAY_MILLIS,
) : SocialAuthClient {

    override suspend fun requestCredential(provider: SocialProvider): SocialCredential {
        delay(signInDelayMillis.milliseconds)
        return SocialCredential.Granted("$DEMO_TOKEN_PREFIX${provider.key}")
    }

    companion object {
        const val DEFAULT_SIGN_IN_DELAY_MILLIS = 1_200L

        /** Recognisable in a server log as "no SDK is wired up on this client yet". */
        const val DEMO_TOKEN_PREFIX = "demo-provider-token:"
    }
}
