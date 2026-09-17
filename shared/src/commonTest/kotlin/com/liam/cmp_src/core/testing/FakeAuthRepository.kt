package com.liam.cmp_src.core.testing

import com.example.api.user.UserResponse
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.core.domain.model.SocialProvider
import com.liam.cmp_src.core.domain.repository.AuthRepository
import kotlin.time.Clock

/**
 * Hand-written test double for [AuthRepository]. Records what it was called with so tests can
 * assert on normalization and on call counts, and returns whatever result they set up.
 */
class FakeAuthRepository(
    var emailResult: AuthResult = AuthResult.Success(TEST_USER),
    var socialResult: AuthResult = AuthResult.Success(TEST_USER),
    var registerResult: AuthResult = AuthResult.Success(TEST_USER),
    var currentUserResult: AuthResult = AuthResult.Success(TEST_USER),
    var changePasswordResult: AuthResult = AuthResult.Success(TEST_USER),
) : AuthRepository {

    var lastEmail: String? = null
        private set
    var lastPassword: String? = null
        private set
    var lastProvider: SocialProvider? = null
        private set
    var emailCallCount = 0
        private set
    var socialCallCount = 0
        private set
    var signOutCallCount = 0
        private set
    var registerCallCount = 0
        private set
    var currentUserCallCount = 0
        private set
    var changePasswordCallCount = 0
        private set
    var lastCurrentPassword: String? = null
        private set
    var lastNewPassword: String? = null
        private set

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        emailCallCount++
        lastEmail = email
        lastPassword = password
        return emailResult
    }

    override suspend fun signInWith(provider: SocialProvider): AuthResult {
        socialCallCount++
        lastProvider = provider
        return socialResult
    }

    override suspend fun signOut() {
        signOutCallCount++
    }

    override suspend fun register(email: String, password: String): AuthResult {
        registerCallCount++
        lastEmail = email
        lastPassword = password
        return registerResult
    }

    override suspend fun currentUser(): AuthResult {
        currentUserCallCount++
        return currentUserResult
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        changePasswordCallCount++
        lastCurrentPassword = currentPassword
        lastNewPassword = newPassword
        return changePasswordResult
    }

    companion object {
        val TEST_USER = UserResponse(
            id = "test-user",
            email = "test@example.com",
            displayName = "Test User",
            avatarUrl = null,
            isEmailVerified = false,
            createdAt = Clock.System.now().toString(),
            linkedProviders = emptyList(),
        )
    }
}
