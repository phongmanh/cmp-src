package com.liam.cmp_src.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64

private const val PROVIDER = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_BITS = 256
private const val IV_BYTES = 12
private const val TAG_BITS = 128
private const val DEFAULT_ALIAS = "com.liam.cmp_src.token-cipher"

/**
 * Android AES-256-GCM with the key held by the platform keystore, where it is non-extractable and
 * hardware-backed on most devices. minSdk is 24, so no version guard is needed.
 *
 * This class has no automated test — the keystore provider does not exist on a host JVM, and this
 * project has no instrumented source set. It is therefore written to contain **no decisions**:
 * load or create by alias, encrypt, decrypt. Everything that could be wrong lives in
 * `RoomTokenStore` and `TokenMapping`, which `commonTest` covers on every target.
 *
 * Two keystore options are deliberately left alone. `setUserAuthenticationRequired` would block
 * decryption whenever the device is locked, which breaks background token refresh, and would
 * invalidate the key on any biometric enrollment change. StrongBox is unavailable on many devices
 * and throws when asked for. The default `setRandomizedEncryptionRequired` stays on, which is why
 * the IV is read back off the cipher rather than supplied to it.
 *
 * [alias] is a parameter so a future instrumented test can isolate itself.
 */
class KeystoreTokenCipher(private val alias: String = DEFAULT_ALIAS) : TokenCipher {

    private val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    override fun encrypt(plaintext: String): String = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        Base64.encode(cipher.iv + cipher.doFinal(plaintext.encodeToByteArray()))
    } catch (cause: Exception) {
        throw TokenCipherException("Could not encrypt a token with the Android keystore", cause)
    }

    override fun decrypt(ciphertext: String): String? = runCatching {
        // Base64.decode throws on anything outside the alphabet, which is exactly what a row
        // written by an older build that stored plaintext looks like. Inside the catch on purpose.
        val bytes = Base64.decode(ciphertext)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString()
    }.getOrNull()

    /** A fresh [Cipher] per call, because `javax.crypto.Cipher` is not thread-safe. */
    private fun key(): SecretKey =
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: generateKey()

    /**
     * Generation fails on a nonzero share of OEM keystores. Deleting the alias and trying once more
     * clears the case where a half-written entry is the problem; a second failure is real, and
     * throwing beats the alternative of storing the credential in the clear.
     */
    private fun generateKey(): SecretKey = try {
        newKey()
    } catch (first: Exception) {
        runCatching { keyStore.deleteEntry(alias) }
        try {
            newKey()
        } catch (second: Exception) {
            second.addSuppressed(first)
            throw TokenCipherException("The Android keystore would not produce a key", second)
        }
    }

    private fun newKey(): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        .apply {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .build(),
            )
        }
        .generateKey()
}
