package com.liam.cmp_src.feature.profile

import com.example.api.common.FieldLimits
import com.liam.cmp_src.feature.auth.FakeAuthRepository
import com.liam.cmp_src.feature.auth.domain.model.AuthError
import com.liam.cmp_src.feature.auth.domain.model.AuthResult
import com.liam.cmp_src.feature.auth.domain.model.DisplayNameError
import com.liam.cmp_src.feature.profile.domain.usecase.RemoveAvatarUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.UpdateDisplayNameUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.UploadAvatarUseCase
import com.liam.cmp_src.feature.profile.domain.usecase.ValidateDisplayNameUseCase
import com.liam.cmp_src.feature.profile.profileinfo.ProfileInfoAction
import com.liam.cmp_src.feature.profile.profileinfo.ProfileInfoEvent
import com.liam.cmp_src.feature.profile.profileinfo.ProfileInfoStatus
import com.liam.cmp_src.feature.profile.profileinfo.ProfileInfoViewModel
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileInfoViewModelTest {

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

    private fun viewModelWith(repository: FakeProfileRepository) = ProfileInfoViewModel(
        updateDisplayName = UpdateDisplayNameUseCase(repository),
        uploadAvatar = UploadAvatarUseCase(repository),
        removeAvatar = RemoveAvatarUseCase(repository),
        validateDisplayName = ValidateDisplayNameUseCase(),
    )

    /** Subscribes to the one-shot event flow before the action under test fires. */
    private fun TestScope.collectEvents(
        viewModel: ProfileInfoViewModel,
    ): MutableList<ProfileInfoEvent> {
        val events = mutableListOf<ProfileInfoEvent>()
        CoroutineScope(UnconfinedTestDispatcher(testScheduler)).launch {
            viewModel.events.toList(events)
        }
        return events
    }

    @Test
    fun `opening seeds the form from the account it was given`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeProfileRepository())

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))

        val state = viewModel.uiState.value
        assertEquals(FakeAuthRepository.TEST_USER, state.user)
        assertEquals(FakeAuthRepository.TEST_USER.displayName, state.displayName)
        assertFalse(state.isNameDirty, "a freshly opened form has nothing to save")
    }

    /** The ViewModel outlives the dialog, so reopening must not inherit the last edit. */
    @Test
    fun `reopening starts from the account again rather than the last edit`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeProfileRepository())

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        viewModel.onAction(ProfileInfoAction.NameChanged("half-typed"))
        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))

        assertEquals(FakeAuthRepository.TEST_USER.displayName, viewModel.uiState.value.displayName)
    }

    @Test
    fun `saving a name reports the account the server answered with`() = runTest(testDispatcher) {
        val renamed = FakeAuthRepository.TEST_USER.copy(displayName = "Ada Lovelace")
        val repository = FakeProfileRepository(updateResult = AuthResult.Success(renamed))
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        viewModel.onAction(ProfileInfoAction.NameChanged("Ada Lovelace"))
        viewModel.onAction(ProfileInfoAction.SaveName)
        advanceUntilIdle()

        assertEquals(1, repository.updateCallCount)
        assertEquals<List<ProfileInfoEvent>>(listOf(ProfileInfoEvent.Updated(renamed)), events)
        assertEquals(renamed, viewModel.uiState.value.user)
    }

    /**
     * The name and the picture are saved by separate calls, so the dialog has to stay open after
     * the first one or the second could never be made.
     */
    @Test
    fun `a save does not dismiss the dialog`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeProfileRepository())
        val events = collectEvents(viewModel)

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        viewModel.onAction(ProfileInfoAction.NameChanged("Ada"))
        viewModel.onAction(ProfileInfoAction.SaveName)
        advanceUntilIdle()

        assertTrue(events.none { it is ProfileInfoEvent.Dismissed })
    }

    @Test
    fun `the form settles back to idle so a second change can be made`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeProfileRepository())

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        viewModel.onAction(ProfileInfoAction.NameChanged("Ada"))
        viewModel.onAction(ProfileInfoAction.SaveName)
        advanceUntilIdle()

        assertEquals(ProfileInfoStatus.Idle, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `a name the server would refuse never reaches it`() = runTest(testDispatcher) {
        val repository = FakeProfileRepository()
        val viewModel = viewModelWith(repository)

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        viewModel.onAction(
            ProfileInfoAction.NameChanged("a".repeat(FieldLimits.MAX_DISPLAY_NAME_LENGTH + 1)),
        )
        viewModel.onAction(ProfileInfoAction.SaveName)
        advanceUntilIdle()

        assertEquals(0, repository.updateCallCount)
        assertEquals(
            DisplayNameError.TooLong(FieldLimits.MAX_DISPLAY_NAME_LENGTH),
            viewModel.uiState.value.nameError,
        )
    }

    @Test
    fun `editing the name clears a previous failure`() = runTest(testDispatcher) {
        val repository = FakeProfileRepository(
            updateResult = AuthResult.Failure(AuthError.Network),
        )
        val viewModel = viewModelWith(repository)

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        viewModel.onAction(ProfileInfoAction.NameChanged("Ada"))
        viewModel.onAction(ProfileInfoAction.SaveName)
        advanceUntilIdle()
        assertIs<ProfileInfoStatus.Failed>(viewModel.uiState.value.status)

        viewModel.onAction(ProfileInfoAction.NameChanged("Ada L"))

        assertEquals(ProfileInfoStatus.Idle, viewModel.uiState.value.status)
    }

    @Test
    fun `a picked photo is uploaded and the new account adopted`() = runTest(testDispatcher) {
        val withPhoto = FakeProfileRepository.USER_WITH_UPLOADED_PHOTO
        val repository = FakeProfileRepository(uploadResult = AuthResult.Success(withPhoto))
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        viewModel.onAction(ProfileInfoAction.PhotoPicked(JPEG, "face.jpg"))
        advanceUntilIdle()

        assertEquals(1, repository.uploadCallCount)
        assertEquals<List<ProfileInfoEvent>>(listOf(ProfileInfoEvent.Updated(withPhoto)), events)
        assertTrue(viewModel.uiState.value.hasPhoto)
    }

    /**
     * An upload answers with the whole account, including the name the server still holds. Taking
     * that would throw away a rename the user had typed but not yet saved.
     */
    @Test
    fun `uploading a photo does not discard a name the user is still typing`() =
        runTest(testDispatcher) {
            val repository = FakeProfileRepository(
                uploadResult = AuthResult.Success(FakeProfileRepository.USER_WITH_UPLOADED_PHOTO),
            )
            val viewModel = viewModelWith(repository)

            viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
            viewModel.onAction(ProfileInfoAction.NameChanged("Ada Lovelace"))
            viewModel.onAction(ProfileInfoAction.PhotoPicked(JPEG, "face.jpg"))
            advanceUntilIdle()

            assertEquals("Ada Lovelace", viewModel.uiState.value.displayName)
            assertTrue(viewModel.uiState.value.isNameDirty, "the rename is still unsaved")
        }

    @Test
    fun `removing a photo leaves the name alone`() = runTest(testDispatcher) {
        val repository = FakeProfileRepository()
        val viewModel = viewModelWith(repository)

        viewModel.onAction(
            ProfileInfoAction.Opened(FakeProfileRepository.USER_WITH_UPLOADED_PHOTO),
        )
        viewModel.onAction(ProfileInfoAction.RemovePhoto)
        advanceUntilIdle()

        assertEquals(1, repository.removeCallCount)
        assertEquals(0, repository.updateCallCount)
        assertFalse(viewModel.uiState.value.hasPhoto)
    }

    /** The button stays on screen while the call runs, so a second tap must do nothing. */
    @Test
    fun `a second save while one is in flight is ignored`() = runTest(testDispatcher) {
        val repository = FakeProfileRepository()
        val viewModel = viewModelWith(repository)

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        viewModel.onAction(ProfileInfoAction.NameChanged("Ada"))
        viewModel.onAction(ProfileInfoAction.SaveName)
        viewModel.onAction(ProfileInfoAction.SaveName)
        advanceUntilIdle()

        assertEquals(1, repository.updateCallCount)
    }

    @Test
    fun `closing reports the dismissal and writes nothing`() = runTest(testDispatcher) {
        val repository = FakeProfileRepository()
        val viewModel = viewModelWith(repository)
        val events = collectEvents(viewModel)

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        viewModel.onAction(ProfileInfoAction.Close)
        advanceUntilIdle()

        assertEquals<List<ProfileInfoEvent>>(listOf(ProfileInfoEvent.Dismissed), events)
        assertEquals(0, repository.updateCallCount)
    }

    /**
     * The warning under the name field exists only for a picture this backend stores, which the
     * write is going to retire. A provider's picture survives, so warning about it would be false.
     */
    @Test
    fun `the warning is raised only for a picture the write would destroy`() = runTest(testDispatcher) {
        val viewModel = viewModelWith(FakeProfileRepository())

        viewModel.onAction(
            ProfileInfoAction.Opened(FakeProfileRepository.USER_WITH_UPLOADED_PHOTO),
        )
        assertTrue(viewModel.uiState.value.willClearPhoto)

        viewModel.onAction(
            ProfileInfoAction.Opened(FakeProfileRepository.USER_WITH_PROVIDER_PHOTO),
        )
        assertFalse(viewModel.uiState.value.willClearPhoto)

        viewModel.onAction(ProfileInfoAction.Opened(FakeAuthRepository.TEST_USER))
        assertFalse(viewModel.uiState.value.willClearPhoto)
    }
}
