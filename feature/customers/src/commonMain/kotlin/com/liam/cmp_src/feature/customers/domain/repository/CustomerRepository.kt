package com.liam.cmp_src.feature.customers.domain.repository

import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import kotlinx.coroutines.flow.Flow

/**
 * The customer boundary the domain layer depends on. The implementation lives in the data layer
 * (`feature.customers.data.CustomerRepositoryImpl`) and is bound in `customersModule`.
 *
 * **Offline-first.** Reads come from the device and writes land on the device, so none of them
 * wait for — or fail because of — the network. The server is reconciled separately by [sync].
 */
interface CustomerRepository {

    /** [ownerId]'s customers matching [search] (see `CustomerDao.customers`), newest first. */
    fun customers(ownerId: String, search: String): Flow<List<Customer>>

    /** How many of [ownerId]'s changes are still waiting to reach the server. */
    fun pendingChanges(ownerId: String): Flow<Int>

    /** The customer with this [id], or `null` if there is none (or it was deleted). */
    suspend fun customer(id: String): Customer?

    /** Stores a new customer for [ownerId] and returns its id. */
    suspend fun create(ownerId: String, draft: CustomerDraft): String

    /** Replaces the customer [id] with [draft]. Returns `false` if it no longer exists. */
    suspend fun update(id: String, draft: CustomerDraft): Boolean

    /** Deletes the customer [id]. Deleting one that is already gone is not an error. */
    suspend fun delete(id: String)

    /** Pushes [ownerId]'s waiting changes, then pulls the server's list over the rest. */
    suspend fun sync(ownerId: String): SyncOutcome
}
