package com.liam.cmp_src.feature.profile.domain.usecase

import com.example.api.common.FieldLimits
import com.liam.cmp_src.feature.profile.domain.avatarContentType
import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.profile.domain.repository.ProfileRepository

/**
 * Replaces the signed-in user's picture with a file they picked.
 *
 * Both checks here are ones the server makes too, done early only to save an upload that was going
 * to be refused — which on this route also costs one of the few the account is allowed per minute.
 * Neither weakens the server's own checking, and neither is trusted in its place.
 */
class UploadAvatarUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(bytes: ByteArray, fileName: String): AuthResult {
        if (bytes.size.toLong() > FieldLimits.MAX_AVATAR_BYTES) {
            return AuthResult.Failure(AuthError.ImageTooLarge(FieldLimits.MAX_AVATAR_BYTES))
        }

        val contentType = bytes.avatarContentType()
            ?: return AuthResult.Failure(AuthError.UnsupportedImage)

        return profileRepository.uploadAvatar(
            bytes = bytes,
            fileName = fileName,
            contentType = contentType,
        )
    }
}
