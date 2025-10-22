package com.hereliesaz.geministrator.ui.settings

import android.app.Application
import android.content.Context
import com.hereliesaz.geministrator.MainDispatcherRule
import com.hereliesaz.geministrator.data.SettingsRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SettingsViewModel
    private val mockApplication: Application = mockk()
    private val mockContext: Context = mockk()
    private val mockSettingsRepository: SettingsRepository = mockk(relaxed = true)
    private lateinit var tempDir: File
    private lateinit var promptsFile: File

    @Before
    fun setUp() {
        tempDir = createTempDir()
        promptsFile = File(tempDir, "prompts.json")
        promptsFile.createNewFile()
        promptsFile.writeText("[]")

        every { mockApplication.applicationContext } returns mockContext
        every { mockContext.filesDir } returns tempDir
        every { mockApplication.filesDir } returns tempDir

        // Mock the flows from the repository
        every { mockSettingsRepository.apiKey } returns MutableStateFlow("test_api_key")
        every { mockSettingsRepository.geminiApiKey } returns MutableStateFlow("test_gemini_api_key")
        every { mockSettingsRepository.theme } returns MutableStateFlow("Dark")
        every { mockSettingsRepository.gcpProjectId } returns MutableStateFlow("test_project_id")
        every { mockSettingsRepository.gcpLocation } returns MutableStateFlow("us-west1")
        every { mockSettingsRepository.geminiModelName } returns MutableStateFlow("gemini-pro")

        viewModel = SettingsViewModel(mockApplication, mockSettingsRepository)
    }

    @Test
    fun `init loads settings and updates uiState`() = runTest {
        // The init block triggers the load, so we just need to observe the state.
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        val uiState = viewModel.uiState.value

        assertEquals("test_api_key", uiState.apiKey)
        assertEquals("test_gemini_api_key", uiState.geminiApiKey)
        assertEquals("Dark", uiState.theme)
        assertEquals("test_project_id", uiState.gcpProjectId)
        assertEquals("us-west1", uiState.gcpLocation)
        assertEquals("gemini-pro", uiState.geminiModelName)
    }

    @Test
    fun `onApiKeyChange updates uiState`() {
        val newKey = "new_api_key"
        viewModel.onApiKeyChange(newKey)
        assertEquals(newKey, viewModel.uiState.value.apiKey)
    }

    @Test
    fun `onGeminiApiKeyChange updates uiState`() {
        val newKey = "new_gemini_api_key"
        viewModel.onGeminiApiKeyChange(newKey)
        assertEquals(newKey, viewModel.uiState.value.geminiApiKey)
    }

    @Test
    fun `onThemeChange updates uiState`() {
        val newTheme = "Light"
        viewModel.onThemeChange(newTheme)
        assertEquals(newTheme, viewModel.uiState.value.theme)
    }

    @Test
    fun `onGcpProjectIdChange updates uiState`() {
        val newId = "new_project_id"
        viewModel.onGcpProjectIdChange(newId)
        assertEquals(newId, viewModel.uiState.value.gcpProjectId)
    }

    @Test
    fun `saveSettings calls repository methods`() = runTest {
        // Change a value to ensure we are saving the current state
        val newApiKey = "updated_api_key"
        viewModel.onApiKeyChange(newApiKey)

        viewModel.saveSettings()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockSettingsRepository.saveApiKey(newApiKey) }
        coVerify { mockSettingsRepository.saveGeminiApiKey(any()) }
        coVerify { mockSettingsRepository.saveTheme(any()) }
        coVerify { mockSettingsRepository.saveGcpProjectId(any()) }
        coVerify { mockSettingsRepository.saveGcpLocation(any()) }
        coVerify { mockSettingsRepository.saveGeminiModelName(any()) }
    }
}
