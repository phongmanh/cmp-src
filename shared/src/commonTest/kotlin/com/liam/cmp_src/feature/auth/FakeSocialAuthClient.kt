package com.liam.cmp_src.feature.auth

import com.liam.cmp_src.feature.auth.data.social.SocialAuthClient
import com.liam.cmp_src.feature.auth.data.social.SocialCredential
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider

/**
 * Stands in for a provider SDK. Returns whatever [result] is set to and records which provider
 * it was asked for, so a test can assert that the repository asked before it called the server.
 */
class FakeSocialAuthClient(
    var result: SocialCredential = SocialCredential.Granted(TOKEN),
) : SocialAuthClient {

    var lastProvider: SocialProvider? = null
        private set
    var callCount = 0
        private set

    override suspend fun requestCredential(provider: SocialProvider): SocialCredential {
        callCount++
        lastProvider = provider
        return result
    }

    companion object {
        const val TOKEN = "provider-id-token"
    }
}
