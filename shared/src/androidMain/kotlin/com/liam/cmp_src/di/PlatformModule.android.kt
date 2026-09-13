package com.liam.cmp_src.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.liam.cmp_src.core.database.getDatabaseBuilder
import com.liam.cmp_src.core.security.KeystoreTokenCipher
import org.koin.core.module.Module
import org.koin.dsl.module

@Composable
actual fun rememberPlatformModule(): Module {
    // The application context, not the Activity's: the database outlives every screen.
    val context = LocalContext.current.applicationContext
    return remember(context) {
        module { roomTokenStore(builder = { getDatabaseBuilder(context) }, cipher = { KeystoreTokenCipher() }) }
    }
}
