package com.liam.cmp_src.feature.auth.data

import com.example.api.common.ErrorCode
import com.liam.cmp_src.core.network.ApiError
import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.core.domain.model.SocialProvider
import io.ktor.http.HttpStatusCode
import com.example.api.auth.SocialProvider as ContractSocialProvider

/**
 * The data layer's translation of the wire into the domain: transport failures become
 * [AuthError]s, and the domain's provider enum becomes the contract's.
 *
 * Both directions live here rather than in the repository so the repository reads as the flow
 * it implements, and so the mappings can be tested on their own.
 */

/**
 * Maps a failed call onto the closed set of failures the login screen knows how to say out loud.
 *
 * [provider] is set for a social exchange and null for email/password, because the same status
 * means different things on the two paths — see [toAuthError] below.
 */
internal fun ApiError.toAuthError(provider: SocialProvider? = null): AuthError = when (this) {
    // A request that never completed and one that took too long are the same thing to a user:
    // the app could not reach the server, and trying again later is the only remedy.
    ApiError.Network, ApiError.Timeout -> AuthError.Network
    is ApiError.Http -> toAuthError(provider)
    is ApiError.Serialization -> AuthError.Unknown(message)
    is ApiError.Unknown -> AuthError.Unknown(message)
}

/**
 * Maps a rejection the server explained.
 *
 * `code` is matched before `status` because the contract's codes are the stable part: a server
 * may move a rejection between statuses, and a proxy can return a bare 401 with no body at all.
 *
 * Only the email/password path can report [AuthError.InvalidCredentials]. A provider token the
 * server refuses is not something the user can fix by retyping, and telling them their "email and
 * password don't match" when they never typed either would be a lie.
 */
private fun ApiError.Http.toAuthError(provider: SocialProvider?): AuthError = when {
    code == ErrorCode.PROVIDER_NOT_ENABLED && provider != null ->
        AuthError.ProviderUnavailable(provider)

    provider != null -> AuthError.Unknown(message)

    code == ErrorCode.UNAUTHENTICATED -> AuthError.InvalidCredentials
    status == HttpStatusCode.Unauthorized.value -> AuthError.InvalidCredentials
    else -> AuthError.Unknown(message)
}

/**
 * Maps a failure on a call made *with* an existing session, such as `GET /users/me`.
 *
 * Deliberately not [toAuthError]: that turns a 401 into [AuthError.InvalidCredentials], which is
 * only ever true of the email/password path. A 401 here means the session expired after the client
 * had already failed to refresh it, and telling the user their password is wrong when they never
 * typed one would be a lie.
 */
internal fun ApiError.toSessionError(): AuthError = when (this) {
    ApiError.Network, ApiError.Timeout -> AuthError.Network
    is ApiError.Http -> AuthError.Unknown(message)
    is ApiError.Serialization -> AuthError.Unknown(message)
    is ApiError.Unknown -> AuthError.Unknown(message)
}

/**
 * The contract's spelling of this provider.
 *
 * An exhaustive `when` rather than `ContractSocialProvider.valueOf(name)`: the two enums are free
 * to diverge, and if they ever do this stops compiling instead of throwing at sign-in time.
 */
internal fun SocialProvider.toContract(): ContractSocialProvider = when (this) {
    SocialProvider.GOOGLE -> ContractSocialProvider.GOOGLE
    SocialProvider.FACEBOOK -> ContractSocialProvider.FACEBOOK
}

/**
 * Maps a failure from `POST /auth/password`.
 *
 * Deliberately neither [toAuthError] nor [toSessionError]. The call is made with a live session
 * *and* a typed password, so a 401 means "that is not your current password" — not the sign-in
 * path's [AuthError.InvalidCredentials], which names an email the user never entered here, and not
 * an expired session.
 *
 * Note that a 401 on this path has already cost a token refresh: `/auth/password` is an
 * authenticated route, so the client's `Auth` plugin spends the refresh token and replays the
 * request before the second 401 reaches this function. That is the right trade — the same status
 * also covers a genuinely stale access token, and the server cannot tell the two apart by code.
 *
 * Nothing carries the server's own text into the UI: `ErrorResponse.message` is written in English
 * for an API consumer, so an unclassified failure resolves to the app's generic wording instead.
 */
internal fun ApiError.toChangePasswordError(): AuthError = when (this) {
    ApiError.Network, ApiError.Timeout -> AuthError.Network
    is ApiError.Http -> when {
        code == ErrorCode.PASSWORD_NOT_SET -> AuthError.PasswordNotSet
        code == ErrorCode.UNAUTHENTICATED -> AuthError.WrongPassword
        status == HttpStatusCode.Unauthorized.value -> AuthError.WrongPassword
        else -> AuthError.Unknown()
    }

    is ApiError.Serialization -> AuthError.Unknown()
    is ApiError.Unknown -> AuthError.Unknown()
}
