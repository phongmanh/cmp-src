package com.liam.cmp_src.feature.auth.domain.usecase

import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository

/**
 * Reads the signed-in user back from the server.
 *
 * A screen that shows the account cannot rely on the copy handed to it at sign-in: the token
 * response leaves `linkedProviders` empty by contract, and the profile may have changed since.
 * Routing the read through a use case keeps the ViewModel off the repository, and gives whatever
 * has to wrap it next (a cache, a refresh policy) one place to live.
 */
class GetCurrentUserUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.currentUser()
}
