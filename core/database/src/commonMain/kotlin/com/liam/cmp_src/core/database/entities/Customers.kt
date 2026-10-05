package com.liam.cmp_src.core.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One customer as the device holds it, including the bookkeeping that lets it be edited offline.
 *
 * [id] is this row's identity for its whole life on the device, and never changes: a customer
 * created offline gets a random one, a customer pulled from the server takes the server's. What
 * the server calls it is [remoteId], which stays `null` until the create has reached the server.
 * Keeping the two apart is what lets a list key or an open editor hold on to a row while a sync
 * gives it a server identity underneath.
 *
 * [syncState] is one of [CustomerSyncState]'s values; a pending delete is a non-null [deletedAt]
 * (see `CustomerDao.softDelete`), not a state of its own.
 */
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val countryCode: String? = null,
    val notes: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val remoteId: String? = null,
    @ColumnInfo(defaultValue = CustomerSyncState.SYNCED)
    val syncState: String = CustomerSyncState.SYNCED,
)

/**
 * The values [Customer.syncState] holds. Strings rather than an enum so the column's contents are
 * readable in a database inspector and a value from a newer build does not crash an older one.
 */
object CustomerSyncState {
    /** The row matches what the server last said. */
    const val SYNCED = "SYNCED"

    /** Created or edited on the device, and the server has not heard about it yet. */
    const val PENDING = "PENDING"

    /**
     * The server refused the change as invalid. Retrying cannot help, so the row waits for the
     * user to fix it — saving it again puts it back to [PENDING].
     */
    const val REJECTED = "REJECTED"
}
