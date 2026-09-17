package com.liam.cmp_src.feature.profile.data.remote

import com.example.api.ApiRoutes
import com.example.api.common.FieldLimits
import com.example.api.user.UpdateProfileRequest
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.network.ApiResult
import com.liam.cmp_src.core.network.apiCall
import com.liam.cmp_src.core.network.apiCallForStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

/**
 * The calls that change a profile, and the read that confirms one, spoken entirely in
 * `api-contract` types.
 *
 * Kept apart from `AuthApi` because none of this is the session: no call here issues, refreshes or
 * drops a token, so this class needs no `TokenStore` and the client's `Auth` plugin is the only
 * thing that has to know a request is authenticated at all.
 *
 * Nothing throws — see [apiCall].
 */
class ProfileApi(
    private val client: HttpClient,
) {

    /**
     * Writes the editable half of the profile.
     *
     * **Replace semantics, not a patch.** Both fields are stored exactly as they arrive, and the
     * server additionally retires any uploaded avatar on every call, whatever [avatarUrl] says.
     * Deciding what to send is therefore a domain question, not a transport one — see
     * `UpdateDisplayNameUseCase`.
     */
    suspend fun updateProfile(
        displayName: String?,
        avatarUrl: String?,
    ): ApiResult<UserResponse> = apiCall {
        client.put(ApiRoutes.Users.ME) {
            // ContentNegotiation only serializes a typed body when the request declares a content
            // type it has a converter for; without this the call fails before leaving the device.
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(displayName = displayName, avatarUrl = avatarUrl))
        }
    }

    /**
     * Uploads a new picture and returns the profile that now carries it.
     *
     * Exactly one part, named [FieldLimits.AVATAR_PART_NAME] — the server refuses anything else,
     * including the right bytes under a different name. The declared [contentType] and [fileName]
     * are courtesy only: the server decides what the file is by reading its leading bytes, because
     * both of those are written by the client and neither can be trusted.
     */
    suspend fun uploadAvatar(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
    ): ApiResult<UserResponse> = apiCall {
        client.post(ApiRoutes.Users.ME_AVATAR) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = FieldLimits.AVATAR_PART_NAME,
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, contentType)
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"$fileName\"",
                                )
                            },
                        )
                    },
                ),
            )
        }
    }

    /**
     * Drops the stored picture. Answers `204`, and is idempotent — removing an avatar the account
     * does not have still succeeds — so [apiCallForStatus] rather than [apiCall]: there is no body
     * to decode, and asking Ktor to decode one would fail the call that worked.
     */
    suspend fun removeAvatar(): ApiResult<Unit> =
        apiCallForStatus { client.delete(ApiRoutes.Users.ME_AVATAR) }

    /**
     * The account as the server now holds it — what a call that answers without a body (see
     * [removeAvatar]) is followed by.
     *
     * The same `GET /users/me` that `AuthApi` makes to hydrate a sign-in. Repeated here rather than
     * shared because the two features read it for different reasons, and profile must not depend
     * on auth's data layer to ask.
     */
    suspend fun currentUser(): ApiResult<UserResponse> =
        apiCall { client.get(ApiRoutes.Users.ME) }
}
