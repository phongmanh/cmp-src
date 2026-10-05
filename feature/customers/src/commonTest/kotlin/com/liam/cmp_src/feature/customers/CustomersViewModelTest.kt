package com.liam.cmp_src.feature.customers

import com.liam.cmp_src.core.testing.type
import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.usecase.DeleteCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ObserveCustomersUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ObservePendingChangesUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.SyncCustomersUseCase
import com.liam.cmp_src.feature.customers.list.CustomersAction
import com.liam.cmp_src.feature.customers.list.CustomersEvent
import com.liam.cmp_src.feature.customers.list.CustomersUiState
import com.liam.cmp_src.feature.customers.list.CustomersViewModel
import com.liam.cmp_src.feature.customers.list.SEARCH_DEBOUNCE_MILLIS
import com.liam.cmp_src.feature.customers.list.SyncBanner
import com.liam.cmp_src.feature.customers.list.SyncProblem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which has no implementation under test.
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(repository: FakeCustomerRepository, scheduler: FakeSyncScheduler = FakeSyncScheduler()) =
        CustomersViewModel(
            ownerId = OWNER,
            observeCustomers = ObserveCustomersUseCase(repository),
            observePendingChanges = ObservePendingChangesUseCase(repository),
            syncCustomers = SyncCustomersUseCase(repository),
            deleteCustomer = DeleteCustomerUseCase(repository, scheduler),
        )

    /** `state` only runs while collected, as it would under a screen. */
    private fun TestScope.observe(viewModel: CustomersViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    private fun TestScope.collectEvents(viewModel: CustomersViewModel): MutableList<CustomersEvent> {
        val events = mutableListOf<CustomersEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    private val CustomersViewModel.success get() = assertIs<CustomersUiState.Success>(state.value)

    @Test
    fun `an empty table shows loading until the first sync has answered`() = runTest(testDispatcher) {
        val repository = FakeCustomerRepository().apply { syncGate = CompletableDeferred() }
        val viewModel = viewModelWith(repository)
        observe(viewModel)
        runCurrent()

        assertEquals(CustomersUiState.Loading, viewModel.state.value)

        repository.syncGate?.complete(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.success.customers.isEmpty())
    }

    /** Offline-first: what is on the device shows straight away, sync or no sync. */
    @Test
    fun `saved customers show while the first sync is still running`() = runTest(testDispatcher) {
        val repository = FakeCustomerRepository(sampleCustomer()).apply { syncGate = CompletableDeferred() }
        val viewModel = viewModelWith(repository)
        observe(viewModel)
        runCurrent()

        assertEquals(listOf(sampleCustomer()), viewModel.success.customers)
    }

    @Test
    fun `opening the list syncs once`() = runTest(testDispatcher) {
        val repository = FakeCustomerRepository()
        viewModelWith(repository)
        advanceUntilIdle()

        assertEquals(1, repository.syncCount)
    }

    @Test
    fun `a search is applied once typing pauses`() = runTest(testDispatcher) {
        val repository = FakeCustomerRepository(
            sampleCustomer(id = "a", firstName = "Ada"),
            sampleCustomer(id = "g", firstName = "Grace"),
        )
        val viewModel = viewModelWith(repository)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.search.state.type("gr")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)
        assertEquals("", viewModel.success.query, "still typing")

        advanceTimeBy(2)
        assertEquals("gr", viewModel.success.query)
        assertEquals(listOf("g"), viewModel.success.customers.map { it.id })
    }

    @Test
    fun `clearing the search applies at once`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeCustomerRepository(sampleCustomer()))
        observe(viewModel)
        viewModel.search.state.type("zz")
        advanceUntilIdle()

        viewModel.search.state.type("")
        runCurrent()

        assertEquals("", viewModel.success.query)
    }

    @Test
    fun `waiting changes after an offline sync say why they are waiting`() = runTest(testDispatcher) {
        val repository = FakeCustomerRepository(sampleCustomer(), syncOutcome = SyncOutcome.Offline)
        repository.pending.value = 2
        val viewModel = viewModelWith(repository)
        observe(viewModel)
        advanceUntilIdle()

        assertEquals(SyncBanner(pendingCount = 2, problem = SyncProblem.OFFLINE), viewModel.success.banner)
    }

    /** The background job may catch up after the list's own sync failed; a stale warning would lie. */
    @Test
    fun `the banner goes once nothing is waiting whatever the last sync said`() = runTest(testDispatcher) {
        val repository = FakeCustomerRepository(sampleCustomer(), syncOutcome = SyncOutcome.Offline)
        repository.pending.value = 1
        val viewModel = viewModelWith(repository)
        observe(viewModel)
        advanceUntilIdle()

        repository.pending.value = 0
        runCurrent()

        assertNull(viewModel.success.banner)
    }

    @Test
    fun `pull to refresh syncs again and shows it is working until it answers`() = runTest(testDispatcher) {
        val repository = FakeCustomerRepository(sampleCustomer())
        val viewModel = viewModelWith(repository)
        observe(viewModel)
        advanceUntilIdle()
        repository.syncGate = CompletableDeferred()

        viewModel.onAction(CustomersAction.Refresh)
        runCurrent()
        assertTrue(viewModel.success.isRefreshing)

        repository.syncGate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(false, viewModel.success.isRefreshing)
        assertEquals(2, repository.syncCount)
    }

    @Test
    fun `a delete waits for confirmation`() = runTest(testDispatcher) {
        val customer = sampleCustomer()
        val repository = FakeCustomerRepository(customer)
        val viewModel = viewModelWith(repository)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onAction(CustomersAction.Delete(customer))
        runCurrent()

        assertEquals(customer, viewModel.success.deleteCandidate)
        assertTrue(repository.deleted.isEmpty())
    }

    @Test
    fun `a confirmed delete removes the customer and says so`() = runTest(testDispatcher) {
        val customer = sampleCustomer()
        val repository = FakeCustomerRepository(customer)
        val scheduler = FakeSyncScheduler()
        val viewModel = viewModelWith(repository, scheduler)
        observe(viewModel)
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        viewModel.onAction(CustomersAction.Delete(customer))
        viewModel.onAction(CustomersAction.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(listOf(customer.id), repository.deleted)
        assertEquals(listOf(OWNER), scheduler.requests)
        assertEquals<List<CustomersEvent>>(listOf(CustomersEvent.Deleted), events)
        assertNull(viewModel.success.deleteCandidate)
    }

    @Test
    fun `a dismissed delete deletes nothing`() = runTest(testDispatcher) {
        val customer = sampleCustomer()
        val repository = FakeCustomerRepository(customer)
        val viewModel = viewModelWith(repository)
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onAction(CustomersAction.Delete(customer))
        viewModel.onAction(CustomersAction.DismissDelete)
        advanceUntilIdle()

        assertTrue(repository.deleted.isEmpty())
        assertNull(viewModel.success.deleteCandidate)
    }

    @Test
    fun `adding and opening ask for the editor`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeCustomerRepository())
        val events = collectEvents(viewModel)

        viewModel.onAction(CustomersAction.Add)
        viewModel.onAction(CustomersAction.Open("c1"))
        advanceUntilIdle()

        assertEquals<List<CustomersEvent>>(
            listOf(CustomersEvent.OpenEditor(null), CustomersEvent.OpenEditor("c1")),
            events,
        )
    }

    private companion object {
        const val OWNER = "owner-1"
    }
}
