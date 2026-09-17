package com.liam.cmp_src.feature.auth.domain.usecase

import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.core.domain.model.SocialProvider
import com.liam.cmp_src.core.domain.repository.AuthRepository

/** Signs a user in through a third-party identity provider. */
class SignInWithSocialUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(provider: SocialProvider): AuthResult =
        authRepository.signInWith(provider)
}
