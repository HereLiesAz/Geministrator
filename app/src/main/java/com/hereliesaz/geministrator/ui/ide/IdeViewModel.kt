package com.hereliesaz.geministrator.ui.ide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.cloud.vertexai.generativeai.ResponseHandler
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.GeminiApiClient
import com.jules.apiclient.JulesApiClient
import com.jules.apiclient.ToolOutputActivity
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IdeUiState(
    val editor: CodeEditor? = null,
    val currentFile: String? = null,
    val fileContent: String = "",
    val isLoading: Boolean = false
)

class IdeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private var geminiApiClient: GeminiApiClient?,
    private var julesApiClient: JulesApiClient?
) : ViewModel() {
    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState = _uiState.asStateFlow()
    private val sessionId: String = savedStateHandle.get<String>("sessionId")!!
    private val filePath: String = savedStateHandle.get<String>("filePath")!!

    init {
        viewModelScope.launch {
            if (geminiApiClient == null) {
                val gcpProjectId = settingsRepository.gcpProjectId.first()
                val gcpLocation = settingsRepository.gcpLocation.first()
                val geminiModelName = settingsRepository.geminiModelName.first()

                if (!gcpProjectId.isNullOrBlank() && !gcpLocation.isNullOrBlank() && !geminiModelName.isNullOrBlank()) {
                    geminiApiClient = GeminiApiClient(gcpProjectId, gcpLocation, geminiModelName)
                }
            }

            if (julesApiClient == null) {
                val apiKey = settingsRepository.apiKey.first()
                if (!apiKey.isNullOrBlank()) {
                    julesApiClient = JulesApiClient(apiKey)
                    loadActivities()
                }
            }
        }
    }

    private fun loadActivities() {
        val client = julesApiClient ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val activities = client.getActivities(sessionId).activities
                val fileWriteActivities = activities.filterIsInstance<ToolOutputActivity>()
                    .filter { it.toolName == "file.write" && it.output.startsWith(filePath) }
                if (fileWriteActivities.isNotEmpty()) {
                    val latestContent = fileWriteActivities.last().output.substringAfter(filePath).trim()
                    _uiState.update { it.copy(fileContent = latestContent) }
                }
            } catch (e: Exception) {
                // TODO: Handle error
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onEditorAttached(editor: CodeEditor) {
        _uiState.update { it.copy(editor = editor) }
    }

    fun onFileOpened(filePath: String, content: String) {
        _uiState.update { it.copy(currentFile = filePath, fileContent = content) }
    }

    fun onContentChanged(content: String) {
        _uiState.update { it.copy(fileContent = content) }
    }

    fun onAutocompleteClick() {
        val client = geminiApiClient ?: return
        val editor = _uiState.value.editor ?: return
        val content = editor.text.toString()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = client.generateContent("Complete the following code:\n\n$content")
                val suggestion = ResponseHandler.getText(response) ?: ""
                editor.insertText(suggestion, suggestion.length)
            } catch (e: Exception) {
                // TODO: Handle error
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onGenerateDocsClick() {
        val client = geminiApiClient ?: return
        val editor = _uiState.value.editor ?: return
        val content = editor.text.toString()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = client.generateContent("Generate documentation for the following code:\n\n$content")
                val suggestion = ResponseHandler.getText(response) ?: ""
                editor.insertText(suggestion, 0)
            } catch (e: Exception) {
                // TODO: Handle error
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
