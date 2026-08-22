package com.liam.cmp_src.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The credential pair a `TokenResponse` hands back, kept apart from the user it came with. */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

/**
 * Holds the tokens the Ktor `Auth` plugin attaches to requests and refreshes when they expire.
 *
 * An interface because *where* the tokens live is a platform decision — EncryptedSharedPreferences
 * on Android, the Keychain on iOS — while everything above this only needs to read and write them.
 * [InMemoryTokenStore] is the default and deliberately forgets everything on restart: it keeps the
 * network layer honest without pretending to be secure storage.
 */
interface TokenStore {

    /** Emits the current tokens, or `null` while signed out. */
    val tokens: StateFlow<AuthTokens?>

    suspend fun save(tokens: AuthTokens)

    suspend fun clear()
}

/**
 * Session-lifetime token storage.
 *
 * Backed by a [MutableStateFlow] so concurrent reads and writes from the `Auth` plugin's refresh
 * path are safe on every target without a platform lock.
 *
 * Not persistent, and not secure storage. Replace it before shipping — a real implementation is a
 * new [TokenStore] bound in the DI module, and nothing else changes.
 */
class InMemoryTokenStore(initial: AuthTokens? = null) : TokenStore {

    private val state = MutableStateFlow(initial)

    override val tokens: StateFlow<AuthTokens?> = state.asStateFlow()

    override suspend fun save(tokens: AuthTokens) {
        state.value = tokens
    }

    override suspend fun clear() {
        state.value = null
    }
}
