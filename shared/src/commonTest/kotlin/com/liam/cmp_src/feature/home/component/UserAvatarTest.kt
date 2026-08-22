package com.liam.cmp_src.feature.home.component

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The avatar's initials are the only piece of the home shell with a rule worth pinning down:
 * every user has to end up with something drawable, whatever their profile looks like.
 */
class UserAvatarTest {

    @Test
    fun `initials take the first letter of the first and last name`() {
        assertEquals("DU", userNamed("Demo User").initials())
    }

    @Test
    fun `initials skip the middle names`() {
        assertEquals("AJ", userNamed("Ada Byron King Jones").initials())
    }

    @Test
    fun `a single name yields a single initial`() {
        assertEquals("A", userNamed("Ada").initials())
    }

    @Test
    fun `initials are upper-cased`() {
        assertEquals("AL", userNamed("ada lovelace").initials())
    }

    @Test
    fun `surrounding and repeated whitespace is ignored`() {
        assertEquals("AL", userNamed("   Ada    Lovelace  ").initials())
    }

    @Test
    fun `a blank display name falls back to the email`() {
        assertEquals("D", userNamed("   ").initials())
    }

    @Test
    fun `a missing display name falls back to the email`() {
        assertEquals("D", sampleUser(displayName = null).initials())
    }

    @Test
    fun `a user with nothing to draw from yields no initials`() {
        assertEquals("", sampleUser(displayName = null, email = null).initials())
    }

    private fun userNamed(displayName: String) = sampleUser(displayName = displayName)
}
