package com.liam.cmp_src.feature.profile.data

import com.example.api.user.UserResponse
import com.liam.cmp_src.core.network.ApiResult
import com.liam.cmp_src.core.domain.model.AuthResult
import com.liam.cmp_src.feature.profile.data.remote.ProfileApi
import com.liam.cmp_src.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Profile edits, backed by the API.
 *
 * `DELETE /users/me/avatar` answers `204`, so removing a picture is followed by a read of
 * `GET /users/me` to say what the account looks like afterwards. Copying the old user with a
 * blanked avatar would usually be right and would occasionally be a lie, and this repository's
 * contract is that every answer is the server's.
 *
 * Nothing here throws: [ProfileApi] returns `ApiResult`, and each failure is mapped to the domain
 * by [toProfileError]. The [dispatcher] is injected rather than named here so tests can run the
 * flow on their own scheduler.
 */
class ProfileRepositoryImpl(
    private val profileApi: ProfileApi,
    private val dispatcher: CoroutineDispatcher,
) : ProfileRepository {

    override suspend fun updateProfile(
        displayName: String?,
        avatarUrl: String?,
    ): AuthResult = withContext(dispatcher) {
        profileApi.updateProfile(displayName = displayName, avatarUrl = avatarUrl).toAuthResult()
    }

    override suspend fun uploadAvatar(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
    ): AuthResult = withContext(dispatcher) {
        profileApi.uploadAvatar(bytes = bytes, fileName = fileName, contentType = contentType)
            .toAuthResult()
    }

    override suspend fun removeAvatar(): AuthResult = withContext(dispatcher) {
        when (val result = profileApi.removeAvatar()) {
            is ApiResult.Success -> profileApi.currentUser().toAuthResult()
            is ApiResult.Failure -> AuthResult.Failure(result.error.toProfileError())
        }
    }

    private fun ApiResult<UserResponse>.toAuthResult(): AuthResult = when (this) {
        is ApiResult.Success -> AuthResult.Success(data)
        is ApiResult.Failure -> AuthResult.Failure(error.toProfileError())
    }
}
