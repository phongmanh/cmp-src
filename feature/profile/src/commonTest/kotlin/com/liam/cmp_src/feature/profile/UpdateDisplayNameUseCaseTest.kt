package com.liam.cmp_src.feature.profile

import com.liam.cmp_src.core.testing.FakeAuthRepository
import com.liam.cmp_src.feature.profile.domain.usecase.UpdateDisplayNameUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The whole reason this use case exists is the avatar field.
 *
 * `PUT /users/me` replaces the profile outright and retires an uploaded picture whatever it is
 * sent, so echoing a stored address back would leave the account pointing at an image the same
 * request had just deleted. A provider's address is untouched by the write and has to survive.
 */
class UpdateDisplayNameUseCaseTest {

    @Test
    fun `sends the typed name trimmed`() = runTest {
        val repository = FakeProfileRepository()
        val update = UpdateDisplayNameUseCase(repository)

        update(displayName = "  Ada Lovelace  ", current = FakeAuthRepository.TEST_USER)

        assertEquals("Ada Lovelace", repository.lastDisplayName)
    }

    /** The server rejects a blank string, so an emptied field is sent as the clear it means. */
    @Test
    fun `an emptied field clears the name rather than sending whitespace`() = runTest {
        val repository = FakeProfileRepository()
        val update = UpdateDisplayNameUseCase(repository)

        update(displayName = "   ", current = FakeAuthRepository.TEST_USER)

        assertTrue(repository.sawUpdate)
        assertNull(repository.lastDisplayName)
    }

    @Test
    fun `an uploaded picture is not echoed back at an address the write is about to delete`() =
        runTest {
            val repository = FakeProfileRepository()
            val update = UpdateDisplayNameUseCase(repository)

            update(
                displayName = "Ada",
                current = FakeProfileRepository.USER_WITH_UPLOADED_PHOTO,
            )

            assertNull(repository.lastAvatarUrl)
        }

    @Test
    fun `a provider's picture is echoed back so it survives the write`() = runTest {
        val repository = FakeProfileRepository()
        val update = UpdateDisplayNameUseCase(repository)

        update(
            displayName = "Ada",
            current = FakeProfileRepository.USER_WITH_PROVIDER_PHOTO,
        )

        assertEquals(
            FakeProfileRepository.USER_WITH_PROVIDER_PHOTO.avatarUrl,
            repository.lastAvatarUrl,
        )
    }

    @Test
    fun `an account with no picture sends none`() = runTest {
        val repository = FakeProfileRepository()
        val update = UpdateDisplayNameUseCase(repository)

        update(displayName = "Ada", current = FakeAuthRepository.TEST_USER)

        assertNull(repository.lastAvatarUrl)
    }
}
