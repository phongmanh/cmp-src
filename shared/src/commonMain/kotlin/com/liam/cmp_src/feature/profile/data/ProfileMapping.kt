package com.liam.cmp_src.feature.profile.data

import com.example.api.common.ErrorCode
import com.example.api.common.FieldLimits
import com.liam.cmp_src.core.network.ApiError
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import io.ktor.http.HttpStatusCode

/**
 * Maps a failed profile call onto the closed set of failures the edit dialog can say out loud.
 *
 * Deliberately neither `toAuthError` nor `toSessionError`. Both of those turn a 401 into something
 * about credentials, and there are none here: the caller is signed in and typed no password, so an
 * expired session is the only reading left and it is not this dialog's business to explain it.
 *
 * The image cases are matched by contract code first and by status second, for the same reason the
 * auth mapper does it: a code is the stable part, and a proxy can return a bare status with no body
 * at all. `413` in particular can be produced by something in front of the server that never read
 * our error shape.
 *
 * Nothing carries the server's own text into the UI — `ErrorResponse.message` is written in English
 * for an API consumer — so an unclassified failure resolves to the app's generic wording.
 */
internal fun ApiError.toProfileError(): AuthError = when (this) {
    // A request that never completed and one that took too long are the same thing to a user.
    ApiError.Network, ApiError.Timeout -> AuthError.Network
    is ApiError.Http -> toProfileError()
    is ApiError.Serialization -> AuthError.Unknown()
    is ApiError.Unknown -> AuthError.Unknown()
}

private fun ApiError.Http.toProfileError(): AuthError = when {
    code == ErrorCode.UNSUPPORTED_IMAGE -> AuthError.UnsupportedImage
    status == HttpStatusCode.UnprocessableEntity.value -> AuthError.UnsupportedImage

    code == ErrorCode.PAYLOAD_TOO_LARGE -> AuthError.ImageTooLarge(FieldLimits.MAX_AVATAR_BYTES)
    status == HttpStatusCode.PayloadTooLarge.value ->
        AuthError.ImageTooLarge(FieldLimits.MAX_AVATAR_BYTES)

    status == HttpStatusCode.TooManyRequests.value -> AuthError.TooManyUploads

    else -> AuthError.Unknown()
}
