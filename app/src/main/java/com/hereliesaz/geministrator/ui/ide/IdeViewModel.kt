package com.hereliesaz.geministrator.ui.ide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.GeminiApiClient
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.flow.MutableStateFlow
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

class IdeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState = _uiState.asStateFlow()
    private val settingsRepository = SettingsRepository(application)
    private var geminiApiClient: GeminiApiClient? = null

    init {
        viewModelScope.launch {
            val gcpProjectId = settingsRepository.githubRepository.first()
            val gcpLocation = settingsRepository.gcpLocation.first()
            val geminiModelName = settingsRepository.geminiModelName.first()

            if (!gcpProjectId.isNullOrBlank() && !gcpLocation.isNullOrBlank() && !geminiModelName.isNullOrBlank()) {
                geminiApiClient = GeminiApiClient(gcpProjectId, gcpLocation, geminiModelName)
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
                val suggestion = response.text ?: ""
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
                val suggestion = response.text ?: ""
                editor.insertText(suggestion, 0)
            } catch (e: Exception) {
                // TODO: Handle error
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
