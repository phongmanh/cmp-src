package com.liam.cmp_src.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.liam.cmp_src.core.network.AuthTokens
import com.liam.cmp_src.core.security.FileKeyTokenCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the real schema and the real desktop cipher against a real SQLite file rather than a
 * fake DAO: what is worth testing here is that the entity, the single-row key, the queries and the
 * encryption all agree, and a fake would assert none of that. JVM-only because it needs a driver,
 * and `sqlite-bundled` has no web build.
 *
 * Both the database and the key live in a temp directory, so a test run never reads or writes the
 * developer's own key at `~/.cmpsrc/token.key`.
 */
class RoomTokenStoreTest {

    private val directory: File = File.createTempFile("cmpsrc-token-store", "")
        .also { it.delete(); it.mkdirs() }

    private val databaseFile = File(directory, "app.db")
    private val keyFile = File(directory, "token.key")

    private var database = openDatabase()
    private var store = RoomTokenStore(database.tokenStoreDto, FileKeyTokenCipher(keyFile))

    private fun openDatabase() = Room.databaseBuilder<AppDatabase>(name = databaseFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

    @AfterTest
    fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    @Test
    fun `current is null before anything is stored`() = runTest {
        assertNull(store.current())
    }

    @Test
    fun `current returns what was saved`() = runTest {
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))

        assertEquals(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN), store.current())
    }

    @Test
    fun `a second save replaces the session rather than adding a row`() = runTest {
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        store.save(AuthTokens("refreshed-access", "refreshed-refresh"))

        assertEquals(AuthTokens("refreshed-access", "refreshed-refresh"), store.current())
    }

    @Test
    fun `clear leaves no session behind`() = runTest {
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        store.clear()

        assertNull(store.current())
    }

    @Test
    fun `the tokens flow reports the stored session`() = runTest {
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))

        assertEquals(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN), store.tokens.first())
    }

    @Test
    fun `the stored column holds neither token in the clear`() = runTest {
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))

        val payload = requireNotNull(database.tokenStoreDto.tokens().first()).payload
        assertFalse(payload.contains(ACCESS_TOKEN))
        assertFalse(payload.contains(REFRESH_TOKEN))
    }

    /**
     * The column assertion above would still pass if SQLite had left a copy of the plaintext in a
     * free page or the write-ahead log, which a `SELECT` cannot see. This reads the bytes instead.
     */
    @Test
    fun `no file on disk holds either token in the clear`() = runTest {
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        database.close()

        val files = requireNotNull(directory.listFiles()).filter { it.name.startsWith("app.db") }
        assertTrue(files.isNotEmpty(), "expected the database file to exist")
        for (file in files) {
            val contents = file.readBytes().toString(Charsets.ISO_8859_1)
            assertFalse(contents.contains(ACCESS_TOKEN), "${file.name} holds the access token")
            assertFalse(contents.contains(REFRESH_TOKEN), "${file.name} holds the refresh token")
        }
    }

    @Test
    fun `a session survives reopening both the database and the cipher`() = runTest {
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        database.close()

        database = openDatabase()
        store = RoomTokenStore(database.tokenStoreDto, FileKeyTokenCipher(keyFile))

        assertEquals(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN), store.current())
    }

    /** The case a restored backup produces: the ciphertext comes back, the key does not. */
    @Test
    fun `losing the key reads as signed out rather than crashing`() = runTest {
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        database.close()
        keyFile.delete()

        database = openDatabase()
        store = RoomTokenStore(database.tokenStoreDto, FileKeyTokenCipher(keyFile))

        assertNull(store.current())
    }

    private companion object {
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
    }
}
