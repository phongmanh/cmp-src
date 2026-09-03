package com.liam.cmp_src.feature.profile.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liam.cmp_src.core.ui.component.GlassCard
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.ic_chevron_right
import cmpsrc.shared.generated.resources.ic_edit
import cmpsrc.shared.generated.resources.ic_lock
import cmpsrc.shared.generated.resources.profile_change_password
import cmpsrc.shared.generated.resources.profile_edit
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One tappable line in the profile's settings list: an icon, what it does, and a chevron saying
 * there is somewhere to go.
 *
 * The row carries its own padding rather than inheriting the card's, so the press highlight spans
 * the full width of the card instead of stopping short of its edges.
 */
@Composable
fun ProfileActionRow(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .handCursor()
            .defaultMinSize(minHeight = Dimens.navItemHeight)
            .padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceLg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconMd),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconSm),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The hairline between two rows, indented to line up with the labels above and below it. */
@Composable
fun ProfileActionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        // Starts where the label does — past the row's padding, the icon, and the gap after it.
        modifier = modifier.padding(start = Dimens.spaceXl + Dimens.iconMd + Dimens.spaceLg),
        thickness = Dimens.hairline,
        color = auroraColors.glassBorder,
    )
}

@Preview
@Composable
private fun ProfileActionRowPreview() {
    AppTheme {
        GlassCard(
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            ProfileActionRow(
                icon = Res.drawable.ic_edit,
                label = stringResource(Res.string.profile_edit),
                onClick = {},
            )
            ProfileActionDivider()
            ProfileActionRow(
                icon = Res.drawable.ic_lock,
                label = stringResource(Res.string.profile_change_password),
                onClick = {},
            )
        }
    }
}
