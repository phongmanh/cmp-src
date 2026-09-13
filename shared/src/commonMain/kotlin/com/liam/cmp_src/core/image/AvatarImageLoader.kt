package com.liam.cmp_src.core.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.map.Mapper
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.liam.cmp_src.core.network.ApiConfig
import com.liam.cmp_src.core.network.sameOriginImageUrl
import io.ktor.client.HttpClient

/**
 * Teaches Coil how to fetch this app's images, once, for every screen that shows one.
 *
 * Called from `App()` before anything composes an avatar, because Coil resolves its singleton
 * loader the first time an image is requested and keeps whatever it found.
 *
 * Two components are registered:
 *  - a [Mapper] that re-points an uploaded avatar at the backend this build talks to, for the
 *    reason spelled out in [sameOriginImageUrl];
 *  - the Ktor fetcher, wired to the [HttpClient] already in the graph rather than one of Coil's
 *    own, so image traffic shares the connection pool and the engine each target already has.
 */
@Composable
@ReadOnlyComposable
fun setAvatarImageLoaderFactory(client: HttpClient, config: ApiConfig) {
    setSingletonImageLoaderFactory { context -> avatarImageLoaderFactory(context, client, config) }
}

/** Extracted from the composable above so it can be built, and asserted on, outside a composition. */
internal fun avatarImageLoaderFactory(
    context: PlatformContext,
    client: HttpClient,
    config: ApiConfig,
): ImageLoader = ImageLoader.Builder(context)
    .components {
        add<String>(Mapper { data, _ -> sameOriginImageUrl(data, config.baseUrl) })
        add(KtorNetworkFetcherFactory(httpClient = { client }))
    }
    .crossfade(true)
    .build()
