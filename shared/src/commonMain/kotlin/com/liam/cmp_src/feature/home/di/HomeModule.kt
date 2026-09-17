package com.liam.cmp_src.feature.home.di

import com.liam.cmp_src.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The home feature's graph. Expects `SignOutUseCase` from the app graph. */
val homeModule = module {
    viewModelOf(::HomeViewModel)
}
