package com.liam.cmp_src.core.network

import com.example.api.ApiRoutes

/**
 * What the app knows about the addresses this API publishes for stored images.
 *
 * Both questions below are asked of an `avatarUrl`, which the server derives rather than echoes:
 * an uploaded picture is published under [ApiRoutes.Images], while a picture the account brought
 * from a social provider is whatever address that provider gave.
 */

/**
 * True when [url] is a picture this backend stores, rather than one hosted somewhere else.
 *
 * The difference decides whether an address survives a profile write: `PUT /users/me` retires an
 * uploaded avatar whatever it is sent, so echoing one back would leave a stored address pointing
 * at an image that had just been deleted. A provider's address is unaffected and must be echoed.
 */
internal fun isStoredImageUrl(url: String): Boolean =
    url.absolutePath()?.startsWith(ApiRoutes.Images.PATH) == true

/**
 * Re-points a stored image at the backend *this device* can actually reach, or `null` when there
 * is nothing to change.
 *
 * The server builds the address from its own `PUBLIC_BASE_URL`, a single value for every client.
 * In production that is the same https origin the app already calls and this does nothing. In
 * local development it is `http://localhost:8080`, and `localhost` on an Android emulator is the
 * handset rather than the machine running the server — the picture would never load while every
 * other call succeeded against `10.0.2.2`.
 *
 * Returning `null` for "nothing to change" is also what Coil's `Mapper` contract asks for; see
 * `avatarImageLoaderFactory`.
 */
internal fun sameOriginImageUrl(url: String, baseUrl: String): String? {
    if (!isStoredImageUrl(url)) return null
    val path = url.absolutePath() ?: return null
    return (baseUrl.trimEnd('/') + path).takeIf { it != url }
}

/**
 * The path of an absolute `scheme://host[:port]/path` address, or `null` when [this] is not one.
 *
 * Hand-rolled rather than parsed: a malformed avatar URL has to leave this as "not one of ours",
 * never as an exception thrown while an image is being loaded or a profile saved.
 */
private fun String.absolutePath(): String? {
    val schemeEnd = indexOf("://")
    if (schemeEnd <= 0) return null
    val pathStart = indexOf('/', startIndex = schemeEnd + "://".length)
    return if (pathStart < 0) "" else substring(pathStart)
}
