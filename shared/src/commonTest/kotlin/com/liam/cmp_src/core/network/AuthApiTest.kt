package com.liam.cmp_src.core.network

import com.example.api.ApiRoutes
import com.example.api.auth.LoginRequest
import com.example.api.auth.TokenResponse
import com.example.api.common.ErrorCode
import com.example.api.common.ErrorResponse
import com.example.api.user.UserResponse
import com.liam.cmp_src.feature.auth.data.remote.AuthApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the whole client stack — routing, serialization, bearer auth, refresh and error
 * mapping — against a `MockEngine`, so it runs on every target with no server and no network.
 */
class AuthApiTest {

    @Test
    fun `login posts the contract request to the contract route`() = runTest {
        var seenPath: String? = null
        var seenMethod: HttpMethod? = null
        var seenBody: String? = null

        val api = authApi { request ->
            seenPath = request.url.encodedPath
            seenMethod = request.method
            seenBody = request.bodyText()
            respondJson(json.encodeToString(TOKEN_RESPONSE))
        }

        api.login(EMAIL, PASSWORD)

        assertEquals(ApiRoutes.Auth.LOGIN, seenPath)
        assertEquals(HttpMethod.Post, seenMethod)
        assertEquals(LoginRequest(EMAIL, PASSWORD), json.decodeFromString<LoginRequest>(seenBody.orEmpty()))
    }

    @Test
    fun `login stores the tokens it received`() = runTest {
        val tokenStore = InMemoryTokenStore()
        val api = authApi(tokenStore) { respondJson(json.encodeToString(TOKEN_RESPONSE)) }

        val result = api.login(EMAIL, PASSWORD)

        assertIs<ApiResult.Success<TokenResponse>>(result)
        assertEquals(TOKEN_RESPONSE.accessToken, result.data.accessToken)
        assertEquals(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN), tokenStore.tokens.value)
    }

    @Test
    fun `a rejected login is reported as an Http error carrying the server's code`() = runTest {
        val tokenStore = InMemoryTokenStore()
        val api = authApi(tokenStore) {
            respondJson(
                body = json.encodeToString(
                    ErrorResponse(code = ErrorCode.UNAUTHENTICATED, message = "Bad credentials"),
                ),
                status = HttpStatusCode.Unauthorized,
            )
        }

        val result = api.login(EMAIL, "wrong-password")

        val failure = assertIs<ApiResult.Failure>(result)
        val error = assertIs<ApiError.Http>(failure.error)
        assertEquals(HttpStatusCode.Unauthorized.value, error.status)
        assertEquals(ErrorCode.UNAUTHENTICATED, error.code)
        assertNull(tokenStore.tokens.value, "a failed sign-in must not open a session")
    }

    @Test
    fun `an error body that is not the contract's shape still reports the status`() = runTest {
        // What a proxy or load balancer returns when it never reached the app.
        val api = authApi { respondError(HttpStatusCode.BadGateway, "<html>gateway</html>") }

        val failure = assertIs<ApiResult.Failure>(api.login(EMAIL, PASSWORD))
        val error = assertIs<ApiError.Http>(failure.error)
        assertEquals(HttpStatusCode.BadGateway.value, error.status)
        assertNull(error.code)
    }

    @Test
    fun `a transport failure is reported as a network error rather than an exception`() = runTest {
        val api = authApi { throw IOException("connection refused") }

        val failure = assertIs<ApiResult.Failure>(api.login(EMAIL, PASSWORD))
        assertEquals(ApiError.Network, failure.error)
    }

    @Test
    fun `sign-in routes are sent without an Authorization header`() = runTest {
        // A stale session must not leak into the call that is meant to replace it.
        val tokenStore = InMemoryTokenStore(AuthTokens("stale-access", "stale-refresh"))
        var authorization: String? = "unset"

        val api = authApi(tokenStore) { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respondJson(json.encodeToString(TOKEN_RESPONSE))
        }

        api.login(EMAIL, PASSWORD)

        assertNull(authorization)
    }

    @Test
    fun `an authenticated call attaches the stored access token`() = runTest {
        val tokenStore = InMemoryTokenStore(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        var authorization: String? = null

        val api = authApi(tokenStore) { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respondJson(json.encodeToString(USER_RESPONSE))
        }

        val result = api.currentUser()

        assertIs<ApiResult.Success<UserResponse>>(result)
        assertEquals("Bearer $ACCESS_TOKEN", authorization)
    }

    @Test
    fun `an expired access token is refreshed and the call replayed`() = runTest {
        val tokenStore = InMemoryTokenStore(AuthTokens("expired-access", REFRESH_TOKEN))
        val paths = mutableListOf<String>()

        val api = authApi(tokenStore) { request ->
            paths += request.url.encodedPath
            when {
                request.url.encodedPath == ApiRoutes.Auth.REFRESH ->
                    respondJson(json.encodeToString(TOKEN_RESPONSE))

                request.headers[HttpHeaders.Authorization] == "Bearer $ACCESS_TOKEN" ->
                    respondJson(json.encodeToString(USER_RESPONSE))

                else -> unauthorized()
            }
        }

        val result = api.currentUser()

        assertIs<ApiResult.Success<UserResponse>>(result)
        assertEquals(
            listOf(ApiRoutes.Users.ME, ApiRoutes.Auth.REFRESH, ApiRoutes.Users.ME),
            paths,
            "the rejected call should be retried once, after a refresh",
        )
        assertEquals(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN), tokenStore.tokens.value)
    }

    @Test
    fun `a rejected refresh ends the session instead of retrying forever`() = runTest {
        val tokenStore = InMemoryTokenStore(AuthTokens("expired-access", "revoked-refresh"))
        var refreshAttempts = 0

        val api = authApi(tokenStore) { request ->
            if (request.url.encodedPath == ApiRoutes.Auth.REFRESH) refreshAttempts++
            unauthorized()
        }

        val failure = assertIs<ApiResult.Failure>(api.currentUser())

        assertIs<ApiError.Http>(failure.error)
        assertEquals(1, refreshAttempts)
        assertNull(tokenStore.tokens.value, "a spent refresh token must not be kept")
    }

    @Test
    fun `a call made after logout does not carry the dead token`() = runTest {
        val tokenStore = InMemoryTokenStore(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        val sent = mutableListOf<String?>()
        val api = authApi(tokenStore) { request ->
            sent += request.headers[HttpHeaders.Authorization]
            respondJson(json.encodeToString(USER_RESPONSE))
        }

        api.currentUser()
        api.logout()
        api.currentUser()

        assertEquals("Bearer $ACCESS_TOKEN", sent.first())
        assertNull(sent.last(), "the signed-out client must stop sending the old token")
    }

    @Test
    fun `signing in is noticed by calls made on the same client`() = runTest {
        val tokenStore = InMemoryTokenStore()
        var authorization: String? = "unset"
        val api = authApi(tokenStore) { request ->
            when (request.url.encodedPath) {
                ApiRoutes.Auth.LOGIN -> respondJson(json.encodeToString(TOKEN_RESPONSE))
                else -> {
                    authorization = request.headers[HttpHeaders.Authorization]
                    respondJson(json.encodeToString(USER_RESPONSE))
                }
            }
        }

        // The first call runs while signed out, so the client learns "no token" before the login.
        api.currentUser()
        api.login(EMAIL, PASSWORD)
        api.currentUser()

        assertEquals("Bearer $ACCESS_TOKEN", authorization)
    }

    @Test
    fun `logout ends the local session even when the server call fails`() = runTest {
        val tokenStore = InMemoryTokenStore(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        val api = authApi(tokenStore) { throw IOException("offline") }

        val result = api.logout()

        assertTrue(result is ApiResult.Failure)
        assertNull(tokenStore.tokens.value)
    }

    // ---- fixtures -------------------------------------------------------------------------

    private fun authApi(
        tokenStore: TokenStore = InMemoryTokenStore(),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): AuthApi = AuthApi(client = testClient(tokenStore, handler), tokenStore = tokenStore)

    private fun testClient(
        tokenStore: TokenStore,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = createHttpClient(
        tokenStore = tokenStore,
        config = ApiConfig(baseUrl = BASE_URL),
        engine = MockEngine(handler),
        logger = Logger.EMPTY,
        logLevel = LogLevel.NONE,
    )

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    /** A 401 shaped the way a bearer-protected server sends one, `WWW-Authenticate` included. */
    private fun MockRequestHandleScope.unauthorized() = respond(
        content = json.encodeToString(
            ErrorResponse(code = ErrorCode.UNAUTHENTICATED, message = "Token expired"),
        ),
        status = HttpStatusCode.Unauthorized,
        headers = headersOf(
            HttpHeaders.ContentType to listOf("application/json"),
            HttpHeaders.WWWAuthenticate to listOf("Bearer realm=\"api\""),
        ),
    )

    private suspend fun HttpRequestData.bodyText(): String =
        (body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes().decodeToString()

    private companion object {
        const val BASE_URL = "https://api.test"
        const val EMAIL = "demo@cmpsrc.dev"
        const val PASSWORD = "password123"
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"

        val json = Json { ignoreUnknownKeys = true }

        val USER_RESPONSE = UserResponse(
            id = "u1",
            email = EMAIL,
            displayName = "Demo User",
            avatarUrl = null,
            isEmailVerified = true,
            createdAt = "2026-01-01T00:00:00Z",
            linkedProviders = listOf("google"),
        )

        val TOKEN_RESPONSE = TokenResponse(
            accessToken = ACCESS_TOKEN,
            refreshToken = REFRESH_TOKEN,
            expiresIn = 3600,
            user = USER_RESPONSE.copy(linkedProviders = emptyList()),
        )
    }
}
