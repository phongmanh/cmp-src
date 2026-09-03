package com.liam.cmp_src.di

import com.liam.cmp_src.core.network.ApiConfig
import com.liam.cmp_src.core.network.InMemoryTokenStore
import com.liam.cmp_src.core.network.TokenStore
import com.liam.cmp_src.core.network.createHttpClient
import com.liam.cmp_src.feature.auth.data.AuthRepositoryImpl
import com.liam.cmp_src.feature.auth.data.remote.AuthApi
import com.liam.cmp_src.feature.auth.data.social.SocialAuthClient
import com.liam.cmp_src.feature.auth.data.social.createSocialAuthClient
import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository
import com.liam.cmp_src.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithEmailUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithSocialUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignOutUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignUpUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateCredentialsUseCase
import com.liam.cmp_src.feature.auth.presentation.login.LoginViewModel
import com.liam.cmp_src.feature.auth.presentation.signup.SignUpViewModel
import com.liam.cmp_src.feature.home.HomeViewModel
import com.liam.cmp_src.feature.profile.ProfileViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The app's object graph.
 *
 * `Dispatchers.Default` rather than `Dispatchers.IO` because IO does not exist on the JS and
 * Wasm targets; injecting it here (instead of referencing a dispatcher inside the repository)
 * is what lets tests swap in a `TestDispatcher`.
 *
 * The [HttpClient] is a `single` because it owns a connection pool and a coroutine scope —
 * one per call would leak both. [ApiConfig] is a separate binding so a test or a staging build
 * can override the base URL without redefining the client.
 */
val appModule = module {
    single<CoroutineDispatcher> { Dispatchers.Default }

    single { ApiConfig() }
    single<TokenStore> { InMemoryTokenStore() }
    single<HttpClient> { createHttpClient(tokenStore = get(), config = get()) }
    single { AuthApi(client = get(), tokenStore = get()) }

    single<SocialAuthClient> { createSocialAuthClient() }
    single<AuthRepository> {
        AuthRepositoryImpl(authApi = get(), socialAuthClient = get(), dispatcher = get())
    }

    factoryOf(::SignInWithEmailUseCase)
    factoryOf(::SignInWithSocialUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ValidateCredentialsUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::GetCurrentUserUseCase)

    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SignUpViewModel)
    viewModelOf(::ProfileViewModel)
}
