package com.liam.cmp_src.feature.auth

import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithSocialUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SignInWithSocialUseCaseTest {

    @Test
    fun googleSignInReachesTheRepositoryWithThatProvider() = runTest {
        val repository = FakeAuthRepository()
        val signIn = SignInWithSocialUseCase(repository)

        val result = signIn(SocialProvider.GOOGLE)

        assertEquals(SocialProvider.GOOGLE, repository.lastProvider)
        assertEquals(AuthResult.Success(FakeAuthRepository.TEST_USER), result)
    }

    @Test
    fun facebookSignInReachesTheRepositoryWithThatProvider() = runTest {
        val repository = FakeAuthRepository()
        val signIn = SignInWithSocialUseCase(repository)

        signIn(SocialProvider.FACEBOOK)

        assertEquals(SocialProvider.FACEBOOK, repository.lastProvider)
    }

    @Test
    fun cancellationIsPassedThroughUnchanged() = runTest {
        val repository = FakeAuthRepository(
            socialResult = AuthResult.Failure(AuthError.Cancelled),
        )
        val signIn = SignInWithSocialUseCase(repository)

        assertEquals(AuthResult.Failure(AuthError.Cancelled), signIn(SocialProvider.GOOGLE))
    }

    @Test
    fun providerUnavailableIsPassedThroughWithItsProvider() = runTest {
        val expected = AuthError.ProviderUnavailable(SocialProvider.FACEBOOK)
        val repository = FakeAuthRepository(socialResult = AuthResult.Failure(expected))
        val signIn = SignInWithSocialUseCase(repository)

        assertEquals(AuthResult.Failure(expected), signIn(SocialProvider.FACEBOOK))
    }
}
