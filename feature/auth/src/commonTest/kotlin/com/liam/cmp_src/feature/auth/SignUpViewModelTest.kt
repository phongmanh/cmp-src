package com.liam.cmp_src.feature.auth

import com.liam.cmp_src.core.testing.FakeAuthRepository
import com.liam.cmp_src.core.ui.SUCCESS_HOLD_MILLIS
import com.liam.cmp_src.feature.auth.domain.usecase.SignUpUseCase
import com.liam.cmp_src.feature.auth.domain.usecase.ValidateCredentialsUseCase
import com.liam.cmp_src.feature.auth.presentation.signup.SignUpAction
import com.liam.cmp_src.feature.auth.presentation.signup.SignUpEvent
import com.liam.cmp_src.feature.auth.presentation.signup.SignUpUiStatus
import com.liam.cmp_src.feature.auth.presentation.signup.SignUpViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val VALID_EMAIL = "new@cmpsrc.dev"
private const val VALID_PASSWORD = "password123"

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

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
    private fun TestScope.collectEvents(viewModel: SignUpViewModel): MutableList<SignUpEvent> {
        val events = mutableListOf<SignUpEvent>()
        CoroutineScope(UnconfinedTestDispatcher(testScheduler)).launch {
            viewModel.effect.toList(events)
        }
        return events
    }

    /**
     * The success state is held on screen for [SUCCESS_HOLD_MILLIS] before the app moves on, the
     * same as every other form. The duration unit is the point: a hold measured in microseconds
     * would navigate away before the success state was ever drawn.
     */
    @Test
    fun `a successful sign-up holds the success state before navigating home`() = runTest {
        val viewModel = SignUpViewModel(
            signUpUseCase = SignUpUseCase(FakeAuthRepository()),
            validateCredentials = ValidateCredentialsUseCase(),
        )
        val events = collectEvents(viewModel)
        viewModel.onAction(SignUpAction.EmailChanged(VALID_EMAIL))
        viewModel.onAction(SignUpAction.PasswordChanged(VALID_PASSWORD))

        viewModel.onAction(SignUpAction.Submit(VALID_EMAIL, VALID_PASSWORD))
        runCurrent()

        assertEquals(SignUpUiStatus.Succeeded, viewModel.state.value.status)

        advanceTimeBy(SUCCESS_HOLD_MILLIS - 1)
        runCurrent()
        assertTrue(events.isEmpty(), "navigated before the success state was held: $events")

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf<SignUpEvent>(SignUpEvent.NavigateToHome(FakeAuthRepository.TEST_USER)), events)
    }
}
