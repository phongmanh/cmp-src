package com.liam.cmp_src.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

/**
 * OkHttp — the stack Android already ships around, and the one the desktop target uses too, so
 * both JVM platforms behave identically under a flaky connection.
 */
actual fun platformEngine(): HttpClientEngine = OkHttp.create {
    config {
        retryOnConnectionFailure(true)
        connectTimeout(ApiConfig.CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
    }
}
