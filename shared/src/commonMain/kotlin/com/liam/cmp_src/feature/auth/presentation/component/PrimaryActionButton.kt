package com.liam.cmp_src.feature.auth.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.ic_check
import cmpsrc.shared.generated.resources.login_submit
import cmpsrc.shared.generated.resources.login_submit_in_progress
import cmpsrc.shared.generated.resources.login_submit_success
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val PRESSED_SCALE = 0.97f
private const val MORPH_MILLIS = 260
private const val DISABLED_ALPHA = 0.5f

/** The three things the primary button can be showing. */
enum class ActionButtonState { Idle, Loading, Success }

/**
 * The gradient "Sign in" button.
 *
 * Its content morphs between label, spinner and confirmation tick with a scale+fade
 * [AnimatedContent], so the button itself is the progress indicator rather than a separate
 * overlay appearing next to it. Pressing springs it down slightly.
 *
 * Built from a clickable [Box] rather than a Material [androidx.compose.material3.Button]: a
 * Button paints its container itself, so a gradient can only be supplied from outside — under
 * the Button — which leaves the hover/press state layer and the hover elevation compositing
 * against the wrong surface and tearing a hard-edged rectangle out of the gradient. Painting
 * the gradient here and handing the ripple to [clickable] puts the state layer back on top of
 * it, where it follows the button's rounded shape.
 */
@Composable
fun PrimaryActionButton(
    label: String,
    state: ActionButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "primaryButtonScale",
    )

    val colors = MaterialTheme.colorScheme
    val isInteractive = enabled && state == ActionButtonState.Idle
    val inProgressDescription = stringResource(Res.string.login_submit_in_progress)
    val buttonShape = RoundedCornerShape(Dimens.radiusMd)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.buttonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // Dim only a genuinely dormant button — a spinning or confirming one
                // stays at full strength even though it is not clickable.
                alpha = if (enabled || state != ActionButtonState.Idle) 1f else DISABLED_ALPHA
                shape = buttonShape
                clip = true
            }
            .background(Brush.horizontalGradient(listOf(colors.primary, colors.tertiary)))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.onPrimary),
                enabled = isInteractive,
                role = Role.Button,
                onClick = onClick,
            )
            .handCursor(isInteractive),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onPrimary) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (
                        fadeIn(tween(MORPH_MILLIS)) +
                            scaleIn(tween(MORPH_MILLIS), initialScale = 0.7f)
                        )
                        .togetherWith(
                            fadeOut(tween(MORPH_MILLIS)) +
                                scaleOut(tween(MORPH_MILLIS), targetScale = 0.7f),
                        )
                },
                label = "primaryButtonContent",
            ) { target ->
                when (target) {
                    ActionButtonState.Idle -> Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    ActionButtonState.Loading -> CircularProgressIndicator(
                        modifier = Modifier
                            .size(Dimens.progressIndicatorSize)
                            .semantics { contentDescription = inProgressDescription },
                        color = colors.onPrimary,
                        strokeWidth = Dimens.progressStroke,
                    )

                    ActionButtonState.Success -> Icon(
                        painter = painterResource(Res.drawable.ic_check),
                        contentDescription = stringResource(Res.string.login_submit_success),
                        modifier = Modifier.size(Dimens.iconLg),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PrimaryActionButtonPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            PrimaryActionButton(
                label = stringResource(Res.string.login_submit),
                state = ActionButtonState.Idle,
                onClick = {},
            )
        }
    }
}
