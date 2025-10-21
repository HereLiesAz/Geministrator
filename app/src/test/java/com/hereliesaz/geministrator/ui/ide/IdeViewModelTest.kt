package com.hereliesaz.geministrator.ui.ide

import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.MainDispatcherRule
import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.util.ViewModelFactory
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
    private val mockJulesApiClient: JulesApiClient = mockk(relaxed = true)
    private val mockGeminiApiClient: GeminiApiClient = mockk(relaxed = true)
    private val mockSettingsRepository: SettingsRepository = mockk(relaxed = true)
    private val savedStateHandle = SavedStateHandle(mapOf("sessionId" to "test_session", "filePath" to "test_file.kt"))

    @Before
    fun setUp() {
        viewModel = ViewModelFactory {
            IdeViewModel(
                savedStateHandle = savedStateHandle,
                settingsRepository = mockSettingsRepository,
                julesApiClient = mockJulesApiClient,
                geminiApiClient = mockGeminiApiClient
            )
        }.create(IdeViewModel::class.java)
    }

    @Test
    fun `onRunClick should call sendMessage with correct parameters`() = runTest {
        viewModel.onRunClick()
        coVerify { mockJulesApiClient.sendMessage("test_session", "Run the code in the file `test_file.kt`") }
    }

    @Test
    fun `onCommitConfirm should call sendMessage with correct commit message`() = runTest {
        val commitMessage = "Test commit message"
        viewModel.onCommitMessageChanged(commitMessage)
        viewModel.onCommitConfirm()
        coVerify { mockJulesApiClient.sendMessage("test_session", "Commit changes with message: '$commitMessage'") }
    }

    @Test
    fun `onAutocompleteClick should call geminiApiClient`() = runTest {
        val mockEditor = mockk<CodeEditor>(relaxed = true)
        viewModel.onEditorAttached(mockEditor)
        coEvery { mockEditor.text.toString() } returns "test text"
        coEvery { mockGeminiApiClient.generateContent("Complete the following code:\n\ntest text") } returns "test suggestion"

        viewModel.onAutocompleteClick()

        coVerify { mockGeminiApiClient.generateContent("Complete the following code:\n\ntest text") }
        coVerify { mockEditor.insertText("test suggestion", "test suggestion".length) }
    }
}
