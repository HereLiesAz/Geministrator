package com.hereliesaz.geministrator.ui.project

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.GitManager
import com.hereliesaz.geministrator.data.SafProjectCopier
import com.hereliesaz.geministrator.data.cloneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ProjectViewModel(
    private val projectManager: ProjectManager,
    private val application: Application,
    var gitManager: GitManager? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (gitManager == null) {
                projectManager.getProjectFolderUri()?.let { uri ->
                    _uiState.update { it.copy(projectUri = uri, isLoading = true) }
                    val localCopyPath = withContext(Dispatchers.IO) {
                        SafProjectCopier.copyProjectToCache(application, uri)
                    }
                    gitManager = GitManager(localCopyPath)
                    _uiState.update { it.copy(projectUri = uri, localCachePath = localCopyPath, isLoading = false) }
                }
            }
        }
    }

    fun selectProject(launcher: ActivityResultLauncher<Intent>) {
        projectManager.openProjectFolderPicker(launcher)
    }

    fun cloneProject(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result: Result<File> = withContext(Dispatchers.IO) {
                cloneRepository(url, application)
            }
            result.onSuccess { localRepoPath ->
                if (gitManager == null) {
                    gitManager = GitManager(localRepoPath)
                }
                _uiState.update {
                    it.copy(
                        localCachePath = localRepoPath,
                        isLoading = false,
                        cloneUrl = url,
                        projectUri = null // Cloned projects don't have a SAF URI
                    )
                }
            }.onFailure {
                // TODO: Propagate error to the UI
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onProjectSelected(uri: Uri?) {
        uri?.let {
            viewModelScope.launch {
                _uiState.update { state -> state.copy(projectUri = it, isLoading = true) }
                projectManager.onProjectFolderSelected(it)
                val localCopyPath = withContext(Dispatchers.IO) {
                    SafProjectCopier.copyProjectToCache(application, it)
                }
                gitManager = GitManager(localCopyPath)
                _uiState.update { state -> state.copy(projectUri = it, localCachePath = localCopyPath, isLoading = false, cloneUrl = null) }
            }
        }
    }

    fun writeFile(filePath: String, content: String): Result<Unit> = runCatching {
        var safResult: Result<Unit> = Result.success(Unit)
        // If it's a SAF-based project, write back to the original location.
        _uiState.value.projectUri?.let {
            safResult = projectManager.writeFile(it, filePath, content)
        }
        safResult.getOrThrow() // If SAF write failed, throw the exception

        // Always update the file in the local cache for consistency.
        _uiState.value.localCachePath?.let { cachePath ->
            val fileInCache = File(cachePath, filePath)
            fileInCache.parentFile?.mkdirs()
            fileInCache.writeText(content)
        } ?: throw IllegalStateException("Project cache path is not available.")
    }

    fun readFile(filePath: String): Result<String> = runCatching {
        // For reading, the local cache is the source of truth for the app's logic.
        _uiState.value.localCachePath?.let { cachePath ->
            val fileInCache = File(cachePath, filePath)
            if (fileInCache.exists()) fileInCache.readText()
            else throw java.io.FileNotFoundException("File not found in local cache: $filePath")
        } ?: throw IllegalStateException("Project cache path is not available.")
    }
}

data class ProjectUiState(
    val projectUri: Uri? = null,
    val localCachePath: File? = null,
    val isLoading: Boolean = false,
    val cloneUrl: String? = null
)