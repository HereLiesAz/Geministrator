package com.hereliesaz.geministrator.ui.jules

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.util.ViewModelFactory
import com.jules.apiclient.A2ACommunicator
import com.jules.apiclient.ActivityList
import com.jules.apiclient.GeminiApiClient
import com.jules.apiclient.JulesApiClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SessionViewModel
    private val mockJulesApiClient: JulesApiClient = mock()
    private val mockGeminiApiClient: GeminiApiClient = mock()
    private val mockA2ACommunicator: A2ACommunicator = mock()
    private val mockSettingsRepository: SettingsRepository = mock()
    private val mockApplication: Application = mock()
    private val savedStateHandle = SavedStateHandle(mapOf("sessionId" to "test_session", "roles" to "planner,researcher"))

    @Before
    fun setUp() {
        val factory = ViewModelFactory {
            SessionViewModel(
                application = mockApplication,
                savedStateHandle = savedStateHandle,
                settingsRepository = mockSettingsRepository,
                julesApiClient = mockJulesApiClient,
                geminiApiClient = mockGeminiApiClient,
                a2aCommunicator = mockA2ACommunicator
            )
        }
        viewModel = factory.create(SessionViewModel::class.java)
    }

    @Test
    fun `loadActivities should call getActivities with correct session id`() = runTest {
        // Given
        whenever(mockJulesApiClient.getActivities("test_session")).thenReturn(ActivityList(emptyList()))

        // When
        viewModel.loadActivities()

        // Then
        verify(mockJulesApiClient).getActivities("test_session")
    }

    @Test
    fun `sendMessage should call sendMessage with correct parameters`() = runTest {
        // Given
        val prompt = "test prompt"
        whenever(mockJulesApiClient.getActivities("test_session")).thenReturn(ActivityList(emptyList()))

        // When
        viewModel.sendMessage(prompt)

        // Then
        verify(mockJulesApiClient).sendMessage("test_session", prompt)
    }

    @Test
    fun `askGemini should call julesToGemini with correct prompt`() = runTest {
        // Given
        val prompt = "test prompt"

        // When
        viewModel.askGemini(prompt)

        // Then
        verify(mockA2ACommunicator).julesToGemini(prompt)
    }

    @Test
    fun `decomposeTask should call generateContent with correct prompt`() = runTest {
        // Given
        val task = "test task"
        val prompt = "Decompose the following high-level task into a list of smaller, manageable sub-tasks:\n\n$task"

        // When
        viewModel.decomposeTask(task)

        // Then
        verify(mockGeminiApiClient).generateContent(prompt)
    }
}
