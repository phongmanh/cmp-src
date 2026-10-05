package com.liam.cmp_src.core.database.dto

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.liam.cmp_src.core.database.entities.Customer
import com.liam.cmp_src.core.database.entities.CustomerSyncState
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the `customers` table.
 *
 * Deleting a customer here is a tombstone rather than a removal: [softDelete] fills the row's
 * `deletedAt`, which is what lets the deletion still be sent to the server the next time the
 * device is online. Every read the UI makes hides a tombstoned row, so the rest of the app sees
 * the customers the user has and never has to remember to filter them out. [purge] is the only
 * thing that takes a row off the device.
 *
 * Every list is scoped to one `ownerId`. Signing out leaves the rows on the device — a change made
 * offline must still reach the server once its owner signs back in — so another account signing
 * in on the same device must not see them.
 */
@Dao
interface CustomerDao {

    /**
     * Emits [ownerId]'s customers whose first name, last name, company or email starts with
     * [search] (case-insensitive, as the server's `?q=` is), newest first. An empty [search]
     * matches everyone. Cold: Room re-queries on change.
     *
     * Ordered explicitly because SQLite promises nothing about row order otherwise, and a list
     * that reshuffles between emissions would move rows under the reader's finger. `createdAt`
     * rather than `updatedAt`, so editing a customer does not jump it to the top of the list;
     * `id` settles the order of two customers created in the same instant.
     */
    fun customers(ownerId: String, search: String = ""): Flow<List<Customer>> =
        customersMatching(ownerId, search.escapeLike())

    /**
     * [customers], with [prefix] already escaped for `LIKE`. Kept apart so no caller can hand the
     * query an unescaped `%` or `_` and match far more than they typed.
     */
    @Query(
        "SELECT * FROM customers WHERE ownerId = :ownerId AND deletedAt IS NULL AND (" +
            ":prefix = '' " +
            "OR firstName LIKE :prefix || '%' ESCAPE '\\' " +
            "OR lastName LIKE :prefix || '%' ESCAPE '\\' " +
            "OR companyName LIKE :prefix || '%' ESCAPE '\\' " +
            "OR email LIKE :prefix || '%' ESCAPE '\\'" +
            ") ORDER BY createdAt DESC, id",
    )
    fun customersMatching(ownerId: String, prefix: String): Flow<List<Customer>>

    /** The customer with this [id] — `null` when there is no such row, or when it is tombstoned. */
    @Query("SELECT * FROM customers WHERE id = :id AND deletedAt IS NULL")
    suspend fun customerOf(id: String): Customer?

    /** The row with this [id] whatever its state, tombstones included — for the sync, not the UI. */
    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun rowOf(id: String): Customer?

    /** The row the server knows as [remoteId], tombstones included. */
    @Query("SELECT * FROM customers WHERE ownerId = :ownerId AND remoteId = :remoteId")
    suspend fun rowWithRemoteId(ownerId: String, remoteId: String): Customer?

    /**
     * Every change of [ownerId]'s the server has yet to hear about, oldest first — edits and
     * creates waiting as [CustomerSyncState.PENDING], and every tombstone.
     *
     * Oldest first so a customer created and then edited offline goes up as one create carrying
     * the edit, never as an edit to something the server has not seen.
     */
    @Query(
        "SELECT * FROM customers WHERE ownerId = :ownerId " +
            "AND (syncState = '${CustomerSyncState.PENDING}' OR deletedAt IS NOT NULL) " +
            "ORDER BY updatedAt, id",
    )
    suspend fun pending(ownerId: String): List<Customer>

    /** How many of [ownerId]'s changes are waiting for the server — what the list's banner counts. */
    @Query(
        "SELECT COUNT(*) FROM customers WHERE ownerId = :ownerId " +
            "AND (syncState = '${CustomerSyncState.PENDING}' OR deletedAt IS NOT NULL)",
    )
    fun pendingCount(ownerId: String): Flow<Int>

    /** The server ids of [ownerId]'s rows that have nothing waiting to go up. */
    @Query(
        "SELECT remoteId FROM customers WHERE ownerId = :ownerId AND remoteId IS NOT NULL " +
            "AND syncState = '${CustomerSyncState.SYNCED}' AND deletedAt IS NULL",
    )
    suspend fun syncedRemoteIds(ownerId: String): List<String>

    /** Inserts the customer, or replaces the row an earlier read of the same `id` left behind. */
    @Upsert
    suspend fun save(customer: Customer): Long

    /**
     * Replaces what the user can edit on the row [id], and queues the change for the server.
     *
     * One `UPDATE` rather than a read, a copy and an upsert: a sync can give the row its server id
     * at any moment, and a copy taken just before that would write the old, empty `remoteId` back
     * and send the customer to the server a second time. Bookkeeping columns are never touched.
     *
     * @return how many rows changed — `0` when [id] is gone or already tombstoned.
     */
    @Query(
        "UPDATE customers SET firstName = :firstName, lastName = :lastName, " +
            "companyName = :companyName, email = :email, phone = :phone, " +
            "addressLine1 = :addressLine1, addressLine2 = :addressLine2, city = :city, " +
            "region = :region, postalCode = :postalCode, countryCode = :countryCode, " +
            "notes = :notes, status = :status, updatedAt = :updatedAt, " +
            "syncState = '${CustomerSyncState.PENDING}' " +
            "WHERE id = :id AND deletedAt IS NULL",
    )
    suspend fun updateDetails(
        id: String,
        firstName: String,
        lastName: String?,
        companyName: String?,
        email: String?,
        phone: String?,
        addressLine1: String?,
        addressLine2: String?,
        city: String?,
        region: String?,
        postalCode: String?,
        countryCode: String?,
        notes: String?,
        status: String,
        updatedAt: String,
    ): Int

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

    /** Records the server's id for a row without touching anything the user may have changed. */
    @Query("UPDATE customers SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: String, remoteId: String)

    /**
     * Marks the row [CustomerSyncState.REJECTED], unless it changed after [expectedUpdatedAt] —
     * a newer edit deserves its own attempt rather than inheriting the old one's refusal.
     */
    @Query(
        "UPDATE customers SET syncState = '${CustomerSyncState.REJECTED}' " +
            "WHERE id = :id AND updatedAt = :expectedUpdatedAt AND deletedAt IS NULL",
    )
    suspend fun markRejected(id: String, expectedUpdatedAt: String)

    /**
     * Adopts the server's answer to a create or replace of the row [id].
     *
     * Only when the row still reads as it did when it was sent ([expectedUpdatedAt]) does the
     * server's copy replace it: an edit or a delete made while the request was in flight is newer
     * than anything the server said, so it keeps its place in the queue and only learns its
     * server id — which is all the next push needs to send it to the right place.
     *
     * @param server the server's copy, already mapped to a row; its `id` is ignored.
     */
    @Transaction
    suspend fun acceptServerCopy(id: String, expectedUpdatedAt: String, server: Customer) {
        val local = rowOf(id) ?: return
        val remoteId = requireNotNull(server.remoteId) { "A server copy always carries its id" }
        if (local.updatedAt == expectedUpdatedAt && local.deletedAt == null) {
            save(server.copy(id = id, ownerId = local.ownerId, syncState = CustomerSyncState.SYNCED))
        } else {
            setRemoteId(id, remoteId)
        }
    }

    /**
     * Merges one page of the server's list into [ownerId]'s rows.
     *
     * A row with a change still waiting to go up keeps the device's copy — the user's edit is
     * newer than the page, and overwriting it would silently undo it. Everything else takes the
     * server's copy, under the row's existing local id when it has one.
     */
    @Transaction
    suspend fun applyServerPage(ownerId: String, page: List<Customer>) {
        page.forEach { server ->
            val remoteId = requireNotNull(server.remoteId) { "A server copy always carries its id" }
            val local = rowWithRemoteId(ownerId, remoteId)
            when {
                local == null -> save(server.copy(id = remoteId, ownerId = ownerId))
                local.hasLocalChanges() -> Unit
                else -> save(server.copy(id = local.id, ownerId = ownerId))
            }
        }
    }

    /**
     * Drops [ownerId]'s settled rows the server no longer lists — customers deleted from another
     * device. Only for after a *complete* walk of the server's list: a partial one would read as
     * everything past the last page having been deleted.
     */
    @Transaction
    suspend fun purgeSyncedMissing(ownerId: String, serverIds: Set<String>) {
        syncedRemoteIds(ownerId)
            .filterNot { it in serverIds }
            .forEach { remoteId -> rowWithRemoteId(ownerId, remoteId)?.let { purge(it.id) } }
    }
}

/** Whether the row holds something the server has not accepted yet — an edit, a refusal, a delete. */
private fun Customer.hasLocalChanges(): Boolean =
    syncState != CustomerSyncState.SYNCED || deletedAt != null

/** The character `customersMatching` declares as its `LIKE` escape. */
private const val LIKE_ESCAPE = '\\'

/** Makes the user's text match literally inside a `LIKE` pattern: `%` and `_` are not wildcards. */
private fun String.escapeLike(): String = buildString {
    this@escapeLike.forEach { char ->
        if (char == '%' || char == '_' || char == LIKE_ESCAPE) append(LIKE_ESCAPE)
        append(char)
    }
}
