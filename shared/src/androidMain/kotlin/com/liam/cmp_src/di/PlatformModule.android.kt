package com.liam.cmp_src.di

import android.content.Context
import com.liam.cmp_src.core.database.getDatabaseBuilder
import com.liam.cmp_src.core.security.KeystoreTokenCipher
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android's half of the graph: the Room file and the Keystore cipher, plus the application
 * [Context] itself, which WorkManager is built from.
 *
 * [context] must be the application context, not an Activity's: everything here outlives every
 * screen.
 */
fun androidPlatformModule(context: Context): Module = module {
    single<Context> { context }
    roomPersistence(builder = { getDatabaseBuilder(context) }, cipher = { KeystoreTokenCipher() })
}

/** What the `Application` calls first: the graph, built on its [context]. */
fun initAndroidApp(context: Context) = initKoin(androidPlatformModule(context.applicationContext))
