package com.hereliesaz.geministrator.android.ui.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

data class FileExplorerUiState(
    val projectRoot: File,
    val currentPath: File,
    val files: List<File> = emptyList(),
    val error: String? = null,
) {
    val canNavigateUp: Boolean
        get() = currentPath != projectRoot && currentPath.parentFile != null
}

class FileExplorerViewModel(projectViewModel: ProjectViewModel) : ViewModel() {

    private val projectRoot = projectViewModel.uiState.value.localCachePath
        ?: throw IllegalStateException("FileExplorerViewModel requires a valid project path.")

    private val _uiState = MutableStateFlow(
        FileExplorerUiState(
            projectRoot = projectRoot,
            currentPath = projectRoot
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadDirectory(projectRoot)
    }

    fun navigateTo(directory: File) {
        if (directory.isDirectory) {
            loadDirectory(directory)
        }
    }

    fun navigateUp() {
        if (_uiState.value.canNavigateUp) {
            _uiState.value.currentPath.parentFile?.let {
                loadDirectory(it)
            }
        }
    }

    private fun loadDirectory(dir: File) {
        try {
            val fileList = dir.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: emptyList()

            _uiState.update {
                it.copy(currentPath = dir, files = fileList, error = null)
            }
        } catch (e: SecurityException) {
            _uiState.update {
                it.copy(error = "Error: Permission denied.")
            }
        }
    }

    companion object {
        fun provideFactory(
            projectViewModel: ProjectViewModel,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FileExplorerViewModel(projectViewModel) as T
            }
        }
    }
}