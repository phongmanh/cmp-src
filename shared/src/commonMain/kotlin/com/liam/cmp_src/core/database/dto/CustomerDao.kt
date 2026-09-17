package com.liam.cmp_src.core.database.dto

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.liam.cmp_src.core.database.entities.Customer
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the `customers` table.
 *
 * Deleting a customer here is a tombstone rather than a removal: [softDelete] fills the row's
 * `deletedAt`, which is what lets the deletion still be sent to the server the next time the
 * device is online. Every read below hides a tombstoned row, so the rest of the app sees the
 * customers the user has and never has to remember to filter them out. [purge] is the only thing
 * that takes a row off the device.
 */
@Dao
interface CustomerDao {

    /**
     * Emits the customers the user still has, newest first. Cold: Room re-queries on change.
     *
     * Ordered explicitly because SQLite promises nothing about row order otherwise, and a list
     * that reshuffles between emissions would move rows under the reader's finger. `createdAt`
     * rather than `updatedAt`, so editing a customer does not jump it to the top of the list;
     * `id` settles the order of two customers created in the same instant.
     */
    @Query("SELECT * FROM customers WHERE deletedAt IS NULL ORDER BY createdAt DESC, id")
    fun customers(): Flow<List<Customer>>

    /** The customer with this [id] — `null` when there is no such row, or when it is tombstoned. */
    @Query("SELECT * FROM customers WHERE id = :id AND deletedAt IS NULL")
    suspend fun customerOf(id: String): Customer?

    /** Inserts the customer, or replaces the row an earlier read of the same `id` left behind. */
    @Upsert
    suspend fun save(customer: Customer): Long

    /**
     * Marks the row deleted at [deletedAt], which is also when it last changed — the tombstone
     * *is* the change the server has yet to hear about, so both columns take the same instant.
     *
     * A row that is already tombstoned is left alone: a second call must not overwrite the moment
     * the first one recorded, or a deletion that has not reached the server yet would keep moving.
     */
    @Query(
        "UPDATE customers SET deletedAt = :deletedAt, updatedAt = :deletedAt " +
            "WHERE id = :id AND deletedAt IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: String)

    /**
     * Takes the row off the device, for once the server has been told about the tombstone.
     *
     * Unconditional on purpose: a caller reconciling with the server needs to be able to drop a
     * row whatever its local state, and a guard here would quietly do nothing instead.
     */
    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun purge(id: String)
}
