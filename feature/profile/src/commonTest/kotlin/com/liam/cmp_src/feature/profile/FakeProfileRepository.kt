package com.liam.cmp_src.feature.profile

import com.example.api.user.UserResponse
import com.liam.cmp_src.core.testing.FakeAuthRepository
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.profile.domain.repository.ProfileRepository

/**
 * Hand-written test double for [ProfileRepository], shaped like [FakeAuthRepository].
 *
 * Records what it was called with so tests can assert on what the domain decided to send — which
 * for a profile write is the interesting part, because what goes in the avatar field is a decision
 * and not an echo.
 */
class FakeProfileRepository(
    var updateResult: AuthResult = AuthResult.Success(FakeAuthRepository.TEST_USER),
    var uploadResult: AuthResult = AuthResult.Success(FakeAuthRepository.TEST_USER),
    var removeResult: AuthResult = AuthResult.Success(FakeAuthRepository.TEST_USER),
) : ProfileRepository {

    var updateCallCount = 0
        private set
    var uploadCallCount = 0
        private set
    var removeCallCount = 0
        private set

    var lastDisplayName: String? = null
        private set
    var lastAvatarUrl: String? = null
        private set
    var lastBytes: ByteArray? = null
        private set
    var lastFileName: String? = null
        private set
    var lastContentType: String? = null
        private set

    /** Distinguishes "sent null" from "never called", which [lastAvatarUrl] alone cannot. */
    var sawUpdate = false
        private set

    override suspend fun updateProfile(displayName: String?, avatarUrl: String?): AuthResult {
        updateCallCount++
        sawUpdate = true
        lastDisplayName = displayName
        lastAvatarUrl = avatarUrl
        return updateResult
    }

    override suspend fun uploadAvatar(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
    ): AuthResult {
        uploadCallCount++
        lastBytes = bytes
        lastFileName = fileName
        lastContentType = contentType
        return uploadResult
    }

    override suspend fun removeAvatar(): AuthResult {
        removeCallCount++
        return removeResult
    }

    companion object {
        /** An account whose picture is one this backend stores, and so one a write would destroy. */
        val USER_WITH_UPLOADED_PHOTO: UserResponse = FakeAuthRepository.TEST_USER.copy(
            avatarUrl = "https://api.test/api/v1/images/abc-123",
        )

        /** An account whose picture came from a provider, and so one a write leaves alone. */
        val USER_WITH_PROVIDER_PHOTO: UserResponse = FakeAuthRepository.TEST_USER.copy(
            avatarUrl = "https://lh3.googleusercontent.com/a/photo.jpg",
        )
    }
}
