package com.liam.cmp_src.feature.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.component.GlassCard
import com.liam.cmp_src.core.ui.component.StaggeredEntrance
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import com.liam.cmp_src.feature.auth.presentation.component.ActionButtonState
import com.liam.cmp_src.feature.auth.presentation.component.PrimaryActionButton
import com.liam.cmp_src.feature.auth.presentation.login.asMessage
import com.liam.cmp_src.feature.home.component.sampleUser
import com.liam.cmp_src.feature.profile.component.ProfileActionDivider
import com.liam.cmp_src.feature.profile.component.ProfileActionRow
import com.liam.cmp_src.feature.profile.component.ProfileHeader
import com.liam.cmp_src.feature.profile.component.ProfileLinkedAccounts
import com.liam.cmp_src.feature.profile.component.ProfileSkeleton
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.home_sign_out
import cmpsrc.shared.generated.resources.ic_edit
import cmpsrc.shared.generated.resources.ic_lock
import cmpsrc.shared.generated.resources.login_not_implemented
import cmpsrc.shared.generated.resources.profile_change_password
import cmpsrc.shared.generated.resources.profile_edit
import cmpsrc.shared.generated.resources.profile_error_title
import cmpsrc.shared.generated.resources.profile_try_again
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val STATE_CROSSFADE_MILLIS = 240

/**
 * Profile wired to its [ProfileViewModel].
 *
 * Same split as `LoginRoute` and `HomeRoute`: the route owns the ViewModel and reports what
 * happened, while the stateless [ProfileScreen] below takes a state and a callback and can be
 * previewed without Koin. [onLogout] fires only once the session has actually been ended.
 */
@Composable
fun ProfileRoute(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEvent.GoToLogin -> onLogout()
                // getString rather than stringResource: read here, a composable-scoped
                // resource would have been captured before the event arrived.
                ProfileEvent.ShowNotImplemented ->
                    snackbarHostState.showSnackbar(getString(Res.string.login_not_implemented))
            }
        }
    }

    ProfileScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * The account screen: who is signed in, what is linked to them, and what they can do about it.
 *
 * Carries its own [SnackbarHostState] so it stands alone — it is hosted inside the home shell's
 * tab today, whose Scaffold slots are not reachable from here.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = state,
            // Keyed on the state's type: re-reading the same profile should not re-run the
            // crossfade, only a move between loading, loaded and failed should.
            contentKey = { it::class },
            transitionSpec = {
                fadeIn(tween(STATE_CROSSFADE_MILLIS)) togetherWith
                    fadeOut(tween(STATE_CROSSFADE_MILLIS))
            },
            label = "profileState",
        ) { targetState ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = Dimens.cardMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (targetState) {
                        ProfileUiState.Loading -> ProfileSkeleton()

                        is ProfileUiState.Success -> ProfileContent(
                            user = targetState.user,
                            onAction = onAction,
                        )

                        is ProfileUiState.Error -> ProfileError(
                            error = targetState.error,
                            onTryAgain = { onAction(ProfileAction.Retry) },
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The loaded profile, its sections cascading in the way the rest of the app's screens do. */
@Composable
private fun ProfileContent(
    user: UserResponse,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = auroraColors
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StaggeredEntrance(visible = isVisible, index = 1) {
            ProfileHeader(user = user)
        }

        Spacer(Modifier.size(Dimens.spaceLg))

        StaggeredEntrance(visible = isVisible, index = 2) {
            ProfileLinkedAccounts(user = user)
        }

        Spacer(Modifier.size(Dimens.spaceLg))

        StaggeredEntrance(visible = isVisible, index = 3) {
            GlassCard(
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                ProfileActionRow(
                    icon = Res.drawable.ic_edit,
                    label = stringResource(Res.string.profile_edit),
                    onClick = { onAction(ProfileAction.EditProfile) },
                )
                ProfileActionDivider()
                ProfileActionRow(
                    icon = Res.drawable.ic_lock,
                    label = stringResource(Res.string.profile_change_password),
                    onClick = { onAction(ProfileAction.ChangePassword) },
                )
            }
        }

        Spacer(Modifier.size(Dimens.spaceLg))

        StaggeredEntrance(visible = isVisible, index = 4) {
            OutlinedButton(
                onClick = { onAction(ProfileAction.Logout) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight)
                    .handCursor(),
                shape = RoundedCornerShape(Dimens.radiusMd),
                // The default outline token is far too dark to read against the aurora
                // backdrop; match the glass treatment used on the login card.
                border = BorderStroke(Dimens.hairline, glass.glassBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = glass.glassFill,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
            ) {
                Text(stringResource(Res.string.home_sign_out))
            }
        }
    }
}

/** The profile could not be read. Says what went wrong, and offers the only useful remedy. */
@Composable
private fun ProfileError(
    error: AuthError,
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassCard {
            Text(
                text = stringResource(Res.string.profile_error_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(Dimens.spaceSm))
            Text(
                text = error.asMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(Dimens.spaceLg))
            PrimaryActionButton(
                label = stringResource(Res.string.profile_try_again),
                state = ActionButtonState.Idle,
                onClick = onTryAgain,
            )
        }
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    AppTheme {
        ProfileScreen(
            state = ProfileUiState.Success(
                sampleUser(linkedProviders = listOf(SocialProvider.GOOGLE.key)),
            ),
            onAction = {},
            modifier = Modifier.padding(Dimens.screenPadding),
        )
    }
}

@Preview
@Composable
private fun ProfileScreenLoadingPreview() {
    AppTheme {
        ProfileScreen(
            state = ProfileUiState.Loading,
            onAction = {},
            modifier = Modifier.padding(Dimens.screenPadding),
        )
    }
}

@Preview
@Composable
private fun ProfileScreenErrorPreview() {
    AppTheme {
        ProfileScreen(
            state = ProfileUiState.Error(AuthError.Network),
            onAction = {},
            modifier = Modifier.padding(Dimens.screenPadding),
        )
    }
}
