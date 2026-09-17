package com.liam.cmp_src.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Non-Material colour tokens for the current theme. Static because the whole tree
 * recomposes anyway when the theme flips.
 */
val LocalAuroraColors = staticCompositionLocalOf { DarkAuroraColors }

/** Shorthand for `LocalAuroraColors.current`, mirroring `MaterialTheme.colorScheme`. */
val auroraColors: AuroraColors
    @Composable get() = LocalAuroraColors.current

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAuroraColors provides if (useDarkTheme) DarkAuroraColors else LightAuroraColors,
    ) {
        MaterialTheme(
            colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme,
            content = content,
        )
    }
}
