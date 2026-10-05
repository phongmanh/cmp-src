package com.liam.cmp_src.feature.customers

import com.liam.cmp_src.core.database.dto.CustomerDao
import com.liam.cmp_src.core.database.entities.Customer
import com.liam.cmp_src.core.database.entities.CustomerSyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * [CustomerDao] over a map, for running the repository in `commonTest`, which has no SQLite.
 *
 * Only the queries are re-implemented; the transactional merges (`acceptServerCopy`,
 * `applyServerPage`, `purgeSyncedMissing`) are the interface's own default methods, so the real
 * merge logic runs here too. The queries themselves are pinned against SQLite by `core:database`'s
 * `CustomerDaoTest`.
 */
class FakeCustomerDao(vararg rows: Customer) : CustomerDao {

    val rows = MutableStateFlow(rows.associateBy { it.id })

    override fun customersMatching(ownerId: String, prefix: String): Flow<List<Customer>> {
        val search = prefix.replace("\\", "")
        return rows.map { all ->
            all.values
                .filter { it.ownerId == ownerId && it.deletedAt == null }
                .filter { row ->
                    search.isEmpty() || listOf(row.firstName, row.lastName, row.companyName, row.email)
                        .any { it?.startsWith(search, ignoreCase = true) == true }
                }
                .sortedWith(compareByDescending<Customer> { it.createdAt }.thenBy { it.id })
        }
    }

    override suspend fun customerOf(id: String): Customer? = rows.value[id]?.takeIf { it.deletedAt == null }

    override suspend fun rowOf(id: String): Customer? = rows.value[id]

    override suspend fun rowWithRemoteId(ownerId: String, remoteId: String): Customer? =
        rows.value.values.firstOrNull { it.ownerId == ownerId && it.remoteId == remoteId }

    override suspend fun pending(ownerId: String): List<Customer> =
        pendingRows(ownerId).sortedWith(compareBy<Customer> { it.updatedAt }.thenBy { it.id })

    override fun pendingCount(ownerId: String): Flow<Int> = rows.map { pendingRows(ownerId).size }

    override suspend fun syncedRemoteIds(ownerId: String): List<String> =
        rows.value.values
            .filter { it.ownerId == ownerId && it.syncState == CustomerSyncState.SYNCED && it.deletedAt == null }
            .mapNotNull { it.remoteId }

    override suspend fun save(customer: Customer): Long {
        rows.value += customer.id to customer
        return 1
    }

    override suspend fun updateDetails(
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
    ): Int {
        val row = customerOf(id) ?: return 0
        save(
            row.copy(
                firstName = firstName, lastName = lastName, companyName = companyName,
                email = email, phone = phone, addressLine1 = addressLine1,
                addressLine2 = addressLine2, city = city, region = region,
                postalCode = postalCode, countryCode = countryCode, notes = notes,
                status = status, updatedAt = updatedAt, syncState = CustomerSyncState.PENDING,
            ),
        )
        return 1
    }

    override suspend fun softDelete(id: String, deletedAt: String) {
        val row = customerOf(id) ?: return
        save(row.copy(deletedAt = deletedAt, updatedAt = deletedAt))
    }

    override suspend fun purge(id: String) {
        rows.value -= id
    }

    override suspend fun setRemoteId(id: String, remoteId: String) {
        rowOf(id)?.let { save(it.copy(remoteId = remoteId)) }
    }

    override suspend fun markRejected(id: String, expectedUpdatedAt: String) {
        val row = customerOf(id)?.takeIf { it.updatedAt == expectedUpdatedAt } ?: return
        save(row.copy(syncState = CustomerSyncState.REJECTED))
    }

    private fun pendingRows(ownerId: String) = rows.value.values.filter {
        it.ownerId == ownerId && (it.syncState == CustomerSyncState.PENDING || it.deletedAt != null)
    }
}
