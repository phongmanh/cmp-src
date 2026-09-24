package com.liam.cmp_src.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import cmpsrc.core.ui.generated.resources.Res
import cmpsrc.core.ui.generated.resources.ic_check
import cmpsrc.core.ui.generated.resources.ic_expand_more
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val ARROW_TURN_MILLIS = 200
private const val ARROW_EXPANDED_DEGREES = 180f

/**
 * Pick one of [options] from a menu that drops down under an [AppOutlinedTextField].
 *
 * The field is read-only — it shows [optionLabel] of the [selected] option, or [placeholder] while
 * nothing is — and tapping anywhere on it opens the menu. The arrow turns over as the menu opens,
 * and the chosen row carries a check, so the current value is visible from inside the menu too.
 *
 * The menu's open/closed state lives here: it is pure presentation, and no screen needs to
 * restore a menu that was open when it went away. The selection is the caller's.
 *
 * @param optionLabel the text for an option — in the field and in its menu row.
 * @param errorMessage shown under the field, as with [AppOutlinedTextField]; `null` means valid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppDropdownField(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: DrawableResource? = null,
    leadingIconDescription: String? = null,
    enabled: Boolean = true,
    errorMessage: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) ARROW_EXPANDED_DEGREES else 0f,
        animationSpec = tween(ARROW_TURN_MILLIS),
        label = "dropdownArrowRotation",
    )

    // The field only ever shows the selection, so its state follows [selected] rather than input.
    val selectedLabel = selected?.let { optionLabel(it) }.orEmpty()
    val fieldState = rememberTextFieldState(selectedLabel)
    LaunchedEffect(selectedLabel) {
        if (fieldState.text.toString() != selectedLabel) {
            fieldState.setTextAndPlaceCursorAtEnd(selectedLabel)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        AppOutlinedTextField(
            state = fieldState,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .handCursor(enabled),
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            leadingIconDescription = leadingIconDescription,
            trailingIcon = { tint ->
                Icon(
                    painter = painterResource(Res.drawable.ic_expand_more),
                    // The field itself announces the expand/collapse action.
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(Dimens.iconMd)
                        .rotate(arrowRotation),
                )
            },
            enabled = enabled,
            readOnly = true,
            errorMessage = errorMessage,
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(Dimens.radiusMd),
            containerColor = MaterialTheme.colorScheme.surface,
            border = BorderStroke(Dimens.hairline, auroraColors.glassBorder),
        ) {
            options.forEach { option ->
                DropdownOptionRow(
                    text = optionLabel(option),
                    isSelected = option == selected,
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DropdownOptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) colors.primary else colors.onSurface,
            )
        },
        onClick = onClick,
        modifier = Modifier.handCursor(),
        trailingIcon = if (isSelected) {
            {
                Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(Dimens.iconSm),
                )
            }
        } else {
            null
        },
    )
}

@Preview
@Composable
private fun AppDropdownFieldPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(PaddingValues(Dimens.spaceLg)),
        ) {
            AppDropdownField(
                options = listOf("English", "Tiếng Việt", "日本語"),
                selected = "English",
                onSelect = {},
                optionLabel = { it },
                label = "Language",
            )
        }
    }
}
