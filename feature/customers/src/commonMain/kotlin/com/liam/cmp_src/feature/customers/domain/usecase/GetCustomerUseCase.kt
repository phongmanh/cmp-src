package com.liam.cmp_src.feature.customers.domain.usecase

import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.repository.CustomerRepository

/** One customer from the device, or `null` if it has been deleted. */
class GetCustomerUseCase(
    private val repository: CustomerRepository,
) {
    suspend operator fun invoke(id: String): Customer? = repository.customer(id)
}
