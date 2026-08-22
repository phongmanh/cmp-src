package com.liam.cmp_src.feature.auth.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.AuroraColors
import com.liam.cmp_src.core.ui.theme.auroraColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val FULL_TURN = (2 * PI).toFloat()
private const val CYCLE_MILLIS = 22_000

/**
 * The drifting gradient backdrop behind the login card.
 *
 * Three soft radial blobs travel on Lissajous paths driven by one shared phase, over a
 * vertical base gradient. Everything is drawn in a single [Canvas] pass — no `Modifier.blur`,
 * which is a no-op below Android API 31 and would make this look flat on older devices.
 */
@Composable
fun AnimatedAuthBackground(modifier: Modifier = Modifier) {
    val colors = auroraColors
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auroraPhase",
    )

    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.backgroundTop, colors.backgroundBottom),
            ),
        )
        drawAurora(colors, phase)
    }
}

private fun DrawScope.drawAurora(colors: AuroraColors, phase: Float) {
    val unit = maxOf(size.width, size.height)

    drawBlob(
        color = colors.blobPrimary,
        center = Offset(
            x = size.width * (0.28f + 0.20f * cos(phase)),
            y = size.height * (0.22f + 0.12f * sin(phase * 1.3f)),
        ),
        radius = unit * (0.62f + 0.06f * sin(phase)),
    )
    drawBlob(
        color = colors.blobSecondary,
        center = Offset(
            x = size.width * (0.82f + 0.16f * cos(phase * 0.8f + 2f)),
            y = size.height * (0.34f + 0.16f * sin(phase * 1.1f + 1f)),
        ),
        radius = unit * (0.52f + 0.07f * cos(phase * 1.2f)),
    )
    drawBlob(
        color = colors.blobTertiary,
        center = Offset(
            x = size.width * (0.52f + 0.24f * sin(phase * 0.9f + 4f)),
            y = size.height * (0.86f + 0.10f * cos(phase * 1.4f)),
        ),
        radius = unit * (0.58f + 0.05f * sin(phase * 0.7f)),
    )
}

/** A radial wash that fades to fully transparent at its edge, so blobs blend instead of stacking. */
private fun DrawScope.drawBlob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

@Preview
@Composable
private fun AnimatedAuthBackgroundPreview() {
    AppTheme {
        AnimatedAuthBackground(Modifier.fillMaxSize())
    }
}
