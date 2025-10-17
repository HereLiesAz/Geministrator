package com.hereliesaz.geministrator.ui.jules

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.A2ACommunicator
import com.jules.apiclient.GeminiApiClient
import com.jules.apiclient.JulesApiClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@ExperimentalCoroutinesApi
class SessionViewModelTest {

    private class TestViewModelFactory(
        private val savedStateHandle: SavedStateHandle,
        private val settingsRepository: SettingsRepository,
        private val julesApiClient: JulesApiClient,
        private val geminiApiClient: GeminiApiClient,
        private val a2aCommunicator: A2ACommunicator
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SessionViewModel(savedStateHandle, settingsRepository, julesApiClient, geminiApiClient, a2aCommunicator) as T
        }
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var julesApiClient: JulesApiClient
    private lateinit var geminiApiClient: GeminiApiClient
    private lateinit var a2aCommunicator: A2ACommunicator
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        julesApiClient = mockk(relaxed = true)
        geminiApiClient = mockk(relaxed = true)
        a2aCommunicator = mockk(relaxed = true)
        val savedStateHandle = SavedStateHandle().apply {
            set("sessionId", "test-session")
            set("roles", "planner,researcher")
        }
        coEvery { settingsRepository.apiKey } returns flowOf("test-api-key")
        coEvery { settingsRepository.githubRepository } returns flowOf("test-repo")
        coEvery { settingsRepository.gcpLocation } returns flowOf("test-location")
        coEvery { settingsRepository.geminiModelName } returns flowOf("test-model")
        viewModel = TestViewModelFactory(savedStateHandle, settingsRepository, julesApiClient, geminiApiClient, a2aCommunicator).create(SessionViewModel::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendMessage should call julesApiClient`() = runTest {
        // Given
        val prompt = "test-prompt"
        coEvery { julesApiClient.sendMessage("test-session", prompt) } returns Unit

        // When
        viewModel.sendMessage(prompt)

        // Then
        coVerify(exactly = 1) { julesApiClient.sendMessage("test-session", prompt) }
    }
}
