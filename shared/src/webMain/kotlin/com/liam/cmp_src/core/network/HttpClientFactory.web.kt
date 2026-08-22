package com.liam.cmp_src.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

/**
 * The browser's own `fetch`, shared by the JS and Wasm targets — `ktor-client-js` publishes both,
 * and the configuration is identical, so one actual in `webMain` beats two that would drift.
 *
 * Nothing to configure: connect timeouts, the proxy and the connection pool all belong to the
 * browser here, and requests are subject to its CORS rules. A local dev server therefore has to
 * allow this app's origin — the other targets have no such constraint.
 */
actual fun platformEngine(): HttpClientEngine = Js.create()
