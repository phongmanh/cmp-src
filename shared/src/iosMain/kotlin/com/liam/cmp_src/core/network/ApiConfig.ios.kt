package com.liam.cmp_src.core.network

/**
 * The simulator shares the host's network stack, so `localhost` is the host machine. A physical
 * device does not — point [ApiConfig.baseUrl] at the machine's LAN address there.
 *
 * App Transport Security already exempts `localhost` from its HTTPS requirement, so this needs no
 * `Info.plist` change; a LAN address would.
 */
internal actual fun localApiHost(): String = "localhost"
