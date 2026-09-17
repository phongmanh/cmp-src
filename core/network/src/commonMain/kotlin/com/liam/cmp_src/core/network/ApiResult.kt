package com.liam.cmp_src.core.network

/**
 * The outcome of a network call.
 *
 * Every function in this package returns one of these instead of throwing, so no transport or
 * serialization exception ever escapes the data layer — callers branch on a closed set rather
 * than guessing which exceptions a Ktor engine raises on which platform.
 */
sealed interface ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/** Maps a successful payload, leaving a failure untouched. */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

/** The payload, or `null` if the call failed. */
fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

/**
 * Everything a call can fail with, as a closed set.
 *
 * Carries no user-facing text: each feature maps these to its own domain errors and string
 * resources, which is what keeps this package free of localization concerns.
 */
sealed interface ApiError {

    /** Nothing came back — DNS, refused connection, TLS failure, dropped socket, offline. */
    data object Network : ApiError

    /** The request was sent but did not complete within the configured timeout. */
    data object Timeout : ApiError

    /**
     * The server answered with a non-2xx status.
     *
     * [code] is `ErrorResponse.code` when the body parsed, and stays a raw `String` on purpose —
     * the contract requires a client to keep working when the server returns a code it has never
     * heard of, which an enum would fail to decode instead. Compare against `ErrorCode`.
     *
     * [message] is the server's English text for an API consumer. It is for logs, never the UI.
     */
    data class Http(val status: Int, val code: String? = null, val message: String? = null) : ApiError

    /** A 2xx body that did not match the contract — the two sides have drifted. */
    data class Serialization(val message: String? = null) : ApiError

    /** Anything unclassified. [message] is for logs, never the UI. */
    data class Unknown(val message: String? = null) : ApiError
}
