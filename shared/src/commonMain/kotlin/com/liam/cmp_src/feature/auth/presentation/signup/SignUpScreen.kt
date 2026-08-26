package com.liam.cmp_src.feature.auth.presentation.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cmpsrc.shared.generated.resources.*
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.feature.auth.presentation.component.ActionButtonState
import com.liam.cmp_src.feature.auth.presentation.component.AnimatedAuthBackground
import com.liam.cmp_src.feature.auth.presentation.component.AuthTextField
import com.liam.cmp_src.feature.auth.presentation.component.ErrorBanner
import com.liam.cmp_src.feature.auth.presentation.component.PrimaryActionButton
import com.liam.cmp_src.feature.auth.presentation.login.asMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import cmpsrc.shared.generated.resources.signup_title
import cmpsrc.shared.generated.resources.signup_subtitle
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.CredentialErrors
import com.liam.cmp_src.feature.auth.domain.model.EmailError
import com.liam.cmp_src.feature.auth.presentation.component.BrandMark
import com.liam.cmp_src.feature.auth.presentation.login.LoginUiState

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
fun SignUpScreen(state: SignUpUiState, onAction: (SignUpAction) -> Unit, modifier: Modifier = Modifier) {
    val glass = auroraColors
    val focusManager = LocalFocusManager.current
    val passwordVisible = remember { mutableStateOf(false) }

    val onSubmit = {
        focusManager.clearFocus()
        onAction(SignUpAction.Submit(email = state.email, password = state.password))
    }

    Box(modifier.fillMaxSize()) {
        AnimatedAuthBackground(modifier.matchParentSize())

        BoxWithConstraints(
            modifier = modifier.fillMaxSize().safeContentPadding(),
        ) {
            // Fill the viewport so the card sits centred, but keep scrolling available once
            // a soft keyboard or a short window makes the content taller than the screen.
            val viewportHeight = maxHeight

            Column(
                modifier = modifier.verticalScroll(rememberScrollState()).fillMaxWidth().heightIn(min = viewportHeight)
                    .padding(Dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    modifier = modifier.widthIn(max = Dimens.cardMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    BrandMark()

                    Spacer(modifier.size(Dimens.spaceXl))

                    HeaderText()

                    Spacer(modifier.size(Dimens.spaceXl))

                    Surface(
                        modifier = modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.radiusXl),
                        color = glass.glassFill,
                        border = BorderStroke(Dimens.hairline, glass.glassBorder),
                    ) {
                        Column(modifier.padding(Dimens.spaceXl)) {
                            AuthTextField(
                                value = state.email,
                                onValueChange = {
                                    onAction(SignUpAction.EmailChanged(it))
                                },
                                label = stringResource(Res.string.login_email_label),
                                placeholder = stringResource(Res.string.login_email_placeholder),
                                leadingIcon = Res.drawable.ic_email,
                                leadingIconDescription = stringResource(Res.string.cd_email_icon),
                                enabled = !state.isBusy,
                                errorMessage = state.fieldErrors.email?.asMessage(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next,
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            )

                            Spacer(modifier.size(Dimens.spaceMd))

                            AuthTextField(
                                value = state.password,
                                onValueChange = {
                                    onAction(SignUpAction.PasswordChanged(it))
                                },
                                label = stringResource(Res.string.login_password_label),
                                placeholder = stringResource(Res.string.login_password_placeholder),
                                leadingIcon = Res.drawable.ic_lock,
                                leadingIconDescription = stringResource(Res.string.cd_password_icon),
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

                            Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                TextButton(
                                    onClick = { onAction(SignUpAction.NavigateBack) },
                                    modifier = Modifier.handCursor(state.status != SignUpUiStatus.Submitted),
                                    enabled = !state.isBusy,
                                ) {
                                    Text(stringResource(Res.string.signup_has_account))
                                }
                            }

                            ErrorBanner(error = state.error)

                            Spacer(modifier.size(Dimens.spaceMd))

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
            }
        }
    }
}

@Composable
private fun HeaderText() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.signup_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(Dimens.spaceXs))
        Text(
            text = stringResource(Res.string.signup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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