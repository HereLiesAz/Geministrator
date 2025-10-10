package com.hereliesaz.geministrator.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.BuildConfig
import com.hereliesaz.geministrator.data.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val promptsFile = File(application.filesDir, "prompts.json")
    private val authService = AuthorizationService(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        loadSettings()
        loadPrompts()
    }

    private fun loadSettings() {
        val flows = listOf(
            settingsRepository.apiKey,
            settingsRepository.theme,
            settingsRepository.gcpProjectId,
            settingsRepository.gcpLocation,
            settingsRepository.geminiModelName,
            settingsRepository.githubUsername
        )
        combine(flows) { settings ->
            val apiKey = settings[0]
            val theme = settings[1]
            val gcpProjectId = settings[2]
            val gcpLocation = settings[3]
            val geminiModelName = settings[4]
            val githubUsername = settings[5]

            // Create a temporary state object, don't overwrite prompts state
            SettingsUiState(
                apiKey = apiKey ?: "",
                theme = theme ?: "System",
                gcpProjectId = gcpProjectId ?: "",
                gcpLocation = gcpLocation ?: "us-central1",
                geminiModelName = geminiModelName ?: "gemini-1.0-pro",
                githubUsername = githubUsername
            )
        }.onEach { newSettingsState ->
            _uiState.update {
                it.copy(
                    apiKey = newSettingsState.apiKey,
                    theme = newSettingsState.theme,
                    gcpProjectId = newSettingsState.gcpProjectId,
                    gcpLocation = newSettingsState.gcpLocation,
                    geminiModelName = newSettingsState.geminiModelName,
                    githubUsername = newSettingsState.githubUsername
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            val promptsJson = if (promptsFile.exists()) {
                promptsFile.readText()
            } else {
                getApplication<Application>().assets.open("prompts.json").bufferedReader().use { it.readText() }
            }
            _uiState.update { it.copy(promptsJsonString = promptsJson, promptsDirty = false) }
        }
    }

    fun onApiKeyChange(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey) }
    }

    fun onThemeChange(newTheme: String) {
        _uiState.update { it.copy(theme = newTheme) }
    }

    fun onGcpProjectIdChange(newProjectId: String) {
        _uiState.update { it.copy(gcpProjectId = newProjectId) }
    }

    fun onGcpLocationChange(newLocation: String) {
        _uiState.update { it.copy(gcpLocation = newLocation) }
    }

    fun onGeminiModelNameChange(newModelName: String) {
        _uiState.update { it.copy(geminiModelName = newModelName) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            settingsRepository.saveApiKey(_uiState.value.apiKey)
            settingsRepository.saveTheme(_uiState.value.theme)
            settingsRepository.saveGcpProjectId(_uiState.value.gcpProjectId)
            settingsRepository.saveGcpLocation(_uiState.value.gcpLocation)
            settingsRepository.saveGeminiModelName(_uiState.value.geminiModelName)
            _events.emit(UiEvent.ShowSaveConfirmation)
        }
    }

    fun onPromptsChange(newText: String) {
        _uiState.update { it.copy(promptsJsonString = newText, promptsDirty = true) }
    }

    fun resetPrompts() {
        viewModelScope.launch {
            if (promptsFile.exists()) {
                promptsFile.delete()
            }
            loadPrompts()
        }
    }

    fun savePrompts() {
        viewModelScope.launch {
            promptsFile.writeText(_uiState.value.promptsJsonString)
            _uiState.update { it.copy(promptsDirty = false) }
        }
    }

    fun onSignInWithGitHubClick() {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(GITHUB_AUTH_ENDPOINT),
            Uri.parse(GITHUB_TOKEN_ENDPOINT)
        )
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            GITHUB_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(GITHUB_REDIRECT_URI)
        ).setScope(GITHUB_SCOPE).build()

        val authIntent = authService.getAuthorizationRequestIntent(authRequest)

        viewModelScope.launch {
            _events.emit(UiEvent.LaunchUrl(authIntent))
        }
    }

    fun onSignOutFromGitHubClick() {
        viewModelScope.launch {
            settingsRepository.clearGithubUsername()
            settingsRepository.clearGithubAccessToken()
            // TODO: Consider revoking the token via API call to GitHub
        }
    }

    sealed class UiEvent {
        data object ShowSaveConfirmation : UiEvent()
        data class LaunchUrl(val intent: Intent) : UiEvent()
    }

    companion object {
        private const val GITHUB_AUTH_ENDPOINT = "https://github.com/login/oauth/authorize"
        private const val GITHUB_TOKEN_ENDPOINT = "https://github.com/login/oauth/access_token"
        private const val GITHUB_CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID
        private const val GITHUB_REDIRECT_URI = "com.hereliesaz.geministrator://oauth2redirect"
        private const val GITHUB_SCOPE = "repo"
    }
}

data class SettingsUiState(
    val apiKey: String = "",
    val theme: String = "System",
    val gcpProjectId: String = "",
    val gcpLocation: String = "us-central1",
    val geminiModelName: String = "gemini-1.0-pro",
    val promptsJsonString: String = "",
    val promptsDirty: Boolean = false,
    val githubUsername: String? = null,
)
