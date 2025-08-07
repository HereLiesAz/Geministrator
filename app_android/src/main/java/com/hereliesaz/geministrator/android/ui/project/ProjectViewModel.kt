package com.hereliesaz.geministrator.android.ui.project

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.GitManager
import com.hereliesaz.geministrator.android.data.SafProjectCopier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProjectViewModel(application: Application) : AndroidViewModel(application) {
    private val projectManager = ProjectManager(application)
    var gitManager: GitManager? = null
        private set

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            projectManager.getProjectFolderUri()?.let { uri ->
                _uiState.update { it.copy(projectUri = uri, isLoading = true) }
                val localCopyPath = withContext(Dispatchers.IO) {
                    SafProjectCopier.copyProjectToCache(getApplication(), uri)
                }
                if (localCopyPath != null) {
                    gitManager = GitManager(localCopyPath)
                    _uiState.update {
                        it.copy(
                            projectUri = uri,
                            localCachePath = localCopyPath,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to copy project to cache.")
                    }
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
            val result = withContext(Dispatchers.IO) {
                GitManager.cloneRepository(url, getApplication())
            }
            result.onSuccess { localRepoPath ->
                gitManager = GitManager(localRepoPath)
                _uiState.update {
                    it.copy(
                        localCachePath = localRepoPath,
                        isLoading = false,
                        cloneUrl = url,
                        projectUri = null // Cloned projects don't have a SAF URI
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to clone repository: ${throwable.message}")
                }
            }
        }
    }

    fun onProjectSelected(uri: Uri?) {
        uri?.let {
            viewModelScope.launch {
                _uiState.update { state -> state.copy(projectUri = it, isLoading = true) }
                projectManager.onProjectFolderSelected(it)
                val localCopyPath = withContext(Dispatchers.IO) {
                    SafProjectCopier.copyProjectToCache(getApplication(), it)
                }
                if (localCopyPath != null) {
                    gitManager = GitManager(localCopyPath)
                    _uiState.update { state ->
                        state.copy(
                            projectUri = it,
                            localCachePath = localCopyPath,
                            isLoading = false,
                            cloneUrl = null
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(isLoading = false, error = "Failed to copy project to cache.")
                    }
                }
            }
        }
    }

    fun writeFile(filePath: String, content: String) {
        // If it's a SAF-based project, write back to the original location.
        _uiState.value.projectUri?.let {
            projectManager.writeFile(it, filePath, content)
        }

        // Always update the file in the local cache for consistency.
        _uiState.value.localCachePath?.let { cachePath ->
            val fileInCache = File(cachePath, filePath)
            fileInCache.parentFile?.mkdirs()
            fileInCache.writeText(content)
        }
    }

    fun readFile(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = _uiState.value.localCachePath?.let { cachePath ->
                val fileInCache = File(cachePath, filePath)
                if (fileInCache.exists()) fileInCache.readText() else "File not found."
            } ?: "Project cache path not found."
            _uiState.update { it.copy(fileContent = content) }
        }
    }

    fun loadFileTree() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.localCachePath?.let { cachePath ->
                val rootFile = File(cachePath)
                val fileTree = buildFileTree(rootFile)
                _uiState.update { it.copy(fileTree = fileTree) }
            }
        }
    }

    private fun buildFileTree(file: File): FileNode {
        return FileNode(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            children = if (file.isDirectory) {
                file.listFiles()?.map { buildFileTree(it) } ?: emptyList()
            } else {
                emptyList()
            }
        )
    }
}

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList()
)

data class ProjectUiState(
    val projectUri: Uri? = null,
    val localCachePath: File? = null,
    val isLoading: Boolean = false,
    val cloneUrl: String? = null,
    val error: String? = null,
    val fileTree: FileNode? = null,
    val fileContent: String = ""
)