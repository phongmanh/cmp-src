package com.liam.cmp_src.feature.customers.list

import com.liam.cmp_src.feature.customers.domain.model.Customer

/** Everything the user can do on the customer list. */
sealed interface CustomersAction {
    /** Pull-to-refresh: sync now, showing that it is happening. */
    data object Refresh : CustomersAction

    data object Add : CustomersAction
    data class Open(val customerId: String) : CustomersAction

    /** Asks to delete [customer]; nothing is deleted until [ConfirmDelete]. */
    data class Delete(val customer: Customer) : CustomersAction
    data object ConfirmDelete : CustomersAction
    data object DismissDelete : CustomersAction
}
