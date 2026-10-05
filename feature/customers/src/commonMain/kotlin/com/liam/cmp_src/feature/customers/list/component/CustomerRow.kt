package com.liam.cmp_src.feature.customers.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cmpsrc.feature.customers.generated.resources.Res
import cmpsrc.feature.customers.generated.resources.cd_delete_customer
import cmpsrc.feature.customers.generated.resources.customers_row_pending
import cmpsrc.feature.customers.generated.resources.customers_row_rejected
import cmpsrc.feature.customers.generated.resources.ic_delete
import cmpsrc.feature.customers.generated.resources.ic_sync
import cmpsrc.feature.customers.generated.resources.ic_warning
import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.core.ui.component.AppListItem
import com.liam.cmp_src.core.ui.component.GlassSurface
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.feature.customers.component.CustomerIconButton
import com.liam.cmp_src.feature.customers.component.asLabel
import com.liam.cmp_src.feature.customers.component.displayName
import com.liam.cmp_src.feature.customers.domain.model.Customer
import com.liam.cmp_src.feature.customers.domain.model.SyncState
import com.liam.cmp_src.feature.customers.sampleCustomer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** How strongly a status chip's colour tints its background. */
private const val CHIP_FILL_ALPHA = 0.16f

/**
 * One customer in the list, on its own glass tile: the name, what else identifies them, a status
 * chip, and a delete action. Tapping the tile opens the editor.
 *
 * A change that has not reached the server is marked, and a refused one says so in place of the
 * secondary line — the user is the only one who can fix it, so it has to be noticed.
 */
@Composable
internal fun CustomerRow(
    customer: Customer,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = customer.details
    val name = details.displayName()
    val supporting = if (customer.syncState == SyncState.REJECTED) {
        stringResource(Res.string.customers_row_rejected)
    } else {
        listOfNotNull(details.companyName, details.email).joinToString(SEPARATOR).ifEmpty { null }
    }

    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.radiusLg),
    ) {
        AppListItem(
            headline = name,
            supportingText = supporting,
            onClick = onClick,
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SyncMarker(customer.syncState)
                    details.status?.let { StatusChip(it) }
                    CustomerIconButton(
                        icon = Res.drawable.ic_delete,
                        contentDescription = stringResource(Res.string.cd_delete_customer, name),
                        onClick = onDelete,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}

@Composable
private fun SyncMarker(state: SyncState) {
    val (icon, description, tint) = when (state) {
        SyncState.SYNCED -> return
        SyncState.PENDING -> Triple(
            Res.drawable.ic_sync,
            Res.string.customers_row_pending,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SyncState.REJECTED -> Triple(
            Res.drawable.ic_warning,
            Res.string.customers_row_rejected,
            MaterialTheme.colorScheme.error,
        )
    }
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(description),
        modifier = Modifier.size(Dimens.iconSm),
        tint = tint,
    )
}

@Composable
private fun StatusChip(status: CustomerStatus) {
    val color = status.chipColor()
    Text(
        text = status.asLabel(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = CHIP_FILL_ALPHA), RoundedCornerShape(Dimens.radiusPill))
            .padding(horizontal = Dimens.spaceSm, vertical = Dimens.spaceXxs),
    )
}

@Composable
private fun CustomerStatus.chipColor(): Color = when (this) {
    CustomerStatus.ACTIVE -> MaterialTheme.colorScheme.primary
    CustomerStatus.LEAD -> MaterialTheme.colorScheme.tertiary
    CustomerStatus.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
}

private const val SEPARATOR = " · "

@Preview
@Composable
private fun CustomerRowPreview() {
    AppTheme {
        CustomerRow(customer = sampleCustomer(), onClick = {}, onDelete = {})
    }
}

@Preview
@Composable
private fun CustomerRowRejectedPreview() {
    AppTheme {
        CustomerRow(
            customer = sampleCustomer(syncState = SyncState.REJECTED),
            onClick = {},
            onDelete = {},
        )
    }
}
