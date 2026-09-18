package com.liam.cmp_src.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens

/**
 * Which of the two headers this is. They differ in more than one token at once — size, colour and
 * alignment all move together — so the variant is named rather than assembled from four
 * parameters at every call site.
 */
enum class SectionHeaderStyle {
    /** Tops a whole screen, over the aurora backdrop: large, centred, on the background colour. */
    Screen,

    /** Tops a dialog, over its opaque surface: smaller, left-aligned, on the surface colour. */
    Dialog,
}

/**
 * A title with a line of explanation under it.
 *
 * The pairing appears on every screen and dialog that introduces itself, always with the subtitle
 * in [MaterialTheme.colorScheme.onSurfaceVariant] a hair below a bold title — so it is one
 * component with two presets rather than the same two `Text`s written out each time.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    style: SectionHeaderStyle = SectionHeaderStyle.Screen,
) {
    val isScreen = style == SectionHeaderStyle.Screen
    val textAlign = if (isScreen) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier,
        horizontalAlignment = if (isScreen) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = title,
            style = if (isScreen) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.titleLarge
            },
            fontWeight = FontWeight.Bold,
            color = if (isScreen) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = textAlign,
        )
        Spacer(Modifier.size(Dimens.spaceXs))
        Text(
            text = subtitle,
            style = if (isScreen) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
        )
    }
}

@Preview
@Composable
private fun SectionHeaderPreview() {
    AppTheme {
        Column {
            SectionHeader(title = "Welcome back", subtitle = "Sign in to continue")
            Spacer(Modifier.size(Dimens.spaceXl))
            SectionHeader(
                title = "Change password",
                subtitle = "Pick something you have not used before",
                style = SectionHeaderStyle.Dialog,
            )
        }
    }
}
