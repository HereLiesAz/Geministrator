package com.hereliesaz.geministrator.ui.settings

import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.ui.ide.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SettingsViewModel
    private val mockSettingsRepository: SettingsRepository = mock()
    private val savedStateHandle = SavedStateHandle()

    @Before
    fun setUp() {
        whenever(mockSettingsRepository.isDarkMode).thenReturn(flowOf(true))
        whenever(mockSettingsRepository.julesApiKey).thenReturn(flowOf("jules_api_key"))
        whenever(mockSettingsRepository.geminiApiKey).thenReturn(flowOf("gemini_api_key"))
        whenever(mockSettingsRepository.gcpProjectId).thenReturn(flowOf("gcp_project_id"))
        whenever(mockSettingsRepository.gcpLocation).thenReturn(flowOf("gcp_location"))
        whenever(mockSettingsRepository.geminiModelName).thenReturn(flowOf("gemini_model_name"))

        viewModel = SettingsViewModel(mockSettingsRepository, savedStateHandle)
    }

    @Test
    fun `onJulesApiKeyChanged should update julesApiKey in state`() {
        // When
        viewModel.onJulesApiKeyChanged("new_jules_api_key")

        // Then
        assert(viewModel.uiState.value.julesApiKey == "new_jules_api_key")
    }

    @Test
    fun `onGeminiApiKeyChanged should update geminiApiKey in state`() {
        // When
        viewModel.onGeminiApiKeyChanged("new_gemini_api_key")

        // Then
        assert(viewModel.uiState.value.geminiApiKey == "new_gemini_api_key")
    }

    @Test
    fun `onGcpProjectIdChanged should update gcpProjectId in state`() {
        // When
        viewModel.onGcpProjectIdChanged("new_gcp_project_id")

        // Then
        assert(viewModel.uiState.value.gcpProjectId == "new_gcp_project_id")
    }

    @Test
    fun `onGcpLocationChanged should update gcpLocation in state`() {
        // When
        viewModel.onGcpLocationChanged("new_gcp_location")

        // Then
        assert(viewModel.uiState.value.gcpLocation == "new_gcp_location")
    }

    @Test
    fun `onGeminiModelNameChanged should update geminiModelName in state`() {
        // When
        viewModel.onGeminiModelNameChanged("new_gemini_model_name")

        // Then
        assert(viewModel.uiState.value.geminiModelName == "new_gemini_model_name")
    }

    @Test
    fun `onSaveClick should call repository to save settings`() = runTest {
        // Given
        viewModel.onJulesApiKeyChanged("new_jules_api_key")
        viewModel.onGeminiApiKeyChanged("new_gemini_api_key")
        viewModel.onGcpProjectIdChanged("new_gcp_project_id")
        viewModel.onGcpLocationChanged("new_gcp_location")
        viewModel.onGeminiModelNameChanged("new_gemini_model_name")

        // When
        viewModel.onSaveClick()

        // Then
        verify(mockSettingsRepository).setJulesApiKey("new_jules_api_key")
        verify(mockSettingsRepository).setGeminiApiKey("new_gemini_api_key")
        verify(mockSettingsRepository).setGcpProjectId("new_gcp_project_id")
        verify(mockSettingsRepository).setGcpLocation("new_gcp_location")
        verify(mockSettingsRepository).setGeminiModelName("new_gemini_model_name")
    }

    @Test
    fun `onThemeChanged should call repository to set dark mode`() = runTest {
        // When
        viewModel.onThemeChanged(true)

        // Then
        verify(mockSettingsRepository).setDarkMode(true)
    }
}
