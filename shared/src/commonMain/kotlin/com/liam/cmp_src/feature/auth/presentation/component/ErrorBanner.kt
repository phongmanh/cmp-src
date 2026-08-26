package com.liam.cmp_src.feature.auth.presentation.component

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.presentation.login.asMessage

private const val SHAKE_STEP_MILLIS = 55
private const val SHAKE_AMPLITUDE = 16f
private const val ERROR_REVEAL_MILLIS = 220

/**
 * Slides the sign-in failure into view and shakes it.
 *
 * The shake is keyed on [nonce] rather than on the error value so that failing twice with
 * the same error still re-runs the animation — otherwise a repeated wrong password would
 * look like nothing happened.
 */
@Composable
fun ErrorBanner(error: AuthError?) {
    // Hold the last error so the text does not blank out mid-exit-animation.
    var lastError by remember { mutableStateOf(error) }
    if (error != null) lastError = error

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        shakeOffset.snapTo(0f)
        listOf(
            SHAKE_AMPLITUDE,
            -SHAKE_AMPLITUDE,
            SHAKE_AMPLITUDE * 0.6f,
            -SHAKE_AMPLITUDE * 0.6f,
            0f,
        ).forEach { target ->
            shakeOffset.animateTo(target, tween(SHAKE_STEP_MILLIS))
        }
    }

    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn(tween(ERROR_REVEAL_MILLIS)) + expandVertically(tween(ERROR_REVEAL_MILLIS)),
        exit = fadeOut(tween(ERROR_REVEAL_MILLIS)) + shrinkVertically(tween(ERROR_REVEAL_MILLIS)),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spaceSm)
                .graphicsLayer { translationX = shakeOffset.value },
            shape = RoundedCornerShape(Dimens.radiusSm),
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Text(
                text = lastError?.asMessage().orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(
                    horizontal = Dimens.spaceMd,
                    vertical = Dimens.spaceSm,
                ),
            )
        }
    }
}