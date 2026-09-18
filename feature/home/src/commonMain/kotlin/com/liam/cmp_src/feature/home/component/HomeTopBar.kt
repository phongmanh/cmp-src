package com.liam.cmp_src.feature.home.component

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.component.GlassSurface
import com.liam.cmp_src.core.ui.component.UserAvatar
import com.liam.cmp_src.core.ui.component.displayLabel
import com.liam.cmp_src.core.ui.component.sampleUser
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.modifier.pressScale
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import cmpsrc.feature.home.generated.resources.Res
import cmpsrc.feature.home.generated.resources.cd_notifications
import cmpsrc.feature.home.generated.resources.home_welcome_back
import cmpsrc.feature.home.generated.resources.ic_bell
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
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            bottomStart = Dimens.radiusXl,
            bottomEnd = Dimens.radiusXl,
        ),
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
    val interactionSource = remember { MutableInteractionSource() }

    GlassSurface(
        modifier = modifier
            .size(Dimens.iconButtonSize)
            .pressScale(interactionSource, PRESSED_SCALE)
            .handCursor(),
        shape = CircleShape,
        contentColor = MaterialTheme.colorScheme.onBackground,
        onClick = onClick,
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
        )
    }
}
