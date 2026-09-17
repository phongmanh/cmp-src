package com.liam.cmp_src.feature.profile.domain.usecase

import com.example.api.user.UserResponse
import com.liam.cmp_src.core.network.isStoredImageUrl
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.profile.domain.repository.ProfileRepository

/**
 * Renames the signed-in user.
 *
 * The write it makes is a whole-profile replace, so this use case has to decide what happens to
 * the picture as well, and [current] is what it decides from:
 *
 *  - a picture uploaded to this backend is deleted by the write whatever is sent, so sending its
 *    address back would leave the account pointing at an image that no longer exists. `null` goes
 *    instead, and the account is left with no picture — which is why the dialog warns about it;
 *  - a picture from a social provider is untouched by the write, so it is echoed back verbatim and
 *    survives.
 *
 * An empty name is sent as `null`, which clears it. The server rejects a blank string, so the two
 * are not interchangeable and the name is never sent as typed whitespace.
 */
class UpdateDisplayNameUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(displayName: String, current: UserResponse): AuthResult =
        profileRepository.updateProfile(
            displayName = displayName.trim().takeIf { it.isNotEmpty() },
            avatarUrl = current.avatarUrl?.takeUnless { isStoredImageUrl(it) },
        )
}
