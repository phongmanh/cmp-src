package com.liam.cmp_src.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.liam.cmp_src.core.database.dto.TokenStoreDto
import com.liam.cmp_src.core.database.entities.EncryptedAuthTokens
import kotlinx.coroutines.Dispatchers

/** The database file name every platform builder resolves against its own app directory. */
internal const val DATABASE_NAME = "app.db"

/**
 * Bumped to 2 when the two plaintext token columns became one encrypted blob.
 *
 * Any version 1 row is plaintext that no key can read, so there is nothing in it worth
 * migrating — see the destructive fallback in [getRoomDatabase].
 */
internal const val DATABASE_VERSION = 2

/**
 * Finishes a platform's [builder] into a usable database.
 *
 * The driver is *not* set here: `androidx.sqlite:sqlite-bundled` publishes no js/wasmJs variants,
 * so it cannot live in `commonMain`. Each platform's `getDatabaseBuilder` picks its own driver and
 * this function only applies the settings that are the same everywhere.
 *
 * `Dispatchers.Default` rather than `Dispatchers.IO`, which does not exist on the JS and Wasm
 * targets — the same reason `AppModule` injects `Default`.
 *
 * The destructive fallback is safe *only* while this database holds nothing but a session the
 * user can recreate by signing in again. **Replace it with a real migration before adding a
 * second table**, or the first schema change after that will silently delete real user data.
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}

@Database(entities = [EncryptedAuthTokens::class], version = DATABASE_VERSION, exportSchema = false)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val tokenStoreDto: TokenStoreDto
}

/**
 * Room's KSP processor generates the `actual object` for every target, which is why the missing
 * actuals are suppressed rather than written by hand.
 */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
