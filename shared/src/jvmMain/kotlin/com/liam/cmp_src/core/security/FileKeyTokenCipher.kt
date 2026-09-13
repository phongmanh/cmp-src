package com.liam.cmp_src.core.security

import com.liam.cmp_src.core.database.APP_DIRECTORY_NAME
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_ALGORITHM = "AES"
private const val KEY_BITS = 256
private const val IV_BYTES = 12
private const val TAG_BITS = 128
private const val KEY_FILE_NAME = "token.key"
private const val POSIX_VIEW = "posix"

/**
 * Desktop AES-256-GCM, with the key in a file beside the database.
 *
 * **What this does and does not protect.** The key is a file the desktop user can read, so anything
 * already running as that user can read it too. What this buys is that the database alone is
 * useless: a copied file, a stray backup, a synced home directory or a support bundle no longer
 * carries a live session. It is not, and cannot be, protection against local code execution — the
 * JVM has no OS secret store reachable without a native dependency.
 *
 * [keyFile] is a parameter so tests never touch the developer's real key.
 */
class FileKeyTokenCipher(
    private val keyFile: File = File(
        File(System.getProperty("user.home"), APP_DIRECTORY_NAME),
        KEY_FILE_NAME,
    ),
) : TokenCipher {

    private val random = SecureRandom()

    private val key: SecretKeySpec by lazy { SecretKeySpec(loadOrCreateKey(), KEY_ALGORITHM) }

    override fun encrypt(plaintext: String): String = try {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        Base64.encode(iv + cipher.doFinal(plaintext.encodeToByteArray()))
    } catch (cause: Exception) {
        throw TokenCipherException("Could not encrypt a token with the desktop key file", cause)
    }

    override fun decrypt(ciphertext: String): String? = runCatching {
        // Base64.decode throws on anything outside the alphabet, which is exactly what a row
        // written by an older build that stored plaintext looks like. Inside the catch on purpose.
        val bytes = Base64.decode(ciphertext)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString()
    }.getOrNull()

    private fun loadOrCreateKey(): ByteArray {
        if (keyFile.exists()) return keyFile.readBytes()

        val generated = KeyGenerator.getInstance(KEY_ALGORITHM)
            .apply { init(KEY_BITS) }
            .generateKey()
            .encoded

        keyFile.parentFile?.mkdirs()
        keyFile.writeBytes(generated)
        restrictToOwner()
        return generated
    }

    /**
     * Windows is a target — `desktopApp` builds an MSI — and has no POSIX permission bits, so this
     * asks the filesystem first rather than throwing there. Windows leaves the file at its default
     * user-profile access control.
     */
    private fun restrictToOwner() {
        if (POSIX_VIEW !in FileSystems.getDefault().supportedFileAttributeViews()) return
        Files.setPosixFilePermissions(
            keyFile.toPath(),
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
        )
    }
}
