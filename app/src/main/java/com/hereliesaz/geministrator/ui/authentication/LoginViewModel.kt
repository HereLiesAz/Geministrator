package com.hereliesaz.geministrator.ui.authentication

import android.app.Application
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.Identity
import com.hereliesaz.geministrator.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Intent
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = application.applicationContext,
            oneTapClient = Identity.getSignInClient(application.applicationContext)
        )
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onSignInClick() {
        viewModelScope.launch {
            val signInIntentSender = googleAuthUiClient.signIn()
            _uiState.update {
                it.copy(
                    signInIntentSender = signInIntentSender
                )
            }
        }
    }

    fun onSignInResult(intent: Intent) {
        viewModelScope.launch {
            val result = googleAuthUiClient.signInWithIntent(intent)
            _uiState.update {
                it.copy(
                    isSignInSuccessful = result.data != null,
                    signInError = result.errorMessage
                )
            }
            result.data?.let {
                settingsRepository.saveUserId(it.userId)
                it.username?.let { username -> settingsRepository.saveUsername(username) }
                it.profilePictureUrl?.let { url -> settingsRepository.saveProfilePictureUrl(url) }
            }
        }
    }

    fun resetState() {
        _uiState.update { LoginUiState() }
    }
}

data class LoginUiState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null,
    val signInIntentSender: IntentSender? = null
)
