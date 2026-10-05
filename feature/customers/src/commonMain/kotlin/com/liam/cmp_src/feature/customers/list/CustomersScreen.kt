package com.liam.cmp_src.feature.customers.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cmpsrc.feature.customers.generated.resources.Res
import cmpsrc.feature.customers.generated.resources.cd_add_customer
import cmpsrc.feature.customers.generated.resources.cd_clear_search
import cmpsrc.feature.customers.generated.resources.cd_scroll_to_top
import cmpsrc.feature.customers.generated.resources.cd_search
import cmpsrc.feature.customers.generated.resources.customers_delete_body
import cmpsrc.feature.customers.generated.resources.customers_delete_cancel
import cmpsrc.feature.customers.generated.resources.customers_delete_confirm
import cmpsrc.feature.customers.generated.resources.customers_delete_title
import cmpsrc.feature.customers.generated.resources.customers_deleted
import cmpsrc.feature.customers.generated.resources.customers_empty_body
import cmpsrc.feature.customers.generated.resources.customers_empty_title
import cmpsrc.feature.customers.generated.resources.customers_no_results
import cmpsrc.feature.customers.generated.resources.customers_search_label
import cmpsrc.feature.customers.generated.resources.customers_search_placeholder
import cmpsrc.feature.customers.generated.resources.ic_add
import cmpsrc.feature.customers.generated.resources.ic_arrow_up
import cmpsrc.feature.customers.generated.resources.ic_close
import cmpsrc.feature.customers.generated.resources.ic_search
import com.liam.cmp_src.core.ui.component.AppOutlinedTextField
import com.liam.cmp_src.core.ui.component.GlassCard
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.feature.customers.component.CustomerIconButton
import com.liam.cmp_src.feature.customers.component.displayName
import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.model.SyncState
import com.liam.cmp_src.feature.customers.list.component.CustomerRow
import com.liam.cmp_src.feature.customers.list.component.CustomersSkeleton
import com.liam.cmp_src.feature.customers.list.component.SyncStatusBanner
import com.liam.cmp_src.feature.customers.sampleCustomer
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The customer list wired to its [CustomersViewModel].
 *
 * Same split as the app's other routes: this owns the ViewModel and reports what happened, while
 * the stateless [CustomersScreen] can be previewed without Koin. [ownerId] is the signed-in
 * account, whose customers these are. [onOpenEditor] opens the editor on a customer, or on a new
 * one for `null` — navigating is the app shell's job, not this screen's.
 */
@Composable
fun CustomersRoute(
    ownerId: String,
    onOpenEditor: (customerId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomersViewModel = koinViewModel { parametersOf(ownerId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CustomersEvent.OpenEditor -> onOpenEditor(event.customerId)
                CustomersEvent.Deleted ->
                    launch { snackbarHostState.showSnackbar(getString(Res.string.customers_deleted)) }
            }
        }
    }

    CustomersScreen(
        state = state,
        search = viewModel.search.state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Search box, sync banner and the list, with the add and back-to-top buttons floating over it.
 *
 * Carries its own [SnackbarHostState] for the reason `ProfileScreen` does: it is hosted inside the
 * home shell's tab, whose Scaffold slots are not reachable from here.
 */
@Composable
fun CustomersScreen(
    state: CustomersUiState,
    search: TextFieldState,
    onAction: (CustomersAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val query = (state as? CustomersUiState.Success)?.query

    // A new search starts at its first result, not wherever the last one was scrolled to.
    LaunchedEffect(query) { if (query != null) listState.scrollToItem(0) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = Dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            SearchField(search)

            when (state) {
                CustomersUiState.Loading -> CustomersSkeleton()
                is CustomersUiState.Success -> {
                    state.banner?.let { SyncStatusBanner(it) }
                    CustomerList(state = state, listState = listState, onAction = onAction)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = Dimens.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.handCursor(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_up),
                        contentDescription = stringResource(Res.string.cd_scroll_to_top),
                        modifier = Modifier.size(Dimens.iconMd),
                    )
                }
            }
            FloatingActionButton(
                onClick = { onAction(CustomersAction.Add) },
                modifier = Modifier.handCursor(),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_add),
                    contentDescription = stringResource(Res.string.cd_add_customer),
                    modifier = Modifier.size(Dimens.iconMd),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    (state as? CustomersUiState.Success)?.deleteCandidate?.let { candidate ->
        DeleteCustomerDialog(customer = candidate, onAction = onAction)
    }
}

@Composable
private fun SearchField(search: TextFieldState) {
    AppOutlinedTextField(
        state = search,
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(Res.string.customers_search_label),
        placeholder = stringResource(Res.string.customers_search_placeholder),
        leadingIcon = Res.drawable.ic_search,
        leadingIconDescription = stringResource(Res.string.cd_search),
        trailingIcon = if (search.text.isEmpty()) {
            null
        } else {
            { tint ->
                CustomerIconButton(
                    icon = Res.drawable.ic_close,
                    contentDescription = stringResource(Res.string.cd_clear_search),
                    onClick = { search.clearText() },
                    tint = tint,
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/**
 * The customers, pull-to-refresh around them. The empty and no-results messages sit inside the
 * same scrolling list so the pull gesture still works when there is nothing to scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerList(
    state: CustomersUiState.Success,
    listState: LazyListState,
    onAction: (CustomersAction) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(CustomersAction.Refresh) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Room under the floating buttons, so the last row can scroll clear of them.
            contentPadding = PaddingValues(bottom = LIST_BOTTOM_CLEARANCE),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            if (state.customers.isEmpty()) {
                item(key = EMPTY_KEY) { EmptyList(query = state.query) }
            }
            items(state.customers, key = { it.id }) { customer ->
                CustomerRow(
                    customer = customer,
                    onClick = { onAction(CustomersAction.Open(customer.id)) },
                    onDelete = { onAction(CustomersAction.Delete(customer)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun EmptyList(query: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        if (query.isEmpty()) {
            Text(
                text = stringResource(Res.string.customers_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.customers_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = stringResource(Res.string.customers_no_results, query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DeleteCustomerDialog(
    customer: Customer,
    onAction: (CustomersAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(CustomersAction.DismissDelete) },
        title = {
            Text(stringResource(Res.string.customers_delete_title, customer.details.displayName()))
        },
        text = { Text(stringResource(Res.string.customers_delete_body)) },
        confirmButton = {
            TextButton(
                onClick = { onAction(CustomersAction.ConfirmDelete) },
                modifier = Modifier.handCursor(),
            ) {
                Text(
                    text = stringResource(Res.string.customers_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(CustomersAction.DismissDelete) },
                modifier = Modifier.handCursor(),
            ) {
                Text(stringResource(Res.string.customers_delete_cancel))
            }
        },
    )
}

private const val EMPTY_KEY = "empty"

/** A large FAB, a small one, and the gap between and below them. */
private val LIST_BOTTOM_CLEARANCE = Dimens.spaceXxxl * 3

@Preview
@Composable
private fun CustomersScreenPreview() {
    AppTheme {
        CustomersScreen(
            state = CustomersUiState.Success(
                customers = listOf(
                    sampleCustomer(),
                    sampleCustomer(id = "2", firstName = "Grace", lastName = "Hopper", syncState = SyncState.PENDING),
                    sampleCustomer(id = "3", firstName = "Alan", lastName = "Turing", syncState = SyncState.REJECTED),
                ),
                query = "",
                isRefreshing = false,
                banner = SyncBanner(pendingCount = 1, problem = SyncProblem.OFFLINE),
            ),
            search = rememberTextFieldState(),
            onAction = {},
            modifier = Modifier.padding(Dimens.screenPadding),
        )
    }
}

@Preview
@Composable
private fun CustomersScreenEmptyPreview() {
    AppTheme {
        CustomersScreen(
            state = CustomersUiState.Success(
                customers = emptyList(),
                query = "",
                isRefreshing = false,
                banner = null,
            ),
            search = rememberTextFieldState(),
            onAction = {},
            modifier = Modifier.padding(Dimens.screenPadding),
        )
    }
}

@Preview
@Composable
private fun CustomersScreenLoadingPreview() {
    AppTheme {
        CustomersScreen(
            state = CustomersUiState.Loading,
            search = rememberTextFieldState(),
            onAction = {},
            modifier = Modifier.padding(Dimens.screenPadding),
        )
    }
}
