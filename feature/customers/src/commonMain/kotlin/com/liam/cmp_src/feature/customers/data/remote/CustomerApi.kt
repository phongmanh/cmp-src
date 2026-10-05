package com.liam.cmp_src.feature.customers.data.remote

import com.example.api.ApiRoutes
import com.example.api.common.PageQuery
import com.example.api.common.PageResponse
import com.example.api.customer.CustomerRequest
import com.example.api.customer.CustomerResponse
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.network.ApiResult
import com.liam.cmp_src.core.network.apiCall
import com.liam.cmp_src.core.network.apiCallForStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * The customer routes, spoken entirely in `api-contract` types. Only the sync talks to this — the
 * screens read and write the device's copy.
 *
 * Nothing throws — see [apiCall].
 */
class CustomerApi(
    private val client: HttpClient,
) {

    /** One page of the signed-in account's customers. Pass [cursor] `null` for the first. */
    suspend fun customers(cursor: String?, limit: Int): ApiResult<PageResponse<CustomerResponse>> =
        apiCall {
            client.get(ApiRoutes.Customers.PATH) {
                parameter(PageQuery.LIMIT, limit)
                cursor?.let { parameter(PageQuery.CURSOR, it) }
            }
        }

    /** Creates a customer; the server assigns its id. */
    suspend fun create(request: CustomerRequest): ApiResult<CustomerResponse> = apiCall {
        client.post(ApiRoutes.Customers.PATH) {
            // ContentNegotiation only serializes a typed body when the request declares a content
            // type it has a converter for; without this the call fails before leaving the device.
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /** Replaces the customer the server knows as [remoteId] — every field, not a patch. */
    suspend fun replace(remoteId: String, request: CustomerRequest): ApiResult<CustomerResponse> =
        apiCall {
            client.put(ApiRoutes.Customers.byId(remoteId)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    /** Deletes the customer the server knows as [remoteId]. Answers with no body. */
    suspend fun delete(remoteId: String): ApiResult<Unit> =
        apiCallForStatus { client.delete(ApiRoutes.Customers.byId(remoteId)) }

    /**
     * Who the stored session belongs to — checked before every sync, so rows written by one
     * account are never sent with another account's token.
     *
     * The same `GET /users/me` auth and profile make; repeated here because features never borrow
     * one another's data layer.
     */
    suspend fun currentUser(): ApiResult<UserResponse> =
        apiCall { client.get(ApiRoutes.Users.ME) }
}
