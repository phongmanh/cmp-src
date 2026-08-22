package com.liam.cmp_src.core.network

/**
 * The dev server's own origin, which is not the webpack dev server this app is served from —
 * so the API has to send CORS headers for that origin, or the browser drops the response before
 * Ktor sees it. Serving both from one origin in production removes the problem entirely.
 */
internal actual fun localApiHost(): String = "localhost"
