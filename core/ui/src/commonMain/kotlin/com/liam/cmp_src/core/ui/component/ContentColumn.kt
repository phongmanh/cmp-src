package com.liam.cmp_src.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.liam.cmp_src.core.ui.theme.Dimens

/**
 * Caps a screen's content at [Dimens.cardMaxWidth] and centres what sits inside it.
 *
 * Every screen in the app is a single column of cards, and none of them should stretch to the
 * full width of a tablet or an unfolded device. The cap is the same everywhere, so it lives here
 * instead of being re-measured per screen; the caller still owns the layout *around* this — the
 * scrolling, the padding, and whether the column is centred in the viewport or starts at the top.
 */
@Composable
fun ContentColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.widthIn(max = Dimens.cardMaxWidth),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
