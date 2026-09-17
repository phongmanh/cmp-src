package com.liam.cmp_src.feature.profile.di

import com.liam.cmp_src.feature.profile.ProfileViewModel
import com.liam.cmp_src.feature.profile.changepassword.ChangePasswordViewModel
import com.liam.cmp_src.feature.profile.data.ProfileRepositoryImpl
import com.liam.cmp_src.feature.profile.data.remote.ProfileApi
import com.liam.cmp_src.feature.profile.domain.repository.ProfileRepository
import com.liam.cmp_src.feature.profile.domain.usecase.ChangePasswordUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.GetCurrentUserUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.RemoveAvatarUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.UpdateDisplayNameUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.UploadAvatarUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.ValidateChangePasswordUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.ValidateDisplayNameUseCase
import com.liam.cmp_src.feature.profile.profileinfo.ProfileInfoViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The profile feature's graph: viewing and editing the account, and changing its password.
 *
 * Expects the app graph to provide the `HttpClient`, `CoroutineDispatcher` and `SignOutUseCase`,
 * and the auth feature to provide the `AuthRepository`.
 */
val profileModule = module {
    single { ProfileApi(client = get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(profileApi = get(), dispatcher = get()) }

    factoryOf(::GetCurrentUserUseCase)
    factoryOf(::ChangePasswordUseCase)
    factoryOf(::ValidateChangePasswordUseCase)
    factoryOf(::UpdateDisplayNameUseCase)
    factoryOf(::UploadAvatarUseCase)
    factoryOf(::RemoveAvatarUseCase)
    factoryOf(::ValidateDisplayNameUseCase)

    viewModelOf(::ProfileViewModel)
    viewModelOf(::ChangePasswordViewModel)
    viewModelOf(::ProfileInfoViewModel)
}
