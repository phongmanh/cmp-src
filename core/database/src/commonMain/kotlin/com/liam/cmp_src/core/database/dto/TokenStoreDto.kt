package com.liam.cmp_src.core.database.dto

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.liam.cmp_src.core.database.entities.EncryptedAuthTokens
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the single [EncryptedAuthTokens] row that backs a persistent `TokenStore`.
 *
 * Speaks only ciphertext. Nothing here can decrypt, which is what keeps the plaintext path down to
 * the one place that holds a cipher.
 */
@Dao
interface TokenStoreDto {

    /** Emits the stored row, or `null` while signed out. Cold: Room re-queries on change. */
    @Query("SELECT * FROM authToken WHERE id = ${EncryptedAuthTokens.ROW_ID}")
    fun tokens(): Flow<EncryptedAuthTokens?>

    /** Inserts the row, or replaces it when a sign-in supersedes an earlier session. */
    @Upsert
    suspend fun save(tokens: EncryptedAuthTokens)

    /** Drops the row on sign-out. A `@Delete` would need the entity the caller is trying to forget. */
    @Query("DELETE FROM authToken")
    suspend fun clear()
}
