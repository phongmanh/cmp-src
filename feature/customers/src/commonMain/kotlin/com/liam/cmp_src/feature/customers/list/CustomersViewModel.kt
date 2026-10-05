package com.liam.cmp_src.feature.customers.list

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.cmp_src.core.ui.input.FormField
import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.usecase.DeleteCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ObserveCustomersUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ObservePendingChangesUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.SyncCustomersUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How long typing has to pause before the search is applied. */
internal const val SEARCH_DEBOUNCE_MILLIS = 300L

/**
 * The signed-in account's customers, read from the device and kept in step with the server.
 *
 * The list is the table: every save, delete and sync lands there and the list follows, so nothing
 * here refreshes by hand. Syncing runs once when the list opens and again on pull-to-refresh; the
 * edits themselves ask for their own sync through the use cases that store them.
 *
 * [ownerId] is the signed-in account's id, passed in by the app shell — the rows are scoped to it.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CustomersViewModel(
    private val ownerId: String,
    private val observeCustomers: ObserveCustomersUseCase,
    observePendingChanges: ObservePendingChangesUseCase,
    private val syncCustomers: SyncCustomersUseCase,
    private val deleteCustomer: DeleteCustomerUseCase,
) : ViewModel() {

    /** The search box. Its text is read through [results]; typing needs no handling of its own. */
    val search = FormField(viewModelScope) {}

    private val syncStatus = MutableStateFlow(SyncStatus())
    private val deleteCandidate = MutableStateFlow<Customer?>(null)

    private val _events = MutableSharedFlow<CustomersEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CustomersEvent> = _events.asSharedFlow()

    /**
     * The applied query with the customers it matches. Clearing the box applies at once; typing
     * waits for a pause, so a burst of keystrokes is one query rather than one per letter.
     */
    private val results = snapshotFlow { search.state.text.toString().trim() }
        .debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .distinctUntilChanged()
        .flatMapLatest { query -> observeCustomers(ownerId, query).map { query to it } }

    val state: StateFlow<CustomersUiState> = combine(
        results,
        observePendingChanges(ownerId),
        syncStatus,
        deleteCandidate,
    ) { (query, customers), pendingCount, sync, candidate ->
        if (customers.isEmpty() && query.isEmpty() && sync.isFirstSyncRunning) {
            CustomersUiState.Loading
        } else {
            CustomersUiState.Success(
                customers = customers,
                query = query,
                isRefreshing = sync.isRefreshing,
                banner = bannerFor(pendingCount, sync.lastOutcome),
                deleteCandidate = candidate,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CustomersUiState.Loading)

    init {
        sync(showIndicator = false)
    }

    fun onAction(action: CustomersAction) {
        when (action) {
            CustomersAction.Refresh -> sync(showIndicator = true)
            CustomersAction.Add -> emit(CustomersEvent.OpenEditor(customerId = null))
            is CustomersAction.Open -> emit(CustomersEvent.OpenEditor(action.customerId))
            is CustomersAction.Delete -> deleteCandidate.value = action.customer
            CustomersAction.DismissDelete -> deleteCandidate.value = null
            CustomersAction.ConfirmDelete -> confirmDelete()
        }
    }

    /**
     * Runs one sync. A second request while one is running only takes over the indicator: the
     * repository would queue it behind the first anyway, and the first is already doing the work.
     */
    private fun sync(showIndicator: Boolean) {
        if (syncStatus.value.isSyncing) {
            if (showIndicator) syncStatus.update { it.copy(isRefreshing = true) }
            return
        }
        syncStatus.update { it.copy(isSyncing = true, isRefreshing = showIndicator) }
        viewModelScope.launch {
            val outcome = syncCustomers(ownerId)
            syncStatus.value = SyncStatus(lastOutcome = outcome)
        }
    }

    private fun confirmDelete() {
        val customer = deleteCandidate.value ?: return
        deleteCandidate.value = null
        viewModelScope.launch {
            deleteCustomer(ownerId, customer.id)
            _events.emit(CustomersEvent.Deleted)
        }
    }

    private fun emit(event: CustomersEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    private fun bannerFor(pendingCount: Int, outcome: SyncOutcome?): SyncBanner? {
        if (pendingCount == 0) return null
        val problem = when (outcome) {
            SyncOutcome.Offline -> SyncProblem.OFFLINE
            SyncOutcome.Failed -> SyncProblem.FAILED
            SyncOutcome.SessionMismatch -> SyncProblem.SESSION_MISMATCH
            SyncOutcome.Synced, null -> null
        }
        return SyncBanner(pendingCount = pendingCount, problem = problem)
    }

    /** Where the list's own syncing stands. [lastOutcome] is `null` until the first one ends. */
    private data class SyncStatus(
        val isSyncing: Boolean = false,
        val isRefreshing: Boolean = false,
        val lastOutcome: SyncOutcome? = null,
    ) {
        val isFirstSyncRunning: Boolean get() = isSyncing && lastOutcome == null
    }

    private companion object {
        /** Keeps the queries alive across a rotation rather than restarting them. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
