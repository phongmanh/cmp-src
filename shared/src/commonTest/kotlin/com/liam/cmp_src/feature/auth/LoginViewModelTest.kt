package com.liam.cmp_src.feature.auth

import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.EmailError
import com.liam.cmp_src.feature.auth.domain.model.PasswordError
import com.liam.cmp_src.feature.auth.domain.model.SocialProvider
import com.liam.cmp_src.feature.auth.domain.repository.AuthRepository
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithEmailUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignInWithSocialUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateCredentialsUseCase
import com.liam.cmp_src.feature.auth.presentation.login.LoginAction
import com.liam.cmp_src.feature.auth.presentation.login.LoginEvent
import com.liam.cmp_src.feature.auth.presentation.login.LoginStatus
import com.liam.cmp_src.feature.auth.presentation.login.LoginViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val VALID_EMAIL = "demo@cmpsrc.dev"
private const val VALID_PASSWORD = "password123"

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which has no implementation under test.
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(repository: AuthRepository) = LoginViewModel(
        signInWithEmail = SignInWithEmailUseCase(repository),
        signInWithSocial = SignInWithSocialUseCase(repository),
        validateCredentials = ValidateCredentialsUseCase(),
    )

    /** Subscribes to the one-shot event flow before the action under test fires. */
    private fun TestScope.collectEvents(viewModel: LoginViewModel): MutableList<LoginEvent> {
        val events = mutableListOf<LoginEvent>()
        CoroutineScope(UnconfinedTestDispatcher(testScheduler)).launch {
            viewModel.events.toList(events)
        }
        return events
    }

    private fun LoginViewModel.enterValidCredentials() {
        onAction(LoginAction.EmailChanged(VALID_EMAIL))
        onAction(LoginAction.PasswordChanged(VALID_PASSWORD))
    }

    @Test
    fun startsIdleWithEmptyFields() {
        val viewModel = viewModelWith(FakeAuthRepository())

        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals(LoginStatus.Idle, state.status)
    }

    @Test
    fun typingUpdatesTheFields() {
        val viewModel = viewModelWith(FakeAuthRepository())

        viewModel.enterValidCredentials()

        assertEquals(VALID_EMAIL, viewModel.uiState.value.email)
        assertEquals(VALID_PASSWORD, viewModel.uiState.value.password)
    }

    @Test
    fun successfulEmailSignInMovesFromSubmittingToSucceeded() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeAuthRepository())
        viewModel.enterValidCredentials()

        viewModel.onAction(LoginAction.Submit)
        assertEquals(LoginStatus.Submitting(null), viewModel.uiState.value.status)

        advanceUntilIdle()
        assertEquals(LoginStatus.Succeeded, viewModel.uiState.value.status)
    }

    @Test
    fun successfulEmailSignInEmitsNavigateToHomeExactlyOnce() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeAuthRepository())
        val events = collectEvents(viewModel)
        viewModel.enterValidCredentials()

        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        assertEquals(1, events.count { it is LoginEvent.NavigateToHome })
        val event = events.first { it is LoginEvent.NavigateToHome } as LoginEvent.NavigateToHome
        assertEquals(FakeAuthRepository.TEST_USER, event.user)
    }

    @Test
    fun failedEmailSignInSurfacesTheError() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(
            emailResult = AuthResult.Failure(AuthError.InvalidCredentials),
        )
        val viewModel = viewModelWith(repository)
        viewModel.enterValidCredentials()

        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        val status = assertIs<LoginStatus.Failed>(viewModel.uiState.value.status)
        assertEquals(AuthError.InvalidCredentials, status.error)
    }

    @Test
    fun failedSignInEmitsNoNavigationEvent() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(
            emailResult = AuthResult.Failure(AuthError.InvalidCredentials),
        )
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)
        viewModel.enterValidCredentials()

        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        assertTrue(events.none { it is LoginEvent.NavigateToHome })
    }

    @Test
    fun editingAFieldClearsAPreviousFailure() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(
            emailResult = AuthResult.Failure(AuthError.InvalidCredentials),
        )
        val viewModel = viewModelWith(repository)
        viewModel.enterValidCredentials()
        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        viewModel.onAction(LoginAction.PasswordChanged("password1234"))

        assertEquals(LoginStatus.Idle, viewModel.uiState.value.status)
    }

    @Test
    fun reenteringTheScreenAfterSignInClearsTheForm() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeAuthRepository())
        viewModel.enterValidCredentials()
        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()
        assertEquals(LoginStatus.Succeeded, viewModel.uiState.value.status)

        // What signing out and landing back on login does.
        viewModel.onAction(LoginAction.ScreenEntered)

        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals(LoginStatus.Idle, state.status)
        assertTrue(!state.isBusy)
    }

    @Test
    fun reenteringTheScreenWhileTypingKeepsWhatWasTyped() {
        val viewModel = viewModelWith(FakeAuthRepository())
        viewModel.enterValidCredentials()

        viewModel.onAction(LoginAction.ScreenEntered)

        val state = viewModel.uiState.value
        assertEquals(VALID_EMAIL, state.email)
        assertEquals(VALID_PASSWORD, state.password)
        assertEquals(LoginStatus.Idle, state.status)
    }

    @Test
    fun reenteringTheScreenMidSubmitDoesNotInterruptTheAttempt() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        viewModel.enterValidCredentials()
        viewModel.onAction(LoginAction.Submit)

        viewModel.onAction(LoginAction.ScreenEntered)
        assertEquals(LoginStatus.Submitting(null), viewModel.uiState.value.status)

        advanceUntilIdle()
        assertEquals(LoginStatus.Succeeded, viewModel.uiState.value.status)
        assertEquals(1, repository.emailCallCount)
    }

    @Test
    fun invalidInputBlocksSubmissionAndNeverReachesTheRepository() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)

        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EmailError.Blank, state.fieldErrors.email)
        assertEquals(PasswordError.Blank, state.fieldErrors.password)
        assertEquals(LoginStatus.Idle, state.status)
        assertEquals(0, repository.emailCallCount)
    }

    @Test
    fun shortPasswordBlocksSubmission() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        viewModel.onAction(LoginAction.EmailChanged(VALID_EMAIL))
        viewModel.onAction(LoginAction.PasswordChanged("short"))

        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        assertEquals(0, repository.emailCallCount)
        assertIs<PasswordError.TooShort>(viewModel.uiState.value.fieldErrors.password)
    }

    @Test
    fun editingAFieldClearsThatFieldsValidationError() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeAuthRepository())
        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        viewModel.onAction(LoginAction.EmailChanged(VALID_EMAIL))

        val errors = viewModel.uiState.value.fieldErrors
        assertEquals(null, errors.email)
        assertEquals(PasswordError.Blank, errors.password)
    }

    @Test
    fun submittingTwiceWhileInFlightOnlyCallsTheRepositoryOnce() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        viewModel.enterValidCredentials()

        viewModel.onAction(LoginAction.Submit)
        viewModel.onAction(LoginAction.Submit)
        advanceUntilIdle()

        assertEquals(1, repository.emailCallCount)
    }

    @Test
    fun socialSignInReportsWhichProviderIsSubmitting() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeAuthRepository())

        viewModel.onAction(LoginAction.SocialSignInClicked(SocialProvider.GOOGLE))

        assertEquals(SocialProvider.GOOGLE, viewModel.uiState.value.submittingProvider)
    }

    @Test
    fun socialSignInSucceedsAndNavigatesHome() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)

        viewModel.onAction(LoginAction.SocialSignInClicked(SocialProvider.FACEBOOK))
        advanceUntilIdle()

        assertEquals(SocialProvider.FACEBOOK, repository.lastProvider)
        assertEquals(LoginStatus.Succeeded, viewModel.uiState.value.status)
        assertEquals(1, events.count { it is LoginEvent.NavigateToHome })
    }

    @Test
    fun cancelledSocialSignInLeavesTheFormUsable() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(socialResult = AuthResult.Failure(AuthError.Cancelled))
        val viewModel = viewModelWith(repository)

        viewModel.onAction(LoginAction.SocialSignInClicked(SocialProvider.GOOGLE))
        advanceUntilIdle()

        val status = assertIs<LoginStatus.Failed>(viewModel.uiState.value.status)
        assertEquals(AuthError.Cancelled, status.error)
        assertEquals(null, viewModel.uiState.value.submittingProvider)
    }

    @Test
    fun socialSignInIsIgnoredWhileAnEmailSignInIsInFlight() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        viewModel.enterValidCredentials()

        viewModel.onAction(LoginAction.Submit)
        viewModel.onAction(LoginAction.SocialSignInClicked(SocialProvider.GOOGLE))
        advanceUntilIdle()

        assertEquals(0, repository.socialCallCount)
    }

    @Test
    fun togglingPasswordVisibilityFlipsTheFlag() {
        val viewModel = viewModelWith(FakeAuthRepository())

        viewModel.onAction(LoginAction.TogglePasswordVisibility)

        assertTrue(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun secondaryActionsEmitNotImplemented() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeAuthRepository())
        val events = collectEvents(viewModel)

        viewModel.onAction(LoginAction.ForgotPasswordClicked)
        viewModel.onAction(LoginAction.SignUpClicked)
        advanceUntilIdle()

        assertEquals(2, events.count { it is LoginEvent.ShowNotImplemented })
    }
}
