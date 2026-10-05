package com.liam.cmp_src.feature.customers.domain.usecase

import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow

/**
 * The owner's customers matching [search], as they stand on the device and as they change.
 *
 * Searching is local on purpose: it has to work offline, and it shows customers created offline
 * that the server's `?q=` has never seen.
 */
class ObserveCustomersUseCase(
    private val repository: CustomerRepository,
) {
    operator fun invoke(ownerId: String, search: String): Flow<List<Customer>> =
        repository.customers(ownerId, search.trim())
}
