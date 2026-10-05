package com.liam.cmp_src.feature.customers.domain.model

import com.example.api.customer.CustomerAddress
import com.example.api.customer.CustomerStatus

/**
 * A customer as the app shows and edits it.
 *
 * Neither the wire shape (`CustomerResponse`) nor the stored row: [id] is the device's identity for
 * the customer, stable across syncs, and [syncState] says whether the server has what the user
 * sees. The address and status reuse the contract's types, as the signed-in user reuses
 * `UserResponse` — they carry no wire-only baggage and a copy would only drift.
 *
 * [status] is `null` when the server sent a status this build does not know. Guessing one would
 * mean an unrelated edit silently changing it — the contract's own reason for refusing a default.
 */
data class Customer(
    val id: String,
    val details: CustomerDraft,
    val syncState: SyncState,
    /** ISO-8601 in UTC: when the customer last changed, here or on the server. */
    val updatedAt: String,
)

/**
 * Everything about a customer a person can type — what the editor submits and a save stores.
 *
 * Optional text is `null` rather than blank: the form trims and drops empty values before a draft
 * is built (see `ValidateCustomerUseCase`), so nothing downstream tells the two apart.
 */
data class CustomerDraft(
    val firstName: String,
    val lastName: String? = null,
    val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: CustomerAddress? = null,
    val notes: String? = null,
    val status: CustomerStatus?,
)

/** Whether the server holds the version of a customer the device is showing. */
enum class SyncState {
    SYNCED,

    /** Changed on the device; goes up with the next sync. */
    PENDING,

    /** The server refused the change as invalid. Waits for the user to fix and save it again. */
    REJECTED,
}
