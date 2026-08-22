package com.liam.cmp_src.feature.auth.presentation.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.cd_app_logo
import org.jetbrains.compose.resources.stringResource

private const val GLOW_CYCLE_MILLIS = 2_800
private const val SPIN_CYCLE_MILLIS = 24_000
private const val CORNER_FRACTION = 0.30f
private const val STAR_INNER_FRACTION = 0.30f

/**
 * The app's logo: a gradient rounded square holding a four-point spark, with a glow that
 * breathes behind it and a very slow rotation on the spark itself.
 *
 * Drawn rather than loaded so it inherits the theme's gradient and scales to any density.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.cd_app_logo)
    val transition = rememberInfiniteTransition(label = "brandMark")

    val glow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GLOW_CYCLE_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "brandGlow",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SPIN_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "brandSpin",
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier.semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(Dimens.brandMarkGlow)) {
            val radius = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.16f + 0.24f * glow),
                        primary.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
            )
        }

        Canvas(Modifier.size(Dimens.brandMarkSize)) {
            val side = size.minDimension
            val corner = side * CORNER_FRACTION

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(primary, tertiary),
                    start = Offset.Zero,
                    end = Offset(side, side),
                ),
                cornerRadius = CornerRadius(corner, corner),
            )
            rotate(degrees = spin) {
                drawPath(
                    path = sparkPath(center, side * 0.30f),
                    color = onPrimary,
                )
            }
        }
    }
}

/** A sharp four-point star centred on [center] with arm length [outerRadius]. */
private fun sparkPath(center: Offset, outerRadius: Float): Path {
    val inner = outerRadius * STAR_INNER_FRACTION
    return Path().apply {
        moveTo(center.x, center.y - outerRadius)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + outerRadius, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + outerRadius)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - outerRadius, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
}

@Preview
@Composable
private fun BrandMarkPreview() {
    AppTheme {
        Box(Modifier.size(Dimens.brandMarkGlow)) {
            BrandMark()
        }
    }
}
