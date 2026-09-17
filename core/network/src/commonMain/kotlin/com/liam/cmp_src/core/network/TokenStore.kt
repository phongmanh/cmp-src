package com.liam.cmp_src.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The credential pair a `TokenResponse` hands back, kept apart from the user it came with. */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

/**
 * Holds the tokens the Ktor `Auth` plugin attaches to requests and refreshes when they expire.
 *
 * An interface because *where* the tokens live is a platform decision — a SQLite file on both
 * Android and iOS, with the row encrypted by that platform's key store — while everything above
 * this only needs to read and write them. `RoomTokenStore` is the persistent implementation;
 * [InMemoryTokenStore] is the fallback for tests.
 */
interface TokenStore {

    /** Emits the stored credentials and re-emits on every change. `null` means signed out. */
    val tokens: Flow<AuthTokens?>

    /**
     * Reads the credentials once, now.
     *
     * Suspending rather than a `StateFlow.value` read: persistent storage cannot answer inline,
     * and a `StateFlow` seeded with `null` would report a signed-in user as signed out until its
     * first emission landed. The `Auth` plugin's `loadTokens` is itself suspending, so the one
     * caller that has to be right on the very first request can simply wait here.
     */
    suspend fun current(): AuthTokens?

    suspend fun save(tokens: AuthTokens)

    suspend fun clear()
}

/**
 * Session-lifetime token storage.
 *
 * Backed by a [MutableStateFlow] so concurrent reads and writes from the `Auth` plugin's refresh
 * path are safe on every target without a platform lock.
 *
 * Not persistent, and not secure storage: it is a test double, and nothing in the shipped
 * graph binds it — both targets bind `RoomTokenStore`.
 */
class InMemoryTokenStore(initial: AuthTokens? = null) : TokenStore {

    private val state = MutableStateFlow(initial)

    override val tokens: Flow<AuthTokens?> = state.asStateFlow()

    override suspend fun current(): AuthTokens? = state.value

    override suspend fun save(tokens: AuthTokens) {
        state.value = tokens
    }

    override suspend fun clear() {
        state.value = null
    }
}
