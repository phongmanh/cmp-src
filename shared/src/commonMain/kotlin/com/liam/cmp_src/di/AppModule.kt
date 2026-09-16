package com.liam.cmp_src.di

import com.liam.cmp_src.core.network.ApiConfig
import com.liam.cmp_src.core.network.TokenStore
import com.liam.cmp_src.core.network.createHttpClient
import com.liam.cmp_src.feature.auth.data.AuthRepositoryImpl
import com.liam.cmp_src.feature.auth.data.remote.AuthApi
import com.liam.cmp_src.feature.auth.data.social.SocialAuthClient
import com.liam.cmp_src.feature.auth.data.social.createSocialAuthClient
import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository
import com.liam.cmp_src.feature.auth.domain.usecase.ChangePasswordUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithEmailUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithSocialUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignOutUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignUpUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateChangePasswordUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateCredentialsUseCase
import com.liam.cmp_src.feature.auth.presentation.login.LoginViewModel
import com.liam.cmp_src.feature.auth.presentation.signup.SignUpViewModel
import com.liam.cmp_src.feature.home.HomeViewModel
import com.liam.cmp_src.feature.profile.ProfileViewModel
import com.liam.cmp_src.feature.profile.changepassword.ChangePasswordViewModel
import com.liam.cmp_src.feature.profile.data.ProfileRepositoryImpl
import com.liam.cmp_src.feature.profile.data.remote.ProfileApi
import com.liam.cmp_src.feature.profile.domain.repository.ProfileRepository
import com.liam.cmp_src.feature.profile.domain.usecase.RemoveAvatarUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.UpdateDisplayNameUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.UploadAvatarUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.ValidateDisplayNameUseCase
import com.liam.cmp_src.feature.profile.profileinfo.ProfileInfoViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The app's object graph.
 *
 * One `CoroutineDispatcher` for the whole graph, injected here rather than referenced inside
 * each repository — that indirection is what lets tests swap in a `TestDispatcher`.
 *
 * The [HttpClient] is a `single` because it owns a connection pool and a coroutine scope —
 * one per call would leak both. [ApiConfig] is a separate binding so a test or a staging build
 * can override the base URL without redefining the client.
 *
 * [TokenStore] is bound by `rememberPlatformModule` rather than here: where the tokens live
 * differs by target, and only Android can supply the `Context` its database builder needs.
 */
val appModule = module {
    single<CoroutineDispatcher> { Dispatchers.Default }

    single { ApiConfig() }

    single<HttpClient> { createHttpClient(tokenStore = get(), config = get()) }
    single { AuthApi(client = get(), tokenStore = get()) }
    single { ProfileApi(client = get()) }

    single<SocialAuthClient> { createSocialAuthClient() }
    single<AuthRepository> {
        AuthRepositoryImpl(authApi = get(), socialAuthClient = get(), dispatcher = get())
    }
    single<ProfileRepository> {
        ProfileRepositoryImpl(profileApi = get(), authApi = get(), dispatcher = get())
    }

    factoryOf(::SignInWithEmailUseCase)
    factoryOf(::SignInWithSocialUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ValidateCredentialsUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::GetCurrentUserUseCase)
    factoryOf(::ChangePasswordUseCase)
    factoryOf(::ValidateChangePasswordUseCase)
    factoryOf(::UpdateDisplayNameUseCase)
    factoryOf(::UploadAvatarUseCase)
    factoryOf(::RemoveAvatarUseCase)
    factoryOf(::ValidateDisplayNameUseCase)

    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SignUpViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ChangePasswordViewModel)
    viewModelOf(::ProfileInfoViewModel)
}
