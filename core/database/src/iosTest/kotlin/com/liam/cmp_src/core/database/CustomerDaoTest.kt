package com.liam.cmp_src.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.liam.cmp_src.core.database.dto.CustomerDao
import com.liam.cmp_src.core.database.entities.Customer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Exercises [CustomerDao] against a real SQLite file on the simulator, via `iosSimulatorArm64Test`.
 *
 * Lives in `iosTest` for the reason the migration test next door does: `commonTest` has no SQLite
 * driver to open a file with, so the daos can only be run where one exists.
 *
 * What is worth pinning here is the tombstone contract — a soft-deleted customer is invisible to
 * every read but still on the device — because the two halves are written in different places (the
 * `WHERE deletedAt IS NULL` on each query, the `UPDATE` in [CustomerDao.softDelete]) and nothing
 * but a test notices when only one of them is changed.
 */
class CustomerDaoTest {

    private val databasePath = "${NSTemporaryDirectory()}customer-dao-test-${Random.nextLong()}.db"
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
    fun `the list holds every customer the table has and puts the newest first`() = runTest {
        val dao = openDao()
        listOf(MIDDLE, NEWEST, OLDEST).forEach { dao.save(it) }

        assertEquals(listOf(NEWEST, MIDDLE, OLDEST), dao.customers().first())
    }

    @Test
    fun `customers created in the same instant are ordered by id`() = runTest {
        val dao = openDao()
        val second = customer(id = "customer-b", createdAt = OLDEST.createdAt)
        val first = customer(id = "customer-a", createdAt = OLDEST.createdAt)
        listOf(second, first).forEach { dao.save(it) }

        assertEquals(listOf(first, second), dao.customers().first())
    }

    @Test
    fun `a saved customer reads back as it was written`() = runTest {
        val dao = openDao()

        dao.save(NEWEST)

        assertEquals(NEWEST, dao.customerOf(NEWEST.id))
    }

    @Test
    fun `a tombstoned customer is left out of the list`() = runTest {
        val dao = openDao()
        listOf(NEWEST, OLDEST).forEach { dao.save(it) }

        dao.softDelete(NEWEST.id, DELETED_AT)

        assertEquals(listOf(OLDEST), dao.customers().first())
    }

    @Test
    fun `a tombstoned customer cannot be read back by id`() = runTest {
        val dao = openDao()
        dao.save(NEWEST)

        dao.softDelete(NEWEST.id, DELETED_AT)

        assertNull(dao.customerOf(NEWEST.id))
    }

    @Test
    fun `a tombstone keeps the row and records when it happened`() = runTest {
        val dao = openDao()
        dao.save(NEWEST)

        dao.softDelete(NEWEST.id, DELETED_AT)

        assertEquals(listOf(DELETED_AT to DELETED_AT), storedStamps(NEWEST.id))
    }

    @Test
    fun `a second soft delete leaves the first tombstone alone`() = runTest {
        val dao = openDao()
        dao.save(NEWEST)
        dao.softDelete(NEWEST.id, DELETED_AT)

        dao.softDelete(NEWEST.id, LATER_THAN_DELETED_AT)

        assertEquals(listOf(DELETED_AT to DELETED_AT), storedStamps(NEWEST.id))
    }

    @Test
    fun `saving over a tombstone brings the customer back`() = runTest {
        val dao = openDao()
        dao.save(NEWEST)
        dao.softDelete(NEWEST.id, DELETED_AT)

        dao.save(NEWEST)

        assertEquals(NEWEST, dao.customerOf(NEWEST.id))
    }

    @Test
    fun `purging a tombstoned customer takes the row off the device`() = runTest {
        val dao = openDao()
        dao.save(NEWEST)
        dao.softDelete(NEWEST.id, DELETED_AT)

        dao.purge(NEWEST.id)

        assertEquals(emptyList(), storedStamps(NEWEST.id))
    }

    /** Purging is the reconciliation path, so it answers for a row that was never tombstoned too. */
    @Test
    fun `purging a customer that was never tombstoned still takes the row off the device`() = runTest {
        val dao = openDao()
        dao.save(NEWEST)

        dao.purge(NEWEST.id)

        assertEquals(emptyList(), storedStamps(NEWEST.id))
    }

    /** Opens [databasePath] through the same builder settings the app uses. */
    private fun openDao(): CustomerDao =
        getRoomDatabase(
            Room.databaseBuilder<AppDatabase>(name = databasePath).setDriver(BundledSQLiteDriver()),
        ).also { database = it }.customerDAO

    /**
     * The `updatedAt`/`deletedAt` pairs the table holds for [id], tombstoned rows included.
     *
     * A second connection, because that is the only way to see what the daos hide: every read on
     * [CustomerDao] filters tombstones out, so asserting that a soft-deleted row is still on the
     * device cannot go through them. Room has committed by the time a test calls this, so the read
     * is not racing the write it is checking.
     */
    private fun storedStamps(id: String): List<Pair<String, String?>> =
        BundledSQLiteDriver().open(databasePath).use { connection ->
            connection.prepare("SELECT updatedAt, deletedAt FROM customers WHERE id = ?").use { statement ->
                statement.bindText(1, id)
                buildList {
                    while (statement.step()) {
                        add(statement.getText(0) to statement.getText(1).takeUnless { statement.isNull(1) })
                    }
                }
            }
        }

    private companion object {

        const val DELETED_AT = "2026-09-17T09:00:00Z"
        const val LATER_THAN_DELETED_AT = "2026-09-17T10:30:00Z"

        /**
         * A customer with every column filled, so a query that drops or misnames one fails here.
         * Only [id] and [createdAt] vary, because those are what the list's order turns on.
         */
        fun customer(id: String, createdAt: String) = Customer(
            id = id,
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
            notes = "Saved while offline.",
            status = "active",
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null,
        )

        val NEWEST = customer(id = "customer-newest", createdAt = "2026-09-16T12:00:00Z")
        val MIDDLE = customer(id = "customer-middle", createdAt = "2026-09-15T12:00:00Z")
        val OLDEST = customer(id = "customer-oldest", createdAt = "2026-09-14T12:00:00Z")
    }
}
