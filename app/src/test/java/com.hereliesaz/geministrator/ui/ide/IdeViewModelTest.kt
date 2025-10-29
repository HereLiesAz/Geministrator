package com.hereliesaz.geministrator.ui.ide

import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.MainDispatcherRule
//import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.JulesApiClient
import io.github.rosemoe.sora.widget.CodeEditor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class IdeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: IdeViewModel
    private val mockJulesApiClient: JulesApiClient = mockk()
//    private val mockGeminiApiClient: GeminiApiClient = mockk()
    private val mockSettingsRepository: SettingsRepository = mockk()
    private val savedStateHandle = SavedStateHandle(mapOf("sessionId" to "test_session", "filePath" to "test_file.kt"))

    @Before
    fun setUp() {
        coEvery { mockSettingsRepository.apiKey } returns kotlinx.coroutines.flow.flowOf("test_api_key")
//        coEvery { mockSettingsRepository.geminiApiKey } returns kotlinx.coroutines.flow.flowOf("test_gemini_api_key")
        coEvery { mockJulesApiClient.getActivities(any()) } returns com.jules.apiclient.ActivityList(emptyList())

        viewModel = IdeViewModel(
            savedStateHandle = savedStateHandle,
            settingsRepository = mockSettingsRepository,
            julesApiClient = mockJulesApiClient,
//            geminiApiClient = mockGeminiApiClient
        )
    }

    @Test
    fun `onRunClick should call sendMessage with correct parameters`() = runTest {
        coEvery { mockJulesApiClient.sendMessage(any(), any()) } returns Unit
        viewModel.onRunClick()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        coVerify { mockJulesApiClient.sendMessage("test_session", "Run the code in the file `test_file.kt`") }
    }

    @Test
    fun `onCommitConfirm should call sendMessage with correct commit message`() = runTest {
        val commitMessage = "Test commit message"
        coEvery { mockJulesApiClient.sendMessage(any(), any()) } returns Unit
        viewModel.onCommitMessageChanged(commitMessage)
        viewModel.onCommitConfirm()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        coVerify { mockJulesApiClient.sendMessage("test_session", "Commit changes with message: '$commitMessage'") }
    }

//    @Test
//    fun `onAutocompleteClick should call geminiApiClient`() = runTest {
//        val mockEditor = mockk<CodeEditor>(relaxed = true)
//        viewModel.onEditorAttached(mockEditor)
//        coEvery { mockEditor.text.toString() } returns "test text"
//        coEvery { mockGeminiApiClient.generateContent("Complete the following code:\n\ntest text") } returns "test suggestion"
//
//        viewModel.onAutocompleteClick()
//        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
//
//        coVerify { mockGeminiApiClient.generateContent("Complete the following code:\n\ntest text") }
//        coVerify { mockEditor.insertText("test suggestion", "test suggestion".length) }
//    }
}
