package com.liam.cmp_src.feature.auth

import com.example.api.ApiRoutes
import com.example.api.auth.SocialSignInRequest
import com.example.api.auth.TokenResponse
import com.example.api.common.ErrorCode
import com.example.api.common.ErrorResponse
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.network.ApiConfig
import com.liam.cmp_src.core.network.AuthTokens
import com.liam.cmp_src.core.network.InMemoryTokenStore
import com.liam.cmp_src.core.network.TokenStore
import com.liam.cmp_src.core.network.createHttpClient
import com.liam.cmp_src.feature.auth.data.AuthRepositoryImpl
import com.liam.cmp_src.feature.auth.data.remote.AuthApi
import com.liam.cmp_src.feature.auth.data.social.SocialAuthClient
import com.liam.cmp_src.feature.auth.data.social.SocialCredential
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import com.example.api.auth.SocialProvider as ContractSocialProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * The auth flow as the app runs it, from the use-case boundary down to the wire: the credential
 * a provider hands over, the call it is exchanged in, the profile that fills in what a token
 * response leaves out, and what each failure looks like by the time a screen sees it.
 *
 * Runs against a `MockEngine`, so it needs no server and passes on every target.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    @Test
    fun `an email sign-in returns the signed-in user`() = runTest {
        val repository = repository { request ->
            when (request.url.encodedPath) {
                ApiRoutes.Auth.LOGIN -> respondJson(json.encodeToString(TOKEN_RESPONSE))
                else -> respondJson(json.encodeToString(USER_RESPONSE))
            }
        }

        val result = repository.signInWithEmail(EMAIL, PASSWORD)

        val success = assertIs<AuthResult.Success>(result)
        assertEquals(USER_RESPONSE.id, success.user.id)
    }

    @Test
    fun `the signed-in user carries the linked providers only the profile call knows`() = runTest {
        // A TokenResponse's copy of the user has an empty linkedProviders by contract; the home
        // screen reads that list, so the sign-in has to follow up with GET /users/me.
        val paths = mutableListOf<String>()
        val repository = repository { request ->
            paths += request.url.encodedPath
            when (request.url.encodedPath) {
                ApiRoutes.Auth.LOGIN -> respondJson(json.encodeToString(TOKEN_RESPONSE))
                else -> respondJson(json.encodeToString(USER_RESPONSE))
            }
        }

        val success = assertIs<AuthResult.Success>(repository.signInWithEmail(EMAIL, PASSWORD))

        assertEquals(listOf(ApiRoutes.Auth.LOGIN, ApiRoutes.Users.ME), paths)
        assertEquals(listOf(SocialProvider.GOOGLE.key), success.user.linkedProviders)
    }

    @Test
    fun `a failed profile call does not undo a successful sign-in`() = runTest {
        val tokenStore = InMemoryTokenStore()
        val repository = repository(tokenStore = tokenStore) { request ->
            when (request.url.encodedPath) {
                ApiRoutes.Auth.LOGIN -> respondJson(json.encodeToString(TOKEN_RESPONSE))
                else -> throw IOException("profile call dropped")
            }
        }

        val success = assertIs<AuthResult.Success>(repository.signInWithEmail(EMAIL, PASSWORD))

        // The tokens are already stored, so the session is open — the response's own copy of the
        // user stands in for the one the profile call would have returned.
        assertEquals(TOKEN_RESPONSE.user, success.user)
        assertEquals(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN), tokenStore.tokens.value)
    }

    @Test
    fun `a rejected email and password is reported as invalid credentials`() = runTest {
        val repository = repository {
            respondJson(
                body = json.encodeToString(
                    ErrorResponse(ErrorCode.UNAUTHENTICATED, "Invalid email or password"),
                ),
                status = HttpStatusCode.Unauthorized,
            )
        }

        val failure = assertIs<AuthResult.Failure>(repository.signInWithEmail(EMAIL, "wrong"))
        assertEquals(AuthError.InvalidCredentials, failure.error)
    }

    @Test
    fun `an unreachable server is reported as a network failure`() = runTest {
        val repository = repository { throw IOException("connection refused") }

        val failure = assertIs<AuthResult.Failure>(repository.signInWithEmail(EMAIL, PASSWORD))
        assertEquals(AuthError.Network, failure.error)
    }

    @Test
    fun `a server fault is not blamed on the user's credentials`() = runTest {
        val repository = repository {
            respondJson(
                body = json.encodeToString(ErrorResponse(ErrorCode.INTERNAL_ERROR, "boom")),
                status = HttpStatusCode.InternalServerError,
            )
        }

        val failure = assertIs<AuthResult.Failure>(repository.signInWithEmail(EMAIL, PASSWORD))
        assertIs<AuthError.Unknown>(failure.error)
    }

    @Test
    fun `a social sign-in exchanges the provider's credential for a session`() = runTest {
        val socialAuthClient = FakeSocialAuthClient()
        var exchanged: SocialSignInRequest? = null
        val repository = repository(socialAuthClient = socialAuthClient) { request ->
            when (request.url.encodedPath) {
                ApiRoutes.Auth.SOCIAL -> {
                    exchanged = json.decodeFromString(request.bodyText())
                    respondJson(json.encodeToString(TOKEN_RESPONSE))
                }

                else -> respondJson(json.encodeToString(USER_RESPONSE))
            }
        }

        val success = assertIs<AuthResult.Success>(repository.signInWith(SocialProvider.GOOGLE))

        assertEquals(SocialProvider.GOOGLE, socialAuthClient.lastProvider)
        assertEquals(
            SocialSignInRequest(
                provider = ContractSocialProvider.GOOGLE,
                token = FakeSocialAuthClient.TOKEN,
            ),
            exchanged,
            "the token from the provider is what the server is asked to verify",
        )
        assertEquals(USER_RESPONSE.id, success.user.id)
    }

    @Test
    fun `a dismissed provider sheet never reaches the server`() = runTest {
        val socialAuthClient = FakeSocialAuthClient(
            result = SocialCredential.Denied(AuthError.Cancelled),
        )
        var requests = 0
        val repository = repository(socialAuthClient = socialAuthClient) {
            requests++
            respondJson(json.encodeToString(TOKEN_RESPONSE))
        }

        val failure = assertIs<AuthResult.Failure>(repository.signInWith(SocialProvider.GOOGLE))

        assertEquals(AuthError.Cancelled, failure.error)
        assertEquals(0, requests, "there is nothing to exchange when no credential was granted")
    }

    @Test
    fun `a provider the deployment has not enabled is reported as unavailable`() = runTest {
        val repository = repository {
            respondJson(
                body = json.encodeToString(
                    ErrorResponse(ErrorCode.PROVIDER_NOT_ENABLED, "facebook is not configured"),
                ),
                status = HttpStatusCode.BadRequest,
            )
        }

        val failure = assertIs<AuthResult.Failure>(repository.signInWith(SocialProvider.FACEBOOK))
        assertEquals(AuthError.ProviderUnavailable(SocialProvider.FACEBOOK), failure.error)
    }

    @Test
    fun `a refused provider token is not reported as a wrong email and password`() = runTest {
        // The user never typed either, so "that email and password don't match" would be a lie.
        val repository = repository {
            respondJson(
                body = json.encodeToString(
                    ErrorResponse(ErrorCode.UNAUTHENTICATED, "Invalid provider token"),
                ),
                status = HttpStatusCode.Unauthorized,
            )
        }

        val failure = assertIs<AuthResult.Failure>(repository.signInWith(SocialProvider.GOOGLE))
        assertIs<AuthError.Unknown>(failure.error)
    }

    @Test
    fun `signing out ends the session even when the server call fails`() = runTest {
        val tokenStore = InMemoryTokenStore(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        val repository = repository(tokenStore = tokenStore) { throw IOException("offline") }

        repository.signOut()

        assertNull(tokenStore.tokens.value)
    }

    @Test
    fun `signing out tells the server first`() = runTest {
        val tokenStore = InMemoryTokenStore(AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN))
        var loggedOutWith: String? = null
        val repository = repository(tokenStore = tokenStore) { request ->
            loggedOutWith = request.headers[HttpHeaders.Authorization]
            respond("", HttpStatusCode.NoContent)
        }

        repository.signOut()

        assertEquals(
            "Bearer $ACCESS_TOKEN",
            loggedOutWith,
            "the session being ended has to identify itself",
        )
        assertNull(tokenStore.tokens.value)
    }

    // ---- fixtures -------------------------------------------------------------------------

    private fun TestScope.repository(
        socialAuthClient: SocialAuthClient = FakeSocialAuthClient(),
        tokenStore: TokenStore = InMemoryTokenStore(),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): AuthRepositoryImpl {
        val client = createHttpClient(
            tokenStore = tokenStore,
            config = ApiConfig(baseUrl = BASE_URL),
            engine = MockEngine(handler),
            logger = Logger.EMPTY,
            logLevel = LogLevel.NONE,
        )
        return AuthRepositoryImpl(
            authApi = AuthApi(client = client, tokenStore = tokenStore),
            socialAuthClient = socialAuthClient,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private suspend fun HttpRequestData.bodyText(): String =
        (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

    private companion object {
        const val BASE_URL = "https://api.test"
        const val EMAIL = "demo@cmpsrc.dev"
        const val PASSWORD = "password123"
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"

        val json = Json { ignoreUnknownKeys = true }

        /** What `GET /users/me` returns: the full user, linked providers included. */
        val USER_RESPONSE = UserResponse(
            id = "u1",
            email = EMAIL,
            displayName = "Demo User",
            avatarUrl = null,
            isEmailVerified = true,
            createdAt = "2026-01-01T00:00:00Z",
            linkedProviders = listOf(SocialProvider.GOOGLE.key),
        )

        val TOKEN_RESPONSE = TokenResponse(
            accessToken = ACCESS_TOKEN,
            refreshToken = REFRESH_TOKEN,
            expiresIn = 3600,
            // Empty linkedProviders, exactly as the contract publishes it.
            user = USER_RESPONSE.copy(linkedProviders = emptyList()),
        )
    }
}
