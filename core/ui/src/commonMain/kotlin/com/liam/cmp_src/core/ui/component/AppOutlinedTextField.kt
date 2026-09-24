package com.liam.cmp_src.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import cmpsrc.core.ui.generated.resources.Res
import cmpsrc.core.ui.generated.resources.cd_email_icon
import cmpsrc.core.ui.generated.resources.ic_email
import cmpsrc.core.ui.generated.resources.login_email_label
import cmpsrc.core.ui.generated.resources.login_email_placeholder
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val FOCUS_TRANSITION_MILLIS = 220
private const val ERROR_REVEAL_MILLIS = 180

/**
 * The app's outlined text field: glass fill, hairline border that brightens to the accent on focus,
 * floating label, optional leading icon, and an inline error that expands in beneath the field.
 *
 * Focus is animated on two channels at once — the border and the leading icon pick up the same
 * tint — so the active field reads clearly against the moving background. [trailingIcon] is handed
 * that tint too, so an action at the end of the field (a reveal toggle, a dropdown arrow) moves in
 * step with the icon at the start.
 *
 * [AuthTextField] and [AppDropdownField] are this field with their extras attached; reach for it
 * directly for any other free-text input.
 *
 * @param errorMessage shown under the field and turns it to the error colour; `null` means valid.
 * @param interactionSource pass one to observe focus or presses from outside, e.g. to open a menu.
 */
@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: DrawableResource? = null,
    leadingIconDescription: String? = null,
    trailingIcon: (@Composable (tint: Color) -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
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
        label = "outlinedFieldIconTint",
    )

    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = RoundedCornerShape(Dimens.radiusMd),
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = leadingIconDescription,
                        tint = iconTint,
                        modifier = Modifier.size(Dimens.iconMd),
                    )
                }
            },
            trailingIcon = trailingIcon?.let { { it(iconTint) } },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = source,
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

@Preview
@Composable
private fun AppOutlinedTextFieldPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(PaddingValues(Dimens.spaceLg)),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            AppOutlinedTextField(
                value = "",
                onValueChange = {},
                label = stringResource(Res.string.login_email_label),
                placeholder = stringResource(Res.string.login_email_placeholder),
                leadingIcon = Res.drawable.ic_email,
                leadingIconDescription = stringResource(Res.string.cd_email_icon),
            )
            AppOutlinedTextField(
                value = "not-an-email",
                onValueChange = {},
                label = stringResource(Res.string.login_email_label),
                errorMessage = "Enter a valid email",
            )
        }
    }
}
