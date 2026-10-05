package com.liam.cmp_src.feature.customers

import com.example.api.customer.CustomerAddress
import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.model.SyncState

/**
 * A customer with every field filled, for previews and tests — the counterpart of `core:ui`'s
 * `sampleUser()`. Only what a caller varies is a parameter.
 */
internal fun sampleCustomer(
    id: String = "customer-1",
    firstName: String = "Ada",
    lastName: String? = "Lovelace",
    syncState: SyncState = SyncState.SYNCED,
    status: CustomerStatus? = CustomerStatus.ACTIVE,
): Customer = Customer(
    id = id,
    details = CustomerDraft(
        firstName = firstName,
        lastName = lastName,
        companyName = "Analytical Engines",
        email = "ada@example.com",
        phone = "+442079460958",
        address = CustomerAddress(
            line1 = "12 Queen Square",
            city = "London",
            postalCode = "WC1N 3AR",
            countryCode = "GB",
        ),
        notes = "Prefers email.",
        status = status,
    ),
    syncState = syncState,
    updatedAt = "2026-09-16T12:00:00Z",
)
