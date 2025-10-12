package com.hereliesaz.geministrator.ui.settings

import android.content.Intent
import android.content.IntentSender

sealed class UiEvent {
    data class ShowSaveConfirmation(val message: String) : UiEvent()
    data class LaunchUrl(val intent: Intent) : UiEvent()
    data object NavigateToLogin : UiEvent()
}
