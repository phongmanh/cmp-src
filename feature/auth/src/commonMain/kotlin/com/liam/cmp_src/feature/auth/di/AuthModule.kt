package com.liam.cmp_src.feature.auth.di

import com.liam.cmp_src.core.domain.repository.AuthRepository
import com.liam.cmp_src.feature.auth.data.AuthRepositoryImpl
import com.liam.cmp_src.feature.auth.data.remote.AuthApi
import com.liam.cmp_src.feature.auth.data.social.SocialAuthClient
import com.liam.cmp_src.feature.auth.data.social.createSocialAuthClient
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithEmailUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithSocialUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignUpUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateCredentialsUseCase
import com.liam.cmp_src.feature.auth.presentation.login.LoginViewModel
import com.liam.cmp_src.feature.auth.presentation.signup.SignUpViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The auth feature's graph: sign-in and sign-up, and the [AuthRepository] every other feature
 * reads the session through.
 *
 * Expects the app graph to provide the `HttpClient`, `TokenStore` and `CoroutineDispatcher`.
 */
val authModule = module {
    single { AuthApi(client = get(), tokenStore = get()) }
    single<SocialAuthClient> { createSocialAuthClient() }
    single<AuthRepository> {
        AuthRepositoryImpl(authApi = get(), socialAuthClient = get(), dispatcher = get())
    }

    factoryOf(::SignInWithEmailUseCase)
    factoryOf(::SignInWithSocialUseCase)
    factoryOf(::ValidateCredentialsUseCase)
    factoryOf(::SignUpUseCase)

    viewModelOf(::LoginViewModel)
    viewModelOf(::SignUpViewModel)
}
