package com.liam.cmp_src.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * Darwin, which routes through `NSURLSession` and so inherits the system's proxy, ATS and
 * background-transfer behaviour rather than reimplementing it.
 *
 * This actual sits in `iosMain` rather than `nativeMain`: the engine covers every Apple target,
 * and declaring it a level up would collide with this one the moment a non-Apple native target
 * is added.
 */
actual fun platformEngine(): HttpClientEngine = Darwin.create {
    configureRequest {
        setAllowsCellularAccess(true)
        setTimeoutInterval(ApiConfig.CONNECT_TIMEOUT_MILLIS / MILLIS_PER_SECOND)
    }
}

private const val MILLIS_PER_SECOND = 1000.0
