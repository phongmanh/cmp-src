package com.liam.cmp_src.feature.customers.data.sync

import com.liam.cmp_src.feature.customers.domain.repository.CustomerSyncScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val customerSyncPlatformModule: Module = module {
    single<CustomerSyncScheduler> { BackgroundTaskCustomerSyncScheduler(syncCustomers = get()) }
}
