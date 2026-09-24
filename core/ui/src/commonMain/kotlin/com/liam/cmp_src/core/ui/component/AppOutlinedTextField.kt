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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
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
 * The text lives in [state] rather than in a value/callback pair, so the keyboard edits it directly
 * and the field can never fall a frame behind what was typed. Whoever needs the text — usually a
 * ViewModel, through a [com.liam.cmp_src.core.ui.input.FormField] — holds the [TextFieldState].
 *
 * [AuthTextField] and [AppDropdownField] are this field with their extras attached, and
 * [AppOutlinedSecureTextField] is its password twin; reach for this one directly for any other
 * free-text input.
 *
 * @param errorMessage shown under the field and turns it to the error colour; `null` means valid.
 * @param inputTransformation filters or rewrites what is typed before it reaches [state], e.g.
 *   `InputTransformation.maxLength(...)`.
 * @param onKeyboardAction replaces the IME action's default behaviour; `null` keeps the default.
 * @param interactionSource pass one to observe focus or presses from outside, e.g. to open a menu.
 */
@Composable
fun AppOutlinedTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: DrawableResource? = null,
    leadingIconDescription: String? = null,
    trailingIcon: (@Composable (tint: Color) -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    errorMessage: String? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val iconTint = rememberFieldTint(source, isError = errorMessage != null)

    FieldWithError(errorMessage, modifier) {
        OutlinedTextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            isError = errorMessage != null,
            lineLimits = lineLimits,
            shape = RoundedCornerShape(Dimens.radiusMd),
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon?.let { { FieldIcon(it, leadingIconDescription, iconTint) } },
            trailingIcon = trailingIcon?.let { { it(iconTint) } },
            inputTransformation = inputTransformation,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            interactionSource = source,
            colors = appFieldColors(),
        )
    }
}

/**
 * [AppOutlinedTextField] for a secret: the same look, on top of Material's secure field.
 *
 * Unlike a visual transformation over a plain field, the secure field shows each character briefly
 * as it is typed, then masks it — as the platform's own password fields do — and refuses to copy or
 * cut the masked text. [isRevealed] shows the whole value instead.
 */
@Composable
fun AppOutlinedSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: DrawableResource? = null,
    leadingIconDescription: String? = null,
    trailingIcon: (@Composable (tint: Color) -> Unit)? = null,
    enabled: Boolean = true,
    isRevealed: Boolean = false,
    errorMessage: String? = null,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    val source = remember { MutableInteractionSource() }
    val iconTint = rememberFieldTint(source, isError = errorMessage != null)

    FieldWithError(errorMessage, modifier) {
        OutlinedSecureTextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = errorMessage != null,
            textObfuscationMode = if (isRevealed) {
                TextObfuscationMode.Visible
            } else {
                TextObfuscationMode.RevealLastTyped
            },
            shape = RoundedCornerShape(Dimens.radiusMd),
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon?.let { { FieldIcon(it, leadingIconDescription, iconTint) } },
            trailingIcon = trailingIcon?.let { { it(iconTint) } },
            inputTransformation = inputTransformation,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            interactionSource = source,
            colors = appFieldColors(),
        )
    }
}

/** The tint the leading and trailing icons share: error, then focus, then resting. */
@Composable
private fun rememberFieldTint(source: MutableInteractionSource, isError: Boolean): Color {
    val isFocused by source.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        targetValue = when {
            isError -> colors.error
            isFocused -> colors.primary
            else -> colors.onSurfaceVariant
        },
        animationSpec = tween(FOCUS_TRANSITION_MILLIS),
        label = "outlinedFieldIconTint",
    )
    return tint
}

@Composable
private fun FieldIcon(icon: DrawableResource, description: String?, tint: Color) {
    Icon(
        painter = painterResource(icon),
        contentDescription = description,
        tint = tint,
        modifier = Modifier.size(Dimens.iconMd),
    )
}

@Composable
private fun appFieldColors(): TextFieldColors {
    val colors = MaterialTheme.colorScheme
    val glass = auroraColors
    return OutlinedTextFieldDefaults.colors(
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
    )
}

/** [field], with [errorMessage] expanding in beneath it while there is one. */
@Composable
private fun FieldWithError(
    errorMessage: String?,
    modifier: Modifier,
    field: @Composable () -> Unit,
) {
    Column(modifier) {
        field()

        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(tween(ERROR_REVEAL_MILLIS)) + expandVertically(tween(ERROR_REVEAL_MILLIS)),
            exit = fadeOut(tween(ERROR_REVEAL_MILLIS)) + shrinkVertically(tween(ERROR_REVEAL_MILLIS)),
        ) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
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
                state = rememberTextFieldState(),
                label = stringResource(Res.string.login_email_label),
                placeholder = stringResource(Res.string.login_email_placeholder),
                leadingIcon = Res.drawable.ic_email,
                leadingIconDescription = stringResource(Res.string.cd_email_icon),
            )
            AppOutlinedTextField(
                state = rememberTextFieldState("not-an-email"),
                label = stringResource(Res.string.login_email_label),
                errorMessage = "Enter a valid email",
            )
        }
    }
}
