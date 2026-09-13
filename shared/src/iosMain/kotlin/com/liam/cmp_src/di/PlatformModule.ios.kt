package com.liam.cmp_src.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.liam.cmp_src.core.database.getDatabaseBuilder
import com.liam.cmp_src.core.security.KeychainTokenCipher
import org.koin.core.module.Module
import org.koin.dsl.module

@Composable
actual fun rememberPlatformModule(): Module = remember {
    module { roomTokenStore(builder = { getDatabaseBuilder() }, cipher = { KeychainTokenCipher() }) }
}
