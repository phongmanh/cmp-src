package com.liam.cmp_src.feature.customers.editor

import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.feature.customers.domain.model.CustomerField
import com.liam.cmp_src.feature.customers.domain.model.CustomerFieldError

/**
 * What the customer editor is showing. The typed text is not here: each field's text lives in the
 * ViewModel's `FormField`s, which the screen edits directly.
 */
sealed interface CustomerEditorUiState {

    /** Reading the customer being edited. */
    data object Loading : CustomerEditorUiState

    /** The customer asked for is not on the device — deleted since the list was drawn. */
    data object NotFound : CustomerEditorUiState

    data class Editing(
        val isNew: Boolean,
        val status: CustomerStatus?,
        val errors: Map<CustomerField, CustomerFieldError> = emptyMap(),
        /** The server refused this customer's last change; the form says so until it is saved. */
        val isRejected: Boolean = false,
        val isSaving: Boolean = false,
        val isConfirmingDelete: Boolean = false,
    ) : CustomerEditorUiState
}
