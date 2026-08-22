package com.liam.cmp_src.core.network

import com.example.api.common.ErrorResponse
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Runs [request] and decodes a 2xx body into [T].
 *
 * Together with [sendRequest] this is the only place in the app that turns an exception into a
 * value, which is what lets every layer above deal in [ApiResult] alone.
 */
suspend inline fun <reified T> apiCall(
    crossinline request: suspend () -> HttpResponse,
): ApiResult<T> = when (val result = sendRequest { request() }) {
    is ApiResult.Failure -> result
    is ApiResult.Success -> try {
        ApiResult.Success(result.data.body<T>())
    } catch (failure: SerializationException) {
        ApiResult.Failure(ApiError.Serialization(failure.message))
    }
}

/**
 * Runs [request] and reports only whether it succeeded.
 *
 * For endpoints whose body carries nothing worth decoding — logout answers `204 No Content`, and
 * asking Ktor to deserialize an empty body would fail the call that actually worked.
 */
suspend fun apiCallForStatus(request: suspend () -> HttpResponse): ApiResult<Unit> =
    sendRequest(request).map { }

/**
 * Sends [request] and classifies everything that can go wrong on the way.
 *
 * A returned [ApiResult.Success] means only that the status was 2xx — the body is untouched, so
 * both a typed call and a status-only call share one definition of "the call itself failed".
 *
 * `PublishedApi internal` because [apiCall] is public and inline; it is not part of the API.
 */
@PublishedApi
internal suspend fun sendRequest(request: suspend () -> HttpResponse): ApiResult<HttpResponse> {
    val response = try {
        request()
    } catch (cancellation: CancellationException) {
        // A cancelled caller is not a failed call — never swallow this into an ApiError.
        throw cancellation
    } catch (timeout: HttpRequestTimeoutException) {
        return ApiResult.Failure(ApiError.Timeout)
    } catch (timeout: ConnectTimeoutException) {
        return ApiResult.Failure(ApiError.Timeout)
    } catch (timeout: SocketTimeoutException) {
        return ApiResult.Failure(ApiError.Timeout)
    } catch (failure: SerializationException) {
        // Thrown while encoding the request body, before anything left the device.
        return ApiResult.Failure(ApiError.Serialization(failure.message))
    } catch (failure: IOException) {
        return ApiResult.Failure(ApiError.Network)
    } catch (failure: Throwable) {
        return ApiResult.Failure(ApiError.Unknown(failure.message))
    }

    return if (response.status.isSuccess()) {
        ApiResult.Success(response)
    } else {
        ApiResult.Failure(response.toApiError())
    }
}

/**
 * Reads a non-2xx response as the contract's [ErrorResponse].
 *
 * A body that does not parse still produces an [ApiError.Http] carrying the status: a proxy
 * returning an HTML 502 is a real failure the caller must see, not a serialization problem.
 */
private suspend fun HttpResponse.toApiError(): ApiError.Http {
    val body = try {
        body<ErrorResponse>()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        null
    }
    return ApiError.Http(status = status.value, code = body?.code, message = body?.message)
}
