package com.liam.cmp_src.core.security

import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Runs on the simulator via `iosSimulatorArm64Test`; the device target compiles it but cannot run
 * it without hardware.
 *
 * Every instance gets its own randomly named Keychain service. Without that, a run would leave an
 * item behind that the next run would find, so a regression in key creation would pass by reusing
 * the previous run's key — and a test could clobber the key of an app being debugged on the same
 * simulator.
 *
 * **These skip under Gradle.** The test binary is unbundled and has no keychain entitlement, so
 * there is no Keychain to talk to — see [keychainIsUsable]. They run as written under an XCTest
 * host application, and are kept so that becomes a one-line change rather than a rewrite.
 */
class KeychainTokenCipherTest {

    private val service = "com.liam.cmp_src.test.${Random.nextLong()}"
    private val cipher = KeychainTokenCipher(service)

    @AfterTest
    fun tearDown() {
        deleteKeychainItem(service)
    }

    /** Runs first in spirit: a Keychain the simulator refuses fails here, not somewhere subtler. */
    @Test
    fun `the keychain gives up a usable key`() {
        if (!keychainIsUsable()) return

        assertTrue(cipher.encrypt(TOKEN).isNotEmpty())
    }

    @Test
    fun `a token survives a round trip`() {
        if (!keychainIsUsable()) return

        assertEquals(TOKEN, cipher.decrypt(cipher.encrypt(TOKEN)))
    }

    @Test
    fun `the ciphertext does not contain the token`() {
        if (!keychainIsUsable()) return

        assertTrue(TOKEN !in cipher.encrypt(TOKEN))
    }

    @Test
    fun `the same input encrypts differently every time`() {
        if (!keychainIsUsable()) return

        assertNotEquals(cipher.encrypt(TOKEN), cipher.encrypt(TOKEN))
    }

    @Test
    fun `a second cipher on the same service reads the first one's output`() {
        if (!keychainIsUsable()) return

        val encrypted = cipher.encrypt(TOKEN)

        // Proves the key was genuinely persisted rather than regenerated per instance.
        assertEquals(TOKEN, KeychainTokenCipher(service).decrypt(encrypted))
    }

    @Test
    fun `a cipher on a different service cannot read it`() {
        if (!keychainIsUsable()) return

        val encrypted = cipher.encrypt(TOKEN)
        val other = "com.liam.cmp_src.test.${Random.nextLong()}"

        try {
            assertNull(KeychainTokenCipher(other).decrypt(encrypted))
        } finally {
            deleteKeychainItem(other)
        }
    }

    @Test
    fun `a tampered ciphertext is refused rather than returned`() {
        if (!keychainIsUsable()) return

        val encrypted = Base64.decode(cipher.encrypt(TOKEN))
        encrypted[encrypted.lastIndex] = (encrypted[encrypted.lastIndex] + 1).toByte()

        assertNull(cipher.decrypt(Base64.encode(encrypted)))
    }

    @Test
    fun `a value that is not even base64 is refused rather than thrown`() {
        if (!keychainIsUsable()) return

        // What a row written by an older build that stored plaintext looks like.
        assertNull(cipher.decrypt("access-token"))
    }

    private companion object {
        const val TOKEN = "access-token\nrefresh-token"
    }
}
