package com.liam.cmp_src.feature.customers.editor

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.cmp_src.core.ui.input.FormField
import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.model.CustomerField
import com.liam.cmp_src.feature.customers.domain.model.CustomerInput
import com.liam.cmp_src.feature.customers.domain.model.CustomerValidation
import com.liam.cmp_src.feature.customers.domain.model.SyncState
import com.liam.cmp_src.feature.customers.domain.usecase.DeleteCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.GetCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.SaveCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ValidateCustomerUseCase
import com.example.api.customer.CustomerStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Creates a customer ([customerId] `null`) or edits one, and deletes it on request.
 *
 * Every write is to the device and returns at once — the sync the use cases ask for sends it on —
 * so saving works offline and the editor closes as soon as it has.
 *
 * Validation runs before anything is stored rather than being left to the server, because a
 * stored change the server refuses surfaces only later, as a customer stuck unsynced.
 */
class CustomerEditorViewModel(
    private val ownerId: String,
    private val customerId: String?,
    private val getCustomer: GetCustomerUseCase,
    private val saveCustomer: SaveCustomerUseCase,
    private val deleteCustomer: DeleteCustomerUseCase,
    private val validateCustomer: ValidateCustomerUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        if (customerId == null) CustomerEditorUiState.Editing(isNew = true, status = CustomerStatus.LEAD)
        else CustomerEditorUiState.Loading,
    )
    val state: StateFlow<CustomerEditorUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CustomerEditorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CustomerEditorEvent> = _events.asSharedFlow()

    /** One field per text input. Editing a field clears the error it was showing. */
    val fields: Map<CustomerField, FormField> = TEXT_FIELDS.associateWith { field ->
        FormField(viewModelScope) { clearError(field) }
    }

    init {
        if (customerId != null) load(customerId)
    }

    /** The field for [field]. Every text input has one; only [CustomerField.STATUS] does not. */
    fun field(field: CustomerField): FormField =
        requireNotNull(fields[field]) { "$field is not a text input" }

    fun onAction(action: CustomerEditorAction) {
        when (action) {
            is CustomerEditorAction.StatusSelected -> updateEditing {
                it.copy(status = action.status, errors = it.errors - CustomerField.STATUS)
            }
            CustomerEditorAction.Save -> save()
            CustomerEditorAction.Delete -> updateEditing { it.copy(isConfirmingDelete = true) }
            CustomerEditorAction.DismissDelete -> updateEditing { it.copy(isConfirmingDelete = false) }
            CustomerEditorAction.ConfirmDelete -> delete()
            CustomerEditorAction.Close -> finish()
        }
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val customer = getCustomer(id)
            if (customer == null) {
                _state.value = CustomerEditorUiState.NotFound
            } else {
                fill(customer)
                _state.value = CustomerEditorUiState.Editing(
                    isNew = false,
                    status = customer.details.status,
                    isRejected = customer.syncState == SyncState.REJECTED,
                )
            }
        }
    }

    private fun fill(customer: Customer) {
        val details = customer.details
        val values = mapOf(
            CustomerField.FIRST_NAME to details.firstName,
            CustomerField.LAST_NAME to details.lastName,
            CustomerField.COMPANY_NAME to details.companyName,
            CustomerField.EMAIL to details.email,
            CustomerField.PHONE to details.phone,
            CustomerField.ADDRESS_LINE1 to details.address?.line1,
            CustomerField.ADDRESS_LINE2 to details.address?.line2,
            CustomerField.CITY to details.address?.city,
            CustomerField.REGION to details.address?.region,
            CustomerField.POSTAL_CODE to details.address?.postalCode,
            CustomerField.COUNTRY_CODE to details.address?.countryCode,
            CustomerField.NOTES to details.notes,
        )
        values.forEach { (field, value) -> field(field).state.setTextAndPlaceCursorAtEnd(value.orEmpty()) }
    }

    private fun save() {
        val editing = _state.value as? CustomerEditorUiState.Editing ?: return
        if (editing.isSaving) return

        when (val validation = validateCustomer(input(editing))) {
            is CustomerValidation.Invalid -> updateEditing { it.copy(errors = validation.errors) }
            is CustomerValidation.Valid -> {
                updateEditing { it.copy(errors = emptyMap(), isSaving = true) }
                viewModelScope.launch {
                    val saved = saveCustomer(ownerId, customerId, validation.draft)
                    if (saved) finish() else _state.value = CustomerEditorUiState.NotFound
                }
            }
        }
    }

    private fun delete() {
        val id = customerId ?: return
        updateEditing { it.copy(isConfirmingDelete = false, isSaving = true) }
        viewModelScope.launch {
            deleteCustomer(ownerId, id)
            finish()
        }
    }

    /** The form as typed. [FormField.submit] so a keystroke landing late cannot clear the errors. */
    private fun input(editing: CustomerEditorUiState.Editing) = CustomerInput(
        firstName = field(CustomerField.FIRST_NAME).submit(),
        lastName = field(CustomerField.LAST_NAME).submit(),
        companyName = field(CustomerField.COMPANY_NAME).submit(),
        email = field(CustomerField.EMAIL).submit(),
        phone = field(CustomerField.PHONE).submit(),
        addressLine1 = field(CustomerField.ADDRESS_LINE1).submit(),
        addressLine2 = field(CustomerField.ADDRESS_LINE2).submit(),
        city = field(CustomerField.CITY).submit(),
        region = field(CustomerField.REGION).submit(),
        postalCode = field(CustomerField.POSTAL_CODE).submit(),
        countryCode = field(CustomerField.COUNTRY_CODE).submit(),
        notes = field(CustomerField.NOTES).submit(),
        status = editing.status,
    )

    private fun clearError(field: CustomerField) = updateEditing { it.copy(errors = it.errors - field) }

    private fun updateEditing(transform: (CustomerEditorUiState.Editing) -> CustomerEditorUiState.Editing) {
        _state.update { if (it is CustomerEditorUiState.Editing) transform(it) else it }
    }

    private fun finish() {
        viewModelScope.launch { _events.emit(CustomerEditorEvent.Done) }
    }

    private companion object {
        val TEXT_FIELDS = CustomerField.entries - CustomerField.STATUS
    }
}
