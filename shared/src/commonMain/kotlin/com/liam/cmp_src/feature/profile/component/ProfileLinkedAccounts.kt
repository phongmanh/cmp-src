package com.liam.cmp_src.feature.profile.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import com.liam.cmp_src.feature.auth.presentation.login.asLabel
import com.liam.cmp_src.feature.home.component.sampleUser
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.cd_facebook_logo
import cmpsrc.shared.generated.resources.cd_google_logo
import cmpsrc.shared.generated.resources.ic_facebook
import cmpsrc.shared.generated.resources.ic_google
import cmpsrc.shared.generated.resources.profile_linked_accounts
import cmpsrc.shared.generated.resources.profile_no_linked_accounts
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The providers this account can sign in through, as chips.
 *
 * An empty list is a real answer, not a missing one — an email/password account has no linked
 * provider — so it gets a line of its own rather than an empty row. Keys the build does not
 * recognise are skipped by [SocialProvider.fromKey] instead of being shown raw.
 */
@Composable
fun ProfileLinkedAccounts(
    user: UserResponse,
    modifier: Modifier = Modifier,
) {
    val providers = remember(user.linkedProviders) {
        user.linkedProviders.mapNotNull(SocialProvider::fromKey).distinct()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Text(
            text = stringResource(Res.string.profile_linked_accounts),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (providers.isEmpty()) {
            Text(
                text = stringResource(Res.string.profile_no_linked_accounts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                providers.forEach { provider -> LinkedAccountChip(provider) }
            }
        }
    }
}

@Composable
private fun LinkedAccountChip(
    provider: SocialProvider,
    modifier: Modifier = Modifier,
) {
    val glass = auroraColors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.radiusPill),
        color = glass.glassFill,
        border = BorderStroke(Dimens.hairline, glass.glassBorder),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Dimens.spaceMd,
                vertical = Dimens.spaceSm,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(provider.icon()),
                contentDescription = stringResource(provider.iconDescription()),
                modifier = Modifier.size(Dimens.iconSm),
                // The provider marks are brand artwork; tinting them would be wrong.
                tint = Color.Unspecified,
            )
            Text(
                text = provider.asLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

private fun SocialProvider.icon(): DrawableResource = when (this) {
    SocialProvider.GOOGLE -> Res.drawable.ic_google
    SocialProvider.FACEBOOK -> Res.drawable.ic_facebook
}

private fun SocialProvider.iconDescription(): StringResource = when (this) {
    SocialProvider.GOOGLE -> Res.string.cd_google_logo
    SocialProvider.FACEBOOK -> Res.string.cd_facebook_logo
}

@Preview
@Composable
private fun ProfileLinkedAccountsPreview() {
    AppTheme {
        ProfileLinkedAccounts(
            user = sampleUser(
                linkedProviders = listOf(
                    SocialProvider.GOOGLE.key,
                    SocialProvider.FACEBOOK.key,
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun ProfileLinkedAccountsEmptyPreview() {
    AppTheme {
        ProfileLinkedAccounts(user = sampleUser())
    }
}
