package com.liam.cmp_src.feature.auth.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.cd_facebook_logo
import cmpsrc.shared.generated.resources.cd_google_logo
import cmpsrc.shared.generated.resources.ic_facebook
import cmpsrc.shared.generated.resources.ic_google
import cmpsrc.shared.generated.resources.login_facebook
import cmpsrc.shared.generated.resources.login_google
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val PRESSED_SCALE = 0.96f
private const val MORPH_MILLIS = 200
private const val DISABLED_ALPHA = 0.45f

/**
 * One third-party sign-in button.
 *
 * Only the provider the user actually tapped swaps its logo for a spinner — the other stays
 * legible but disabled, so it is always obvious which sign-in is running.
 */
@Composable
fun SocialSignInButton(
    provider: SocialProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "socialButtonScale",
    )

    val glass = auroraColors
    val isInteractive = enabled && !isLoading
    val logo = when (provider) {
        SocialProvider.GOOGLE -> Res.drawable.ic_google
        SocialProvider.FACEBOOK -> Res.drawable.ic_facebook
    }
    val logoDescription = stringResource(
        when (provider) {
            SocialProvider.GOOGLE -> Res.string.cd_google_logo
            SocialProvider.FACEBOOK -> Res.string.cd_facebook_logo
        },
    )
    val label = stringResource(
        when (provider) {
            SocialProvider.GOOGLE -> Res.string.login_google
            SocialProvider.FACEBOOK -> Res.string.login_facebook
        },
    )
    val contentColor = when (provider) {
        SocialProvider.GOOGLE -> glass.googleContent
        SocialProvider.FACEBOOK -> glass.facebookContent
    }
    val containerColor = when (provider) {
        SocialProvider.GOOGLE -> glass.googleSurface
        SocialProvider.FACEBOOK -> glass.facebookSurface
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.socialButtonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // The provider being signed into keeps full contrast; only the other one dims.
                alpha = if (enabled || isLoading) 1f else DISABLED_ALPHA
            }
            .handCursor(isInteractive),
        enabled = isInteractive,
        shape = RoundedCornerShape(Dimens.radiusMd),
        interactionSource = interactionSource,
        border = BorderStroke(Dimens.hairline, glass.glassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                fadeIn(tween(MORPH_MILLIS)).togetherWith(fadeOut(tween(MORPH_MILLIS)))
            },
            label = "socialButtonContent",
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.progressIndicatorSize),
                    color = contentColor,
                    strokeWidth = Dimens.progressStroke,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        Dimens.spaceSm,
                        Alignment.CenterHorizontally,
                    ),
                ) {
                    Image(
                        painter = painterResource(logo),
                        contentDescription = logoDescription,
                        modifier = Modifier.size(Dimens.iconMd),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SocialSignInButtonPreview() {
    AppTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            SocialSignInButton(
                provider = SocialProvider.GOOGLE,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            SocialSignInButton(
                provider = SocialProvider.FACEBOOK,
                onClick = {},
                modifier = Modifier.weight(1f),
                isLoading = true,
            )
        }
    }
}
