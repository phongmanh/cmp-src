package com.liam.cmp_src.feature.home.component

import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The bridge between the contract's `linkedProviders` wire keys and the providers this build
 * offers. Getting it wrong shows the profile panel nothing rather than failing loudly, so the
 * mapping is pinned down here.
 */
class UserIdentityTest {

    @Test
    fun `a linked provider maps back to the provider that signed in`() {
        assertEquals(
            SocialProvider.GOOGLE,
            sampleUser(linkedProviders = listOf(SocialProvider.GOOGLE.key)).signedInProvider(),
        )
    }

    @Test
    fun `an email account has no linked provider`() {
        assertNull(sampleUser().signedInProvider())
    }

    @Test
    fun `a provider this build does not offer is skipped`() {
        assertNull(sampleUser(linkedProviders = listOf("apple")).signedInProvider())
    }

    @Test
    fun `a known provider is still found alongside an unknown one`() {
        assertEquals(
            SocialProvider.FACEBOOK,
            sampleUser(
                linkedProviders = listOf("apple", SocialProvider.FACEBOOK.key),
            ).signedInProvider(),
        )
    }

    @Test
    fun `provider keys are the contract's wire form`() {
        assertEquals("google", SocialProvider.GOOGLE.key)
        assertEquals("facebook", SocialProvider.FACEBOOK.key)
        assertEquals(SocialProvider.GOOGLE, SocialProvider.fromKey("google"))
        assertNull(SocialProvider.fromKey("GOOGLE"))
    }
}
