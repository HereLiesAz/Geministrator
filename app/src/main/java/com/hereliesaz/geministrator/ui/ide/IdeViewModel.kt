package com.hereliesaz.geministrator.ui.ide

import androidx.lifecycle.ViewModel
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class IdeUiState(
    val editor: CodeEditor? = null,
    val currentFile: String? = null,
    val fileContent: String = "",
    val isLoading: Boolean = false
)

class IdeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState = _uiState.asStateFlow()

    fun onEditorAttached(editor: CodeEditor) {
        _uiState.update { it.copy(editor = editor) }
    }

    fun onFileOpened(filePath: String, content: String) {
        _uiState.update { it.copy(currentFile = filePath, fileContent = content) }
    }

    fun onContentChanged(content: String) {
        _uiState.update { it.copy(fileContent = content) }
    }
}
