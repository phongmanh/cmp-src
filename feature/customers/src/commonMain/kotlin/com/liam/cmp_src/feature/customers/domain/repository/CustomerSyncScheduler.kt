package com.liam.cmp_src.feature.customers.domain.repository

/**
 * Asks the platform to run a customer sync as soon as it sensibly can — now if the device is
 * online, otherwise once it is, even if the app has been closed in the meantime.
 *
 * Android hands this to WorkManager and iOS to BGTaskScheduler; both actuals live in this module's
 * `data.sync` package. Fire and forget: the caller has already stored its change, and the list
 * learns the outcome by watching the pending count fall.
 */
fun interface CustomerSyncScheduler {
    fun requestSync(ownerId: String)
}
