package com.liam.cmp_src.feature.auth.presentation.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cmpsrc.core.ui.generated.resources.Res as UiRes
import cmpsrc.core.ui.generated.resources.cd_email_icon
import cmpsrc.core.ui.generated.resources.cd_password_icon
import cmpsrc.core.ui.generated.resources.ic_email
import cmpsrc.core.ui.generated.resources.ic_lock
import cmpsrc.core.ui.generated.resources.login_email_label
import cmpsrc.core.ui.generated.resources.login_email_placeholder
import cmpsrc.feature.auth.generated.resources.*
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.component.ActionButtonState
import com.liam.cmp_src.core.ui.component.AuthTextField
import com.liam.cmp_src.core.ui.component.ErrorBanner
import com.liam.cmp_src.core.ui.component.GlassCard
import com.liam.cmp_src.core.ui.component.PrimaryActionButton
import com.liam.cmp_src.core.ui.component.SectionHeader
import com.liam.cmp_src.core.ui.message.asMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import cmpsrc.feature.auth.generated.resources.signup_subtitle
import cmpsrc.feature.auth.generated.resources.signup_title
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.CredentialErrors
import com.liam.cmp_src.feature.auth.domain.model.EmailError
import com.liam.cmp_src.feature.auth.presentation.component.AuthScreenScaffold
import com.liam.cmp_src.feature.auth.presentation.component.BrandMark
import com.liam.cmp_src.feature.auth.presentation.login.asMessage

@Composable
fun SignUpRoute(
    goHome: (UserResponse) -> Unit,
    backToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SignUpEvent.NavigateBackToLogin -> backToLogin()
                is SignUpEvent.NavigateToHome -> goHome(effect.user)
            }
        }
    }

    SignUpScreen(state, viewModel::onAction, modifier)

}

@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onAction: (SignUpAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val passwordVisible = remember { mutableStateOf(false) }

    val onSubmit = {
        focusManager.clearFocus()
        onAction(SignUpAction.Submit(email = state.email, password = state.password))
    }

    AuthScreenScaffold(modifier = modifier) {
        BrandMark()

        Spacer(Modifier.size(Dimens.spaceXl))

        SectionHeader(
            title = stringResource(Res.string.signup_title),
            subtitle = stringResource(Res.string.signup_subtitle),
        )

        Spacer(Modifier.size(Dimens.spaceXl))

        GlassCard(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            AuthTextField(
                value = state.email,
                onValueChange = {
                    onAction(SignUpAction.EmailChanged(it))
                },
                label = stringResource(UiRes.string.login_email_label),
                placeholder = stringResource(UiRes.string.login_email_placeholder),
                leadingIcon = UiRes.drawable.ic_email,
                leadingIconDescription = stringResource(UiRes.string.cd_email_icon),
                enabled = !state.isBusy,
                errorMessage = state.fieldErrors.email?.asMessage(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )

            Spacer(Modifier.size(Dimens.spaceMd))

            AuthTextField(
                value = state.password,
                onValueChange = {
                    onAction(SignUpAction.PasswordChanged(it))
                },
                label = stringResource(Res.string.login_password_label),
                placeholder = stringResource(Res.string.login_password_placeholder),
                leadingIcon = UiRes.drawable.ic_lock,
                leadingIconDescription = stringResource(UiRes.string.cd_password_icon),
                enabled = !state.isBusy,
                errorMessage = state.fieldErrors.password?.asMessage(),
                isPassword = true,
                isPasswordVisible = passwordVisible.value,
                onTogglePasswordVisibility = { passwordVisible.value = !passwordVisible.value },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(
                    onClick = { onAction(SignUpAction.NavigateBack) },
                    modifier = Modifier.handCursor(state.status != SignUpUiStatus.Submitted),
                    enabled = !state.isBusy,
                ) {
                    Text(stringResource(Res.string.signup_has_account))
                }
            }

            ErrorBanner(error = state.error)

            Spacer(Modifier.size(Dimens.spaceMd))

            PrimaryActionButton(
                label = stringResource(Res.string.signup_submit),
                state = when (state.status) {
                    is SignUpUiStatus.Succeeded -> ActionButtonState.Success
                    is SignUpUiStatus.Submitted -> ActionButtonState.Loading
                    else -> ActionButtonState.Idle
                },
                onClick = { onSubmit() },
                enabled = !state.isBusy,
            )
        }
    }
}

@Composable
@Preview
fun SignUpScreenPreview() {
    AppTheme {
        SignUpScreen(state = SignUpUiState(), onAction = {})
    }
}

@Preview
@Composable
fun SignUpScreenPreviewError() {
    AppTheme {
        SignUpScreen(
            state = SignUpUiState(
                status = SignUpUiStatus.Failed(AuthError.InvalidCredentials),
                fieldErrors = CredentialErrors(email = EmailError.Blank)
            ), onAction = {})
    }
}
