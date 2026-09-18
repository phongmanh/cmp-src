package com.liam.cmp_src.feature.auth.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.liam.cmp_src.core.ui.component.AnimatedAuthBackground
import com.liam.cmp_src.core.ui.component.ContentColumn
import com.liam.cmp_src.core.ui.theme.Dimens

/**
 * The shell every signed-out screen sits in: the aurora backdrop, a width-capped column centred
 * in the viewport, and room for a snackbar.
 *
 * The column fills the viewport so its content sits centred, but stays scrollable for when a soft
 * keyboard or a short window makes that content taller than the screen — which is the whole
 * reason this is a [BoxWithConstraints] and not a plain [Box].
 *
 * [content] is handed `isCompact`, true below [Dimens.compactWidthThreshold], because the measured
 * width is only available in here and the sign-in card needs it to decide whether the two social
 * buttons share a row.
 *
 * Passing no [snackbarHostState] means the screen has nothing to announce and gets no host.
 */
@Composable
internal fun AuthScreenScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.(isCompact: Boolean) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        AnimatedAuthBackground(Modifier.matchParentSize())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
        ) {
            val viewportHeight = maxHeight
            val isCompact = maxWidth < Dimens.compactWidthThreshold

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .padding(Dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ContentColumn { content(isCompact) }
            }
        }

        if (snackbarHostState != null) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Dimens.spaceLg),
            )
        }
    }
}
