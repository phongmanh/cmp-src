package com.liam.cmp_src.feature.customers.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.usecase.SyncCustomersUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Runs one customer sync for the owner in its input data.
 *
 * Built by WorkManager's default factory, possibly in a process the UI never started, so it reaches
 * the graph through [KoinComponent] — which is why Koin is started by the `Application` rather
 * than inside `App()`.
 */
class CustomerSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val syncCustomers: SyncCustomersUseCase by inject()

    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(KEY_OWNER_ID) ?: return Result.failure()
        return when (syncCustomers(ownerId)) {
            // A mismatched session will not fix itself by retrying; the owner's next sign-in and
            // visit to the list will sync instead.
            SyncOutcome.Synced, SyncOutcome.SessionMismatch -> Result.success()
            SyncOutcome.Offline, SyncOutcome.Failed ->
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_OWNER_ID = "ownerId"
        const val UNIQUE_NAME_PREFIX = "customer-sync-"

        /** With exponential backoff from 30 s this spans several hours before giving up. */
        private const val MAX_ATTEMPTS = 10
    }
}
