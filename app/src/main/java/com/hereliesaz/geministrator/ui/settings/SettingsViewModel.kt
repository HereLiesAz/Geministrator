package com.hereliesaz.geministrator.ui.settings

import android.app.Application
import android.app.Activity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.hereliesaz.geministrator.BuildConfig
import com.hereliesaz.geministrator.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val promptsFile = File(application.filesDir, "prompts.json")
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        loadSettings()
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = firebaseAuth.currentUser
        _uiState.update { it.copy(username = user?.displayName, profilePictureUrl = user?.photoUrl?.toString()) }
    }

    private fun loadSettings() {
        combine(
            settingsRepository.apiKey,
            settingsRepository.geminiApiKey,
            settingsRepository.theme,
            settingsRepository.gcpLocation,
            settingsRepository.geminiModelName
        ) { values ->
            val apiKey = values[0] as String?
            val geminiApiKey = values[1] as String?
            val theme = values[2] as String?
            val gcpProjectId = values[3] as String?
            val geminiModelName = values[5] as String?
            val gcpLocation = values[3] as String?
            val username = values[5] as String?
            val profilePictureUrl = values[6] as String?
            // Create a temporary state object, don't overwrite prompts state
            SettingsUiState(
                apiKey = apiKey ?: "",
                geminiApiKey = geminiApiKey ?: "",
                theme = theme ?: "System",
                gcpLocation = gcpLocation ?: "us-central1",
                geminiModelName = geminiModelName ?: "gemini-1.0-pro"
            )
        }.onEach { newSettingsState ->
            _uiState.update {
                it.copy(
                    apiKey = newSettingsState.apiKey,
                    geminiApiKey = newSettingsState.geminiApiKey,
                    theme = newSettingsState.theme,
                    gcpLocation = newSettingsState.gcpLocation,
                    geminiModelName = newSettingsState.geminiModelName
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

    fun onGeminiApiKeyChange(newKey: String) {
        _uiState.update { it.copy(geminiApiKey = newKey) }
    }

    fun onThemeChange(newTheme: String) {
        _uiState.update { it.copy(theme = newTheme) }
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
            settingsRepository.saveGeminiApiKey(_uiState.value.geminiApiKey)
            settingsRepository.saveTheme(_uiState.value.theme)
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

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            googleAuthUiClient.signOut()
            settingsRepository.saveUserId("")
            settingsRepository.saveUsername("")
            settingsRepository.saveProfilePictureUrl("")
            _uiState.update { it.copy(isLoading = false) }
            _events.emit(UiEvent.NavigateToLogin)
        }
    }

    fun onSignInWithGitHubClick() {
        viewModelScope.launch {
            val serviceConfig = net.openid.appauth.AuthorizationServiceConfiguration(
                android.net.Uri.parse("https://github.com/login/oauth/authorize"),
                android.net.Uri.parse("https://github.com/login/oauth/access_token")
            )

            val authRequestBuilder = net.openid.appauth.AuthorizationRequest.Builder(
                serviceConfig,
                com.hereliesaz.geministrator.BuildConfig.GITHUB_CLIENT_ID,
                net.openid.appauth.ResponseTypeValues.CODE,
                android.net.Uri.parse("com.hereliesaz.geministrator://oauth2redirect")
            )

            val authRequest = authRequestBuilder
                .setScope("repo,user")
                .build()

            val authIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, authRequest.toUri())
            _events.emit(UiEvent.LaunchUrl(authIntent))
        }
    }

    fun onSignOutFromGitHubClick() {
        viewModelScope.launch {
            settingsRepository.saveGithubUsername("")
            settingsRepository.saveGithubAccessToken("")
            _uiState.update { it.copy(githubUsername = null) }
        }
    }

    fun signInWithGitHub(activity: Activity) {
        val provider = OAuthProvider.newBuilder("github.com")
            .addCustomParameter("login", uiState.value.username ?: "")
            .setScopes(listOf("repo", "user"))
            .build()

        firebaseAuth
            .startActivityForSignInWithProvider(activity, provider)
            .addOnSuccessListener {
                checkCurrentUser()
            }
            .addOnFailureListener {
                // Handle failure.
            }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(activity, request)
                handleGoogleSignIn(result.credential)
            } catch (e: Exception) {
                // Handle failure.
            }
        }
    }

    fun handleGoogleSignIn(credential: androidx.credentials.Credential) {
        val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkCurrentUser()
                } else {
                    // Handle failure.
                }
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
        _uiState.update { it.copy(username = null, profilePictureUrl = null) }
    }
}

data class SettingsUiState(
    val apiKey: String = "",
    val geminiApiKey: String = "",
    val theme: String = "System",
    val gcpLocation: String = "us-central1",
    val geminiModelName: String = "gemini-1.0-pro",
    val promptsJsonString: String = "",
    val promptsDirty: Boolean = false,
    val username: String? = null,
    val profilePictureUrl: String? = null,
    val githubUsername: String? = null,
    val isLoading: Boolean = false
)
