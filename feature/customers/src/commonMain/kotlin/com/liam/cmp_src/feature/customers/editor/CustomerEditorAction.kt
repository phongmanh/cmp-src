package com.liam.cmp_src.feature.customers.editor

import com.example.api.customer.CustomerStatus

/** Everything the user can do in the customer editor. Typing goes straight to the fields. */
sealed interface CustomerEditorAction {
    data class StatusSelected(val status: CustomerStatus) : CustomerEditorAction
    data object Save : CustomerEditorAction

    /** Asks to delete; nothing is deleted until [ConfirmDelete]. */
    data object Delete : CustomerEditorAction
    data object ConfirmDelete : CustomerEditorAction
    data object DismissDelete : CustomerEditorAction

    /** Leave without saving. */
    data object Close : CustomerEditorAction
}
