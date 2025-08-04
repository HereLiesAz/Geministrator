package com.hereliesaz.geministrator.android.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.adapter.CliConfigStorage
import com.hereliesaz.geministrator.android.data.AndroidExecutionAdapter
import com.hereliesaz.geministrator.android.data.AndroidLogger
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.PromptManager
import com.hereliesaz.geministrator.core.Orchestrator
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
            // For now, we use the CLI's file-based config storage, assuming it can write to a cache dir.
            // A true Android implementation would use SharedPreferences or DataStore.
            val configDir = File(getApplication<Application>().cacheDir, "gemini_config")
            configDir.mkdirs()
            val configStorage = CliConfigStorage(configDir)
            val executionAdapter = AndroidExecutionAdapter(projectViewModel, logger)
            val promptManager = PromptManager(configDir) // Uses the same config dir for prompts.json

            // 2. Configure Gemini Service
            // This part is simplified. A real app would need a settings screen to manage the API key.
            // We'll hardcode a dummy key check for now.
            val apiKey = "YOUR_API_KEY_HERE" // This needs to be managed via a settings screen
            if (apiKey == "YOUR_API_KEY_HERE") {
                logger.error("FATAL: Gemini API Key is not configured. Please add it.")
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

            // For now, projectType is hardcoded. This could be a dropdown in the UI later.
            orchestrator.run(
                prompt = prompt,
                projectRoot = projectRootPath,
                projectType = "Android Application",
                specFileContent = null
            )
        }
    }
}