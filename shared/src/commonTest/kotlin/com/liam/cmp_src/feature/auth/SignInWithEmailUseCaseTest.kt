package com.liam.cmp_src.feature.auth

import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithEmailUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SignInWithEmailUseCaseTest {

    @Test
    fun successfulSignInReturnsTheUser() = runTest {
        val repository = FakeAuthRepository()
        val signIn = SignInWithEmailUseCase(repository)

        val result = signIn("demo@cmpsrc.dev", "password123")

        assertEquals(AuthResult.Success(FakeAuthRepository.TEST_USER), result)
    }

    @Test
    fun failureIsPassedThroughUnchanged() = runTest {
        val repository = FakeAuthRepository(
            emailResult = AuthResult.Failure(AuthError.InvalidCredentials),
        )
        val signIn = SignInWithEmailUseCase(repository)

        val result = signIn("demo@cmpsrc.dev", "wrong")

        assertEquals(AuthResult.Failure(AuthError.InvalidCredentials), result)
    }

    @Test
    fun networkFailureIsPassedThroughUnchanged() = runTest {
        val repository = FakeAuthRepository(emailResult = AuthResult.Failure(AuthError.Network))
        val signIn = SignInWithEmailUseCase(repository)

        assertEquals(
            AuthResult.Failure(AuthError.Network),
            signIn("demo@cmpsrc.dev", "password123"),
        )
    }

    @Test
    fun emailIsTrimmedAndLowercasedBeforeReachingTheRepository() = runTest {
        val repository = FakeAuthRepository()
        val signIn = SignInWithEmailUseCase(repository)

        signIn("  Demo@CMPsrc.Dev  ", "password123")

        assertEquals("demo@cmpsrc.dev", repository.lastEmail)
    }

    @Test
    fun passwordIsForwardedVerbatim() = runTest {
        val repository = FakeAuthRepository()
        val signIn = SignInWithEmailUseCase(repository)

        signIn("demo@cmpsrc.dev", "  PaSsWoRd 123  ")

        assertEquals("  PaSsWoRd 123  ", repository.lastPassword)
    }
}
