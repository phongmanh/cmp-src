package com.liam.cmp_src.feature.customers

import com.example.api.ApiRoutes
import com.example.api.common.PageQuery
import com.example.api.common.PageResponse
import com.example.api.customer.CustomerRequest
import com.example.api.customer.CustomerResponse
import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.core.network.ApiConfig
import com.liam.cmp_src.core.network.ApiResult
import com.liam.cmp_src.core.network.InMemoryTokenStore
import com.liam.cmp_src.core.network.createHttpClient
import com.liam.cmp_src.feature.customers.data.remote.CustomerApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Pins each call to the contract's route, method and body shape, against a `MockEngine`. */
class CustomerApiTest {

    @Test
    fun `a page is read from the collection with its limit and cursor`() = runTest {
        var seen: HttpRequestData? = null
        val api = api { request ->
            seen = request
            json(Json.encodeToString(PageResponse(listOf(RESPONSE), nextCursor = "next")))
        }

        val result = api.customers(cursor = "abc", limit = 50)

        val page = assertIs<ApiResult.Success<PageResponse<CustomerResponse>>>(result).data
        assertEquals("next", page.nextCursor)
        assertEquals(ApiRoutes.Customers.PATH, seen?.url?.encodedPath)
        assertEquals("50", seen?.url?.parameters?.get(PageQuery.LIMIT))
        assertEquals("abc", seen?.url?.parameters?.get(PageQuery.CURSOR))
    }

    @Test
    fun `the first page sends no cursor`() = runTest {
        var seen: HttpRequestData? = null
        val api = api { request ->
            seen = request
            json(Json.encodeToString(PageResponse(emptyList<CustomerResponse>(), nextCursor = null)))
        }

        api.customers(cursor = null, limit = 10)

        assertNull(seen?.url?.parameters?.get(PageQuery.CURSOR))
    }

    @Test
    fun `a create posts the request as json to the collection`() = runTest {
        var seen: HttpRequestData? = null
        var body: String? = null
        val api = api { request ->
            seen = request
            body = request.body.toByteArray().decodeToString()
            json(Json.encodeToString(RESPONSE))
        }

        api.create(REQUEST)

        assertEquals(HttpMethod.Post, seen?.method)
        assertEquals(ApiRoutes.Customers.PATH, seen?.url?.encodedPath)
        assertEquals(ContentType.Application.Json, seen?.body?.contentType?.withoutParameters())
        assertEquals(REQUEST, Json.decodeFromString<CustomerRequest>(body.orEmpty()))
    }

    @Test
    fun `a replace puts to the customer's own path`() = runTest {
        var seen: HttpRequestData? = null
        val api = api { request ->
            seen = request
            json(Json.encodeToString(RESPONSE))
        }

        api.replace("server-1", REQUEST)

        assertEquals(HttpMethod.Put, seen?.method)
        assertEquals(ApiRoutes.Customers.byId("server-1"), seen?.url?.encodedPath)
    }

    @Test
    fun `a delete succeeds on an empty response`() = runTest {
        var seen: HttpRequestData? = null
        val api = api { request ->
            seen = request
            respond(ByteArray(0), HttpStatusCode.NoContent)
        }

        val result = api.delete("server-1")

        assertIs<ApiResult.Success<Unit>>(result)
        assertEquals(HttpMethod.Delete, seen?.method)
        assertEquals(ApiRoutes.Customers.byId("server-1"), seen?.url?.encodedPath)
    }

    private fun api(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        CustomerApi(
            createHttpClient(
                tokenStore = InMemoryTokenStore(),
                config = ApiConfig(baseUrl = "https://api.test"),
                engine = MockEngine(handler),
                logger = Logger.EMPTY,
                logLevel = LogLevel.NONE,
            ),
        )

    private fun MockRequestHandleScope.json(body: String) = respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private companion object {
        val REQUEST = CustomerRequest(firstName = "Ada", status = CustomerStatus.ACTIVE)
        val RESPONSE = CustomerResponse(
            id = "server-1", firstName = "Ada", lastName = null, companyName = null, email = null,
            phone = null, address = null, notes = null, status = "active",
            createdAt = "2026-09-01T00:00:00Z", updatedAt = "2026-09-01T00:00:00Z",
        )
    }
}
