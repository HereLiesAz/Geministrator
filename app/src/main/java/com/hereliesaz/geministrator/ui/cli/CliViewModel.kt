package com.hereliesaz.geministrator.ui.cli

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CliUiState(
    val output: String = "",
    val isLoading: Boolean = false
)

class CliViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CliUiState())
    val uiState = _uiState.asStateFlow()

    fun sendCommand(apiKey: String, command: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                runGeminiCommand(apiKey, command)
            }
            _uiState.update { it.copy(output = it.output + "\n> " + command + "\n" + result, isLoading = false) }
        }
    }

    private fun runGeminiCommand(apiKey: String, command: String): String {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("main")
            module.callAttr("run_gemini", apiKey, command).toString()
        } catch (e: Exception) {
            "An error occurred: ${e.message}"
        }
    }
}
