package com.liam.cmp_src.feature.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.feature.home.HomeTab
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val PRESSED_SCALE = 0.90f
private const val SELECTED_FILL_ALPHA = 0.18f
private const val LABEL_MILLIS = 220
private const val INDICATOR_MILLIS = 260

/**
 * The floating navigation pill.
 *
 * Only the selected tab spells out its name; the rest stay as icons and the pill slides and
 * re-spaces itself as the label expands. That keeps four destinations legible at phone widths
 * without shrinking every label to an unreadable size, and gives the selection somewhere to
 * animate to that is more informative than a dot.
 */
@Composable
fun HomeBottomBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = auroraColors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            )
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            // Capped so the pill stays a pill on desktop and in a browser window, where an
            // unbounded bar would stretch the four tabs across the whole screen.
            modifier = Modifier
                .widthIn(max = Dimens.navBarMaxWidth)
                .fillMaxWidth()
                .height(Dimens.navBarHeight),
            shape = RoundedCornerShape(Dimens.radiusPill),
            color = glass.glassFill,
            border = BorderStroke(Dimens.hairline, glass.glassBorder),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                HomeTab.entries.forEach { tab ->
                    HomeNavItem(
                        tab = tab,
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                    )
                }
            }
        }
    }
}

/** One tab: an icon that grows a label and a tinted pill behind it when it becomes selected. */
@Composable
private fun HomeNavItem(
    tab: HomeTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(tab.label)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navItemScale",
    )
    // Fading the primary's own alpha rather than crossfading with Color.Transparent, which is a
    // transparent *black* and would drag the tint through a muddy grey on the way in.
    val indicatorColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary
            .copy(alpha = if (selected) SELECTED_FILL_ALPHA else 0f),
        animationSpec = tween(INDICATOR_MILLIS, easing = FastOutSlowInEasing),
        label = "navItemIndicator",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(INDICATOR_MILLIS, easing = FastOutSlowInEasing),
        label = "navItemContent",
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(Dimens.navItemHeight)
            .clip(RoundedCornerShape(Dimens.radiusPill))
            .background(indicatorColor)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Tab,
                onClick = onClick,
            )
            .handCursor()
            .padding(horizontal = Dimens.spaceMd)
            // The label is only drawn when selected, so it cannot be relied on to name the tab.
            // Setting the description here names every tab, and stops the visible label being
            // announced twice on the one that has it.
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(tab.icon),
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconMd),
            tint = contentColor,
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(LABEL_MILLIS)) +
                expandHorizontally(tween(LABEL_MILLIS, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(LABEL_MILLIS)) +
                shrinkHorizontally(tween(LABEL_MILLIS, easing = FastOutSlowInEasing)),
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(start = Dimens.spaceSm),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun HomeBottomBarPreview() {
    AppTheme {
        HomeBottomBar(
            selectedTab = HomeTab.HOME,
            onTabSelected = {},
        )
    }
}
