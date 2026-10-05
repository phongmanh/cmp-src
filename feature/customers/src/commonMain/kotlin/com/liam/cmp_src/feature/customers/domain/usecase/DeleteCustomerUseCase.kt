package com.liam.cmp_src.feature.customers.domain.usecase

import com.liam.cmp_src.feature.customers.domain.repository.CustomerRepository
import com.liam.cmp_src.feature.customers.domain.repository.CustomerSyncScheduler

/**
 * Deletes a customer on the device at once, and asks for the deletion to be sent to the server.
 */
class DeleteCustomerUseCase(
    private val repository: CustomerRepository,
    private val scheduler: CustomerSyncScheduler,
) {
    suspend operator fun invoke(ownerId: String, id: String) {
        repository.delete(id)
        scheduler.requestSync(ownerId)
    }
}
