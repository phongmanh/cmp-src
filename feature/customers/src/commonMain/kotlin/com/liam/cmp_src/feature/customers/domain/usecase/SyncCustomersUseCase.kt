package com.liam.cmp_src.feature.customers.domain.usecase

import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.repository.CustomerRepository

/**
 * Reconciles the owner's customers with the server, now: what the list runs on open and on
 * pull-to-refresh, and what the background job runs when the platform wakes it.
 */
class SyncCustomersUseCase(
    private val repository: CustomerRepository,
) {
    suspend operator fun invoke(ownerId: String): SyncOutcome = repository.sync(ownerId)
}
