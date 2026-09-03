package com.liam.cmp_src.feature.profile.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liam.cmp_src.core.ui.component.GlassCard
import com.liam.cmp_src.core.ui.component.SkeletonBlock
import com.liam.cmp_src.core.ui.component.SkeletonCircle
import com.liam.cmp_src.core.ui.component.SkeletonLine
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.profile_loading
import org.jetbrains.compose.resources.stringResource

private const val NAME_WIDTH_FRACTION = 0.45f
private const val EMAIL_WIDTH_FRACTION = 0.65f
private const val MEMBER_SINCE_WIDTH_FRACTION = 0.35f
private const val SECTION_LABEL_WIDTH_FRACTION = 0.3f
private const val ACTION_LABEL_WIDTH_FRACTION = 0.5f
private const val ACTION_LABEL_WIDTH_FRACTION_ALT = 0.62f
private val CHIP_WIDTH = 108.dp

/**
 * What the profile looks like before its data arrives.
 *
 * Built from the same cards, at the same sizes, as [com.liam.cmp_src.feature.profile.ProfileScreen]'s
 * loaded content — the point of a skeleton is that nothing moves when the real thing replaces it.
 *
 * The whole thing announces itself once as "loading", and every bar inside it is invisible to a
 * screen reader, so the reader does not narrate a dozen anonymous rectangles.
 */
@Composable
fun ProfileSkeleton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val loadingLabel = stringResource(Res.string.profile_loading)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = loadingLabel },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassCard {
            SkeletonCircle(size = Dimens.avatarLg, enabled = enabled)
            Spacer(Modifier.size(Dimens.spaceLg))
            SkeletonLine(NAME_WIDTH_FRACTION, height = Dimens.skeletonLineLg, enabled = enabled)
            Spacer(Modifier.size(Dimens.spaceSm))
            SkeletonLine(EMAIL_WIDTH_FRACTION, enabled = enabled)
            Spacer(Modifier.size(Dimens.spaceSm))
            SkeletonLine(
                MEMBER_SINCE_WIDTH_FRACTION,
                height = Dimens.skeletonLineSm,
                enabled = enabled,
            )
        }

        Spacer(Modifier.size(Dimens.spaceLg))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            SkeletonLine(
                SECTION_LABEL_WIDTH_FRACTION,
                height = Dimens.skeletonLineSm,
                enabled = enabled,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                repeat(2) {
                    SkeletonBlock(
                        modifier = Modifier
                            .size(width = CHIP_WIDTH, height = Dimens.navItemHeight),
                        shape = RoundedCornerShape(Dimens.radiusPill),
                        enabled = enabled,
                    )
                }
            }
        }

        Spacer(Modifier.size(Dimens.spaceLg))

        GlassCard(
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            ActionRowSkeleton(ACTION_LABEL_WIDTH_FRACTION, enabled)
            ProfileActionDivider()
            ActionRowSkeleton(ACTION_LABEL_WIDTH_FRACTION_ALT, enabled)
        }

        Spacer(Modifier.size(Dimens.spaceLg))

        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.buttonHeight),
            shape = RoundedCornerShape(Dimens.radiusMd),
            enabled = enabled,
        )
    }
}

/** Matches [ProfileActionRow]'s padding and icon size so the two lists sit at the same height. */
@Composable
private fun ActionRowSkeleton(
    labelWidthFraction: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceLg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonCircle(size = Dimens.iconMd, enabled = enabled)
        SkeletonLine(labelWidthFraction, enabled = enabled)
    }
}

@Preview
@Composable
private fun ProfileSkeletonPreview() {
    AppTheme {
        ProfileSkeleton(modifier = Modifier.padding(Dimens.screenPadding))
    }
}
