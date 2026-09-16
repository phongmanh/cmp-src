package com.liam.cmp_src.core.database

import com.liam.cmp_src.core.database.dto.TokenStoreDto
import com.liam.cmp_src.core.network.AuthTokens
import com.liam.cmp_src.core.network.TokenStore
import com.liam.cmp_src.core.security.TokenCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * A [TokenStore] that survives process death, backed by the single-row `authToken` table.
 *
 * The only place a plaintext token meets the database. Everything below holds ciphertext and
 * everything above holds plaintext, so no other code can write a credential down by accident.
 *
 * Deliberately holds no cached copy and no [kotlinx.coroutines.CoroutineScope]. A `stateIn` cache
 * would need a scope that never cancels, would keep a Room query collecting for the life of the
 * process, and would still have to answer `null` until its first emission arrived. Reading the
 * table on demand is a primary-key lookup against a local file, and the `Auth` plugin caches what
 * [current] returns anyway — it re-reads only after [clear] or a refresh.
 */
class RoomTokenStore(
    private val dao: TokenStoreDto,
    private val cipher: TokenCipher,
) : TokenStore {

    /**
     * `flowOn` because decryption would otherwise run wherever this is collected, which on Android
     * is the main thread. `Default` to match the database's own query context — see
     * `getRoomDatabase`.
     */
    override val tokens: Flow<AuthTokens?> = dao.tokens()
        .map { it?.decryptWith(cipher) }
        .flowOn(Dispatchers.Default)

    /**
     * Reads the session, dropping the row if it can no longer be decrypted.
     *
     * A row that will not decrypt is a session that no longer exists: the key went away with a
     * restored backup, or it was written by a build that stored plaintext. Clearing it here keeps
     * the table honest and stops every later request paying for the same failed decrypt. The
     * healing lives on this path rather than in [tokens], which stays free of writes.
     */
    override suspend fun current(): AuthTokens? {
        val stored = dao.tokens().first() ?: return null
        return stored.decryptWith(cipher) ?: run {
            dao.clear()
            null
        }
    }

    override suspend fun save(tokens: AuthTokens) = dao.save(tokens.encryptWith(cipher))

    override suspend fun clear() = dao.clear()
}
