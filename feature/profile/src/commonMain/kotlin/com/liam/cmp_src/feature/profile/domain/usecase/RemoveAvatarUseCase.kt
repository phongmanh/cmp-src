package com.liam.cmp_src.feature.profile.domain.usecase

import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.profile.domain.repository.ProfileRepository

/**
 * Drops the signed-in user's picture.
 *
 * Unlike a rename this leaves the display name alone: removal has its own route, and it is the
 * only way to clear a picture without also rewriting the rest of the profile.
 */
class RemoveAvatarUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): AuthResult = profileRepository.removeAvatar()
}
