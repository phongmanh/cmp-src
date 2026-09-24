package com.liam.cmp_src.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import cmpsrc.core.ui.generated.resources.Res
import cmpsrc.core.ui.generated.resources.ic_check
import cmpsrc.core.ui.generated.resources.ic_email
import cmpsrc.core.ui.generated.resources.ic_lock
import cmpsrc.core.ui.generated.resources.ic_person
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Measurements an [AppList] and its rows agree on. */
object AppListDefaults {
    /** The rows' own padding; they carry it so a press highlight spans the whole card. */
    val RowPadding = PaddingValues(horizontal = Dimens.spaceXl, vertical = Dimens.spaceLg)

    /** Where a row's text starts when it has a leading icon: past the padding, icon, and gap. */
    val IconTextInset: Dp = Dimens.spaceXl + Dimens.iconMd + Dimens.spaceLg

    /** Where a row's text starts when it has no leading icon. */
    val TextInset: Dp = Dimens.spaceXl
}

/**
 * One line of a list: an optional leading icon, a headline with an optional line under it, and
 * an optional [trailing] slot (a chevron, a switch, a value).
 *
 * Passing [onClick] makes the whole row the tap target, with the hand cursor to match; without
 * it the row is display-only.
 */
@Composable
fun AppListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: DrawableResource? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick).handCursor()
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .defaultMinSize(minHeight = Dimens.navItemHeight)
            .padding(AppListDefaults.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconMd),
                tint = colors.primary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * The hairline between two rows. [startInset] lines it up with the rows' text — use
 * [AppListDefaults.IconTextInset] for rows with a leading icon.
 */
@Composable
fun AppListDivider(
    modifier: Modifier = Modifier,
    startInset: Dp = AppListDefaults.TextInset,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset),
        thickness = Dimens.hairline,
        color = auroraColors.glassBorder,
    )
}

/**
 * A short list of rows in a [GlassCard], with a divider between each pair.
 *
 * It lays out every item at once, which suits a settings-sized list inside a scrolling screen.
 * A long or unbounded list belongs in a `LazyColumn` instead, built from [AppListItem] and
 * [AppListDivider] directly.
 *
 * @param key a stable identity per item, so a row keeps its state when the list reorders.
 */
@Composable
fun <T> AppList(
    items: List<T>,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    dividerStartInset: Dp = AppListDefaults.TextInset,
    itemContent: @Composable (T) -> Unit,
) {
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        items.forEachIndexed { index, item ->
            androidx.compose.runtime.key(key?.invoke(item) ?: index) {
                if (index > 0) AppListDivider(startInset = dividerStartInset)
                itemContent(item)
            }
        }
    }
}

@Preview
@Composable
private fun AppListPreview() {
    AppTheme {
        AppList(
            items = listOf(
                Triple(Res.drawable.ic_person, "Account", "Name, email and photo"),
                Triple(Res.drawable.ic_lock, "Security", "Password and sign-in methods"),
                Triple(Res.drawable.ic_email, "Notifications", null),
            ),
            dividerStartInset = AppListDefaults.IconTextInset,
        ) { (icon, title, subtitle) ->
            AppListItem(
                headline = title,
                supportingText = subtitle,
                leadingIcon = icon,
                trailing = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = {},
            )
        }
    }
}
