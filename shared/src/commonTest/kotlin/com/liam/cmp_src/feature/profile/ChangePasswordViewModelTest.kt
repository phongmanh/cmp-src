package com.liam.cmp_src.feature.profile

import com.example.api.common.FieldLimits
import com.liam.cmp_src.feature.auth.FakeAuthRepository
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.PasswordError
import com.liam.cmp_src.feature.auth.domain.usecase.ChangePasswordUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateChangePasswordUseCase
import com.liam.cmp_src.feature.profile.changepassword.ChangePasswordAction
import com.liam.cmp_src.feature.profile.changepassword.ChangePasswordEvent
import com.liam.cmp_src.feature.profile.changepassword.ChangePasswordStatus
import com.liam.cmp_src.feature.profile.changepassword.ChangePasswordViewModel
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val CURRENT_PASSWORD = "a-long-enough-password"
private const val NEW_PASSWORD = "an-entirely-different-one"

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {

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

    private fun viewModelWith(repository: FakeAuthRepository) = ChangePasswordViewModel(
        changePassword = ChangePasswordUseCase(repository),
        validateChangePassword = ValidateChangePasswordUseCase(),
    )

    /** Subscribes to the one-shot event flow before the action under test fires. */
    private fun TestScope.collectEvents(
        viewModel: ChangePasswordViewModel,
    ): MutableList<ChangePasswordEvent> {
        val events = mutableListOf<ChangePasswordEvent>()
        CoroutineScope(UnconfinedTestDispatcher(testScheduler)).launch {
            viewModel.events.toList(events)
        }
        return events
    }

    private fun ChangePasswordViewModel.enterValidForm() {
        onAction(ChangePasswordAction.CurrentPasswordChanged(CURRENT_PASSWORD))
        onAction(ChangePasswordAction.NewPasswordChanged(NEW_PASSWORD))
        onAction(ChangePasswordAction.ConfirmPasswordChanged(NEW_PASSWORD))
    }

    @Test
    fun startsIdleWithEmptyFields() {
        val state = viewModelWith(FakeAuthRepository()).uiState.value

        assertEquals("", state.currentPassword)
        assertEquals("", state.newPassword)
        assertEquals("", state.confirmPassword)
        assertEquals(ChangePasswordStatus.Idle, state.status)
    }

    @Test
    fun invalidInputIsReportedPerFieldAndSendsNothing() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)

        viewModel.onAction(ChangePasswordAction.NewPasswordChanged("short"))
        viewModel.onAction(ChangePasswordAction.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PasswordError.Blank, state.fieldErrors.currentPassword)
        assertEquals(
            PasswordError.TooShort(FieldLimits.MIN_PASSWORD_LENGTH),
            state.fieldErrors.newPassword,
        )
        assertEquals(PasswordError.Mismatch, state.fieldErrors.confirmPassword)
        assertEquals(ChangePasswordStatus.Idle, state.status)
        assertEquals(0, repository.changePasswordCallCount)
    }

    @Test
    fun editingAFieldClearsThatFieldsError() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeAuthRepository())
        viewModel.onAction(ChangePasswordAction.Submit)
        advanceUntilIdle()
        assertEquals(PasswordError.Blank, viewModel.uiState.value.fieldErrors.currentPassword)

        viewModel.onAction(ChangePasswordAction.CurrentPasswordChanged(CURRENT_PASSWORD))

        val errors = viewModel.uiState.value.fieldErrors
        assertNull(errors.currentPassword)
        // The other fields keep theirs — the user has not touched them yet.
        assertEquals(PasswordError.Blank, errors.newPassword)
    }

    @Test
    fun aValidSubmitSendsBothPasswordsAndReportsSuccess() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)
        viewModel.enterValidForm()

        viewModel.onAction(ChangePasswordAction.Submit)
        assertEquals(ChangePasswordStatus.Submitting, viewModel.uiState.value.status)

        advanceUntilIdle()
        assertEquals(1, repository.changePasswordCallCount)
        assertEquals(CURRENT_PASSWORD, repository.lastCurrentPassword)
        assertEquals(NEW_PASSWORD, repository.lastNewPassword)
        assertEquals<List<ChangePasswordEvent>>(listOf(ChangePasswordEvent.Changed), events)
    }

    /**
     * The server keeps the device that made the change signed in, on a fresh pair the data layer
     * has already stored. Ending the session here would throw that away for nothing.
     */
    @Test
    fun aSuccessfulChangeDoesNotEndTheSession() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        viewModel.enterValidForm()

        viewModel.onAction(ChangePasswordAction.Submit)
        advanceUntilIdle()

        assertEquals(0, repository.signOutCallCount)
    }

    @Test
    fun aWrongCurrentPasswordSurfacesAsAFailureAndKeepsTheForm() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(
            changePasswordResult = AuthResult.Failure(AuthError.WrongPassword),
        )
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)
        viewModel.enterValidForm()

        viewModel.onAction(ChangePasswordAction.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthError.WrongPassword, state.error)
        // Nothing is cleared: the user retypes one field, not the whole form.
        assertEquals(CURRENT_PASSWORD, state.currentPassword)
        assertEquals(NEW_PASSWORD, state.newPassword)
        assertTrue(events.isEmpty())
    }

    @Test
    fun editingAfterAFailureClearsTheBanner() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(
            changePasswordResult = AuthResult.Failure(AuthError.WrongPassword),
        )
        val viewModel = viewModelWith(repository)
        viewModel.enterValidForm()
        viewModel.onAction(ChangePasswordAction.Submit)
        advanceUntilIdle()

        viewModel.onAction(ChangePasswordAction.CurrentPasswordChanged("something-else-entirely"))

        assertEquals(ChangePasswordStatus.Idle, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun submittingTwiceOnlyCallsOnce() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        viewModel.enterValidForm()

        viewModel.onAction(ChangePasswordAction.Submit)
        viewModel.onAction(ChangePasswordAction.Submit)
        advanceUntilIdle()

        assertEquals(1, repository.changePasswordCallCount)
    }

    @Test
    fun cancellingReportsItWithoutCallingAnything() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)

        viewModel.onAction(ChangePasswordAction.Cancel)
        advanceUntilIdle()

        assertEquals<List<ChangePasswordEvent>>(listOf(ChangePasswordEvent.Dismissed), events)
        assertEquals(0, repository.changePasswordCallCount)
    }

    /**
     * This ViewModel outlives the dialog, so reopening must not hand back the last attempt's
     * input — least of all a plaintext current password.
     */
    @Test
    fun reopeningBlanksWhateverThePreviousAttemptLeftBehind() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(
            changePasswordResult = AuthResult.Failure(AuthError.WrongPassword),
        )
        val viewModel = viewModelWith(repository)
        viewModel.enterValidForm()
        viewModel.onAction(ChangePasswordAction.Submit)
        advanceUntilIdle()

        viewModel.onAction(ChangePasswordAction.Opened)

        val state = viewModel.uiState.value
        assertEquals("", state.currentPassword)
        assertEquals("", state.newPassword)
        assertEquals("", state.confirmPassword)
        assertEquals(ChangePasswordStatus.Idle, state.status)
    }

    @Test
    fun togglingVisibilityFlipsOnlyThatFieldsFlag() {
        val viewModel = viewModelWith(FakeAuthRepository())

        viewModel.onAction(ChangePasswordAction.ToggleCurrentVisibility)

        assertTrue(viewModel.uiState.value.isCurrentVisible)
        assertEquals(false, viewModel.uiState.value.isNewVisible)
    }

    @Test
    fun aProviderOnlyAccountIsToldSoRatherThanBeingGivenAGenericFailure() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(
            changePasswordResult = AuthResult.Failure(AuthError.PasswordNotSet),
        )
        val viewModel = viewModelWith(repository)
        viewModel.enterValidForm()

        viewModel.onAction(ChangePasswordAction.Submit)
        advanceUntilIdle()

        val status = assertIs<ChangePasswordStatus.Failed>(viewModel.uiState.value.status)
        assertEquals(AuthError.PasswordNotSet, status.error)
    }
}
