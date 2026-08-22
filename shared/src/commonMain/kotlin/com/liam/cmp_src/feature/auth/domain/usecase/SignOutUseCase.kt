package com.liam.cmp_src.feature.auth.domain.usecase

import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository

/**
 * Ends the signed-in session, server-side and locally.
 *
 * Signing out is a single call today, but it is the action every screen that offers it depends
 * on — routing it through a use case keeps the ViewModel off the repository, and gives whatever
 * has to be cleared next (cached profile, pending uploads) one place to be added.
 */
class SignOutUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.signOut()
}
