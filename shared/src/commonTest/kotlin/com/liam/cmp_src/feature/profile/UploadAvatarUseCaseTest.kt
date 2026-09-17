package com.liam.cmp_src.feature.profile

import com.example.api.common.FieldLimits
import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.profile.domain.usecase.UploadAvatarUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Both checks here are the server's own, made early to save an upload that was going to be
 * refused — and on this route a refused upload still spends one of the five the account gets a
 * minute, so the saving is real.
 */
class UploadAvatarUseCaseTest {

    @Test
    fun `a jpeg is sent with the type read from its bytes`() = runTest {
        val repository = FakeProfileRepository()
        val upload = UploadAvatarUseCase(repository)

        upload(bytes = JPEG, fileName = "face.jpg")

        assertEquals(1, repository.uploadCallCount)
        assertEquals("image/jpeg", repository.lastContentType)
        assertEquals("face.jpg", repository.lastFileName)
    }

    /** The filename says PNG and the bytes say otherwise; the bytes win, as they do on the server. */
    @Test
    fun `a file that is not a picture never leaves the device`() = runTest {
        val repository = FakeProfileRepository()
        val upload = UploadAvatarUseCase(repository)

        val result = upload(bytes = "GIF89a...".encodeToByteArray(), fileName = "face.png")

        val failure = assertIs<AuthResult.Failure>(result)
        assertEquals(AuthError.UnsupportedImage, failure.error)
        assertEquals(0, repository.uploadCallCount)
    }

    @Test
    fun `a file past the cap never leaves the device and says what the cap is`() = runTest {
        val repository = FakeProfileRepository()
        val upload = UploadAvatarUseCase(repository)
        val tooBig = JPEG + ByteArray(FieldLimits.MAX_AVATAR_BYTES.toInt())

        val result = upload(bytes = tooBig, fileName = "face.jpg")

        val failure = assertIs<AuthResult.Failure>(result)
        assertEquals(AuthError.ImageTooLarge(FieldLimits.MAX_AVATAR_BYTES), failure.error)
        assertEquals(0, repository.uploadCallCount)
    }

    /** Size is checked first: an oversized file is oversized whether or not it is a picture. */
    @Test
    fun `an oversized file is reported as oversized rather than unsupported`() = runTest {
        val repository = FakeProfileRepository()
        val upload = UploadAvatarUseCase(repository)
        val tooBig = ByteArray(FieldLimits.MAX_AVATAR_BYTES.toInt() + 1)

        val result = upload(bytes = tooBig, fileName = "face.jpg")

        val failure = assertIs<AuthResult.Failure>(result)
        assertIs<AuthError.ImageTooLarge>(failure.error)
    }

    private companion object {
        val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    }
}
