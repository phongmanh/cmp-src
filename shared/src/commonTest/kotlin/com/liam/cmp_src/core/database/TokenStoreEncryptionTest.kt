package com.liam.cmp_src.core.database

import com.liam.cmp_src.core.database.entities.EncryptedAuthTokens
import com.liam.cmp_src.core.network.AuthTokens
import com.liam.cmp_src.core.security.TokenCipherException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * What [RoomTokenStore] does with a cipher, on every target — against a [FakeTokenStoreDto],
 * because `commonTest` has no SQLite driver to run the real schema on. The platform test source
 * sets cover the real database.
 */
class TokenStoreEncryptionTest {

    @Test
    fun `a saved session reads back as plaintext`() = runTest {
        val store = RoomTokenStore(FakeTokenStoreDto(), FakeTokenCipher())

        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))

        assertEquals(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN), store.current())
    }

    @Test
    fun `what reaches the dao holds neither token in the clear`() = runTest {
        val dao = FakeTokenStoreDto()
        val store = RoomTokenStore(dao, FakeTokenCipher())

        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))

        val payload = requireNotNull(dao.stored).payload
        assertFalse(payload.contains(ACCESS_TOKEN), "the access token reached storage in the clear")
        assertFalse(payload.contains(REFRESH_TOKEN), "the refresh token reached storage in the clear")
    }

    @Test
    fun `the pair is encrypted once rather than once per token`() = runTest {
        val cipher = FakeTokenCipher()
        val store = RoomTokenStore(FakeTokenStoreDto(), cipher)

        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))

        assertEquals(1, cipher.encryptCount)
    }

    @Test
    fun `a row that will not decrypt reads as signed out on both paths`() = runTest {
        val cipher = FakeTokenCipher()
        val dao = FakeTokenStoreDto()
        val store = RoomTokenStore(dao, cipher)
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))

        cipher.failDecrypt = true

        assertNull(store.current())
        assertNull(store.tokens.first())
    }

    @Test
    fun `a row that will not decrypt is dropped rather than re-read forever`() = runTest {
        val cipher = FakeTokenCipher()
        val dao = FakeTokenStoreDto()
        val store = RoomTokenStore(dao, cipher)
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))

        cipher.failDecrypt = true
        store.current()

        assertNull(dao.stored, "the undecryptable row should have been cleared")
    }

    @Test
    fun `a row written before encryption existed reads as signed out`() = runTest {
        // Exactly what an older build left behind: the plaintext token, unwrapped.
        val dao = FakeTokenStoreDto(EncryptedAuthTokens(payload = ACCESS_TOKEN))

        assertNull(RoomTokenStore(dao, FakeTokenCipher()).current())
    }

    @Test
    fun `a cipher that cannot encrypt stops the save instead of storing plaintext`() = runTest {
        val dao = FakeTokenStoreDto()
        val store = RoomTokenStore(dao, FakeTokenCipher(failEncrypt = true))

        assertFailsWith<TokenCipherException> {
            store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        }
        assertNull(dao.stored, "nothing should have been written")
    }

    @Test
    fun `a second save replaces the session rather than adding a row`() = runTest {
        val store = RoomTokenStore(FakeTokenStoreDto(), FakeTokenCipher())

        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        store.save(AuthTokens("refreshed-access", "refreshed-refresh"))

        assertEquals(AuthTokens("refreshed-access", "refreshed-refresh"), store.current())
    }

    @Test
    fun `clear leaves no session behind`() = runTest {
        val dao = FakeTokenStoreDto()
        val store = RoomTokenStore(dao, FakeTokenCipher())
        store.save(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))

        store.clear()

        assertNull(store.current())
        assertNull(dao.stored)
    }

    private companion object {
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
    }
}
