package com.liam.cmp_src.core.database

import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.liam.cmp_src.core.database.entities.Customer
import com.liam.cmp_src.core.database.entities.EncryptedAuthTokens
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Runs the real migration chain against a real SQLite file on the simulator, via
 * `iosSimulatorArm64Test`.
 *
 * This lives in `iosTest` rather than `commonTest` for the reason the module's other schema test
 * does: `commonTest` has no SQLite driver to open a file with. Unlike the Keychain tests next door
 * these need no entitlement, so they do run under Gradle.
 *
 * Every test gets its own randomly named file in the temporary directory. A shared name would let
 * one test's migrated database satisfy the next test's assertions without migrating anything.
 */
class AppDatabaseMigrationTest {

    private val databasePath = "${NSTemporaryDirectory()}migration-test-${Random.nextLong()}.db"
    private var database: AppDatabase? = null

    @OptIn(ExperimentalForeignApi::class)
    @AfterTest
    fun tearDown() {
        database?.close()
        // The journal and shared-memory sidecars are separate files; leaving them behind would
        // hand the next run a half-open database.
        listOf("", "-wal", "-shm").forEach { suffix ->
            NSFileManager.defaultManager.removeItemAtPath("$databasePath$suffix", error = null)
        }
    }

    @Test
    fun `a version 2 database keeps its session through the upgrade`() = runTest {
        seedVersion2Database(payload = STORED_PAYLOAD)

        val tokens = openDatabase().tokenStoreDto.tokens().first()

        assertEquals(STORED_PAYLOAD, assertNotNull(tokens).payload)
    }

    /**
     * The failure this guards against is the quiet one: [MIGRATION_2_3] creating a `customers`
     * table that is *almost* the one the entity describes. Room validates the schema after
     * migrating, so a mismatch fails here instead of on a user's device.
     */
    @Test
    fun `a migrated database accepts the customer rows a fresh one does`() = runTest {
        seedVersion2Database(payload = STORED_PAYLOAD)
        val database = openDatabase()

        database.customerDAO.save(CUSTOMER)

        assertEquals(CUSTOMER, database.customerDAO.customerOf(CUSTOMER.id))
    }

    @Test
    fun `a fresh database is created at the current version`() = runTest {
        val database = openDatabase()

        database.customerDAO.save(CUSTOMER)

        assertEquals(CUSTOMER, database.customerDAO.customerOf(CUSTOMER.id))
        assertEquals(DATABASE_VERSION, userVersionOf(databasePath))
    }

    /** Opens [databasePath] through the same builder settings the app uses. */
    private fun openDatabase(): AppDatabase =
        getRoomDatabase(
            Room.databaseBuilder<AppDatabase>(name = databasePath).setDriver(BundledSQLiteDriver()),
        ).also { database = it }

    /**
     * Writes the version 2 schema by hand — the table Room exported before `customers` existed,
     * plus one session row and the `user_version` that makes Room treat the file as version 2.
     *
     * `room_master_table` is deliberately absent: Room only checks the stored identity hash once a
     * file is already at the current version, so an upgrade path does not need one, and writing a
     * hash by hand here would be a second copy of a value only Room should produce.
     */
    private fun seedVersion2Database(payload: String) {
        BundledSQLiteDriver().open(databasePath).use { connection ->
            connection.execSQL(VERSION_2_AUTH_TOKEN_TABLE)
            connection.prepare("INSERT INTO `authToken` (`payload`, `id`) VALUES (?, ?)").use {
                it.bindText(1, payload)
                it.bindInt(2, EncryptedAuthTokens.ROW_ID)
                it.step()
            }
            connection.execSQL("PRAGMA user_version = 2")
        }
    }

    private fun userVersionOf(path: String): Int =
        BundledSQLiteDriver().open(path).use { connection: SQLiteConnection ->
            connection.prepare("PRAGMA user_version").use {
                it.step()
                it.getInt(0)
            }
        }

    private companion object {

        /** Exactly what Room exported for `authToken` at version 2, with the table name resolved. */
        const val VERSION_2_AUTH_TOKEN_TABLE =
            "CREATE TABLE IF NOT EXISTS `authToken` " +
                "(`payload` TEXT NOT NULL, `id` INTEGER NOT NULL, PRIMARY KEY(`id`))"

        /** Stands in for a session: opaque ciphertext as far as the database is concerned. */
        const val STORED_PAYLOAD = "v2-ciphertext-that-must-survive"

        /** Every nullable column is filled, so a dropped column fails rather than reading null. */
        val CUSTOMER = Customer(
            id = "customer-1",
            ownerId = "owner-1",
            firstName = "Ada",
            lastName = "Lovelace",
            companyName = "Analytical Engines",
            email = "ada@example.com",
            phone = "+44 20 7946 0958",
            addressLine1 = "12 Queen Square",
            addressLine2 = "Floor 2",
            city = "London",
            region = "Greater London",
            postalCode = "WC1N 3AR",
            countryCode = "GB",
            notes = "First customer on the migrated table.",
            status = "active",
            createdAt = "2026-09-16T00:00:00Z",
            updatedAt = "2026-09-16T00:00:00Z",
            deletedAt = null,
        )
    }
}
