package com.liam.cmp_src.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

/**
 * OkHttp — the stack Android already ships around, with its own connect timeout because the
 * common `HttpTimeout` plugin does not set one.
 */
actual fun platformEngine(): HttpClientEngine = OkHttp.create {
    config {
        retryOnConnectionFailure(true)
        connectTimeout(ApiConfig.CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
    }
}
