package com.liam.cmp_src.core.security

/**
 * Turns a credential into something safe to keep in a `TEXT` column, and back.
 *
 * Implementations hold their key in the strongest store their platform offers and never write it
 * next to the ciphertext. What lands in the database must be self-contained — any nonce or tag the
 * algorithm needs travels inside the returned string, so the schema never has to know how the
 * encryption works.
 *
 * The two directions fail differently, on purpose:
 *
 * - [encrypt] throws, because a failure has no safe reading. The platform could not protect a
 *   credential the user just earned, and carrying on would leave the app apparently signed in with
 *   nothing written down. Falling back to plaintext would defeat the point entirely.
 * - [decrypt] returns `null`, because every failure means the same thing to the caller. A key that
 *   is gone, bytes that were tampered with, a row written by an older build before this existed —
 *   all of them say "there is no session here", and none of them should crash an app at launch.
 */
interface TokenCipher {

    /** @throws TokenCipherException when the platform cannot protect [plaintext]. */
    fun encrypt(plaintext: String): String

    /** Returns `null` when [ciphertext] cannot be read back, for any reason at all. */
    fun decrypt(ciphertext: String): String?
}

/** Raised when a credential could not be encrypted, and so must not be stored. */
class TokenCipherException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
