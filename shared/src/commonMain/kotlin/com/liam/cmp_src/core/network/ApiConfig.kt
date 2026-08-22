package com.liam.cmp_src.core.network

/**
 * Which backend the app talks to.
 *
 * The choice lives here, not in per-target code, so switching is one edit in one file and every
 * target — Android, iOS, desktop, browser — moves together. Only [LOCAL] still needs a
 * platform-specific value, and only for its host (see [localApiHost]); the production URL is a
 * single shared constant.
 */
enum class ApiEnvironment {
    /** A dev server on the machine running the app, reached over cleartext HTTP. */
    LOCAL,

    /** The deployed backend, HTTPS only. */
    PRODUCTION,
    ;

    /** Scheme, host and port — never a path, because every `ApiRoutes` path is absolute. */
    val baseUrl: String
        get() = when (this) {
            LOCAL -> "http://${localApiHost()}:${ApiConfig.LOCAL_PORT}"
            PRODUCTION -> ApiConfig.PRODUCTION_BASE_URL
        }

    companion object {
        /**
         * The environment every default-constructed [ApiConfig] uses.
         *
         * **This is the switch.** Flip it to [PRODUCTION] to point the whole app at the deployed
         * backend; nothing else needs to change. Anything that must not follow the switch — a
         * test, a staging build — passes its own environment or base URL to [ApiConfig] instead.
         */
        val ACTIVE: ApiEnvironment = LOCAL
    }
}

/**
 * Where the app sends requests, and how long it is willing to wait for one.
 *
 * Injected rather than read from a global: tests point it at a `MockEngine`, and a staging or
 * production build overrides [environment] or [baseUrl] without any client code changing. Nothing
 * secret belongs in here — it is a host and a timeout, both of which ship in the binary anyway.
 *
 * [baseUrl] carries scheme, host and port only, with no path. Every path in `ApiRoutes` is
 * absolute and already versioned, so a request appends nothing and assembles nothing by hand.
 * It defaults to [environment]'s URL, so `ApiConfig(ApiEnvironment.PRODUCTION)` is enough to
 * retarget one caller, and `ApiConfig(baseUrl = mockServerUrl)` still wins outright for a test.
 */
data class ApiConfig(
    val environment: ApiEnvironment = ApiEnvironment.ACTIVE,
    val baseUrl: String = environment.baseUrl,
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
) {
    companion object {
        /** Covers the whole call — connect, send, receive — not a single socket operation. */
        const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 30_000L

        /**
         * Applied by each engine in `HttpClientFactory.<target>.kt` rather than by the common
         * `HttpTimeout` plugin, because the browser engine cannot honour a connect timeout set
         * from common code and quietly ignores it.
         */
        const val CONNECT_TIMEOUT_MILLIS = 15_000L

        /** The port the api-contract server listens on in local development. */
        const val LOCAL_PORT = 8080

        /**
         * The deployed backend, shared by every target.
         *
         * HTTPS is not optional: the Android debug-only network security config and iOS App
         * Transport Security both permit cleartext to the local host alone, so an `http://` URL
         * here fails on device rather than falling back.
         */
        const val PRODUCTION_BASE_URL = "https://api.example.com"
    }
}

/**
 * The host that reaches a locally running server from this target — the one thing [ApiEnvironment]
 * cannot state once for everyone.
 *
 * Every target sees the host machine as `localhost` except the Android emulator, which is its own
 * virtual device and reaches the host's loopback at `10.0.2.2`. A single shared constant would
 * silently fail on exactly one target, which is why this stays `expect`/`actual` while the
 * production URL does not.
 */
internal expect fun localApiHost(): String
