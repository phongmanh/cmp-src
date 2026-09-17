package com.liam.cmp_src.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens

private val WHITESPACE = Regex("\\s+")

/**
 * The user's picture, over their initials in a gradient circle.
 *
 * The initials are always drawn and the image is laid over them, rather than being swapped in as a
 * Coil placeholder. That way the circle is never empty: an account with no [UserResponse.avatarUrl]
 * gets the initials, and so does one whose picture is still loading or failed to load, with no
 * second code path to keep in step.
 *
 * Decorative by default — the top bar shows the user's name right beside it, so announcing the
 * initials again would only repeat what the reader has already heard.
 */
@Composable
fun UserAvatar(
    user: UserResponse,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.avatarSm,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val initials = remember(user.displayName, user.email) { user.initials() }
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        ),
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )

        user.avatarUrl?.takeIf { it.isNotBlank() }?.let { avatarUrl ->
            AsyncImage(
                model = avatarUrl,
                // The Box already clears its semantics; a description here would be announced
                // through it and repeat the name sitting next to the circle.
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                // An uploaded avatar arrives square, but a provider's can be any shape, and a
                // letterboxed face inside a circle reads as a mistake.
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * One or two letters standing in for the user's face: the first letter of their first and last
 * name, or of their only name, falling back to their email when the display name is empty.
 *
 * Returns an empty string when there is nothing at all to draw from, which renders as a plain
 * gradient circle rather than a placeholder glyph.
 */
internal fun UserResponse.initials(): String {
    val words = displayName.orEmpty().split(WHITESPACE).filter { it.isNotBlank() }
    return when (words.size) {
        0 -> email.orEmpty().trim().take(1)
        1 -> words.first().take(1)
        else -> words.first().take(1) + words.last().take(1)
    }.uppercase()
}

@Preview
@Composable
private fun UserAvatarPreview() {
    AppTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                user = sampleUser(),
            )
            UserAvatar(
                user = sampleUser(id = "2", displayName = "Ada", email = "ada@cmpsrc.dev"),
                size = Dimens.avatarLg,
                textStyle = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
