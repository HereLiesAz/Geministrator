package com.hereliesaz.geministrator.android.ui.project

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.GitManager
import com.hereliesaz.geministrator.android.data.ProjectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProjectViewModel(application: Application) : AndroidViewModel(application) {
    private val projectManager = ProjectManager(application)
    var gitManager: GitManager? = null
        private set

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            projectManager.getProjectFolderUri()?.let { uri ->
                gitManager = GitManager(getApplication(), uri)
                _uiState.update { it.copy(projectUri = uri) }
            }
        }
    }

    fun selectProject(launcher: ActivityResultLauncher<Intent>) {
        projectManager.openProjectFolderPicker(launcher)
    }

    fun onProjectSelected(uri: Uri?) {
        uri?.let {
            viewModelScope.launch {
                projectManager.onProjectFolderSelected(it)
                gitManager = GitManager(getApplication(), it)
                _uiState.update { state -> state.copy(projectUri = it) }
            }
        }
    }

    fun writeFile(filePath: String, content: String) {
        _uiState.value.projectUri?.let {
            projectManager.writeFile(it, filePath, content)
        }
    }

    fun readFile(filePath: String): String? {
        return _uiState.value.projectUri?.let {
            projectManager.readFile(it, filePath)
        }
    }
}

data class ProjectUiState(
    val projectUri: Uri? = null
)