package com.liam.cmp_src.core.network

import com.example.api.ApiRoutes
import com.example.api.auth.RefreshTokenRequest
import com.example.api.auth.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The engine this platform's HTTP calls run on.
 *
 * Actuals live in `HttpClientFactory.<target>.kt`, matching the `Platform.<target>.kt` convention.
 * Each one sets its own connect timeout from [ApiConfig.CONNECT_TIMEOUT_MILLIS], because that knob
 * is engine-level and the browser engine ignores it when set from common code.
 */
expect fun platformEngine(): HttpClientEngine

/**
 * The JSON format the whole app speaks.
 *
 * [ignoreUnknownKeys] is not laxness — the contract requires a client built against today's shape
 * to keep working when the server adds a field, and the default would throw instead. Nulls are
 * dropped on the way out so an omitted optional (`RegisterRequest.displayName`) is sent as absent
 * rather than as an explicit `null`.
 */
private val apiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** 5xx is worth another try; a request the server never answered may have arrived all the same. */
private val RETRYABLE_METHODS = setOf(
    HttpMethod.Get,
    HttpMethod.Head,
    HttpMethod.Options,
    HttpMethod.Put,
    HttpMethod.Delete,
)

/** The endpoints reached while signed out. Everything else gets the access token up front. */
private val UNAUTHENTICATED_PATHS = setOf(
    ApiRoutes.Auth.REGISTER,
    ApiRoutes.Auth.LOGIN,
    ApiRoutes.Auth.SOCIAL,
    ApiRoutes.Auth.REFRESH,
)

private const val MAX_RETRIES = 2

/**
 * Builds the app's single [HttpClient].
 *
 * Create one and share it: a client owns a connection pool and a coroutine scope, so building one
 * per call leaks both. The DI module holds the only instance.
 *
 * What is installed, and why:
 * - **ContentNegotiation** — the contract's DTOs in and out, over [apiJson].
 * - **Auth (bearer)** — attaches the access token, and on a 401 spends the refresh token against
 *   `/auth/refresh` and replays the request once. [tokenStore] is both the source and the sink, so
 *   a refresh survives into the next call.
 * - **HttpTimeout** — a ceiling on the whole call. Connect timeouts are set per engine instead.
 * - **HttpRequestRetry** — 5xx only, and only for methods that are safe to send twice. A retried
 *   `POST /auth/register` would risk a second account.
 * - **Logging** — method, URL and status. Deliberately not [LogLevel.BODY]: login bodies carry
 *   passwords and token bodies carry credentials, and raising [logLevel] here writes both to the
 *   platform log. The `Authorization` header is redacted at any level.
 *
 * `expectSuccess` stays false so a non-2xx response comes back as a value for `sendRequest` to
 * classify, rather than as an exception thrown from somewhere inside the plugin pipeline.
 */
fun createHttpClient(
    tokenStore: TokenStore,
    config: ApiConfig = ApiConfig(),
    engine: HttpClientEngine = platformEngine(),
    logger: Logger = Logger.SIMPLE,
    logLevel: LogLevel = LogLevel.INFO,
): HttpClient = HttpClient(engine) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(apiJson)
    }

    install(Auth) {
        bearer {
            loadTokens {
                tokenStore.tokens.value?.let { BearerTokens(it.accessToken, it.refreshToken) }
            }

            refreshTokens {
                val refreshToken = oldTokens?.refreshToken
                    ?: tokenStore.tokens.value?.refreshToken
                    ?: return@refreshTokens null

                // `client` here is the same client with this plugin disabled, so refreshing
                // cannot recurse into another refresh.
                val response = client.post(ApiRoutes.Auth.REFRESH) {
                    markAsRefreshTokenRequest()
                    contentType(ContentType.Application.Json)
                    setBody(RefreshTokenRequest(refreshToken))
                }

                if (!response.status.isSuccess()) {
                    // The refresh token is spent or revoked: this session is over, and holding
                    // on to it would retry a doomed refresh on every subsequent call.
                    tokenStore.clear()
                    return@refreshTokens null
                }

                val tokens = response.body<TokenResponse>()
                tokenStore.save(AuthTokens(tokens.accessToken, tokens.refreshToken))
                BearerTokens(tokens.accessToken, tokens.refreshToken)
            }

            sendWithoutRequest { request ->
                UNAUTHENTICATED_PATHS.none { request.url.encodedPath.endsWith(it) }
            }
        }
    }

    install(HttpTimeout) {
        requestTimeoutMillis = config.requestTimeoutMillis
    }

    install(HttpRequestRetry) {
        retryIf(maxRetries = MAX_RETRIES) { request, response ->
            response.status.value in 500..599 && request.method in RETRYABLE_METHODS
        }
        exponentialDelay()
    }

    install(Logging) {
        this.logger = logger
        level = logLevel
        sanitizeHeader { header -> header.equals(HttpHeaders.Authorization, ignoreCase = true) }
    }

    defaultRequest {
        url(config.baseUrl)
        accept(ContentType.Application.Json)
    }
}

/**
 * Drops the `Auth` plugin's cached copy of the tokens so the next request re-reads [TokenStore].
 *
 * The bearer provider caches what `loadTokens` handed it and reloads only when it is cleared or a
 * refresh replaces it — writing to the store is not enough on its own. Without this, signing out
 * leaves a client that keeps presenting the token the server has just revoked.
 *
 * Call it after any write to the store that did not come from the refresh flow.
 */
fun HttpClient.invalidateAuthCache() {
    authProvider<BearerAuthProvider>()?.clearToken()
}
