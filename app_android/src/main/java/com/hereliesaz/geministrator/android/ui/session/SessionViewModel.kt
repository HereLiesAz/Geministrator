package com.hereliesaz.geministrator.android.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.AndroidConfigStorage
import com.hereliesaz.geministrator.android.data.AndroidExecutionAdapter
import com.hereliesaz.geministrator.android.data.AndroidLogger
import com.hereliesaz.geministrator.android.data.local.HistoryDatabase
import com.hereliesaz.geministrator.android.data.local.HistoryRepository
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.PromptManager
import com.hereliesaz.geministrator.core.Orchestrator
import com.hereliesaz.geministrator.core.config.ConfigStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class SessionViewModel(
    application: Application,
    private val prompt: String,
    private val projectViewModel: ProjectViewModel
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()

    private val logger = AndroidLogger()
    private var inputDeferred: CompletableDeferred<String>? = null
    private val historyRepository: HistoryRepository
    private val executionAdapter: AndroidExecutionAdapter

    init {
        val historyDao = HistoryDatabase.getDatabase(application).historyDao()
        historyRepository = HistoryRepository(historyDao)
        executionAdapter = AndroidExecutionAdapter(projectViewModel, logger, this)
        listenToLogs()
        startOrchestration()
        refreshGitStatus()
    }

    private fun listenToLogs() {
        viewModelScope.launch {
            logger.logFlow.collect { newEntry ->
                _uiState.update { currentState ->
                    val isFinished =
                        "Workflow Finished" in newEntry.message || "Workflow Failed" in newEntry.message
                    val finalStatus = when {
                        "Workflow Finished" in newEntry.message -> WorkflowStatus.SUCCESS
                        "Workflow Failed" in newEntry.message -> WorkflowStatus.FAILURE
                        else -> null
                    }

                    if (isFinished && finalStatus != null) {
                        refreshGitStatus()
                        // Save the completed session to the database
                        viewModelScope.launch(Dispatchers.IO) {
                            historyRepository.saveCompletedSession(
                                prompt = prompt,
                                status = finalStatus,
                                logEntries = currentState.logEntries + newEntry
                            )
                        }
                    }

                    val newStatus = when {
                        newEntry.clarificationQuestion != null -> WorkflowStatus.AWAITING_INPUT
                        finalStatus != null -> finalStatus
                        else -> currentState.status
                    }
                    currentState.copy(
                        logEntries = currentState.logEntries + newEntry,
                        status = newStatus,
                        clarificationPrompt = newEntry.clarificationQuestion
                            ?: currentState.clarificationPrompt
                    )
                }
            }
        }
    }

    fun showDiffForFile(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            projectViewModel.gitManager?.getDiff(filePath)?.onSuccess { content ->
                _uiState.update {
                    it.copy(diffViewState = DiffViewState(filePath, content))
                }
            }?.onFailure { error ->
                logger.error("Failed to get diff for $filePath: ${error.message}")
            }
        }
    }

    fun dismissDiff() {
        _uiState.update { it.copy(diffViewState = null) }
    }

    fun toggleFileStaging(filePath: String) {
        val currentSelection = _uiState.value.gitStatus.selectedForStaging
        val newSelection = if (currentSelection.contains(filePath)) {
            currentSelection - filePath
        } else {
            currentSelection + filePath
        }
        _uiState.update {
            it.copy(gitStatus = it.gitStatus.copy(selectedForStaging = newSelection))
        }
    }

    fun stageSelectedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedFiles = _uiState.value.gitStatus.selectedForStaging.toList()
            if (selectedFiles.isEmpty()) return@launch

            logger.info("Staging ${selectedFiles.size} files...")
            val results = selectedFiles.map { filePath ->
                projectViewModel.gitManager?.stageFile(filePath)
            }

            if (results.all { it?.isSuccess == true }) {
                logger.info("All files staged successfully.")
            } else {
                logger.error("Some files failed to stage.")
            }

            // Clear selection and refresh status
            _uiState.update { it.copy(gitStatus = it.gitStatus.copy(selectedForStaging = emptySet())) }
            refreshGitStatus()
        }
    }

    fun refreshGitStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(gitStatus = it.gitStatus.copy(isLoading = true)) }
            val statusResult = projectViewModel.gitManager?.getStatus()

            statusResult?.onSuccess { rawStatus ->
                val modified =
                    "MODIFIED: (.*?)\n".toRegex().findAll(rawStatus).map { it.groupValues[1] }
                        .toList()
                val untracked =
                    "UNTRACKED: (.*?)\n".toRegex().findAll(rawStatus).map { it.groupValues[1] }
                        .toList()
                val added =
                    "ADDED: (.*?)\n".toRegex().findAll(rawStatus).map { it.groupValues[1] }.toList()
                val removed =
                    "REMOVED: (.*?)\n".toRegex().findAll(rawStatus).map { it.groupValues[1] }
                        .toList()
                _uiState.update {
                    it.copy(
                        gitStatus = it.gitStatus.copy(
                            modified = modified,
                            untracked = untracked,
                            added = added,
                            removed = removed,
                            isLoading = false
                        )
                    )
                }
            }?.onFailure { error ->
                _uiState.update {
                    it.copy(
                        gitStatus = it.gitStatus.copy(
                            isLoading = false
                        )
                    )
                }
            }
        }
    }

    fun submitClarification(response: String) {
        inputDeferred?.complete(response)
        _uiState.update { it.copy(status = WorkflowStatus.RUNNING, clarificationPrompt = null) }
    }

    suspend fun awaitClarification(): String {
        inputDeferred = CompletableDeferred()
        return inputDeferred!!.await()
    }

    private fun startOrchestration() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(status = WorkflowStatus.RUNNING) }

            val projectRootPath = projectViewModel.uiState.value.localCachePath?.absolutePath
            if (projectRootPath == null) {
                logger.error("Error: Project cache path is not available.")
                _uiState.update { it.copy(status = WorkflowStatus.FAILURE) }
                return@launch
            }

            val configStorage: ConfigStorage = AndroidConfigStorage(getApplication())

            val configDir = File(getApplication<Application>().cacheDir, "gemini_config")
            configDir.mkdirs()
            val promptManager = PromptManager(configDir)

            val apiKey = configStorage.loadApiKey()
            if (apiKey.isNullOrBlank()) {
                logger.error("FATAL: Gemini API Key is not configured. Please set it in the Settings screen.")
                _uiState.update { it.copy(status = WorkflowStatus.FAILURE) }
                return@launch
            }

            val geminiService = GeminiService(
                authMethod = "apikey",
                apiKey = apiKey,
                logger = logger,
                config = configStorage,
                strategicModelName = configStorage.loadModelName(
                    "strategic",
                    "gemini-1.5-pro-latest"
                ),
                flashModelName = configStorage.loadModelName("flash", "gemini-1.5-flash-latest"),
                promptManager = promptManager,
                adapter = null
            )
            geminiService.initialize()

            val orchestrator = Orchestrator(
                adapter = executionAdapter,
                logger = logger,
                config = configStorage,
                promptManager = promptManager,
                ai = geminiService
            )

            logger.info("Orchestrator initialized. Starting workflow for prompt: \"$prompt\"")

            orchestrator.run(
                prompt = prompt,
                projectRoot = projectRootPath,
                projectType = "Android Application",
                specFileContent = null
            )
        }
    }
}