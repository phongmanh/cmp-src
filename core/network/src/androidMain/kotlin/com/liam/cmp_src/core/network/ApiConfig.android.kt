package com.liam.cmp_src.core.network

/**
 * `10.0.2.2`, not `localhost`.
 *
 * The emulator is its own virtual device, so `localhost` there is the emulator itself; `10.0.2.2`
 * is the alias it exposes for the host machine's loopback. On a physical device neither works —
 * point [ApiConfig.baseUrl] at the machine's LAN address instead.
 *
 * Cleartext to this host is permitted only by the debug build's network security config; release
 * builds reach [ApiConfig.PRODUCTION_BASE_URL] over HTTPS or nothing.
 */
internal actual fun localApiHost(): String = "10.0.2.2"
