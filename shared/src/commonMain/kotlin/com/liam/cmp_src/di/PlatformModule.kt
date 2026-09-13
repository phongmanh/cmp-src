package com.liam.cmp_src.di

import androidx.compose.runtime.Composable
import androidx.room3.RoomDatabase
import com.liam.cmp_src.core.database.AppDatabase
import com.liam.cmp_src.core.database.RoomTokenStore
import com.liam.cmp_src.core.database.dto.TokenStoreDto
import com.liam.cmp_src.core.database.getRoomDatabase
import com.liam.cmp_src.core.network.TokenStore
import com.liam.cmp_src.core.security.TokenCipher
import org.koin.core.module.Module

/**
 * The bindings [appModule] cannot make for itself, because they differ by target.
 *
 * Composable, and remembered, for one reason: Android's `Room.databaseBuilder` needs a `Context`,
 * and the only place shared code can reach one is `LocalContext`. Koin is started inside `App()`,
 * so the module is assembled there too rather than at class-init. Every other target ignores the
 * composition and just builds its module once.
 */
@Composable
expect fun rememberPlatformModule(): Module

/**
 * The persistence graph shared by every target that has a SQLite driver.
 *
 * [builder] and [cipher] are the platform-specific pieces, and both are passed in rather than
 * bound. It keeps everything that varies by target in one signature, and it makes adding a fourth
 * SQLite target without a cipher a compile error instead of a missing-definition crash at runtime.
 * The browser targets call none of this — `androidx.sqlite:sqlite-bundled` publishes no js/wasmJs
 * variants — and bind an `InMemoryTokenStore` instead.
 */
fun Module.roomTokenStore(
    builder: () -> RoomDatabase.Builder<AppDatabase>,
    cipher: () -> TokenCipher,
) {
    single<AppDatabase> { getRoomDatabase(builder()) }
    single<TokenStoreDto> { get<AppDatabase>().tokenStoreDto }
    single<TokenCipher> { cipher() }
    single<TokenStore> { RoomTokenStore(dao = get(), cipher = get()) }
}
