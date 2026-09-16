package com.liam.cmp_src.core.database

import com.liam.cmp_src.core.database.dto.TokenStoreDto
import com.liam.cmp_src.core.database.entities.EncryptedAuthTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Stands in for Room in `commonTest`, which cannot assume a working SQLite driver: it runs on
 * the Android host JVM as well as the iOS simulator, and `sqlite-bundled`'s Android artifact
 * ships no native library the host JVM can load. Coverage of the real schema stays in the
 * platform test source sets.
 */
class FakeTokenStoreDto(initial: EncryptedAuthTokens? = null) : TokenStoreDto {

    private val row = MutableStateFlow(initial)

    /** What is actually on disk, for tests that assert the plaintext never got there. */
    val stored: EncryptedAuthTokens? get() = row.value

    override fun tokens(): Flow<EncryptedAuthTokens?> = row

    override suspend fun save(tokens: EncryptedAuthTokens) {
        row.value = tokens
    }

    override suspend fun clear() {
        row.value = null
    }
}
