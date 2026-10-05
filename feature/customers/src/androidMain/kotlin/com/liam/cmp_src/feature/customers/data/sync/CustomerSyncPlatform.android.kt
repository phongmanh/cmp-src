package com.liam.cmp_src.feature.customers.data.sync

import android.content.Context
import com.liam.cmp_src.feature.customers.domain.repository.CustomerSyncScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

/** Expects the app graph to bind the application [Context] — see `androidPlatformModule`. */
internal actual val customerSyncPlatformModule: Module = module {
    single<CustomerSyncScheduler> { WorkManagerCustomerSyncScheduler(context = get()) }
}
