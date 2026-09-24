package com.liam.cmp_src.core.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
 * A single credential input: an [AppOutlinedTextField] that always has a leading icon and, for a
 * password, a reveal toggle at the end that masks and unmasks the value.
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
    AppOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        leadingIconDescription = leadingIconDescription,
        trailingIcon = if (isPassword) {
            { tint -> PasswordVisibilityToggle(isPasswordVisible, onTogglePasswordVisibility, tint) }
        } else {
            null
        },
        enabled = enabled,
        errorMessage = errorMessage,
        visualTransformation = if (isPassword && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
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
