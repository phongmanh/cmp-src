package com.liam.cmp_src.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

/** The same engine and settings as Android, so desktop and mobile fail the same way. */
actual fun platformEngine(): HttpClientEngine = OkHttp.create {
    config {
        retryOnConnectionFailure(true)
        connectTimeout(ApiConfig.CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
    }
}
