package com.liam.cmp_src.core.ui.modifier

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.liam.cmp_src.core.ui.theme.auroraColors

private const val SWEEP_MILLIS = 1400

/** How much of the element's width the travelling highlight covers. */
private const val BAND_WIDTH_FRACTION = 0.6f

/**
 * Fills the element with the loading placeholder treatment: a flat glass base with a highlight
 * band sweeping across it, looping forever.
 *
 * Draws rather than tints, so it works on any shape the caller has already clipped to — the
 * skeleton primitives in `core.ui.component` are the intended callers, but anything that needs to
 * read as "content on its way" can carry it.
 *
 * Colours come from the theme's skeleton tokens rather than its glass ones: a placeholder sits
 * inside a glass card, and reusing the card's own fill would make it invisible — which is exactly
 * what happens in the light theme, where that fill is nearly white.
 *
 * Pass `enabled = false` to draw the base with no motion, which is what a screenshot test or a
 * reduced-motion caller wants.
 */
@Composable
fun Modifier.shimmer(enabled: Boolean = true): Modifier {
    val colors = auroraColors
    val base = colors.skeletonBase
    val highlight = colors.skeletonHighlight

    val progress = if (enabled) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(SWEEP_MILLIS, easing = LinearEasing),
            ),
            label = "shimmerSweep",
        ).value
    } else {
        0f
    }

    return drawBehind {
        val bandWidth = size.width * BAND_WIDTH_FRACTION
        // Start off the left edge and finish off the right, so the band enters and leaves
        // cleanly instead of appearing and vanishing mid-element.
        val start = -bandWidth + progress * (size.width + 2 * bandWidth)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(start, 0f),
                end = Offset(start + bandWidth, size.height),
            ),
        )
    }
}
