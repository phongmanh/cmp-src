package com.liam.cmp_src.feature.customers

import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.repository.CustomerRepository
import com.liam.cmp_src.feature.customers.domain.repository.CustomerSyncScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A [CustomerRepository] for ViewModel tests: an in-memory list, and a sync whose answer the test
 * chooses — and, through [syncGate], when it arrives.
 */
class FakeCustomerRepository(
    vararg customers: Customer,
    var syncOutcome: SyncOutcome = SyncOutcome.Synced,
) : CustomerRepository {

    val customers = MutableStateFlow(customers.toList())
    val pending = MutableStateFlow(0)
    val searches = mutableListOf<String>()
    val created = mutableListOf<CustomerDraft>()
    val updated = mutableListOf<Pair<String, CustomerDraft>>()
    val deleted = mutableListOf<String>()
    var syncCount = 0
        private set

    /** Replace to hold a sync open until the test completes it. */
    var syncGate: CompletableDeferred<Unit>? = null

    override fun customers(ownerId: String, search: String): Flow<List<Customer>> {
        searches += search
        return customers.map { all ->
            all.filter { search.isEmpty() || it.details.firstName.startsWith(search, ignoreCase = true) }
        }
    }

    override fun pendingChanges(ownerId: String): Flow<Int> = pending

    override suspend fun customer(id: String): Customer? = customers.value.firstOrNull { it.id == id }

    override suspend fun create(ownerId: String, draft: CustomerDraft): String {
        created += draft
        return "new-id"
    }

    override suspend fun update(id: String, draft: CustomerDraft): Boolean {
        if (customer(id) == null) return false
        updated += id to draft
        return true
    }

    override suspend fun delete(id: String) {
        deleted += id
        customers.value = customers.value.filterNot { it.id == id }
    }

    override suspend fun sync(ownerId: String): SyncOutcome {
        syncCount++
        syncGate?.await()
        return syncOutcome
    }
}

/** Counts the sync requests a use case makes. */
class FakeSyncScheduler : CustomerSyncScheduler {
    val requests = mutableListOf<String>()
    override fun requestSync(ownerId: String) {
        requests += ownerId
    }
}
