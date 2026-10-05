package com.liam.cmp_src.feature.customers.di

import com.liam.cmp_src.feature.customers.data.CustomerRepositoryImpl
import com.liam.cmp_src.feature.customers.data.remote.CustomerApi
import com.liam.cmp_src.feature.customers.data.sync.customerSyncPlatformModule
import com.liam.cmp_src.feature.customers.domain.repository.CustomerRepository
import com.liam.cmp_src.feature.customers.domain.usecase.DeleteCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.GetCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ObserveCustomersUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ObservePendingChangesUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.SaveCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.SyncCustomersUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ValidateCustomerUseCase
import com.liam.cmp_src.feature.customers.editor.CustomerEditorViewModel
import com.liam.cmp_src.feature.customers.list.CustomersViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The customers feature's graph: the offline-first list, the editor, and the sync between them and
 * the server.
 *
 * Expects the app graph to provide the `HttpClient`, the `CoroutineDispatcher` and the
 * `CustomerDao` — and, on Android, the application `Context` WorkManager is built from.
 *
 * The repository is a `single` because its sync lock has to be one lock for the whole process: the
 * list, pull-to-refresh and the background job must never push the same change twice.
 *
 * Both ViewModels take their ids as parameters (`koinViewModel { parametersOf(...) }`) — they come
 * from the app shell's navigation keys, which this module never sees.
 */
val customersModule = module {
    includes(customerSyncPlatformModule)

    single { CustomerApi(client = get()) }
    single<CustomerRepository> { CustomerRepositoryImpl(api = get(), dao = get(), dispatcher = get()) }

    factoryOf(::ObserveCustomersUseCase)
    factoryOf(::ObservePendingChangesUseCase)
    factoryOf(::GetCustomerUseCase)
    factoryOf(::SaveCustomerUseCase)
    factoryOf(::DeleteCustomerUseCase)
    factoryOf(::SyncCustomersUseCase)
    factoryOf(::ValidateCustomerUseCase)

    viewModel { (ownerId: String) ->
        CustomersViewModel(
            ownerId = ownerId,
            observeCustomers = get(),
            observePendingChanges = get(),
            syncCustomers = get(),
            deleteCustomer = get(),
        )
    }
    viewModel { (ownerId: String, customerId: String?) ->
        CustomerEditorViewModel(
            ownerId = ownerId,
            customerId = customerId,
            getCustomer = get(),
            saveCustomer = get(),
            deleteCustomer = get(),
            validateCustomer = get(),
        )
    }
}
