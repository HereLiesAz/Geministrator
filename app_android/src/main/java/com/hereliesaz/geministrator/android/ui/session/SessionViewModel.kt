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
import kotlinx.coroutines.launch
import java.io.File

class SessionViewModel(
    application: Application,
    private val prompt: String,
    private val projectViewModel: ProjectViewModel
) : AndroidViewModel(application) {

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries = _logEntries.asStateFlow()

    init {
        startOrchestration()
    }

    private fun startOrchestration() {
        viewModelScope.launch(Dispatchers.IO) {
            val projectRootPath = projectViewModel.uiState.value.localCachePath?.absolutePath
            if (projectRootPath == null) {
                _logEntries.value += LogEntry("Error: Project cache path is not available.", com.hereliesaz.geministrator.android.ui.theme.Agent.ANTAGONIST)
                return@launch
            }

            // 1. Initialize Android-specific implementations
            val logger = AndroidLogger(_logEntries)
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