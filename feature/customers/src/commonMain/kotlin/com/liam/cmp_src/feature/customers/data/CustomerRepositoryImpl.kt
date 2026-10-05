package com.liam.cmp_src.feature.customers.data

import com.example.api.common.FieldLimits
import com.liam.cmp_src.core.database.dto.CustomerDao
import com.liam.cmp_src.core.network.ApiResult
import com.liam.cmp_src.feature.customers.data.remote.CustomerApi
import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.repository.CustomerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import com.liam.cmp_src.core.database.entities.Customer as CustomerEntity

/**
 * Customers, offline-first: the device's table is the source of truth for everything the screens
 * show, and [sync] is the one place the server is spoken to.
 *
 * **Writes** land on the device and return. A new customer gets a random local id and no
 * `remoteId`; an edit or a delete marks the row as waiting. Nothing here waits on the network, so
 * a save behaves the same on a plane.
 *
 * **Sync** is push, then pull:
 * 1. Confirm the stored session is the owner's — rows are never sent with another account's token.
 * 2. Push the waiting changes oldest first: tombstones become `DELETE`s, rows with no `remoteId`
 *    become `POST`s, the rest `PUT`s. A refusal parks that one row as rejected; losing the
 *    connection stops the push, to be retried whole later.
 * 3. Pull every page of the server's list, keeping the device's copy of anything still waiting,
 *    then drop settled rows the server no longer has.
 *
 * Known limit: the contract has no idempotency key, so a `POST` whose response is lost on the way
 * back will create the customer a second time when retried. The server's unique email catches the
 * common case (a 409, which parks the row as rejected rather than duplicating it).
 *
 * One sync at a time, whoever asks — the list, pull-to-refresh, the background job — via
 * [syncLock]; this repository is a Koin `single`, so the lock is process-wide.
 */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
class CustomerRepositoryImpl(
    private val api: CustomerApi,
    private val dao: CustomerDao,
    private val dispatcher: CoroutineDispatcher,
    private val clock: Clock = Clock.System,
    private val newId: () -> String = { Uuid.random().toString() },
) : CustomerRepository {

    private val syncLock = Mutex()

    override fun customers(ownerId: String, search: String): Flow<List<Customer>> =
        dao.customers(ownerId, search)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatcher)

    override fun pendingChanges(ownerId: String): Flow<Int> = dao.pendingCount(ownerId)

    override suspend fun customer(id: String): Customer? =
        withContext(dispatcher) { dao.customerOf(id)?.toDomain() }

    override suspend fun create(ownerId: String, draft: CustomerDraft): String =
        withContext(dispatcher) {
            val row = draft.toNewEntity(id = newId(), ownerId = ownerId, now = now())
            dao.save(row)
            row.id
        }

    override suspend fun update(id: String, draft: CustomerDraft): Boolean =
        withContext(dispatcher) {
            val status = requireNotNull(draft.status) { "A draft is only saved once it has a status" }
            val changed = dao.updateDetails(
                id = id,
                firstName = draft.firstName,
                lastName = draft.lastName,
                companyName = draft.companyName,
                email = draft.email,
                phone = draft.phone,
                addressLine1 = draft.address?.line1,
                addressLine2 = draft.address?.line2,
                city = draft.address?.city,
                region = draft.address?.region,
                postalCode = draft.address?.postalCode,
                countryCode = draft.address?.countryCode,
                notes = draft.notes,
                status = status.key,
                updatedAt = now(),
            )
            changed > 0
        }

    /**
     * Always a tombstone, even for a customer the server has never seen: its `POST` may be in
     * flight right now, and only a row that survives can learn the server id the `DELETE` needs.
     * The push drops a tombstone that never got one without calling the server at all.
     */
    override suspend fun delete(id: String) = withContext(dispatcher) {
        dao.softDelete(id, now())
    }

    override suspend fun sync(ownerId: String): SyncOutcome = syncLock.withLock {
        withContext(dispatcher) {
            checkSession(ownerId) ?: push(ownerId) ?: pull(ownerId)
        }
    }

    /** `null` when the session is [ownerId]'s, or the outcome that ends the sync when it is not. */
    private suspend fun checkSession(ownerId: String): SyncOutcome? =
        when (val result = api.currentUser()) {
            is ApiResult.Success ->
                if (result.data.id == ownerId) null else SyncOutcome.SessionMismatch
            is ApiResult.Failure -> result.error.toSyncOutcome()
        }

    /** Sends every waiting change. `null` when the queue drained, or why it stopped. */
    private suspend fun push(ownerId: String): SyncOutcome? {
        dao.pending(ownerId).forEach { row ->
            pushOne(ownerId, row)?.let { return it }
        }
        return null
    }

    /** Sends one change. `null` when it is settled — sent, dropped or parked — or why to stop. */
    private suspend fun pushOne(ownerId: String, row: CustomerEntity): SyncOutcome? =
        if (row.deletedAt != null) pushDelete(row) else pushSave(ownerId, row)

    private suspend fun pushDelete(row: CustomerEntity): SyncOutcome? {
        val remoteId = row.remoteId
        if (remoteId != null) {
            val result = api.delete(remoteId)
            // Only a lost connection or a struggling server keeps the tombstone for next time.
            // Not found is what a delete wanted; a refusal leaves nothing a retry could change.
            val stop = (result as? ApiResult.Failure)?.error?.toPushFailure() as? PushFailure.Stop
            if (stop != null) return stop.outcome
        }
        dao.purge(row.id)
        return null
    }

    private suspend fun pushSave(ownerId: String, row: CustomerEntity): SyncOutcome? {
        val request = row.toRequest()
        if (request == null) {
            dao.markRejected(row.id, row.updatedAt)
            return null
        }

        val remoteId = row.remoteId
        val result = if (remoteId == null) api.create(request) else api.replace(remoteId, request)
        if (result is ApiResult.Success) {
            dao.acceptServerCopy(row.id, row.updatedAt, result.data.toEntity(ownerId))
            return null
        }

        return when (val failure = (result as ApiResult.Failure).error.toPushFailure()) {
            // Deleted on another device while edited here: the deletion wins, as it would have
            // had the edit gone up first. A create cannot be "not found", so that one is a fault.
            PushFailure.NotFound -> if (remoteId != null) {
                dao.purge(row.id)
                null
            } else {
                SyncOutcome.Failed
            }
            PushFailure.Rejected -> {
                dao.markRejected(row.id, row.updatedAt)
                null
            }
            is PushFailure.Stop -> failure.outcome
        }
    }

    /** Walks the server's whole list into the table. */
    private suspend fun pull(ownerId: String): SyncOutcome {
        val serverIds = mutableSetOf<String>()
        var cursor: String? = null
        do {
            val page = when (val result = api.customers(cursor, FieldLimits.MAX_PAGE_SIZE)) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> return result.error.toSyncOutcome()
            }
            dao.applyServerPage(ownerId, page.items.map { it.toEntity(ownerId) })
            page.items.mapTo(serverIds) { it.id }
            cursor = page.nextCursor
        } while (cursor != null)

        dao.purgeSyncedMissing(ownerId, serverIds)
        return SyncOutcome.Synced
    }

    private fun now(): String = clock.now().toString()
}
