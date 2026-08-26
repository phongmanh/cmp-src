package com.liam.cmp_src.feature.auth.presentation.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.liam.cmp_src.core.ui.component.StaggeredEntrance
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import com.liam.cmp_src.feature.auth.presentation.component.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Login screen wired to its [LoginViewModel].
 *
 * Keeping the stateful route and the stateless [LoginScreen] separate is what lets the
 * screen be previewed and tested with hand-built state, with no Koin graph in the way.
 */
@Composable
fun LoginRoute(
    onSignedIn: (UserResponse) -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        // Runs once per entry into the route, which is where a stale success has to be cleared.
        viewModel.onAction(LoginAction.ScreenEntered)
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome -> onSignedIn(event.user)
                // Resolved here rather than captured from a composable `stringResource`: that
                // returns "" until the resource loads, and this effect only ever reads the
                // value it captured on first composition, which is the empty one.
                LoginEvent.ShowNotImplemented ->
                    snackbarHostState.showSnackbar(getString(Res.string.login_not_implemented))

                LoginEvent.SignUpClicked -> onSignUp()
            }
        }
    }

    LoginScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val focusManager = LocalFocusManager.current
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val submit = {
        focusManager.clearFocus()
        onAction(LoginAction.Submit)
    }

    Box(modifier.fillMaxSize()) {
        AnimatedAuthBackground(Modifier.matchParentSize())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
        ) {
            // Fill the viewport so the card sits centred, but keep scrolling available once
            // a soft keyboard or a short window makes the content taller than the screen.
            val viewportHeight = maxHeight
            val isCompact = maxWidth < Dimens.compactWidthThreshold

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .padding(Dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = Dimens.cardMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StaggeredEntrance(visible = isVisible, index = 0) {
                        BrandMark()
                    }

                    Spacer(Modifier.size(Dimens.spaceLg))

                    StaggeredEntrance(visible = isVisible, index = 1) {
                        HeaderText()
                    }

                    Spacer(Modifier.size(Dimens.spaceXl))

                    StaggeredEntrance(visible = isVisible, index = 2) {
                        LoginCard(
                            state = state,
                            onAction = onAction,
                            onSubmit = submit,
                            onMoveFocusDown = { focusManager.moveFocus(FocusDirection.Down) },
                            isCompact = isCompact,
                        )
                    }

                    Spacer(Modifier.size(Dimens.spaceLg))

                    StaggeredEntrance(visible = isVisible, index = 3) {
                        SignUpPrompt(onClick = { onAction(LoginAction.SignUpClicked) })
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Dimens.spaceLg),
        )
    }
}

@Composable
private fun HeaderText() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(Dimens.spaceXs))
        Text(
            text = stringResource(Res.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoginCard(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    onSubmit: () -> Unit,
    onMoveFocusDown: () -> Unit,
    isCompact: Boolean,
) {
    val glass = auroraColors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusXl),
        color = glass.glassFill,
        border = BorderStroke(Dimens.hairline, glass.glassBorder),
    ) {
        Column(Modifier.padding(Dimens.spaceXl)) {
            AuthTextField(
                value = state.email,
                onValueChange = { onAction(LoginAction.EmailChanged(it)) },
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
                keyboardActions = KeyboardActions(onNext = { onMoveFocusDown() }),
            )

            Spacer(Modifier.size(Dimens.spaceMd))

            AuthTextField(
                value = state.password,
                onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
                label = stringResource(Res.string.login_password_label),
                placeholder = stringResource(Res.string.login_password_placeholder),
                leadingIcon = Res.drawable.ic_lock,
                leadingIconDescription = stringResource(Res.string.cd_password_icon),
                enabled = !state.isBusy,
                errorMessage = state.fieldErrors.password?.asMessage(),
                isPassword = true,
                isPasswordVisible = state.isPasswordVisible,
                onTogglePasswordVisibility = { onAction(LoginAction.TogglePasswordVisibility) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(
                    onClick = { onAction(LoginAction.ForgotPasswordClicked) },
                    modifier = Modifier.handCursor(!state.isBusy),
                    enabled = !state.isBusy,
                ) {
                    Text(stringResource(Res.string.login_forgot_password))
                }
            }

            ErrorBanner(error = state.error)

            Spacer(Modifier.size(Dimens.spaceMd))

            PrimaryActionButton(
                label = stringResource(Res.string.login_submit),
                state = when {
                    state.status is LoginStatus.Succeeded -> ActionButtonState.Success
                    state.isSubmittingEmail -> ActionButtonState.Loading
                    else -> ActionButtonState.Idle
                },
                onClick = onSubmit,
                enabled = !state.isBusy,
            )

            Spacer(Modifier.size(Dimens.spaceXl))

            DividerWithLabel(stringResource(Res.string.login_divider))

            Spacer(Modifier.size(Dimens.spaceLg))

            SocialSignInRow(
                submittingProvider = state.submittingProvider,
                enabled = !state.isBusy,
                isCompact = isCompact,
                onProviderClick = { onAction(LoginAction.SocialSignInClicked(it)) },
            )
        }
    }
}

@Composable
private fun SocialSignInRow(
    submittingProvider: SocialProvider?,
    enabled: Boolean,
    isCompact: Boolean,
    onProviderClick: (SocialProvider) -> Unit,
) {
    val providers = SocialProvider.entries

    if (isCompact) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            providers.forEach { provider ->
                SocialSignInButton(
                    provider = provider,
                    onClick = { onProviderClick(provider) },
                    isLoading = submittingProvider == provider,
                    enabled = enabled,
                )
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            providers.forEach { provider ->
                SocialSignInButton(
                    provider = provider,
                    onClick = { onProviderClick(provider) },
                    modifier = Modifier.weight(1f),
                    isLoading = submittingProvider == provider,
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun SignUpPrompt(onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.login_no_account),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClick, modifier = Modifier.handCursor()) {
            Text(stringResource(Res.string.login_sign_up))
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    AppTheme {
        LoginScreen(state = LoginUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun LoginScreenErrorPreview() {
    AppTheme {
        LoginScreen(
            state = LoginUiState(
                email = "demo@cmpsrc.dev",
                password = "wrong-password",
                status = LoginStatus.Failed(AuthError.InvalidCredentials),
            ),
            onAction = {},
        )
    }
}
