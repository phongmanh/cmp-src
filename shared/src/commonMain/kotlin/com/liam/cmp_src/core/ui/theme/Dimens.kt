package com.liam.cmp_src.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing and sizing tokens. Composables reference these instead of writing `.dp` inline, so
 * the scale stays consistent and adjustable in one place.
 */
object Dimens {
    // Spacing scale.
    val spaceXxs = 2.dp
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 24.dp
    val spaceXxl = 32.dp
    val spaceXxxl = 48.dp

    // Corner radii.
    val radiusSm = 10.dp
    val radiusMd = 14.dp
    val radiusLg = 20.dp
    val radiusXl = 28.dp
    /** Large enough that any bar-height shape resolves to a full pill. */
    val radiusPill = 999.dp

    // Components.
    val fieldHeight = 58.dp
    val buttonHeight = 54.dp
    val socialButtonHeight = 52.dp
    val iconSm = 18.dp
    val iconMd = 22.dp
    val iconLg = 28.dp
    val iconButtonSize = 42.dp
    val brandMarkSize = 68.dp
    val brandMarkGlow = 108.dp
    val progressIndicatorSize = 22.dp
    val progressStroke = 2.dp
    val hairline = 1.dp
    val focusRing = 2.dp

    // Loading skeletons — the placeholder bars stand in for a line of text, so their heights
    // track the type scale they replace rather than being picked per screen.
    val skeletonLineSm = 12.dp
    val skeletonLineMd = 16.dp
    val skeletonLineLg = 22.dp

    // App shell — top bar and bottom navigation.
    val topBarHeight = 64.dp
    val navBarHeight = 64.dp
    val navItemHeight = 44.dp
    val avatarSm = 40.dp
    val avatarLg = 88.dp

    // Layout.
    val cardMaxWidth = 440.dp
    val screenPadding = 24.dp
    /**
     * Widest the floating navigation bar is allowed to get. Without a cap it stretches the full
     * width of a desktop or browser window, where a centred pill reads as intended instead.
     */
    val navBarMaxWidth = 520.dp
    /**
     * Below this window width the two social buttons stack instead of sharing a row. Set from
     * the narrowest point at which "Facebook" plus its logo still fits a half-width button
     * once screen padding (2x) and card padding (2x) are taken out.
     */
    val compactWidthThreshold = 440.dp
}
