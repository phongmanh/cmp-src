package com.liam.cmp_src.feature.auth.domain.usecase

import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository

/**
 * Signs a user in with an email and password.
 *
 * Normalizes the email (trimmed, lowercased) so the same account is not treated as two
 * different ones depending on how it was typed. The password is passed through untouched.
 */
class SignInWithEmailUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): AuthResult =
        authRepository.signInWithEmail(
            email = email.trim().lowercase(),
            password = password,
        )
}
