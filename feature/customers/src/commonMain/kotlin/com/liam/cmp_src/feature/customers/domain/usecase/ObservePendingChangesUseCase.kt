package com.liam.cmp_src.feature.customers.domain.usecase

import com.liam.cmp_src.feature.customers.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow

/** How many of the owner's changes are still waiting to reach the server. */
class ObservePendingChangesUseCase(
    private val repository: CustomerRepository,
) {
    operator fun invoke(ownerId: String): Flow<Int> = repository.pendingChanges(ownerId)
}
