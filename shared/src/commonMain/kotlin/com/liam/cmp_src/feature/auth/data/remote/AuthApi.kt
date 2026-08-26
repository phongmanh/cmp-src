package com.liam.cmp_src.feature.auth.data.remote

import com.example.api.ApiRoutes
import com.example.api.auth.LoginRequest
import com.example.api.auth.RegisterRequest
import com.example.api.auth.SocialProvider
import com.example.api.auth.SocialSignInRequest
import com.example.api.auth.TokenResponse
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.network.ApiResult
import com.liam.cmp_src.core.network.AuthTokens
import com.liam.cmp_src.core.network.TokenStore
import com.liam.cmp_src.core.network.apiCall
import com.liam.cmp_src.core.network.apiCallForStatus
import com.liam.cmp_src.core.network.invalidateAuthCache
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Everything the auth session needs from the server, spoken entirely in `api-contract` types.
 *
 * Paths come from [ApiRoutes] rather than string literals, so a route the server renames breaks
 * this file at compile time instead of at runtime. Nothing here throws — see [apiCall].
 *
 * This class owns the token lifecycle as well as the calls: a successful sign-in writes to
 * [tokenStore], which is the same store the client's `Auth` plugin reads from and refreshes into.
 * Splitting the two would mean every caller had to remember to save, and forgetting once leaves a
 * client that is authenticated in theory and anonymous in practice.
 */
class AuthApi(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
) {

    /** Creates an account and signs it in. [displayName] is optional per the contract. */
    suspend fun register(
        email: String,
        password: String,
        displayName: String? = null,
    ): ApiResult<TokenResponse> = authCall {
        postJson(
            path = ApiRoutes.Auth.REGISTER,
            body = RegisterRequest(email = email, password = password, displayName = displayName),
        )
    }

    suspend fun login(email: String, password: String): ApiResult<TokenResponse> = authCall {
        postJson(ApiRoutes.Auth.LOGIN, LoginRequest(email = email, password = password))
    }

    /**
     * Exchanges a provider credential for this API's own tokens.
     *
     * [token] is what the provider's SDK handed the app — an OpenID Connect id_token for Google,
     * an access token for Facebook. It is spent here and never stored.
     */
    suspend fun signInWithSocial(
        provider: SocialProvider,
        token: String,
    ): ApiResult<TokenResponse> = authCall {
        postJson(ApiRoutes.Auth.SOCIAL, SocialSignInRequest(provider = provider, token = token))
    }

    /**
     * The signed-in user, with `linkedProviders` filled in — the copy inside a `TokenResponse`
     * leaves that list empty, so this is the call that answers "which providers are linked".
     *
     * Authenticated: the client attaches the access token, and refreshes it first if the server
     * rejects it.
     */
    suspend fun currentUser(): ApiResult<UserResponse> =
        apiCall { client.get(ApiRoutes.Users.ME) }

    /**
     * Ends this session, server-side and locally.
     *
     * The local tokens are dropped whether or not the server answered: a user who asked to sign
     * out is signed out, and a refresh token left behind after a failed call is a credential kept
     * on the device for no reason.
     */
    suspend fun logout(): ApiResult<Unit> {
        val result = apiCallForStatus { client.post(ApiRoutes.Auth.LOGOUT) }
        tokenStore.clear()
        client.invalidateAuthCache()
        return result
    }

    /**
     * Posts [body] as JSON.
     *
     * `ContentNegotiation` only serializes a typed body when the request declares a content type
     * it has a converter for — without this the body would be handed to the engine as an unknown
     * object and the call would fail before leaving the device. Declaring it here rather than in
     * `defaultRequest` keeps the header off GETs, which have no body to describe.
     */
    private suspend inline fun <reified B : Any> postJson(path: String, body: B): HttpResponse =
        client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    /** Runs a call that returns a fresh session, and adopts its tokens when it succeeds. */
    private suspend fun authCall(
        request: suspend () -> HttpResponse,
    ): ApiResult<TokenResponse> {
        val result = apiCall<TokenResponse> { request() }
        if (result is ApiResult.Success) {
            tokenStore.save(AuthTokens(result.data.accessToken, result.data.refreshToken))
            client.invalidateAuthCache()
        }
        return result
    }
}
