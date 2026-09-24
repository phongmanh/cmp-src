package com.liam.cmp_src.core.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import cmpsrc.core.ui.generated.resources.Res
import cmpsrc.core.ui.generated.resources.cd_email_icon
import cmpsrc.core.ui.generated.resources.ic_email
import cmpsrc.core.ui.generated.resources.ic_eye
import cmpsrc.core.ui.generated.resources.ic_eye_off
import cmpsrc.core.ui.generated.resources.login_email_label
import cmpsrc.core.ui.generated.resources.login_email_placeholder
import cmpsrc.core.ui.generated.resources.login_hide_password
import cmpsrc.core.ui.generated.resources.login_show_password
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A single credential input: an [AppOutlinedTextField] that always has a leading icon, or — for a
 * password — an [AppOutlinedSecureTextField] with a reveal toggle at the end.
 */
@Composable
fun AuthTextField(
    state: TextFieldState,
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
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    if (isPassword) {
        AppOutlinedSecureTextField(
            state = state,
            modifier = modifier,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            leadingIconDescription = leadingIconDescription,
            trailingIcon = { tint ->
                PasswordVisibilityToggle(isPasswordVisible, onTogglePasswordVisibility, tint)
            },
            enabled = enabled,
            isRevealed = isPasswordVisible,
            errorMessage = errorMessage,
            inputTransformation = inputTransformation,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
        )
    } else {
        AppOutlinedTextField(
            state = state,
            modifier = modifier,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            leadingIconDescription = leadingIconDescription,
            enabled = enabled,
            errorMessage = errorMessage,
            inputTransformation = inputTransformation,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
        )
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
                state = rememberTextFieldState(),
                label = stringResource(Res.string.login_email_label),
                placeholder = stringResource(Res.string.login_email_placeholder),
                leadingIcon = Res.drawable.ic_email,
                leadingIconDescription = stringResource(Res.string.cd_email_icon),
            )
        }
    }
}
