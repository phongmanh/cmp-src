package com.liam.cmp_src.core.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * The stored form of a session: one encrypted blob, not two encrypted columns.
 *
 * Encrypting the pair together is what stops anyone with write access to the file from splicing an
 * access token out of one session into another, or swapping the two columns. It also halves the
 * crypto per save and stops each token's length leaking separately.
 *
 * Single-row table: [id] is fixed to [ROW_ID] so an `@Upsert` replaces the previous session rather
 * than accumulating one row per sign-in.
 */
@Entity(tableName = "authToken")
data class EncryptedAuthTokens(
    val payload: String,
    @PrimaryKey
    val id: Int = ROW_ID,
) {
    companion object {
        /** The only primary key this table ever holds. */
        const val ROW_ID = 1
    }
}
