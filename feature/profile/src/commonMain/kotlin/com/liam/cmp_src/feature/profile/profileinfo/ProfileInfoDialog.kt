package com.liam.cmp_src.feature.profile.profileinfo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.feature.profile.domain.model.DisplayNameError
import com.liam.cmp_src.core.ui.component.ActionButtonState
import com.liam.cmp_src.core.ui.component.AuthTextField
import com.liam.cmp_src.core.ui.component.ErrorBanner
import com.liam.cmp_src.core.ui.component.PrimaryActionButton
import com.liam.cmp_src.core.ui.component.SectionHeader
import com.liam.cmp_src.core.ui.component.SectionHeaderStyle
import com.liam.cmp_src.core.ui.message.asMessage
import com.liam.cmp_src.core.ui.component.UserAvatar
import com.liam.cmp_src.core.ui.component.sampleUser
import cmpsrc.core.ui.generated.resources.Res as UiRes
import cmpsrc.core.ui.generated.resources.ic_person
import cmpsrc.feature.profile.generated.resources.Res
import cmpsrc.feature.profile.generated.resources.cd_name_icon
import cmpsrc.feature.profile.generated.resources.edit_profile_change_photo
import cmpsrc.feature.profile.generated.resources.edit_profile_done
import cmpsrc.feature.profile.generated.resources.edit_profile_name_label
import cmpsrc.feature.profile.generated.resources.edit_profile_name_placeholder
import cmpsrc.feature.profile.generated.resources.edit_profile_photo_warning
import cmpsrc.feature.profile.generated.resources.edit_profile_remove_photo
import cmpsrc.feature.profile.generated.resources.edit_profile_save_name
import cmpsrc.feature.profile.generated.resources.edit_profile_subtitle
import cmpsrc.feature.profile.generated.resources.edit_profile_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The edit-profile dialog, wired to its [ProfileInfoViewModel].
 *
 * Same split as `ChangePasswordDialog`: this half owns the ViewModel and reports what happened,
 * while [ProfileInfoDialogContent] takes a state and a callback and can be previewed without Koin.
 *
 * Unlike that dialog, this one does not close on success — the name and the picture are saved by
 * separate calls, so closing after the first would strand the second. [onUpdated] fires after each
 * one, and [onDismiss] only when the user says they are finished.
 *
 * [onPickPhoto] is a callback rather than a picker of this dialog's own: FileKit requires its
 * launchers to be remembered in a stable scope, never inside a dialog or a popup, so the one that
 * feeds this lives in `ProfileRoute`.
 */
@Composable
fun ProfileInfoDialog(
    user: UserResponse,
    onPickPhoto: () -> Unit,
    onUpdated: (UserResponse) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ProfileInfoViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Keyed on the ViewModel alone. [user] changes as this dialog saves, and restarting on that
    // would re-announce the open, wiping the confirmation the user is still looking at.
    LaunchedEffect(viewModel) {
        // The ViewModel is scoped to the enclosing back-stack entry, so it outlives this dialog:
        // announcing the open is what reseeds the form from the account as it stands now.
        viewModel.onAction(ProfileInfoAction.Opened(user))
        viewModel.events.collect { event ->
            when (event) {
                is ProfileInfoEvent.Updated -> onUpdated(event.user)
                ProfileInfoEvent.Dismissed -> onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = { viewModel.onAction(ProfileInfoAction.Close) },
        properties = DialogProperties(
            // A write in flight must not be dismissed out from under itself: the call would still
            // land, and the user would be left guessing whether it had.
            dismissOnBackPress = !state.isBusy,
            dismissOnClickOutside = !state.isBusy,
        ),
    ) {
        ProfileInfoDialogContent(
            state = state,
            onAction = viewModel::onAction,
            onPickPhoto = onPickPhoto,
        )
    }
}

/**
 * What the dialog window holds: the picture and its two buttons, the name and its own save, and
 * whatever went wrong.
 *
 * Painted on an opaque [MaterialTheme.colorScheme.surface] rather than the `GlassCard` treatment
 * the signed-in screens use, for the same reason `ChangePasswordDialogContent` is — a translucent
 * fill over the dialog scrim reads as muddy, with no aurora backdrop behind it to catch.
 */
@Composable
fun ProfileInfoDialogContent(
    state: ProfileInfoUiState,
    onAction: (ProfileInfoAction) -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = auroraColors

    Surface(
        modifier = modifier
            .widthIn(max = Dimens.cardMaxWidth)
            .fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusXl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.hairline, glass.glassBorder),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spaceXl),
        ) {
            SectionHeader(
                title = stringResource(Res.string.edit_profile_title),
                subtitle = stringResource(Res.string.edit_profile_subtitle),
                style = SectionHeaderStyle.Dialog,
            )

            Spacer(Modifier.size(Dimens.spaceLg))

            PhotoSection(state = state, onAction = onAction, onPickPhoto = onPickPhoto)

            Spacer(Modifier.size(Dimens.spaceLg))

            AuthTextField(
                value = state.displayName,
                onValueChange = { onAction(ProfileInfoAction.NameChanged(it)) },
                label = stringResource(Res.string.edit_profile_name_label),
                placeholder = stringResource(Res.string.edit_profile_name_placeholder),
                leadingIcon = UiRes.drawable.ic_person,
                leadingIconDescription = stringResource(Res.string.cd_name_icon),
                enabled = !state.isBusy,
                errorMessage = state.nameError?.asMessage(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onAction(ProfileInfoAction.SaveName) },
                ),
            )

            // Shown only once it is actually true: an unsaved name and a picture this backend
            // stores, which the write is going to retire whatever we send with it.
            if (state.willClearPhoto && state.isNameDirty) {
                Spacer(Modifier.size(Dimens.spaceSm))
                Text(
                    text = stringResource(Res.string.edit_profile_photo_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ErrorBanner(error = state.error)

            Spacer(Modifier.size(Dimens.spaceLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onAction(ProfileInfoAction.Close) },
                    modifier = Modifier.handCursor(!state.isBusy),
                    enabled = !state.isBusy,
                ) {
                    Text(stringResource(Res.string.edit_profile_done))
                }

                PrimaryActionButton(
                    label = stringResource(Res.string.edit_profile_save_name),
                    state = when (state.status) {
                        ProfileInfoStatus.SavingName -> ActionButtonState.Loading
                        ProfileInfoStatus.Succeeded -> ActionButtonState.Success
                        else -> ActionButtonState.Idle
                    },
                    onClick = { onAction(ProfileInfoAction.SaveName) },
                    // Nothing to write while the field still holds what the server has.
                    enabled = state.isNameDirty && !state.isBusy,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * The picture, and the two things that can be done to it.
 *
 * Both write on their own and neither touches the name, which is the whole reason this dialog has
 * more than one action. The spinner sits over the avatar rather than inside a button, so it reads
 * as "this picture is changing" whichever of the two started it.
 */
@Composable
private fun PhotoSection(
    state: ProfileInfoUiState,
    onAction: (ProfileInfoAction) -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPhotoBusy = state.status == ProfileInfoStatus.UploadingPhoto ||
        state.status == ProfileInfoStatus.RemovingPhoto

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            state.user?.let { user ->
                UserAvatar(
                    user = user,
                    size = Dimens.avatarLg,
                    textStyle = MaterialTheme.typography.headlineMedium,
                )
            }
            if (isPhotoBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.progressIndicatorSize),
                    strokeWidth = Dimens.progressStroke,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs)) {
            TextButton(
                onClick = onPickPhoto,
                modifier = Modifier.handCursor(!state.isBusy),
                enabled = !state.isBusy,
            ) {
                Text(stringResource(Res.string.edit_profile_change_photo))
            }
            TextButton(
                onClick = { onAction(ProfileInfoAction.RemovePhoto) },
                modifier = Modifier.handCursor(state.hasPhoto && !state.isBusy),
                enabled = state.hasPhoto && !state.isBusy,
            ) {
                Text(
                    text = stringResource(Res.string.edit_profile_remove_photo),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProfileInfoDialogPreview() {
    AppTheme {
        ProfileInfoDialogContent(
            state = ProfileInfoUiState(user = sampleUser(), displayName = "Demo User"),
            onAction = {},
            onPickPhoto = {},
        )
    }
}

@Preview
@Composable
private fun ProfileInfoDialogPhotoWarningPreview() {
    AppTheme {
        ProfileInfoDialogContent(
            state = ProfileInfoUiState(
                user = sampleUser(avatarUrl = "https://cmpsrc.dev/api/v1/images/abc"),
                displayName = "Ada Lovelace",
            ),
            onAction = {},
            onPickPhoto = {},
        )
    }
}

@Preview
@Composable
private fun ProfileInfoDialogFailedPreview() {
    AppTheme {
        ProfileInfoDialogContent(
            state = ProfileInfoUiState(
                user = sampleUser(),
                displayName = "Demo User",
                nameError = DisplayNameError.TooLong(maxLength = 120),
                status = ProfileInfoStatus.Failed(AuthError.UnsupportedImage),
            ),
            onAction = {},
            onPickPhoto = {},
        )
    }
}
