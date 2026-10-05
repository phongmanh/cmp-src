package com.liam.cmp_src.feature.customers.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cmpsrc.feature.customers.generated.resources.Res
import cmpsrc.feature.customers.generated.resources.cd_back
import cmpsrc.feature.customers.generated.resources.customer_address_line1
import cmpsrc.feature.customers.generated.resources.customer_address_line2
import cmpsrc.feature.customers.generated.resources.customer_city
import cmpsrc.feature.customers.generated.resources.customer_company
import cmpsrc.feature.customers.generated.resources.customer_country_code
import cmpsrc.feature.customers.generated.resources.customer_country_code_placeholder
import cmpsrc.feature.customers.generated.resources.customer_editor_back_to_list
import cmpsrc.feature.customers.generated.resources.customer_editor_delete
import cmpsrc.feature.customers.generated.resources.customer_editor_not_found
import cmpsrc.feature.customers.generated.resources.customer_editor_rejected
import cmpsrc.feature.customers.generated.resources.customer_editor_save
import cmpsrc.feature.customers.generated.resources.customer_editor_section_address
import cmpsrc.feature.customers.generated.resources.customer_editor_section_contact
import cmpsrc.feature.customers.generated.resources.customer_editor_section_notes
import cmpsrc.feature.customers.generated.resources.customer_editor_title_edit
import cmpsrc.feature.customers.generated.resources.customer_editor_title_new
import cmpsrc.feature.customers.generated.resources.customer_email
import cmpsrc.feature.customers.generated.resources.customer_first_name
import cmpsrc.feature.customers.generated.resources.customer_last_name
import cmpsrc.feature.customers.generated.resources.customer_notes
import cmpsrc.feature.customers.generated.resources.customer_phone
import cmpsrc.feature.customers.generated.resources.customer_phone_placeholder
import cmpsrc.feature.customers.generated.resources.customer_postal_code
import cmpsrc.feature.customers.generated.resources.customer_region
import cmpsrc.feature.customers.generated.resources.customer_status
import cmpsrc.feature.customers.generated.resources.customer_status_placeholder
import cmpsrc.feature.customers.generated.resources.customers_delete_body
import cmpsrc.feature.customers.generated.resources.customers_delete_cancel
import cmpsrc.feature.customers.generated.resources.customers_delete_confirm
import cmpsrc.feature.customers.generated.resources.customers_delete_title
import cmpsrc.feature.customers.generated.resources.ic_arrow_back
import com.example.api.common.FieldLimits
import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.core.ui.component.ActionButtonState
import com.liam.cmp_src.core.ui.component.AnimatedAuthBackground
import com.liam.cmp_src.core.ui.component.AppDropdownField
import com.liam.cmp_src.core.ui.component.AppOutlinedTextField
import com.liam.cmp_src.core.ui.component.ContentColumn
import com.liam.cmp_src.core.ui.component.GlassCard
import com.liam.cmp_src.core.ui.component.PrimaryActionButton
import com.liam.cmp_src.core.ui.component.SkeletonBlock
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.feature.customers.component.CustomerIconButton
import com.liam.cmp_src.feature.customers.component.asLabel
import com.liam.cmp_src.feature.customers.component.asMessage
import com.liam.cmp_src.feature.customers.domain.model.CustomerField
import com.liam.cmp_src.feature.customers.domain.model.CustomerFieldError
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The customer editor wired to its [CustomerEditorViewModel]: a full-screen destination the app
 * shell pushes over the home screen.
 *
 * [customerId] `null` creates a customer for [ownerId]. [onDone] fires once the editor has
 * nothing left to do — saved, deleted, or abandoned — and the shell pops it.
 */
@Composable
fun CustomerEditorRoute(
    ownerId: String,
    customerId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerEditorViewModel = koinViewModel { parametersOf(ownerId, customerId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CustomerEditorEvent.Done -> onDone()
            }
        }
    }

    CustomerEditorScreen(
        state = state,
        fieldState = { viewModel.field(it).state },
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

/**
 * The form: contact details, address and notes in their own glass cards, then save and — for an
 * existing customer — delete.
 *
 * [fieldState] hands over the text of each input, which the ViewModel owns. Keeping it a function
 * rather than twelve parameters is what lets the field list below drive the whole form.
 */
@Composable
fun CustomerEditorScreen(
    state: CustomerEditorUiState,
    fieldState: (CustomerField) -> TextFieldState,
    onAction: (CustomerEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedAuthBackground(Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding(),
        ) {
            EditorTopBar(
                title = stringResource(
                    if ((state as? CustomerEditorUiState.Editing)?.isNew == true) {
                        Res.string.customer_editor_title_new
                    } else {
                        Res.string.customer_editor_title_edit
                    },
                ),
                onBack = { onAction(CustomerEditorAction.Close) },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spaceLg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ContentColumn(verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg)) {
                    when (state) {
                        CustomerEditorUiState.Loading -> EditorSkeleton()
                        CustomerEditorUiState.NotFound -> NotFound(onBack = { onAction(CustomerEditorAction.Close) })
                        is CustomerEditorUiState.Editing -> EditorForm(state, fieldState, onAction)
                    }
                }
            }
        }
    }

    if ((state as? CustomerEditorUiState.Editing)?.isConfirmingDelete == true) {
        DeleteDialog(
            name = fieldState(CustomerField.FIRST_NAME).text.toString(),
            onAction = onAction,
        )
    }
}

@Composable
private fun EditorTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.topBarHeight)
            .padding(horizontal = Dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        CustomerIconButton(
            icon = Res.drawable.ic_arrow_back,
            contentDescription = stringResource(Res.string.cd_back),
            onClick = onBack,
            tint = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun EditorForm(
    state: CustomerEditorUiState.Editing,
    fieldState: (CustomerField) -> TextFieldState,
    onAction: (CustomerEditorAction) -> Unit,
) {
    val formField: @Composable (FieldSpec) -> Unit = { spec ->
        SpecField(spec = spec, state = fieldState(spec.field), error = state.errors[spec.field])
    }

    if (state.isRejected) {
        Text(
            text = stringResource(Res.string.customer_editor_rejected),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }

    FormSection(title = Res.string.customer_editor_section_contact) {
        CONTACT_FIELDS.forEach { formField(it) }
        AppDropdownField(
            options = CustomerStatus.entries,
            selected = state.status,
            onSelect = { onAction(CustomerEditorAction.StatusSelected(it)) },
            optionLabel = { it.asLabel() },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.customer_status),
            placeholder = stringResource(Res.string.customer_status_placeholder),
            errorMessage = state.errors[CustomerField.STATUS]?.asMessage(),
        )
    }

    FormSection(title = Res.string.customer_editor_section_address) {
        ADDRESS_FIELDS.forEach { formField(it) }
    }

    FormSection(title = Res.string.customer_editor_section_notes) {
        formField(NOTES_FIELD)
    }

    PrimaryActionButton(
        label = stringResource(Res.string.customer_editor_save),
        state = if (state.isSaving) ActionButtonState.Loading else ActionButtonState.Idle,
        onClick = { onAction(CustomerEditorAction.Save) },
    )

    if (!state.isNew) {
        OutlinedButton(
            onClick = { onAction(CustomerEditorAction.Delete) },
            enabled = !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.buttonHeight)
                .handCursor(),
            shape = RoundedCornerShape(Dimens.radiusMd),
            border = BorderStroke(Dimens.hairline, auroraColors.glassBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = auroraColors.glassFill,
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(Res.string.customer_editor_delete))
        }
    }
}

@Composable
private fun FormSection(title: StringResource, content: @Composable () -> Unit) {
    GlassCard(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content()
    }
}

@Composable
private fun SpecField(spec: FieldSpec, state: TextFieldState, error: CustomerFieldError?) {
    AppOutlinedTextField(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(spec.label),
        placeholder = spec.placeholder?.let { stringResource(it) },
        errorMessage = error?.asMessage(),
        lineLimits = if (spec.multiLine) TextFieldLineLimits.MultiLine() else TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformation.maxLength(spec.maxLength),
        keyboardOptions = spec.keyboard,
    )
}

/** How many field-shaped placeholders stand in for the form while the customer is read. */
private const val SKELETON_FIELDS = 4

/** The form's shape while the customer is read — a local lookup, so rarely seen for long. */
@Composable
private fun EditorSkeleton() {
    GlassCard(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
        repeat(SKELETON_FIELDS) {
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(Dimens.fieldHeight),
                shape = RoundedCornerShape(Dimens.radiusMd),
            )
        }
    }
}

@Composable
private fun NotFound(onBack: () -> Unit) {
    GlassCard {
        Text(
            text = stringResource(Res.string.customer_editor_not_found),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(Dimens.spaceLg))
        PrimaryActionButton(
            label = stringResource(Res.string.customer_editor_back_to_list),
            state = ActionButtonState.Idle,
            onClick = onBack,
        )
    }
}

@Composable
private fun DeleteDialog(name: String, onAction: (CustomerEditorAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(CustomerEditorAction.DismissDelete) },
        title = { Text(stringResource(Res.string.customers_delete_title, name)) },
        text = { Text(stringResource(Res.string.customers_delete_body)) },
        confirmButton = {
            TextButton(
                onClick = { onAction(CustomerEditorAction.ConfirmDelete) },
                modifier = Modifier.handCursor(),
            ) {
                Text(
                    text = stringResource(Res.string.customers_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(CustomerEditorAction.DismissDelete) },
                modifier = Modifier.handCursor(),
            ) {
                Text(stringResource(Res.string.customers_delete_cancel))
            }
        },
    )
}

/** How one text input on the form looks and what it accepts. */
private data class FieldSpec(
    val field: CustomerField,
    val label: StringResource,
    val maxLength: Int,
    val keyboard: KeyboardOptions = TEXT_KEYBOARD,
    val placeholder: StringResource? = null,
    val multiLine: Boolean = false,
)

private val TEXT_KEYBOARD = KeyboardOptions(
    capitalization = KeyboardCapitalization.Words,
    imeAction = ImeAction.Next,
)

/**
 * Room for the spaces and dashes people type into a phone number, which validation strips before
 * checking it against the server's limit.
 */
private const val PHONE_INPUT_MAX_LENGTH = FieldLimits.MAX_PHONE_LENGTH * 2

/** An ISO 3166-1 alpha-2 code is exactly two letters. */
private const val COUNTRY_CODE_LENGTH = 2

private val CONTACT_FIELDS = listOf(
    FieldSpec(CustomerField.FIRST_NAME, Res.string.customer_first_name, FieldLimits.MAX_CUSTOMER_NAME_LENGTH),
    FieldSpec(CustomerField.LAST_NAME, Res.string.customer_last_name, FieldLimits.MAX_CUSTOMER_NAME_LENGTH),
    FieldSpec(CustomerField.COMPANY_NAME, Res.string.customer_company, FieldLimits.MAX_COMPANY_NAME_LENGTH),
    FieldSpec(
        field = CustomerField.EMAIL,
        label = Res.string.customer_email,
        maxLength = FieldLimits.MAX_EMAIL_LENGTH,
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    ),
    FieldSpec(
        field = CustomerField.PHONE,
        label = Res.string.customer_phone,
        maxLength = PHONE_INPUT_MAX_LENGTH,
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        placeholder = Res.string.customer_phone_placeholder,
    ),
)

private val ADDRESS_FIELDS = listOf(
    FieldSpec(CustomerField.ADDRESS_LINE1, Res.string.customer_address_line1, FieldLimits.MAX_ADDRESS_LINE_LENGTH),
    FieldSpec(CustomerField.ADDRESS_LINE2, Res.string.customer_address_line2, FieldLimits.MAX_ADDRESS_LINE_LENGTH),
    FieldSpec(CustomerField.CITY, Res.string.customer_city, FieldLimits.MAX_CITY_LENGTH),
    FieldSpec(CustomerField.REGION, Res.string.customer_region, FieldLimits.MAX_REGION_LENGTH),
    FieldSpec(
        field = CustomerField.POSTAL_CODE,
        label = Res.string.customer_postal_code,
        maxLength = FieldLimits.MAX_POSTAL_CODE_LENGTH,
        keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
    ),
    FieldSpec(
        field = CustomerField.COUNTRY_CODE,
        label = Res.string.customer_country_code,
        maxLength = COUNTRY_CODE_LENGTH,
        keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
        placeholder = Res.string.customer_country_code_placeholder,
    ),
)

private val NOTES_FIELD = FieldSpec(
    field = CustomerField.NOTES,
    label = Res.string.customer_notes,
    maxLength = FieldLimits.MAX_CUSTOMER_NOTES_LENGTH,
    keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    multiLine = true,
)

@Preview
@Composable
private fun CustomerEditorScreenPreview() {
    val fields = CustomerField.entries.associateWith { rememberTextFieldState() }
    AppTheme {
        CustomerEditorScreen(
            state = CustomerEditorUiState.Editing(
                isNew = false,
                status = CustomerStatus.ACTIVE,
                errors = mapOf(CustomerField.EMAIL to CustomerFieldError.InvalidEmail),
            ),
            fieldState = { requireNotNull(fields[it]) },
            onAction = {},
        )
    }
}
