package com.liam.cmp_src.core.ui.modifier

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** What a control squeezes to when pressed, unless it asks for something else. */
const val DEFAULT_PRESSED_SCALE = 0.96f

/**
 * Springs the element down while [interactionSource] reports a press, and back when it is let go.
 *
 * The press has to be observed through the control's own [InteractionSource] rather than a
 * `clickable` of this modifier's own — the gesture belongs to the button underneath, and a second
 * one stacked on top would claim the same pointer. So a caller remembers one
 * [androidx.compose.foundation.interaction.MutableInteractionSource], hands it to its `Surface`,
 * `OutlinedButton` or `clickable`, and passes it here too.
 *
 * [pressedScale] is a parameter because the squeeze reads as a fixed *distance*, not a fixed
 * ratio: a small control has to travel proportionally further than a full-width button for the
 * press to register at all.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = DEFAULT_PRESSED_SCALE,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressScale",
    )

    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
