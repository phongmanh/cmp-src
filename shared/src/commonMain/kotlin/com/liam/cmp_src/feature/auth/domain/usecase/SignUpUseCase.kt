package com.liam.cmp_src.feature.auth.domain.usecase

import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository

class SignUpUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        return authRepository.register(email.trim().lowercase(), password)
    }
}