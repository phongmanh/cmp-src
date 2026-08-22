package com.liam.cmp_src.feature.home.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.cd_notifications
import cmpsrc.shared.generated.resources.home_sign_out
import cmpsrc.shared.generated.resources.home_welcome_back
import cmpsrc.shared.generated.resources.ic_bell
import cmpsrc.shared.generated.resources.ic_sign_out
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val PRESSED_SCALE = 0.92f

/**
 * The signed-in header: who you are on the left, what you can do on the right.
 *
 * A glass panel with only its bottom corners rounded, so it reads as a sheet pulled down from
 * the top of the window rather than a floating card. Window insets are applied *inside* the
 * panel, which is what lets the glass run all the way up behind the status bar instead of
 * leaving a bare strip of the aurora above it.
 */
@Composable
fun HomeTopBar(
    user: UserResponse,
    onNotificationsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = auroraColors

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            bottomStart = Dimens.radiusXl,
            bottomEnd = Dimens.radiusXl,
        ),
        color = glass.glassFill,
        border = BorderStroke(Dimens.hairline, glass.glassBorder),
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                )
                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd)
                .heightIn(min = Dimens.topBarHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            UserAvatar(user = user)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.home_welcome_back),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user.displayLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            GlassIconButton(
                icon = Res.drawable.ic_bell,
                contentDescription = stringResource(Res.string.cd_notifications),
                onClick = onNotificationsClick,
            )
            GlassIconButton(
                icon = Res.drawable.ic_sign_out,
                contentDescription = stringResource(Res.string.home_sign_out),
                onClick = onSignOutClick,
            )
        }
    }
}

/**
 * A circular glass action button. Presses answer with a spring-loaded squeeze, which is the
 * only affordance an icon this small has room for.
 */
@Composable
private fun GlassIconButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = auroraColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconButtonScale",
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(Dimens.iconButtonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .handCursor(),
        shape = CircleShape,
        color = glass.glassFill,
        contentColor = MaterialTheme.colorScheme.onBackground,
        border = BorderStroke(Dimens.hairline, glass.glassBorder),
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(Dimens.iconSm),
            )
        }
    }
}

@Preview
@Composable
private fun HomeTopBarPreview() {
    AppTheme {
        HomeTopBar(
            user = sampleUser(),
            onNotificationsClick = {},
            onSignOutClick = {},
        )
    }
}
