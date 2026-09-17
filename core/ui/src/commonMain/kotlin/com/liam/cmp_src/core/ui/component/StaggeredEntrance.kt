package com.liam.cmp_src.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val ENTRANCE_DURATION_MILLIS = 460
private const val ENTRANCE_STEP_MILLIS = 70
private const val ENTRANCE_SLIDE_DIVISOR = 3

/**
 * Fades and lifts [content] into place, delayed by [index] steps.
 *
 * Wrapping each section of a screen in one of these — with an incrementing index — produces
 * the staggered cascade on first paint, without every composable owning its own animation
 * bookkeeping.
 */
@Composable
fun StaggeredEntrance(
    visible: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val delayMillis = index * ENTRANCE_STEP_MILLIS
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = ENTRANCE_DURATION_MILLIS,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = ENTRANCE_DURATION_MILLIS,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetY = { fullHeight -> fullHeight / ENTRANCE_SLIDE_DIVISOR },
        ),
        exit = fadeOut(),
    ) {
        content()
    }
}
