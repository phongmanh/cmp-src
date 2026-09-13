package com.liam.cmp_src.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.liam.cmp_src.core.network.InMemoryTokenStore
import com.liam.cmp_src.core.network.TokenStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The browser targets have no SQLite driver to build a database with, so the session lives in
 * memory and ends with the tab. Persisting it would mean `androidx.sqlite:sqlite-async` plus a
 * WASM SQLite build served from an OPFS worker.
 */
@Composable
actual fun rememberPlatformModule(): Module = remember {
    module { single<TokenStore> { InMemoryTokenStore() } }
}
