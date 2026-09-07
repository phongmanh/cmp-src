package com.liam.cmp_src.feature.auth.domain.usecase

import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository

/**
 * Replaces the signed-in user's password.
 *
 * One business action, one use case — which is also what keeps the ViewModel off the repository.
 * The current password is passed alongside the new one because the server requires it on top of
 * the access token; see [AuthRepository.changePassword].
 */
class ChangePasswordUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): AuthResult =
        authRepository.changePassword(currentPassword = currentPassword, newPassword = newPassword)
}
