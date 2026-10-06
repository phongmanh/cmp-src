# core:network

Ktor plumbing every network-backed feature shares: the one `HttpClient` (bearer auth with refresh,
timeouts, retry, logging), `ApiResult`/`ApiError` and the single place an exception becomes an
`ApiError`, the `TokenStore` boundary, base-URL configuration and the avatar URL helpers. Exposes
Ktor core and coroutines through `api`; uses `api-contract` for routes and DTOs. Per-feature API
classes (`AuthApi`, `ProfileApi`, `CustomerApi`) live in their features.

## Classes

```mermaid
classDiagram
    direction TB
    class HttpClientFactory {
        <<file>>
        +createHttpClient(tokenStore: TokenStore, config: ApiConfig, engine: HttpClientEngine, logger: Logger, logLevel: LogLevel) HttpClient
        +HttpClient.invalidateAuthCache()
        +platformEngine() HttpClientEngine
    }
    class ApiCall {
        <<file>>
        +apiCall~T~(request: suspend () -> HttpResponse) ApiResult~T~
        +apiCallForStatus(request: suspend () -> HttpResponse) ApiResult~Unit~
        ~sendRequest(request: suspend () -> HttpResponse) ApiResult~HttpResponse~
        -HttpResponse.toApiError() ApiError.Http
    }
    class ApiResult~T~ {
        <<sealed>>
        Success(data: T)
        Failure(error: ApiError)
        +map(transform: (T) -> R) ApiResult~R~
        +getOrNull() T?
    }
    class ApiError {
        <<sealed>>
        Network
        Timeout
        Http(status: Int, code: String?, message: String?)
        Serialization(message: String?)
        Unknown(message: String?)
    }
    class ApiConfig {
        <<data>>
        +environment: ApiEnvironment
        +baseUrl: String
        +requestTimeoutMillis: Long
        +DEFAULT_REQUEST_TIMEOUT_MILLIS = 30_000$
        +CONNECT_TIMEOUT_MILLIS = 15_000$
        +LOCAL_PORT = 8080$
        +PRODUCTION_BASE_URL$
    }
    class ApiEnvironment {
        <<enumeration>>
        LOCAL
        PRODUCTION
        +baseUrl: String
        +ACTIVE: ApiEnvironment$
    }
    class TokenStore {
        <<interface>>
        +tokens: Flow~AuthTokens?~
        +current() AuthTokens?
        +save(tokens: AuthTokens)
        +clear()
    }
    class InMemoryTokenStore {
        -state: MutableStateFlow~AuthTokens?~
    }
    class AuthTokens {
        <<data>>
        +accessToken: String
        +refreshToken: String
    }
    class ImageUrls {
        <<file>>
        +isStoredImageUrl(url: String) Boolean
        +sameOriginImageUrl(url: String, baseUrl: String) String?
    }
    class PlatformActuals {
        <<expect/actual>>
        platformEngine() OkHttp on Android, Darwin on iOS
        localApiHost() 10.0.2.2 on Android, localhost on iOS
    }
    class RoomTokenStore {
        <<core:database>>
    }

    InMemoryTokenStore ..|> TokenStore : implements
    RoomTokenStore ..|> TokenStore : implements
    TokenStore ..> AuthTokens : holds
    HttpClientFactory --> TokenStore : loadTokens / refreshTokens read and write
    HttpClientFactory --> ApiConfig : baseUrl, timeout
    HttpClientFactory --> PlatformActuals : engine
    ApiConfig --> ApiEnvironment : default from ACTIVE
    ApiEnvironment --> PlatformActuals : LOCAL uses localApiHost
    ApiCall ..> ApiResult : returns
    ApiResult *-- ApiError : Failure carries
```

## Sequences

### Authenticated request

How every feature API call goes out and comes back as an `ApiResult`. Nothing above this layer
catches an exception.

```mermaid
sequenceDiagram
    participant F as Feature API (e.g. CustomerApi)
    participant AC as apiCall / sendRequest
    participant HC as HttpClient
    participant TS as TokenStore
    participant S as Server
    F->>AC: apiCall { client.get(route) }
    AC->>HC: request()
    HC->>TS: loadTokens: current()
    TS-->>HC: AuthTokens or null
    HC->>S: GET route, Authorization: Bearer access
    Note over HC,S: HttpRequestRetry retries 5xx up to 2 times, idempotent methods only
    alt 2xx
        S-->>HC: 200 + JSON
        HC-->>AC: HttpResponse
        AC->>AC: body() decode
        AC-->>F: ApiResult.Success(data)
    else non-2xx
        S-->>HC: 4xx/5xx + ErrorResponse
        AC-->>F: ApiResult.Failure(Http(status, code, message))
    else no connection
        HC--xAC: IOException
        AC-->>F: ApiResult.Failure(Network)
    else timeout
        HC--xAC: HttpRequestTimeout / ConnectTimeout / SocketTimeout
        AC-->>F: ApiResult.Failure(Timeout)
    else bad JSON
        AC-->>F: ApiResult.Failure(Serialization)
    end
    Note over AC: CancellationException is rethrown, never turned into an ApiError
```

### Token refresh on 401

The `Auth` plugin's bearer provider refreshes once, transparently to the caller.

```mermaid
sequenceDiagram
    participant HC as HttpClient (Auth plugin)
    participant TS as TokenStore
    participant S as Server
    HC->>S: request with expired access token
    S-->>HC: 401
    HC->>HC: refreshTokens (plugin disabled for this call, so no recursion)
    HC->>S: POST /api/v1/auth/refresh RefreshTokenRequest(refreshToken)
    alt refreshed
        S-->>HC: 200 TokenResponse
        HC->>TS: save(AuthTokens(access, refresh))
        HC->>S: original request with new access token
        S-->>HC: response
    else refresh refused
        S-->>HC: 4xx
        HC->>TS: clear()
        Note over HC: returns null, so the caller sees the 401 as ApiError.Http
    end
```
