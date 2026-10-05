package com.liam.cmp_src.feature.customers

import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.core.testing.type
import com.liam.cmp_src.feature.customers.domain.model.CustomerField
import com.liam.cmp_src.feature.customers.domain.model.CustomerFieldError
import com.liam.cmp_src.feature.customers.domain.model.SyncState
import com.liam.cmp_src.feature.customers.domain.usecase.DeleteCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.GetCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.SaveCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ValidateCustomerUseCase
import com.liam.cmp_src.feature.customers.editor.CustomerEditorAction
import com.liam.cmp_src.feature.customers.editor.CustomerEditorEvent
import com.liam.cmp_src.feature.customers.editor.CustomerEditorUiState
import com.liam.cmp_src.feature.customers.editor.CustomerEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CustomerEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scheduler = FakeSyncScheduler()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(repository: FakeCustomerRepository, customerId: String? = null) =
        CustomerEditorViewModel(
            ownerId = OWNER,
            customerId = customerId,
            getCustomer = GetCustomerUseCase(repository),
            saveCustomer = SaveCustomerUseCase(repository, scheduler),
            deleteCustomer = DeleteCustomerUseCase(repository, scheduler),
            validateCustomer = ValidateCustomerUseCase(),
        )

    private fun TestScope.collectEvents(viewModel: CustomerEditorViewModel): MutableList<CustomerEditorEvent> {
        val events = mutableListOf<CustomerEditorEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    private val CustomerEditorViewModel.editing
        get() = assertIs<CustomerEditorUiState.Editing>(state.value)

    @Test
    fun `a new customer starts empty as a lead`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeCustomerRepository())

        assertTrue(viewModel.editing.isNew)
        assertEquals(CustomerStatus.LEAD, viewModel.editing.status)
        assertEquals("", viewModel.field(CustomerField.FIRST_NAME).state.text.toString())
    }

    @Test
    fun `an invalid form shows its errors and saves nothing`() = runTest(testDispatcher) {
        val repository = FakeCustomerRepository()
        val viewModel = viewModelWith(repository)

        viewModel.field(CustomerField.EMAIL).state.type("nope")
        advanceUntilIdle()
        viewModel.onAction(CustomerEditorAction.Save)
        advanceUntilIdle()

        assertEquals(
            mapOf(
                CustomerField.FIRST_NAME to CustomerFieldError.Required,
                CustomerField.EMAIL to CustomerFieldError.InvalidEmail,
            ),
            viewModel.editing.errors,
        )
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `fixing a field clears its error and only its error`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeCustomerRepository())
        viewModel.field(CustomerField.EMAIL).state.type("nope")
        advanceUntilIdle()
        viewModel.onAction(CustomerEditorAction.Save)

        viewModel.field(CustomerField.FIRST_NAME).state.type("Ada")
        advanceUntilIdle()

        assertEquals(setOf(CustomerField.EMAIL), viewModel.editing.errors.keys)
    }

    @Test
    fun `a valid new customer is stored and the editor closes`() = runTest(testDispatcher) {
        val repository = FakeCustomerRepository()
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)

        viewModel.field(CustomerField.FIRST_NAME).state.type("Ada")
        viewModel.onAction(CustomerEditorAction.StatusSelected(CustomerStatus.ACTIVE))
        viewModel.onAction(CustomerEditorAction.Save)
        advanceUntilIdle()

        assertEquals("Ada", repository.created.single().firstName)
        assertEquals(CustomerStatus.ACTIVE, repository.created.single().status)
        assertEquals(listOf(OWNER), scheduler.requests)
        assertEquals<List<CustomerEditorEvent>>(listOf(CustomerEditorEvent.Done), events)
    }

    @Test
    fun `an existing customer fills the form`() = runTest(testDispatcher) {
        val customer = sampleCustomer(syncState = SyncState.REJECTED)
        val viewModel = viewModelWith(FakeCustomerRepository(customer), customerId = customer.id)
        advanceUntilIdle()

        assertEquals(false, viewModel.editing.isNew)
        assertTrue(viewModel.editing.isRejected, "a refused change must be pointed out")
        assertEquals("Lovelace", viewModel.field(CustomerField.LAST_NAME).state.text.toString())
        assertEquals("GB", viewModel.field(CustomerField.COUNTRY_CODE).state.text.toString())
    }

    @Test
    fun `saving an existing customer updates it`() = runTest(testDispatcher) {
        val customer = sampleCustomer()
        val repository = FakeCustomerRepository(customer)
        val viewModel = viewModelWith(repository, customerId = customer.id)
        advanceUntilIdle()

        viewModel.field(CustomerField.FIRST_NAME).state.type("Augusta")
        viewModel.onAction(CustomerEditorAction.Save)
        advanceUntilIdle()

        val (id, draft) = repository.updated.single()
        assertEquals(customer.id, id)
        assertEquals("Augusta", draft.firstName)
    }

    @Test
    fun `a customer that is no longer there says so`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeCustomerRepository(), customerId = "gone")
        advanceUntilIdle()

        assertEquals(CustomerEditorUiState.NotFound, viewModel.state.value)
    }

    @Test
    fun `a confirmed delete removes the customer and closes the editor`() = runTest(testDispatcher) {
        val customer = sampleCustomer()
        val repository = FakeCustomerRepository(customer)
        val viewModel = viewModelWith(repository, customerId = customer.id)
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        viewModel.onAction(CustomerEditorAction.Delete)
        assertTrue(viewModel.editing.isConfirmingDelete)
        viewModel.onAction(CustomerEditorAction.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(listOf(customer.id), repository.deleted)
        assertEquals<List<CustomerEditorEvent>>(listOf(CustomerEditorEvent.Done), events)
    }

    private companion object {
        const val OWNER = "owner-1"
    }
}
