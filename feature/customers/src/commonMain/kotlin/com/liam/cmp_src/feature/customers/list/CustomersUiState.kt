package com.liam.cmp_src.feature.customers.list

import com.liam.cmp_src.feature.customers.domain.model.Customer

/**
 * What the customer list is showing.
 *
 * There is no `Error` state, unlike the app's network-backed screens: the list is read from the
 * device, which does not fail in any way a person could act on, and a sync that fails leaves the
 * list exactly as usable as before. Sync trouble is a [SyncBanner] over the list instead.
 */
sealed interface CustomersUiState {

    /** Nothing to show yet — the table's first read, or a first sync filling an empty table. */
    data object Loading : CustomersUiState

    data class Success(
        val customers: List<Customer>,
        /** The search the list is filtered by, as applied — trimmed and debounced. */
        val query: String,
        val isRefreshing: Boolean,
        val banner: SyncBanner?,
        /** The customer whose deletion is waiting on the user's confirmation. */
        val deleteCandidate: Customer? = null,
    ) : CustomersUiState
}

/**
 * The line over the list while changes are waiting to reach the server, saying why when a sync
 * could not send them. Gone as soon as nothing is waiting, whatever the last sync said — a stale
 * "you're offline" after the background job caught up would be worse than none.
 */
data class SyncBanner(
    val pendingCount: Int,
    val problem: SyncProblem?,
)

enum class SyncProblem { OFFLINE, FAILED, SESSION_MISMATCH }
