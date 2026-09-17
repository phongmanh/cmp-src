package com.liam.cmp_src.di

import com.liam.cmp_src.core.domain.usecase.SignOutUseCase
import com.liam.cmp_src.core.network.ApiConfig
import com.liam.cmp_src.core.network.TokenStore
import com.liam.cmp_src.core.network.createHttpClient
import com.liam.cmp_src.feature.auth.di.authModule
import com.liam.cmp_src.feature.home.di.homeModule
import com.liam.cmp_src.feature.profile.di.profileModule
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * The app's object graph: what no single feature owns, plus each feature's own module.
 *
 * One `CoroutineDispatcher` for the whole graph, injected here rather than referenced inside
 * each repository — that indirection is what lets tests swap in a `TestDispatcher`.
 *
 * The [HttpClient] is a `single` because it owns a connection pool and a coroutine scope —
 * one per call would leak both. [ApiConfig] is a separate binding so a test or a staging build
 * can override the base URL without redefining the client.
 *
 * `SignOutUseCase` is bound here rather than in a feature because home and profile both offer it.
 *
 * [TokenStore] is bound by `rememberPlatformModule` rather than here: where the tokens live
 * differs by target, and only Android can supply the `Context` its database builder needs.
 */
val appModule = module {
    includes(authModule, homeModule, profileModule)

    single<CoroutineDispatcher> { Dispatchers.Default }

    single { ApiConfig() }

    single<HttpClient> { createHttpClient(tokenStore = get(), config = get()) }

    factoryOf(::SignOutUseCase)
}
