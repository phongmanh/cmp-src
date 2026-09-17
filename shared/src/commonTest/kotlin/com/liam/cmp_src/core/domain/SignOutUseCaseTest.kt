package com.liam.cmp_src.core.domain

import com.liam.cmp_src.core.domain.usecase.SignOutUseCase
import com.liam.cmp_src.core.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SignOutUseCaseTest {

    @Test
    fun signingOutReachesTheRepository() = runTest {
        val repository = FakeAuthRepository()
        val signOut = SignOutUseCase(repository)

        signOut()

        assertEquals(1, repository.signOutCallCount)
    }

    @Test
    fun eachCallEndsTheSessionAgainRatherThanBeingDeduplicated() = runTest {
        val repository = FakeAuthRepository()
        val signOut = SignOutUseCase(repository)

        signOut()
        signOut()

        assertEquals(2, repository.signOutCallCount)
    }
}
