package com.liam.cmp_src.feature.customers.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.liam.cmp_src.feature.customers.domain.repository.CustomerSyncScheduler
import java.util.concurrent.TimeUnit

/** The first retry's delay; WorkManager doubles it for each one after. */
private const val BACKOFF_SECONDS = 30L

/**
 * Hands each sync request to WorkManager, which runs it as soon as the device has a connection —
 * straight away when it already does — and keeps retrying with backoff, surviving process death.
 *
 * One unique chain per owner, extended with [ExistingWorkPolicy.APPEND_OR_REPLACE]: a request made
 * while a sync is running must run *after* it, because the running one already read its queue and
 * would not see the change that prompted this request. `KEEP` would drop that change until
 * something else asked; `REPLACE` would cancel a push halfway.
 */
class WorkManagerCustomerSyncScheduler(context: Context) : CustomerSyncScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun requestSync(ownerId: String) {
        val request = OneTimeWorkRequestBuilder<CustomerSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setInputData(workDataOf(CustomerSyncWorker.KEY_OWNER_ID to ownerId))
            .build()

        workManager.enqueueUniqueWork(
            "${CustomerSyncWorker.UNIQUE_NAME_PREFIX}$ownerId",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }
}
