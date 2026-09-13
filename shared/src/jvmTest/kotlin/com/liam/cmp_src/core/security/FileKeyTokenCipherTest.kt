package com.liam.cmp_src.core.security

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The one cipher of the three that can be tested. Android's needs a device keystore and iOS's needs
 * a Keychain, so this is where the shape of the contract gets pinned down.
 */
class FileKeyTokenCipherTest {

    private val directory: File = File.createTempFile("cmpsrc-cipher", "")
        .also { it.delete(); it.mkdirs() }

    private val keyFile = File(directory, "token.key")
    private val cipher = FileKeyTokenCipher(keyFile)

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `a token survives a round trip`() {
        assertEquals(TOKEN, cipher.decrypt(cipher.encrypt(TOKEN)))
    }

    @Test
    fun `the same input encrypts differently every time`() {
        assertNotEquals(
            cipher.encrypt(TOKEN),
            cipher.encrypt(TOKEN),
            "a repeated ciphertext means the initialisation vector is not random",
        )
    }

    @Test
    fun `the ciphertext does not contain the token`() {
        assertTrue(TOKEN !in cipher.encrypt(TOKEN))
    }

    @Test
    fun `a tampered ciphertext is refused rather than returned`() {
        val encrypted = Base64.decode(cipher.encrypt(TOKEN))
        encrypted[encrypted.lastIndex] = (encrypted[encrypted.lastIndex] + 1).toByte()

        assertNull(cipher.decrypt(Base64.encode(encrypted)))
    }

    @Test
    fun `a value that is not even base64 is refused rather than thrown`() {
        // What a row written by an older build that stored plaintext looks like.
        assertNull(cipher.decrypt("access-token"))
    }

    @Test
    fun `a second cipher over the same key file reads the first one's output`() {
        val encrypted = cipher.encrypt(TOKEN)

        assertEquals(TOKEN, FileKeyTokenCipher(keyFile).decrypt(encrypted))
    }

    @Test
    fun `a cipher with a different key file cannot read it`() {
        val encrypted = cipher.encrypt(TOKEN)

        assertNull(FileKeyTokenCipher(File(directory, "other.key")).decrypt(encrypted))
    }

    @Test
    fun `the key file is readable only by its owner`() {
        cipher.encrypt(TOKEN)

        if ("posix" !in FileSystems.getDefault().supportedFileAttributeViews()) return
        assertEquals(
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            Files.getPosixFilePermissions(keyFile.toPath()),
        )
    }

    @Test
    fun `a key file that cannot be created is reported rather than ignored`() {
        val unusable = File(directory, "unwritable")
        unusable.writeText("not a directory")

        assertFailsWith<TokenCipherException> {
            FileKeyTokenCipher(File(unusable, "token.key")).encrypt(TOKEN)
        }
    }

    private companion object {
        const val TOKEN = "access-token\nrefresh-token"
    }
}
