package com.liam.cmp_src.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand ramp — violet/indigo primary with cyan and pink accents.
private val Violet200 = Color(0xFFC7BFFF)
private val Violet300 = Color(0xFFA99BFF)
private val Violet400 = Color(0xFF8B78FF)
private val Violet500 = Color(0xFF6F5AF6)
private val Violet600 = Color(0xFF5A45DC)
private val Violet900 = Color(0xFF241C5C)

private val Cyan300 = Color(0xFF6EE7F9)
private val Cyan400 = Color(0xFF22D3EE)
private val Cyan900 = Color(0xFF0B3B45)

private val Pink300 = Color(0xFFF9A8D4)
private val Pink400 = Color(0xFFF472B6)
private val Pink900 = Color(0xFF4A1D36)

// Neutrals.
private val Ink950 = Color(0xFF07070E)
private val Ink900 = Color(0xFF0E0E1A)
private val Ink800 = Color(0xFF1A1A2B)
private val Ink700 = Color(0xFF262639)
private val Ink400 = Color(0xFF8A8AA3)
private val Ink200 = Color(0xFFD6D6E4)
private val Ink100 = Color(0xFFECECF4)
private val Ink50 = Color(0xFFF7F7FC)
private val White = Color(0xFFFFFFFF)

private val Red400 = Color(0xFFFF6B6B)
private val Red600 = Color(0xFFD92D2D)
private val Red50 = Color(0xFFFFF1F1)

internal val DarkColorScheme = darkColorScheme(
    primary = Violet400,
    onPrimary = White,
    primaryContainer = Violet600,
    onPrimaryContainer = Violet200,
    secondary = Cyan400,
    onSecondary = Ink950,
    secondaryContainer = Cyan900,
    onSecondaryContainer = Cyan300,
    tertiary = Pink400,
    onTertiary = Ink950,
    tertiaryContainer = Pink900,
    onTertiaryContainer = Pink300,
    background = Ink950,
    onBackground = Ink100,
    surface = Ink900,
    onSurface = Ink100,
    surfaceVariant = Ink800,
    onSurfaceVariant = Ink400,
    outline = Ink700,
    outlineVariant = Ink800,
    error = Red400,
    onError = Ink950,
    errorContainer = Color(0xFF3D1414),
    onErrorContainer = Color(0xFFFFB4B4),
)

internal val LightColorScheme = lightColorScheme(
    primary = Violet500,
    onPrimary = White,
    primaryContainer = Violet200,
    onPrimaryContainer = Violet900,
    secondary = Color(0xFF0E9AB5),
    onSecondary = White,
    secondaryContainer = Color(0xFFD3F5FB),
    onSecondaryContainer = Cyan900,
    tertiary = Color(0xFFDB5296),
    onTertiary = White,
    tertiaryContainer = Color(0xFFFDE2F0),
    onTertiaryContainer = Pink900,
    background = Ink50,
    onBackground = Ink900,
    surface = White,
    onSurface = Ink900,
    surfaceVariant = Ink100,
    onSurfaceVariant = Color(0xFF5C5C75),
    outline = Ink200,
    outlineVariant = Ink100,
    error = Red600,
    onError = White,
    errorContainer = Red50,
    onErrorContainer = Color(0xFF7A1414),
)

/**
 * Colours the Material [androidx.compose.material3.ColorScheme] has no slot for: the animated
 * background blobs, the glass card's fill and hairline border, and the two brand marks.
 *
 * Exposed through [LocalAuroraColors] so composables read tokens instead of literals.
 */
data class AuroraColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val blobPrimary: Color,
    val blobSecondary: Color,
    val blobTertiary: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val googleSurface: Color,
    val googleContent: Color,
    val facebookSurface: Color,
    val facebookContent: Color,
    val skeletonBase: Color,
    val skeletonHighlight: Color,
)

internal val DarkAuroraColors = AuroraColors(
    backgroundTop = Ink950,
    backgroundBottom = Color(0xFF10091F),
    blobPrimary = Violet500.copy(alpha = 0.55f),
    blobSecondary = Cyan400.copy(alpha = 0.34f),
    blobTertiary = Pink400.copy(alpha = 0.30f),
    glassFill = White.copy(alpha = 0.06f),
    glassBorder = White.copy(alpha = 0.14f),
    glassHighlight = White.copy(alpha = 0.22f),
    googleSurface = White.copy(alpha = 0.10f),
    googleContent = Ink100,
    facebookSurface = White.copy(alpha = 0.10f),
    facebookContent = Ink100,
    // A placeholder sits *inside* a glass card, so it cannot reuse the card's own fill — it
    // would vanish into it. On a dark card the bar is a lift, and the sweep lifts further.
    skeletonBase = White.copy(alpha = 0.07f),
    skeletonHighlight = White.copy(alpha = 0.17f),
)

internal val LightAuroraColors = AuroraColors(
    backgroundTop = Color(0xFFFBFAFF),
    backgroundBottom = Color(0xFFEDE9FF),
    blobPrimary = Violet400.copy(alpha = 0.42f),
    blobSecondary = Cyan300.copy(alpha = 0.40f),
    blobTertiary = Pink300.copy(alpha = 0.38f),
    glassFill = White.copy(alpha = 0.72f),
    glassBorder = White.copy(alpha = 0.90f),
    glassHighlight = White,
    googleSurface = White,
    googleContent = Color(0xFF3C4043),
    facebookSurface = White,
    facebookContent = Color(0xFF1C1E21),
    // Inverted against the dark theme: the light card is already near-white, so the bar is a
    // shadow on it and the sweep is the bar lightening back towards the card.
    skeletonBase = Ink900.copy(alpha = 0.10f),
    skeletonHighlight = Ink900.copy(alpha = 0.03f),
)
