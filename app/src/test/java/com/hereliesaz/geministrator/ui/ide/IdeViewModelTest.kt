package com.hereliesaz.geministrator.ui.ide

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.data.SettingsRepository
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
class IdeViewModelTest {

    private class TestViewModelFactory(
        private val savedStateHandle: SavedStateHandle,
        private val settingsRepository: SettingsRepository,
        private val geminiApiClient: GeminiApiClient,
        private val julesApiClient: JulesApiClient
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdeViewModel(savedStateHandle, settingsRepository, geminiApiClient, julesApiClient) as T
        }
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var geminiApiClient: GeminiApiClient
    private lateinit var julesApiClient: JulesApiClient
    private lateinit var viewModel: IdeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        geminiApiClient = mockk(relaxed = true)
        julesApiClient = mockk(relaxed = true)
        val savedStateHandle = SavedStateHandle().apply {
            set("sessionId", "test-session")
            set("filePath", "test-path")
        }
        coEvery { settingsRepository.apiKey } returns flowOf("test-api-key")
        coEvery { settingsRepository.gcpProjectId } returns flowOf("test-project-id")
        coEvery { settingsRepository.gcpLocation } returns flowOf("test-location")
        coEvery { settingsRepository.geminiModelName } returns flowOf("test-model")
        viewModel = TestViewModelFactory(savedStateHandle, settingsRepository, geminiApiClient, julesApiClient).create(IdeViewModel::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onAutocompleteClick should call geminiApiClient`() = runTest {
        // Given
        val editor = mockk<io.github.rosemoe.sora.widget.CodeEditor>(relaxed = true)
        viewModel.onEditorAttached(editor)
        coEvery { geminiApiClient.generateContent(any()) } returns mockk(relaxed = true)

        // When
        viewModel.onAutocompleteClick()

        // Then
        coVerify(exactly = 1) { geminiApiClient.generateContent(any()) }
    }
}
