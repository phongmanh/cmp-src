package com.liam.cmp_src.feature.customers.data.sync

import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.repository.CustomerSyncScheduler
import com.liam.cmp_src.feature.customers.domain.usecase.SyncCustomersUseCase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSUserDefaults

/**
 * The identifier the background task is registered and submitted under. Must also be listed under
 * `BGTaskSchedulerPermittedIdentifiers` in `iosApp/iosApp/Info.plist`, or registering it crashes.
 */
const val CUSTOMER_SYNC_TASK_ID = "com.liam.cmp_src.customer-sync"

/** Where the owner to sync is kept for a background launch, which has no screen to ask. */
private const val OWNER_ID_KEY = "customerSyncOwnerId"

/**
 * Syncs straight away in-process, and falls back to a background task when that cannot finish.
 *
 * Unlike WorkManager, iOS runs a submitted task whenever it judges best — often hours later — so
 * waiting on it alone would leave an online device's change sitting unsent. The in-process attempt
 * covers the usual case; the [BGProcessingTaskRequest] covers going offline, or the app being
 * closed, before it succeeds.
 *
 * The scope lives as long as this `single` does, which is the process: a sync started from a
 * screen should outlive that screen.
 */
class BackgroundTaskCustomerSyncScheduler(
    private val syncCustomers: SyncCustomersUseCase,
) : CustomerSyncScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun requestSync(ownerId: String) {
        NSUserDefaults.standardUserDefaults.setObject(ownerId, forKey = OWNER_ID_KEY)
        scope.launch {
            if (syncCustomers(ownerId).needsRetry()) submitBackgroundSync()
        }
    }
}

/**
 * Registers the handler iOS calls when it runs [CUSTOMER_SYNC_TASK_ID]. Must run before the app
 * finishes launching, which is why `shared`'s `setUpApp()` calls it from `iOSApp.init()`.
 */
fun registerCustomerBackgroundSync() {
    BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
        identifier = CUSTOMER_SYNC_TASK_ID,
        usingQueue = null,
    ) { task -> task?.let(::runBackgroundSync) }
}

private object BackgroundSyncGraph : KoinComponent

private fun runBackgroundSync(task: BGTask) {
    val ownerId = NSUserDefaults.standardUserDefaults.stringForKey(OWNER_ID_KEY)
    if (ownerId == null) {
        task.setTaskCompletedWithSuccess(true)
        return
    }

    val syncCustomers: SyncCustomersUseCase = BackgroundSyncGraph.get()
    val job = CoroutineScope(Dispatchers.Default).launch {
        val outcome = syncCustomers(ownerId)
        if (outcome.needsRetry()) submitBackgroundSync()
        task.setTaskCompletedWithSuccess(!outcome.needsRetry())
    }
    // iOS takes the time back when it wants it; a cancelled sync leaves every row consistent, and
    // what it did not send is still queued for the next attempt.
    task.expirationHandler = {
        job.cancel()
        submitBackgroundSync()
        task.setTaskCompletedWithSuccess(false)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun submitBackgroundSync() {
    val request = BGProcessingTaskRequest(identifier = CUSTOMER_SYNC_TASK_ID).apply {
        requiresNetworkConnectivity = true
    }
    // Refused in the simulator and when background refresh is off; the next launch syncs instead.
    BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
}

/** Worth trying again later — as opposed to done, or waiting on the right account to sign in. */
private fun SyncOutcome.needsRetry(): Boolean =
    this == SyncOutcome.Offline || this == SyncOutcome.Failed
