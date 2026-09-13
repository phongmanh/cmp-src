package com.liam.cmp_src.core.security

/**
 * A visibly reversible transform, not a cipher.
 *
 * Real encryption is untestable in `commonTest` — two of the three implementations need a device —
 * so this exists to assert what `RoomTokenStore` does *with* a cipher: that it calls one on every
 * write, that it survives one that cannot decrypt, and that it refuses to store anything when one
 * cannot encrypt.
 */
class FakeTokenCipher(
    var failEncrypt: Boolean = false,
    var failDecrypt: Boolean = false,
) : TokenCipher {

    var encryptCount: Int = 0
        private set

    var decryptCount: Int = 0
        private set

    override fun encrypt(plaintext: String): String {
        encryptCount++
        if (failEncrypt) throw TokenCipherException("fake cipher told to fail")
        // Reversed, not just tagged: a test asserting the plaintext never reaches storage is
        // worthless if the fake leaves it sitting there in readable order.
        return "$PREFIX${plaintext.reversed()}"
    }

    override fun decrypt(ciphertext: String): String? {
        decryptCount++
        if (failDecrypt || !ciphertext.startsWith(PREFIX)) return null
        return ciphertext.removePrefix(PREFIX).reversed()
    }

    private companion object {
        const val PREFIX = "enc:"
    }
}
