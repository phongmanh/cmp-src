package com.liam.cmp_src.feature.auth

import com.example.api.user.UserResponse
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository
import kotlin.time.Clock

/**
 * Hand-written test double for [AuthRepository]. Records what it was called with so tests can
 * assert on normalization and on call counts, and returns whatever result they set up.
 */
class FakeAuthRepository(
    var emailResult: AuthResult = AuthResult.Success(TEST_USER),
    var socialResult: AuthResult = AuthResult.Success(TEST_USER),
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
