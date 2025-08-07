package com.hereliesaz.geministrator.android.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.local.HistoryDatabase
import com.hereliesaz.geministrator.android.data.local.HistoryRepository
import com.hereliesaz.geministrator.android.data.local.SessionWithLogs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryDetailViewModel(
    application: Application,
    sessionId: Long,
) : AndroidViewModel(application) {

    private val repository: HistoryRepository

    val sessionWithLogs: StateFlow<SessionWithLogs?>

    init {
        val historyDao = HistoryDatabase.getDatabase(application).historyDao()
        repository = HistoryRepository(historyDao)
        sessionWithLogs = repository.getSessionWithLogs(sessionId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    companion object {
        fun provideFactory(
            application: Application,
            sessionId: Long,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryDetailViewModel(application, sessionId) as T
            }
        }
    }
}