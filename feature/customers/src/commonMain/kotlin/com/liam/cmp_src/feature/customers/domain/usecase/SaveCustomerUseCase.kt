package com.liam.cmp_src.feature.customers.domain.usecase

import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.repository.CustomerRepository
import com.liam.cmp_src.feature.customers.domain.repository.CustomerSyncScheduler

/**
 * Stores a new customer ([id] `null`) or a change to an existing one, then asks for a sync.
 *
 * Returns once the device has the change — never waiting on the network — so saving works the same
 * offline. Returns `false` only when [id] names a customer that has since been deleted.
 */
class SaveCustomerUseCase(
    private val repository: CustomerRepository,
    private val scheduler: CustomerSyncScheduler,
) {
    suspend operator fun invoke(ownerId: String, id: String?, draft: CustomerDraft): Boolean {
        val saved = if (id == null) {
            repository.create(ownerId, draft)
            true
        } else {
            repository.update(id, draft)
        }
        if (saved) scheduler.requestSync(ownerId)
        return saved
    }
}
