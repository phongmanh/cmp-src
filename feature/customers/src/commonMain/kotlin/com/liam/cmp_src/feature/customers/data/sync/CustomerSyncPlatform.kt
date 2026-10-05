package com.liam.cmp_src.feature.customers.data.sync

import org.koin.core.module.Module

/**
 * Binds the platform's `CustomerSyncScheduler`: WorkManager on Android, BGTaskScheduler on iOS.
 *
 * Both outlive the screen that asked — and, once the platform wakes the app for them, the process
 * too — which is why each resolves what it runs from the global Koin graph rather than being handed
 * it. That graph therefore has to be started outside composition; see `initKoin` in `shared`.
 */
internal expect val customerSyncPlatformModule: Module
