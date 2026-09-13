package com.liam.cmp_src.core.database

import com.liam.cmp_src.core.database.dto.TokenStoreDto
import com.liam.cmp_src.core.database.entities.EncryptedAuthTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Stands in for Room in `commonTest`, which has no SQLite driver to run against — `sqlite-bundled`
 * publishes no js/wasmJs variants. Coverage of the real schema stays in `jvmTest`.
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
