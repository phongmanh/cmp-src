package com.liam.cmp_src.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.liam.cmp_src.core.database.dto.CustomerDao
import com.liam.cmp_src.core.database.dto.TokenStoreDto
import com.liam.cmp_src.core.database.entities.Customer
import com.liam.cmp_src.core.database.entities.EncryptedAuthTokens
import kotlinx.coroutines.Dispatchers

/** The database file name every platform builder resolves against its own app directory. */
internal const val DATABASE_NAME = "app.db"

/**
 * The schema version this build expects on disk.
 *
 * - 2 — the two plaintext token columns became one encrypted blob.
 * - 3 — added the `customers` table.
 *
 * A bump here needs a matching entry in [APP_MIGRATIONS]; nothing but a version 1 file is
 * recreated from scratch any more.
 */
internal const val DATABASE_VERSION = 3

/**
 * Finishes a platform's [builder] into a usable database.
 *
 * The driver is *not* set here: each platform's `getDatabaseBuilder` picks its own driver and
 * resolves its own database path, and this function only applies the settings that are the same
 * on both.
 *
 * Queries run on `Dispatchers.Default`, the same dispatcher `AppModule` injects, so nothing in
 * the app has a second opinion about which pool touches the database.
 *
 * Every version from 2 onwards migrates through [APP_MIGRATIONS], so a schema change keeps the
 * rows that are already on the device. Version 1 is the one exception: its only table held
 * plaintext tokens that no key in this build can read, so such a file is dropped and recreated —
 * the user signs in again and loses nothing else. That fallback is scoped to version 1 on purpose.
 * A blanket `fallbackToDestructiveMigration` would answer a missing migration by deleting the
 * customer rows instead of failing, which is not a trade this database can make any more.
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .addMigrations(*APP_MIGRATIONS)
        .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}

@Database(entities = [EncryptedAuthTokens::class, Customer::class], version = DATABASE_VERSION, exportSchema = true)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val tokenStoreDto: TokenStoreDto
    abstract val customerDAO: CustomerDao
}

/**
 * Room's KSP processor generates the `actual object` for every target, which is why the missing
 * actuals are suppressed rather than written by hand.
 */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
