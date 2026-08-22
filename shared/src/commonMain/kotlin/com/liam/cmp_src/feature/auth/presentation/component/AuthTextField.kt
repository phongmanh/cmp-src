package com.liam.cmp_src.feature.auth.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.cd_email_icon
import cmpsrc.shared.generated.resources.ic_email
import cmpsrc.shared.generated.resources.ic_eye
import cmpsrc.shared.generated.resources.ic_eye_off
import cmpsrc.shared.generated.resources.login_email_label
import cmpsrc.shared.generated.resources.login_email_placeholder
import cmpsrc.shared.generated.resources.login_hide_password
import cmpsrc.shared.generated.resources.login_show_password
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val FOCUS_TRANSITION_MILLIS = 220
private const val ERROR_REVEAL_MILLIS = 180

/**
 * A single credential input: leading icon, floating label, optional password reveal toggle,
 * and an inline error that expands in beneath the field.
 *
 * Focus is animated on two channels at once — the border colour brightens to the accent and
 * the leading icon picks up the same tint — so the active field reads clearly even against
 * the moving background.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: DrawableResource,
    leadingIconDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorMessage: String? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isError = errorMessage != null
    val colors = MaterialTheme.colorScheme
    val glass = auroraColors

    val iconTint by animateColorAsState(
        targetValue = when {
            isError -> colors.error
            isFocused -> colors.primary
            else -> colors.onSurfaceVariant
        },
        animationSpec = tween(FOCUS_TRANSITION_MILLIS),
        label = "authFieldIconTint",
    )

    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(Dimens.radiusMd),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = leadingIconDescription,
                    tint = iconTint,
                    modifier = Modifier.size(Dimens.iconMd),
                )
            },
            trailingIcon = if (isPassword) {
                { PasswordVisibilityToggle(isPasswordVisible, onTogglePasswordVisibility, iconTint) }
            } else {
                null
            },
            visualTransformation = if (isPassword && !isPasswordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = glass.glassFill,
                unfocusedContainerColor = glass.glassFill,
                disabledContainerColor = glass.glassFill,
                errorContainerColor = glass.glassFill,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = glass.glassBorder,
                disabledBorderColor = glass.glassBorder,
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface,
                cursorColor = colors.primary,
            ),
        )

        AnimatedVisibility(
            visible = isError,
            enter = fadeIn(tween(ERROR_REVEAL_MILLIS)) + expandVertically(tween(ERROR_REVEAL_MILLIS)),
            exit = fadeOut(tween(ERROR_REVEAL_MILLIS)) + shrinkVertically(tween(ERROR_REVEAL_MILLIS)),
        ) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier.padding(
                    start = Dimens.spaceLg,
                    top = Dimens.spaceXs,
                    end = Dimens.spaceLg,
                ),
            )
        }
    }
}

@Composable
private fun PasswordVisibilityToggle(
    isPasswordVisible: Boolean,
    onToggle: () -> Unit,
    tint: Color,
) {
    IconButton(onClick = onToggle, modifier = Modifier.handCursor()) {
        Crossfade(targetState = isPasswordVisible, label = "passwordVisibility") { visible ->
            Icon(
                painter = painterResource(if (visible) Res.drawable.ic_eye_off else Res.drawable.ic_eye),
                contentDescription = stringResource(
                    if (visible) Res.string.login_hide_password else Res.string.login_show_password,
                ),
                tint = tint,
                modifier = Modifier.size(Dimens.iconMd),
            )
        }
    }
}

@Preview
@Composable
private fun AuthTextFieldPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(PaddingValues(Dimens.spaceLg)),
        ) {
            AuthTextField(
                value = "",
                onValueChange = {},
                label = stringResource(Res.string.login_email_label),
                placeholder = stringResource(Res.string.login_email_placeholder),
                leadingIcon = Res.drawable.ic_email,
                leadingIconDescription = stringResource(Res.string.cd_email_icon),
            )
        }
    }
}
