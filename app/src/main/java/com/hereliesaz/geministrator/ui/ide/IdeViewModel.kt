package com.hereliesaz.geministrator.ui.ide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.GitManager
import com.hereliesaz.geministrator.data.GithubRepository
import com.hereliesaz.geministrator.data.JulesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdeViewModel @Inject constructor(
    private val julesRepository: JulesRepository,
    private val githubRepository: GithubRepository,
    private val gitManager: GitManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId")!!
    private val filePath: String = savedStateHandle.get<String>("filePath")!!

    private val _uiState = MutableStateFlow(IdeUiState(filePath = filePath))
    val uiState = _uiState.asStateFlow()

    init {
        loadFileContent()
    }

    private fun loadFileContent() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val content = julesRepository.getFileContent(sessionId, filePath)
                _uiState.update {
                    it.copy(fileContent = content, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onRunClicked() {
        _uiState.update { it.copy(isLoading = true, consoleOutput = "Sending run command...") }
        viewModelScope.launch {
            try {
                val output = julesRepository.runFile(sessionId, filePath)
                _uiState.update { it.copy(isLoading = false, consoleOutput = output) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, consoleOutput = "Error: ${e.message}") }
            }
        }
    }

    fun onCommitClicked(commitMessage: String) {
        _uiState.update { it.copy(isLoading = true, consoleOutput = "Saving and committing...") }
        viewModelScope.launch {
            val currentContent = _uiState.value.fileContent
            try {
                // Step 1: Update the file content
                julesRepository.updateFileContent(sessionId, filePath, currentContent)

                // Step 2: Commit the changes
                val output = julesRepository.commitChanges(sessionId, commitMessage)

                _uiState.update { it.copy(isLoading = false, consoleOutput = output) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, consoleOutput = "Error: ${e.message}") }
            }
        }
    }

    fun onFileContentChanged(newContent: String) {
        _uiState.update { it.copy(fileContent = newContent) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}