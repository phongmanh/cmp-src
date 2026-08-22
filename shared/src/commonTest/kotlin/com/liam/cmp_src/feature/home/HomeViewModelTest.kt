package com.liam.cmp_src.feature.home

import com.liam.cmp_src.feature.auth.FakeAuthRepository
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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

    /** Subscribes to the one-shot event flow before the action under test fires. */
    private fun TestScope.collectEvents(viewModel: HomeViewModel): MutableList<HomeEvent> {
        val events = mutableListOf<HomeEvent>()
        CoroutineScope(UnconfinedTestDispatcher(testScheduler)).launch {
            viewModel.events.toList(events)
        }
        return events
    }

    @Test
    fun signingOutEndsTheSessionBeforeReportingIt() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = HomeViewModel(SignOutUseCase(repository))
        val events = collectEvents(viewModel)

        viewModel.onSignOut()
        // Still in flight: navigating away now would leave the old tokens live.
        assertTrue(events.isEmpty())

        advanceUntilIdle()
        assertEquals(1, repository.signOutCallCount)
        assertEquals<List<HomeEvent>>(listOf(HomeEvent.SignedOut), events)
    }

    @Test
    fun tappingSignOutTwiceEndsTheSessionOnce() = runTest(testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = HomeViewModel(SignOutUseCase(repository))
        val events = collectEvents(viewModel)

        viewModel.onSignOut()
        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(1, repository.signOutCallCount)
        assertEquals<List<HomeEvent>>(listOf(HomeEvent.SignedOut), events)
    }
}
