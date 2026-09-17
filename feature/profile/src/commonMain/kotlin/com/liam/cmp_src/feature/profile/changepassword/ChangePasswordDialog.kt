package com.liam.cmp_src.feature.profile.changepassword

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.common.FieldLimits
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.feature.profile.domain.model.ChangePasswordErrors
import com.liam.cmp_src.core.domain.model.PasswordError
import com.liam.cmp_src.core.ui.component.ActionButtonState
import com.liam.cmp_src.core.ui.component.AuthTextField
import com.liam.cmp_src.core.ui.component.ErrorBanner
import com.liam.cmp_src.core.ui.component.PrimaryActionButton
import com.liam.cmp_src.core.ui.message.asMessage
import cmpsrc.core.ui.generated.resources.Res as UiRes
import cmpsrc.core.ui.generated.resources.cd_password_icon
import cmpsrc.core.ui.generated.resources.ic_lock
import cmpsrc.feature.profile.generated.resources.Res
import cmpsrc.feature.profile.generated.resources.change_password_cancel
import cmpsrc.feature.profile.generated.resources.change_password_confirm_label
import cmpsrc.feature.profile.generated.resources.change_password_confirm_placeholder
import cmpsrc.feature.profile.generated.resources.change_password_current_label
import cmpsrc.feature.profile.generated.resources.change_password_current_placeholder
import cmpsrc.feature.profile.generated.resources.change_password_new_label
import cmpsrc.feature.profile.generated.resources.change_password_new_placeholder
import cmpsrc.feature.profile.generated.resources.change_password_submit
import cmpsrc.feature.profile.generated.resources.change_password_subtitle
import cmpsrc.feature.profile.generated.resources.change_password_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The change-password dialog, wired to its [ChangePasswordViewModel].
 *
 * Same split as `LoginRoute` and `ProfileRoute`: this half owns the ViewModel and reports what
 * happened, while [ChangePasswordDialogContent] takes a state and a callback and can be previewed
 * without Koin.
 *
 * [onChanged] fires once the password has actually been replaced. The session is still live at
 * that point — the server keeps the device that made the change signed in — so the caller closes
 * the dialog and says so, rather than sending the user back to sign-in.
 */
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    viewModel: ChangePasswordViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        // The ViewModel is scoped to the enclosing back-stack entry, so it outlives this dialog:
        // announcing the open is what clears whatever a previous attempt left behind.
        viewModel.onAction(ChangePasswordAction.Opened)
        viewModel.events.collect { event ->
            when (event) {
                ChangePasswordEvent.Dismissed -> onDismiss()
                ChangePasswordEvent.Changed -> onChanged()
            }
        }
    }

    Dialog(
        onDismissRequest = { viewModel.onAction(ChangePasswordAction.Cancel) },
        properties = DialogProperties(
            // A submit in flight must not be dismissed out from under itself: the call would
            // still land, and the user would be left guessing whether it had.
            dismissOnBackPress = !state.isBusy,
            dismissOnClickOutside = !state.isBusy,
        ),
    ) {
        ChangePasswordDialogContent(state = state, onAction = viewModel::onAction)
    }
}

/**
 * What the dialog window holds: three password fields, whatever went wrong, and the two ways out.
 *
 * Painted on an opaque [MaterialTheme.colorScheme.surface] rather than the `GlassCard` treatment
 * the signed-in screens use — a translucent fill over the dialog scrim reads as muddy rather than
 * as glass, because there is no aurora backdrop behind it to catch.
 */
@Composable
fun ChangePasswordDialogContent(
    state: ChangePasswordUiState,
    onAction: (ChangePasswordAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val glass = auroraColors

    val submit = {
        focusManager.clearFocus()
        onAction(ChangePasswordAction.Submit)
    }
    val moveFocusDown = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

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
            Text(
                text = stringResource(Res.string.change_password_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(Dimens.spaceXs))
            Text(
                text = stringResource(Res.string.change_password_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.size(Dimens.spaceLg))

            AuthTextField(
                value = state.currentPassword,
                onValueChange = { onAction(ChangePasswordAction.CurrentPasswordChanged(it)) },
                label = stringResource(Res.string.change_password_current_label),
                placeholder = stringResource(Res.string.change_password_current_placeholder),
                leadingIcon = UiRes.drawable.ic_lock,
                leadingIconDescription = stringResource(UiRes.string.cd_password_icon),
                enabled = !state.isBusy,
                errorMessage = state.fieldErrors.currentPassword?.asMessage(),
                isPassword = true,
                isPasswordVisible = state.isCurrentVisible,
                onTogglePasswordVisibility = {
                    onAction(ChangePasswordAction.ToggleCurrentVisibility)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = moveFocusDown,
            )

            Spacer(Modifier.size(Dimens.spaceMd))

            AuthTextField(
                value = state.newPassword,
                onValueChange = { onAction(ChangePasswordAction.NewPasswordChanged(it)) },
                label = stringResource(Res.string.change_password_new_label),
                placeholder = stringResource(
                    Res.string.change_password_new_placeholder,
                    FieldLimits.MIN_PASSWORD_LENGTH,
                ),
                leadingIcon = UiRes.drawable.ic_lock,
                leadingIconDescription = stringResource(UiRes.string.cd_password_icon),
                enabled = !state.isBusy,
                errorMessage = state.fieldErrors.newPassword?.asMessage(),
                isPassword = true,
                isPasswordVisible = state.isNewVisible,
                onTogglePasswordVisibility = { onAction(ChangePasswordAction.ToggleNewVisibility) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = moveFocusDown,
            )

            Spacer(Modifier.size(Dimens.spaceMd))

            AuthTextField(
                value = state.confirmPassword,
                onValueChange = { onAction(ChangePasswordAction.ConfirmPasswordChanged(it)) },
                label = stringResource(Res.string.change_password_confirm_label),
                placeholder = stringResource(Res.string.change_password_confirm_placeholder),
                leadingIcon = UiRes.drawable.ic_lock,
                leadingIconDescription = stringResource(UiRes.string.cd_password_icon),
                enabled = !state.isBusy,
                errorMessage = state.fieldErrors.confirmPassword?.asMessage(),
                isPassword = true,
                // Follows the field above it: revealing one half of a pair the user is being
                // asked to match, and not the other, helps nobody.
                isPasswordVisible = state.isNewVisible,
                onTogglePasswordVisibility = { onAction(ChangePasswordAction.ToggleNewVisibility) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )

            ErrorBanner(error = state.error)

            Spacer(Modifier.size(Dimens.spaceLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                // The two buttons are different heights by design — the text one is a way out,
                // not a peer of the primary action — so centre them rather than top-aligning.
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onAction(ChangePasswordAction.Cancel) },
                    modifier = Modifier.handCursor(!state.isBusy),
                    enabled = !state.isBusy,
                ) {
                    Text(stringResource(Res.string.change_password_cancel))
                }

                PrimaryActionButton(
                    label = stringResource(Res.string.change_password_submit),
                    state = when (state.status) {
                        ChangePasswordStatus.Succeeded -> ActionButtonState.Success
                        ChangePasswordStatus.Submitting -> ActionButtonState.Loading
                        else -> ActionButtonState.Idle
                    },
                    onClick = submit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChangePasswordDialogPreview() {
    AppTheme {
        ChangePasswordDialogContent(
            state = ChangePasswordUiState(currentPassword = "hunter2000"),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ChangePasswordDialogFieldErrorsPreview() {
    AppTheme {
        ChangePasswordDialogContent(
            state = ChangePasswordUiState(
                newPassword = "short",
                confirmPassword = "shor",
                fieldErrors = ChangePasswordErrors(
                    currentPassword = PasswordError.Blank,
                    newPassword = PasswordError.TooShort(FieldLimits.MIN_PASSWORD_LENGTH),
                    confirmPassword = PasswordError.Mismatch,
                ),
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ChangePasswordDialogFailedPreview() {
    AppTheme {
        ChangePasswordDialogContent(
            state = ChangePasswordUiState(
                currentPassword = "wrong-one",
                newPassword = "a-long-enough-password",
                confirmPassword = "a-long-enough-password",
                status = ChangePasswordStatus.Failed(AuthError.WrongPassword),
            ),
            onAction = {},
        )
    }
}
