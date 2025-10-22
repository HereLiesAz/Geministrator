package com.hereliesaz.geministrator.ui.jules

import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.MainDispatcherRule
import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.hereliesaz.geministrator.data.A2ACommunicator
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.ActivityList
import com.jules.apiclient.JulesApiClient
import com.jules.apiclient.UserMessageActivity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SessionViewModel
    private val mockSavedStateHandle: SavedStateHandle = mockk()
    private val mockSettingsRepository: SettingsRepository = mockk()
    private val mockJulesApiClient: JulesApiClient = mockk()
    private val mockGeminiApiClient: GeminiApiClient = mockk()
    private val mockA2ACommunicator: A2ACommunicator = mockk()

    private val sessionId = "test_session"

    @Before
    fun setUp() {
        coEvery { mockSavedStateHandle.get<String>("sessionId") } returns sessionId
        coEvery { mockSavedStateHandle.get<String>("roles") } returns "planner,researcher"
    }

    private fun createViewModel() {
        viewModel = SessionViewModel(
            savedStateHandle = mockSavedStateHandle,
            settingsRepository = mockSettingsRepository,
            julesApiClient = mockJulesApiClient,
            geminiApiClient = mockGeminiApiClient,
            a2aCommunicator = mockA2ACommunicator
        )
    }

    @Test
    fun `loadActivities updates uiState with activities on success`() = runTest {
        val activities = listOf(UserMessageActivity("1", "test"))
        coEvery { mockJulesApiClient.getActivities(sessionId) } returns ActivityList(activities)

        createViewModel()
        viewModel.loadActivities()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(activities, viewModel.uiState.value.activities)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `sendMessage to jules calls sendMessage and reloads activities`() = runTest {
        val prompt = "Hello Jules"
        coEvery { mockJulesApiClient.sendMessage(sessionId, prompt) } returns Unit
        coEvery { mockJulesApiClient.getActivities(sessionId) } returns ActivityList(emptyList())

        createViewModel()
        viewModel.sendMessage(prompt)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockJulesApiClient.sendMessage(sessionId, prompt) }
        coVerify { mockJulesApiClient.getActivities(sessionId) }
    }

    @Test
    fun `sendMessage with gemini command calls A2ACommunicator`() = runTest {
        val prompt = "/gemini Hello Gemini"
        val geminiPrompt = "Hello Gemini"
        coEvery { mockA2ACommunicator.sendMessage(any(), any(), any()) } returns Unit

        createViewModel()
        viewModel.sendMessage(prompt)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockA2ACommunicator.sendMessage(sessionId, geminiPrompt, any()) }
    }

    @Test
    fun `decomposeTask calls geminiApiClient when planner role is present`() = runTest {
        val task = "Write a book"
        val expectedSubtasks = listOf("Chapter 1", "Chapter 2")
        coEvery { mockGeminiApiClient.generateContent(any()) } returns "Chapter 1\nChapter 2"

        createViewModel()
        viewModel.decomposeTask(task)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockGeminiApiClient.generateContent(any()) }
        assertEquals(expectedSubtasks, viewModel.uiState.value.subTasks)
    }

    @Test
    fun `decomposeTask sets error when planner role is not present`() = runTest {
        coEvery { mockSavedStateHandle.get<String>("roles") } returns "researcher"

        createViewModel()
        viewModel.decomposeTask("Write a book")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockGeminiApiClient.generateContent(any()) }
        assertNotNull(viewModel.uiState.value.error)
    }
}
