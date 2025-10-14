package com.hereliesaz.geministrator.ui.settings

import android.content.Intent
import android.content.IntentSender

sealed class UiEvent {
    data object ShowSaveConfirmation : UiEvent() {
        const val message: String = "Settings saved!"
    }
    data class LaunchUrl(val intent: Intent) : UiEvent()
    data class LaunchIntentSender(val intentSender: IntentSender) : UiEvent()
    data object NavigateToLogin : UiEvent()
}
