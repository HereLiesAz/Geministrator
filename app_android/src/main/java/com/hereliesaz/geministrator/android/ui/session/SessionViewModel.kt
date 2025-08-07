package com.hereliesaz.geministrator.android.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.AndroidConfigStorage
import com.hereliesaz.geministrator.android.data.AndroidExecutionAdapter
import com.hereliesaz.geministrator.android.data.AndroidLogger
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.PromptManager
import com.hereliesaz.geministrator.core.Orchestrator
import com.hereliesaz.geministrator.core.config.ConfigStorage
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


    init {
        listenToLogs()
        startOrchestration()
    }

    private fun listenToLogs() {
        viewModelScope.launch {
            logger.logFlow.collect { newEntry ->
                _uiState.update { currentState ->
                    val newStatus = when {
                        "Workflow Finished" in newEntry.message -> WorkflowStatus.SUCCESS
                        "Workflow Failed" in newEntry.message -> WorkflowStatus.FAILURE
                        newEntry.isAwaitingInput -> WorkflowStatus.AWAITING_INPUT
                        else -> currentState.status
                    }
                    currentState.copy(
                        logEntries = currentState.logEntries + newEntry,
                        status = newStatus
                    )
                }
            }
        }
    }

    fun getDiff(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val diffResult = projectViewModel.gitManager?.getDiff(filePath)
            diffResult?.onSuccess { diff ->
                _uiState.update { it.copy(diff = diff) }
            }?.onFailure {
                _uiState.update { it.copy(diff = "Error getting diff: ${it.message}") }
            }
        }
    }

    fun getGitStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val statusResult = projectViewModel.gitManager?.getStatus()
            statusResult?.onSuccess { status ->
                _uiState.update { it.copy(gitStatus = status) }
            }?.onFailure {
                // TODO: Handle error properly
            }
        }
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

            // 1. Initialize Android-specific implementations
            val configStorage: ConfigStorage = AndroidConfigStorage(getApplication())
            val executionAdapter = AndroidExecutionAdapter(projectViewModel, logger)

            // Re-use CLI prompt logic, but point it to a writable cache directory
            val configDir = File(getApplication<Application>().cacheDir, "gemini_config")
            configDir.mkdirs()
            val promptManager = PromptManager(configDir)

            // 2. Configure Gemini Service
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
                strategicModelName = "gemini-1.5-pro-latest",
                flashModelName = "gemini-1.5-flash-latest",
                promptManager = promptManager,
                adapter = null // ADC not used in this flow
            )

            // 3. Initialize and run the Orchestrator
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