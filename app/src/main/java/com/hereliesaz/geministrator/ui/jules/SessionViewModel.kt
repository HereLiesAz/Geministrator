package com.hereliesaz.geministrator.ui.jules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.JulesRepository
import com.jules.apiclient.Activity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionUiState(
    val sessionId: String = "",
    val activities: List<Activity> = emptyList(),
    val subTasks: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val julesRepository: JulesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    internal val sessionId: String = savedStateHandle.get<String>("sessionId")!!
    private val roles: Set<String> = savedStateHandle.get<String>("roles")?.split(",")?.toSet() ?: emptySet()

    private val _uiState = MutableStateFlow(SessionUiState(sessionId = sessionId))
    val uiState = _uiState.asStateFlow()

    init {
        loadActivities()
    }

    fun loadActivities() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val activities = julesRepository.listActivities(sessionId)
                _uiState.update {
                    it.copy(
                        activities = activities,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun sendMessage(prompt: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                julesRepository.nextTurn(sessionId, prompt)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            } finally {
                loadActivities() // Refresh after sending
            }
        }
    }

    fun decomposeTask(task: String) {
        val prompt = "Decompose the following high-level task into a list of smaller, manageable sub-tasks:\n\n$task"
        sendMessage(prompt)
    }
}