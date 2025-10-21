package com.hereliesaz.geministrator.ui.jules

import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.MainDispatcherRule
import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.hereliesaz.geministrator.data.A2ACommunicator
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.util.ViewModelFactory
import com.jules.apiclient.ActivityList
import com.jules.apiclient.JulesApiClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SessionViewModel
    private val mockJulesApiClient: JulesApiClient = mockk(relaxed = true)
    private val mockGeminiApiClient: GeminiApiClient = mockk(relaxed = true)
    private val mockA2ACommunicator: A2ACommunicator = mockk(relaxed = true)
    private val mockSettingsRepository: SettingsRepository = mockk(relaxed = true)
    private val savedStateHandle = SavedStateHandle(mapOf("sessionId" to "test_session", "roles" to "planner,researcher"))

    @Before
    fun setUp() {
        viewModel = ViewModelFactory {
            SessionViewModel(
                savedStateHandle = savedStateHandle,
                settingsRepository = mockSettingsRepository,
                julesApiClient = mockJulesApiClient,
                geminiApiClient = mockGeminiApiClient,
                a2aCommunicator = mockA2ACommunicator
            )
        }.create(SessionViewModel::class.java)
    }

    @Test
    fun `loadActivities should call getActivities with correct session id`() = runTest {
        coEvery { mockJulesApiClient.getActivities("test_session") } returns ActivityList(emptyList())
        viewModel.loadActivities()
        coVerify { mockJulesApiClient.getActivities("test_session") }
    }

    @Test
    fun `sendMessage should call sendMessage with correct parameters`() = runTest {
        val prompt = "test prompt"
        coEvery { mockJulesApiClient.getActivities("test_session") } returns ActivityList(emptyList())
        viewModel.sendMessage(prompt)
        coVerify { mockJulesApiClient.sendMessage("test_session", prompt) }
    }

    @Test
    fun `decomposeTask should call generateContent with correct prompt`() = runTest {
        val task = "test task"
        val prompt = "Decompose the following high-level task into a list of smaller, manageable sub-tasks:\n\n$task"
        val mockResponse = "subtask 1\nsubtask 2"
        coEvery { mockGeminiApiClient.generateContent(prompt) } returns mockResponse

        viewModel.decomposeTask(task)

        coVerify { mockGeminiApiClient.generateContent(prompt) }
    }
}
