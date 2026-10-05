package com.liam.cmp_src.feature.customers.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import cmpsrc.feature.customers.generated.resources.Res
import cmpsrc.feature.customers.generated.resources.customers_pending_changes
import cmpsrc.feature.customers.generated.resources.customers_sync_failed
import cmpsrc.feature.customers.generated.resources.customers_sync_offline
import cmpsrc.feature.customers.generated.resources.customers_sync_session
import cmpsrc.feature.customers.generated.resources.ic_cloud_off
import cmpsrc.feature.customers.generated.resources.ic_sync
import com.liam.cmp_src.core.ui.component.GlassSurface
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.feature.customers.list.SyncBanner
import com.liam.cmp_src.feature.customers.list.SyncProblem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * How many changes are waiting for the server, and — when a sync could not send them — why.
 *
 * A polite live region, so a screen reader hears the device go offline without the list being
 * interrupted for it.
 */
@Composable
internal fun SyncStatusBanner(
    banner: SyncBanner,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (banner.problem == null) colors.onSurfaceVariant else colors.error

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(Dimens.radiusMd),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(
                    if (banner.problem == SyncProblem.OFFLINE) Res.drawable.ic_cloud_off else Res.drawable.ic_sync,
                ),
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSm),
                tint = tint,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.customers_pending_changes,
                        banner.pendingCount,
                        banner.pendingCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBackground,
                )
                banner.problem?.let { problem ->
                    Text(
                        text = stringResource(problem.message()),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun SyncProblem.message() = when (this) {
    SyncProblem.OFFLINE -> Res.string.customers_sync_offline
    SyncProblem.FAILED -> Res.string.customers_sync_failed
    SyncProblem.SESSION_MISMATCH -> Res.string.customers_sync_session
}

@Preview
@Composable
private fun SyncStatusBannerPreview() {
    AppTheme {
        SyncStatusBanner(SyncBanner(pendingCount = 3, problem = SyncProblem.OFFLINE))
    }
}
