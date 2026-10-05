package com.liam.cmp_src.feature.customers.list

/** One-shot effects the customer list reports upwards. */
sealed interface CustomersEvent {
    /** Open the editor on [customerId], or on a new customer when it is `null`. */
    data class OpenEditor(val customerId: String?) : CustomersEvent

    /** A customer was deleted; say so. */
    data object Deleted : CustomersEvent
}
