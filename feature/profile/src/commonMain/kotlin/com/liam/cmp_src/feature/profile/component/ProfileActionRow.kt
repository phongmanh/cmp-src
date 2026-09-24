package com.liam.cmp_src.feature.profile.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liam.cmp_src.core.ui.component.AppListDefaults
import com.liam.cmp_src.core.ui.component.AppListDivider
import com.liam.cmp_src.core.ui.component.AppListItem
import com.liam.cmp_src.core.ui.component.GlassCard
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import cmpsrc.core.ui.generated.resources.Res as UiRes
import cmpsrc.core.ui.generated.resources.ic_lock
import cmpsrc.feature.profile.generated.resources.Res
import cmpsrc.feature.profile.generated.resources.ic_chevron_right
import cmpsrc.feature.profile.generated.resources.ic_edit
import cmpsrc.feature.profile.generated.resources.profile_change_password
import cmpsrc.feature.profile.generated.resources.profile_edit
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One tappable line in the profile's settings list: an icon, what it does, and a chevron saying
 * there is somewhere to go.
 */
@Composable
fun ProfileActionRow(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppListItem(
        headline = label,
        modifier = modifier,
        leadingIcon = icon,
        trailing = {
            Icon(
                painter = painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSm),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
    )
}

/** The hairline between two rows, indented to line up with the labels above and below it. */
@Composable
fun ProfileActionDivider(modifier: Modifier = Modifier) {
    AppListDivider(modifier = modifier, startInset = AppListDefaults.IconTextInset)
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
                icon = UiRes.drawable.ic_lock,
                label = stringResource(Res.string.profile_change_password),
                onClick = {},
            )
        }
    }
}
