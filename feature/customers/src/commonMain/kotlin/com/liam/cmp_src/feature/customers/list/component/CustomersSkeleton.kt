package com.liam.cmp_src.feature.customers.list.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import cmpsrc.feature.customers.generated.resources.Res
import cmpsrc.feature.customers.generated.resources.customers_loading
import com.liam.cmp_src.core.ui.component.AppListDefaults
import com.liam.cmp_src.core.ui.component.GlassSurface
import com.liam.cmp_src.core.ui.component.SkeletonLine
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/** How many placeholder rows stand in for the list — enough to fill a phone screen. */
private const val SKELETON_ROWS = 6

/** Differing widths so the placeholder names do not line up like a table. */
private val NAME_WIDTHS = listOf(0.55f, 0.42f, 0.6f, 0.38f, 0.5f, 0.46f)
private const val DETAIL_WIDTH = 0.7f

/** The list's shape while the first sync fills an empty table. */
@Composable
internal fun CustomersSkeleton(modifier: Modifier = Modifier) {
    val loading = stringResource(Res.string.customers_loading)
    Column(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        repeat(SKELETON_ROWS) { index ->
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.radiusLg),
            ) {
                Column(
                    modifier = Modifier.padding(AppListDefaults.RowPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                ) {
                    SkeletonLine(widthFraction = NAME_WIDTHS[index % NAME_WIDTHS.size], height = Dimens.skeletonLineMd)
                    SkeletonLine(widthFraction = DETAIL_WIDTH, height = Dimens.skeletonLineSm)
                }
            }
        }
    }
}

@Preview
@Composable
private fun CustomersSkeletonPreview() {
    AppTheme { CustomersSkeleton() }
}
