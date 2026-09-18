package com.liam.cmp_src.feature.profile.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.liam.cmp_src.core.ui.component.ActionButtonState
import com.liam.cmp_src.core.ui.component.PrimaryActionButton
import com.liam.cmp_src.core.ui.component.SectionHeader
import com.liam.cmp_src.core.ui.component.SectionHeaderStyle
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors

/**
 * The window both of this feature's dialogs are built in: a titled card that scrolls, holding
 * whatever [content] the dialog is actually for, over a way out and a way forward.
 *
 * Painted on an opaque [MaterialTheme.colorScheme.surface] rather than the `GlassCard` treatment
 * the signed-in screens use — a translucent fill over the dialog scrim reads as muddy rather than
 * as glass, because there is no aurora backdrop behind it to catch. The border stays the glass one
 * so the edge still catches the light the way every other surface in the app does.
 *
 * [isBusy] disables the way out while a write is in flight; the confirm button reports the same
 * thing through [confirmState], which is why it is not driven from here. [confirmEnabled] is
 * separate again, for a dialog whose action has nothing to write yet.
 */
@Composable
internal fun ProfileDialogCard(
    title: String,
    subtitle: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    confirmState: ActionButtonState,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .widthIn(max = Dimens.cardMaxWidth)
            .fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusXl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.hairline, auroraColors.glassBorder),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spaceXl),
        ) {
            SectionHeader(
                title = title,
                subtitle = subtitle,
                style = SectionHeaderStyle.Dialog,
            )

            Spacer(Modifier.size(Dimens.spaceLg))

            content()

            Spacer(Modifier.size(Dimens.spaceLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                // The two buttons are different heights by design — the text one is a way out,
                // not a peer of the primary action — so centre them rather than top-aligning.
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.handCursor(!isBusy),
                    enabled = !isBusy,
                ) {
                    Text(dismissLabel)
                }

                PrimaryActionButton(
                    label = confirmLabel,
                    state = confirmState,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = confirmEnabled,
                )
            }
        }
    }
}
