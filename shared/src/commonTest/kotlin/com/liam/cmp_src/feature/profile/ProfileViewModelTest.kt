package com.liam.cmp_src.feature.profile

import com.liam.cmp_src.feature.auth.FakeAuthRepository
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.SignOutUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

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

    private fun viewModelWith(repository: FakeAuthRepository) = ProfileViewModel(
        getCurrentUser = GetCurrentUserUseCase(repository),
        signOut = SignOutUseCase(repository),
    )

    /** Subscribes to the one-shot event flow before the action under test fires. */
    private fun TestScope.collectEvents(viewModel: ProfileViewModel): MutableList<ProfileEvent> {
        val events = mutableListOf<ProfileEvent>()
        CoroutineScope(UnconfinedTestDispatcher(testScheduler)).launch {
            viewModel.events.toList(events)
        }
        return events
    }

    @Test
    fun startsLoadingThenShowsTheUser() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)

        // The skeleton has to be on screen for the whole of the call, not only after it.
        assertEquals(ProfileUiState.Loading, viewModel.state.value)

        advanceUntilIdle()
        val state = assertIs<ProfileUiState.Success>(viewModel.state.value)
        assertEquals(FakeAuthRepository.TEST_USER, state.user)
        assertEquals(1, repository.currentUserCallCount)
    }

    @Test
    fun reportsTheFailureItWasGiven() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(
            currentUserResult = AuthResult.Failure(AuthError.Network),
        )
        val viewModel = viewModelWith(repository)

        advanceUntilIdle()
        val state = assertIs<ProfileUiState.Error>(viewModel.state.value)
        assertEquals(AuthError.Network, state.error)
    }

    @Test
    fun retryingGoesBackToLoadingAndCallsAgain() = runTest(testDispatcher) {
        val repository = FakeAuthRepository(
            currentUserResult = AuthResult.Failure(AuthError.Network),
        )
        val viewModel = viewModelWith(repository)
        advanceUntilIdle()

        repository.currentUserResult = AuthResult.Success(FakeAuthRepository.TEST_USER)
        viewModel.onAction(ProfileAction.Retry)
        // The skeleton comes back rather than the stale error staying up.
        assertEquals(ProfileUiState.Loading, viewModel.state.value)

        advanceUntilIdle()
        assertIs<ProfileUiState.Success>(viewModel.state.value)
        assertEquals(2, repository.currentUserCallCount)
    }

    @Test
    fun signingOutEndsTheSessionBeforeReportingIt() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.Logout)
        // Still in flight: navigating away now would leave the old tokens live.
        assertTrue(events.isEmpty())

        advanceUntilIdle()
        assertEquals(1, repository.signOutCallCount)
        assertEquals<List<ProfileEvent>>(listOf(ProfileEvent.GoToLogin), events)
    }

    @Test
    fun signingOutTwiceOnlyEndsTheSessionOnce() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.Logout)
        viewModel.onAction(ProfileAction.Logout)
        advanceUntilIdle()

        assertEquals(1, repository.signOutCallCount)
        assertEquals<List<ProfileEvent>>(listOf(ProfileEvent.GoToLogin), events)
    }

    @Test
    fun unbuiltActionsSaySoInsteadOfFailing() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.EditProfile)
        advanceUntilIdle()
        viewModel.onAction(ProfileAction.ChangePassword)
        advanceUntilIdle()

        assertEquals<List<ProfileEvent>>(
            listOf(ProfileEvent.ShowNotImplemented, ProfileEvent.ShowNotImplemented),
            events,
        )
        // The profile itself is untouched by an action that does nothing.
        assertIs<ProfileUiState.Success>(viewModel.state.value)
    }
}
