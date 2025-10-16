package com.hereliesaz.geministrator.ui.ide

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.util.ViewModelFactory
import com.jules.apiclient.JulesApiClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class IdeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: IdeViewModel
    private val mockJulesApiClient: JulesApiClient = mock()
    private val mockSettingsRepository: SettingsRepository = mock()
    private val mockApplication: Application = mock()
    private val savedStateHandle = SavedStateHandle(mapOf("sessionId" to "test_session", "filePath" to "test_file.kt"))

    @Before
    fun setUp() {
        val factory = ViewModelFactory {
            IdeViewModel(
                application = mockApplication,
                savedStateHandle = savedStateHandle,
                settingsRepository = mockSettingsRepository,
                julesApiClient = mockJulesApiClient,
                geminiApiClient = null
            )
        }
        viewModel = factory.create(IdeViewModel::class.java)
    }

    @Test
    fun `onRunClick should call sendMessage with correct parameters`() = runTest {
        // When
        viewModel.onRunClick()

        // Then
        verify(mockJulesApiClient).sendMessage("test_session", "Run the code in the file `test_file.kt`")
    }

    @Test
    fun `onCommitConfirm should call sendMessage with correct commit message`() = runTest {
        // Given
        val commitMessage = "Test commit message"
        viewModel.onCommitMessageChanged(commitMessage)

        // When
        viewModel.onCommitConfirm()

        // Then
        verify(mockJulesApiClient).sendMessage("test_session", "Commit changes with message: '$commitMessage'")
    }
}
