package com.liam.cmp_src.feature.customers.data

import com.example.api.customer.CustomerAddress
import com.example.api.customer.CustomerRequest
import com.example.api.customer.CustomerResponse
import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.core.database.entities.CustomerSyncState
import com.liam.cmp_src.core.network.ApiError
import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.model.SyncState
import io.ktor.http.HttpStatusCode
import com.liam.cmp_src.core.database.entities.Customer as CustomerEntity

/**
 * Every crossing between the three shapes a customer takes — the stored row, the wire, and the
 * domain model — plus what a failed call means to the sync.
 */

/** The row as the app shows it. */
internal fun CustomerEntity.toDomain(): Customer = Customer(
    id = id,
    details = toDraft(),
    syncState = when (syncState) {
        CustomerSyncState.PENDING -> SyncState.PENDING
        CustomerSyncState.REJECTED -> SyncState.REJECTED
        else -> SyncState.SYNCED
    },
    updatedAt = updatedAt,
)

private fun CustomerEntity.toDraft(): CustomerDraft = CustomerDraft(
    firstName = firstName.orEmpty(),
    lastName = lastName,
    companyName = companyName,
    email = email,
    phone = phone,
    address = address(),
    notes = notes,
    status = CustomerStatus.fromKey(status),
)

/** The flattened address columns as one address — only when all its required parts are there. */
private fun CustomerEntity.address(): CustomerAddress? {
    val line1 = addressLine1 ?: return null
    val city = city ?: return null
    val countryCode = countryCode ?: return null
    return CustomerAddress(
        line1 = line1,
        line2 = addressLine2,
        city = city,
        region = region,
        postalCode = postalCode,
        countryCode = countryCode,
    )
}

/** A new, never-synced row for [draft]. */
internal fun CustomerDraft.toNewEntity(id: String, ownerId: String, now: String): CustomerEntity =
    CustomerEntity(
        id = id,
        ownerId = ownerId,
        firstName = firstName,
        lastName = lastName,
        companyName = companyName,
        email = email,
        phone = phone,
        addressLine1 = address?.line1,
        addressLine2 = address?.line2,
        city = address?.city,
        region = address?.region,
        postalCode = address?.postalCode,
        countryCode = address?.countryCode,
        notes = notes,
        status = requireNotNull(status) { "A draft is only saved once it has a status" }.key,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        remoteId = null,
        syncState = CustomerSyncState.PENDING,
    )

/** The server's copy as a settled row. Its `id` is the server's; callers re-key it as needed. */
internal fun CustomerResponse.toEntity(ownerId: String): CustomerEntity = CustomerEntity(
    id = id,
    ownerId = ownerId,
    firstName = firstName,
    lastName = lastName,
    companyName = companyName,
    email = email,
    phone = phone,
    addressLine1 = address?.line1,
    addressLine2 = address?.line2,
    city = address?.city,
    region = address?.region,
    postalCode = address?.postalCode,
    countryCode = address?.countryCode,
    notes = notes,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = null,
    remoteId = id,
    syncState = CustomerSyncState.SYNCED,
)

/**
 * The request that sends this row to the server, or `null` if no request could be valid — a
 * missing first name, or a status this build cannot name. Only reachable if something other than
 * the editor wrote the row, and retrying would never help.
 */
internal fun CustomerEntity.toRequest(): CustomerRequest? {
    val firstName = firstName ?: return null
    val status = CustomerStatus.fromKey(status) ?: return null
    return CustomerRequest(
        firstName = firstName,
        lastName = lastName,
        companyName = companyName,
        email = email,
        phone = phone,
        address = address(),
        notes = notes,
        status = status,
    )
}

/** What a failed call means for the change that was being sent. */
internal sealed interface PushFailure {

    /** The server has no such customer — it was deleted elsewhere. */
    data object NotFound : PushFailure

    /**
     * The server refused this change and will refuse it every time — invalid, or conflicting with
     * another customer. The row waits for the user; the rest of the queue carries on.
     */
    data object Rejected : PushFailure

    /** Nothing more can go up right now. The sync ends here with [outcome] and tries again later. */
    data class Stop(val outcome: SyncOutcome) : PushFailure
}

internal fun ApiError.toPushFailure(): PushFailure = when (this) {
    ApiError.Network, ApiError.Timeout -> PushFailure.Stop(SyncOutcome.Offline)
    is ApiError.Http -> when {
        status == HttpStatusCode.Unauthorized.value -> PushFailure.Stop(SyncOutcome.SessionMismatch)
        status == HttpStatusCode.NotFound.value -> PushFailure.NotFound
        // Worth another go later: the server is struggling, not saying no.
        status == HttpStatusCode.RequestTimeout.value ||
            status == HttpStatusCode.TooManyRequests.value ||
            status >= HttpStatusCode.InternalServerError.value -> PushFailure.Stop(SyncOutcome.Failed)
        status >= HttpStatusCode.BadRequest.value -> PushFailure.Rejected
        else -> PushFailure.Stop(SyncOutcome.Failed)
    }
    is ApiError.Serialization, is ApiError.Unknown -> PushFailure.Stop(SyncOutcome.Failed)
}

/** How a sync ends when a call it cannot do without — the owner check, a page of the list — fails. */
internal fun ApiError.toSyncOutcome(): SyncOutcome = when (val failure = toPushFailure()) {
    is PushFailure.Stop -> failure.outcome
    PushFailure.NotFound, PushFailure.Rejected -> SyncOutcome.Failed
}
