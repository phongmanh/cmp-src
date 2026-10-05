package com.liam.cmp_src

import com.liam.cmp_src.di.initKoin
import com.liam.cmp_src.di.iosPlatformModule
import com.liam.cmp_src.feature.customers.data.sync.registerCustomerBackgroundSync

/**
 * Everything the app needs before its first screen, called from `iOSApp.init()` as
 * `AppSetupKt.setUpApp()`.
 *
 * `init()` rather than `ContentView`, because iOS can launch the app in the background to run the
 * customer sync without ever building a scene — and BGTaskScheduler insists its handlers are
 * registered before launch finishes.
 */
fun setUpApp() {
    initKoin(iosPlatformModule())
    registerCustomerBackgroundSync()
}
