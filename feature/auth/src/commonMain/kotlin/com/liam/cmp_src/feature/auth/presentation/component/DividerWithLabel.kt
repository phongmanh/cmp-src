package com.liam.cmp_src.feature.auth.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors

@Composable
fun DividerWithLabel(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = auroraColors.glassBorder,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.spaceMd),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = auroraColors.glassBorder,
        )
    }
}