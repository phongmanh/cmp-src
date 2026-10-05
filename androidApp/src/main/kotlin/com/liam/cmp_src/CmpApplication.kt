package com.liam.cmp_src

import android.app.Application
import com.liam.cmp_src.di.initAndroidApp

/**
 * Starts the Koin graph before anything else in the process runs.
 *
 * Here rather than in [MainActivity] because WorkManager can start the process for a customer
 * sync with no Activity at all, and its worker resolves what it runs from this same graph.
 */
class CmpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAndroidApp(this)
    }
}
