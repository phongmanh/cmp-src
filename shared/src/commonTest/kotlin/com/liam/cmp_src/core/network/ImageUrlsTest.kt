package com.liam.cmp_src.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The two questions asked of an avatar address: is it ours, and can this device reach it.
 *
 * Getting the first one wrong loses a user's picture on their next rename; getting the second one
 * wrong shows a broken image on exactly one target, which is the kind of thing nobody notices
 * until they are holding an emulator.
 */
class ImageUrlsTest {

    @Test
    fun `an address under the images route is one of ours`() {
        assertTrue(isStoredImageUrl("https://api.test/api/v1/images/abc-123"))
    }

    @Test
    fun `a provider's own address is not`() {
        assertFalse(isStoredImageUrl("https://lh3.googleusercontent.com/a/photo.jpg"))
    }

    @Test
    fun `a malformed address is not ours rather than an exception`() {
        assertFalse(isStoredImageUrl("not a url at all"))
        assertFalse(isStoredImageUrl(""))
        assertFalse(isStoredImageUrl("https://api.test"))
    }

    @Test
    fun `a stored address is re-pointed at the backend this build talks to`() {
        val rewritten = sameOriginImageUrl(
            url = "http://localhost:8080/api/v1/images/abc-123",
            baseUrl = "http://10.0.2.2:8080",
        )

        assertEquals("http://10.0.2.2:8080/api/v1/images/abc-123", rewritten)
    }

    @Test
    fun `a stored address already on the right origin is left alone`() {
        assertNull(
            sameOriginImageUrl(
                url = "https://api.test/api/v1/images/abc-123",
                baseUrl = "https://api.test",
            ),
        )
    }

    /** A trailing slash on the configured base must not become a double slash in the path. */
    @Test
    fun `a trailing slash on the base url does not double up`() {
        val rewritten = sameOriginImageUrl(
            url = "http://localhost:8080/api/v1/images/abc",
            baseUrl = "http://10.0.2.2:8080/",
        )

        assertEquals("http://10.0.2.2:8080/api/v1/images/abc", rewritten)
    }

    @Test
    fun `a provider's address is never re-pointed at our backend`() {
        assertNull(
            sameOriginImageUrl(
                url = "https://lh3.googleusercontent.com/a/photo.jpg",
                baseUrl = "https://api.test",
            ),
        )
    }
}
