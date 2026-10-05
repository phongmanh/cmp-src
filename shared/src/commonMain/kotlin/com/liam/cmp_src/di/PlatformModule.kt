package com.liam.cmp_src.di

import androidx.room3.RoomDatabase
import com.liam.cmp_src.core.database.AppDatabase
import com.liam.cmp_src.core.database.RoomTokenStore
import com.liam.cmp_src.core.database.dto.CustomerDao
import com.liam.cmp_src.core.database.dto.TokenStoreDto
import com.liam.cmp_src.core.database.getRoomDatabase
import com.liam.cmp_src.core.network.TokenStore
import com.liam.cmp_src.core.security.TokenCipher
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatform

/**
 * Starts the app's object graph: [appModule] plus [platformModule], the bindings that differ by
 * target (`androidPlatformModule(context)`, `iosPlatformModule()`).
 *
 * Called by each platform's entry point *before* any UI — the Android `Application`, iOS's
 * `setUpApp()` — never from inside `App()`. The customer sync runs from WorkManager and
 * BGTaskScheduler, which can wake the app with no screen at all, and they reach the graph through
 * the global Koin instance; a graph that only existed inside a composition would not be there.
 *
 * Safe to call twice: a second call, as a configuration change or an iOS background launch
 * followed by a foreground one can cause, keeps the graph already running.
 */
fun initKoin(platformModule: Module) {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin { modules(appModule, platformModule) }
}

/**
 * The persistence graph both targets build on.
 *
 * The daos are bound off [AppDatabase] rather than constructed, so every caller reads and writes
 * through the single connection pool the database binding below owns.
 *
 * [builder] and [cipher] are the platform-specific pieces, and both are passed in rather than
 * bound. It keeps everything that varies by target in one signature, and it makes adding a third
 * SQLite target without a cipher a compile error instead of a missing-definition crash at
 * runtime.
 */
fun Module.roomPersistence(
    builder: () -> RoomDatabase.Builder<AppDatabase>,
    cipher: () -> TokenCipher,
) {
    single<AppDatabase> { getRoomDatabase(builder()) }
    single<TokenStoreDto> { get<AppDatabase>().tokenStoreDto }
    single<CustomerDao> { get<AppDatabase>().customerDAO }
    single<TokenCipher> { cipher() }
    single<TokenStore> { RoomTokenStore(dao = get(), cipher = get()) }
}
