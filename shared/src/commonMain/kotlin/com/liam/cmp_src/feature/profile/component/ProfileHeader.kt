package com.liam.cmp_src.feature.profile.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.component.GlassCard
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.feature.home.component.UserAvatar
import com.liam.cmp_src.feature.home.component.displayLabel
import com.liam.cmp_src.feature.home.component.sampleUser
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.cd_verified
import cmpsrc.shared.generated.resources.ic_check
import cmpsrc.shared.generated.resources.profile_email_unverified
import cmpsrc.shared.generated.resources.profile_email_verified
import cmpsrc.shared.generated.resources.profile_member_since
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val YEAR_LENGTH = 4

/**
 * Who the signed-in user is: their face, their name, whether their email is confirmed, and how
 * long the account has existed.
 */
@Composable
fun ProfileHeader(
    user: UserResponse,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier) {
        UserAvatar(
            user = user,
            size = Dimens.avatarLg,
            textStyle = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.size(Dimens.spaceLg))
        Text(
            text = user.displayLabel(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        user.email?.takeIf { it.isNotBlank() }?.let { email ->
            Spacer(Modifier.size(Dimens.spaceXxs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = Dimens.spaceSm,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Yields to the badge rather than squeezing it: a long address is still
                    // readable truncated, a badge one letter per line is not.
                    modifier = Modifier.weight(1f, fill = false),
                )
                VerificationBadge(isVerified = user.isEmailVerified)
            }
        }
        user.memberSinceYear()?.let { year ->
            Spacer(Modifier.size(Dimens.spaceXs))
            Text(
                text = stringResource(Res.string.profile_member_since, year),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Says whether the address has been confirmed, in the colour the answer deserves. */
@Composable
private fun VerificationBadge(
    isVerified: Boolean,
    modifier: Modifier = Modifier,
) {
    val glass = auroraColors
    val contentColor = if (isVerified) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.radiusPill),
        color = glass.glassFill,
        border = BorderStroke(Dimens.hairline, glass.glassBorder),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Dimens.spaceSm,
                vertical = Dimens.spaceXxs,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isVerified) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = stringResource(Res.string.cd_verified),
                    modifier = Modifier.size(Dimens.iconSm),
                    tint = contentColor,
                )
            }
            Text(
                text = if (isVerified) {
                    stringResource(Res.string.profile_email_verified)
                } else {
                    stringResource(Res.string.profile_email_unverified)
                },
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * The year the account was created, or null when the server sent something this can't read.
 *
 * `createdAt` is a raw ISO-8601 string and there is no date library on the classpath, so this
 * takes the year and nothing more — a month name would need either a new dependency or twelve
 * string resources, for a line that is decorative either way. Anything that isn't four digits is
 * dropped rather than shown raw.
 */
internal fun UserResponse.memberSinceYear(): String? =
    createdAt.take(YEAR_LENGTH).takeIf { year ->
        year.length == YEAR_LENGTH && year.all { it.isDigit() }
    }

@Preview
@Composable
private fun ProfileHeaderPreview() {
    AppTheme {
        ProfileHeader(user = sampleUser())
    }
}

@Preview
@Composable
private fun ProfileHeaderUnverifiedPreview() {
    AppTheme {
        ProfileHeader(
            user = sampleUser(displayName = null, email = "ada@cmpsrc.dev")
                .copy(isEmailVerified = false),
        )
    }
}
