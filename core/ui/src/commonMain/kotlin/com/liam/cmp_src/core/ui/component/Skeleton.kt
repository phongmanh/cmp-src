package com.liam.cmp_src.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.liam.cmp_src.core.ui.modifier.shimmer
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens

/**
 * The building blocks a loading skeleton is assembled from.
 *
 * A skeleton is built by laying these out in the shape of the content they stand in for, so the
 * layout does not jump when the real thing arrives. They are deliberately dumb: no screen-specific
 * knowledge lives here, only the one placeholder treatment every screen shares.
 *
 * All three are invisible to a screen reader — the container that composes them is what carries a
 * "loading" description, so the reader hears it once instead of once per bar.
 */

/** A placeholder of whatever size the caller gives it. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.radiusSm),
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmer(enabled)
            .clearAndSetSemantics { },
    )
}

/**
 * A placeholder standing in for one line of text, [widthFraction] of the available width.
 *
 * Lines are given differing fractions so a stack of them reads as a paragraph rather than as a
 * block — text does not end at the same place on every line.
 */
@Composable
fun SkeletonLine(
    widthFraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = Dimens.skeletonLineMd,
    enabled: Boolean = true,
) {
    SkeletonBlock(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        shape = RoundedCornerShape(Dimens.radiusPill),
        enabled = enabled,
    )
}

/** A placeholder standing in for an avatar or a round icon. */
@Composable
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SkeletonBlock(
        modifier = modifier.size(size),
        shape = CircleShape,
        enabled = enabled,
    )
}

@Preview
@Composable
private fun SkeletonPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spaceXl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            SkeletonCircle(size = Dimens.avatarLg)
            SkeletonLine(widthFraction = 0.45f, height = Dimens.skeletonLineLg)
            SkeletonLine(widthFraction = 0.65f)
            SkeletonLine(widthFraction = 0.35f, height = Dimens.skeletonLineSm)
        }
    }
}
