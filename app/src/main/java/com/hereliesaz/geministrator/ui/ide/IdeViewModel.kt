package com.hereliesaz.geministrator.ui.ide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.JulesApiClient
import com.jules.apiclient.ToolOutputActivity
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
    val isLoading: Boolean = false,
    val showCommitDialog: Boolean = false,
    val commitMessage: String = "",
    val error: String? = null
)

class IdeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private var julesApiClient: JulesApiClient?,
    private var geminiApiClient: GeminiApiClient?
) : ViewModel() {
    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState = _uiState.asStateFlow()
    private val sessionId: String = savedStateHandle.get<String>("sessionId")!!
    private val filePath: String = savedStateHandle.get<String>("filePath")!!

    init {
        viewModelScope.launch {
            if (julesApiClient == null) {
                val apiKey = settingsRepository.apiKey.first()
                if (!apiKey.isNullOrBlank()) {
                    julesApiClient = JulesApiClient(apiKey)
                    loadActivities()
                }
            }
            if (geminiApiClient == null) {
                val geminiApiKey = settingsRepository.geminiApiKey.first()
                if (!geminiApiKey.isNullOrBlank()) {
                    geminiApiClient = GeminiApiClient(geminiApiKey)
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
                _uiState.update { it.copy(error = e.message) }
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
                val suggestion = response
                editor.insertText(suggestion, suggestion.length)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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
                val suggestion = response
                editor.insertText(suggestion, 0)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onRunClick() {
        val client = julesApiClient ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                client.sendMessage(sessionId, "Run the code in the file `$filePath`")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onCommitClick() {
        _uiState.update { it.copy(showCommitDialog = true) }
    }

    fun onCommitDialogDismiss() {
        _uiState.update { it.copy(showCommitDialog = false, commitMessage = "") }
    }

    fun onCommitMessageChanged(message: String) {
        _uiState.update { it.copy(commitMessage = message) }
    }

    fun onCommitConfirm() {
        val client = julesApiClient ?: return
        val message = _uiState.value.commitMessage
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCommitDialog = false) }
            try {
                client.sendMessage(sessionId, "Commit changes with message: '$message'")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false, commitMessage = "") }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
