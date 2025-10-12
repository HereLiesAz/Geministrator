package com.hereliesaz.geministrator.ui.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.GeminiApiClient
import com.jules.apiclient.JulesApiClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private var julesApiClient: JulesApiClient? = null
    private var geminiApiClient: GeminiApiClient? = null

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val apiKey = settingsRepository.apiKey.first()
            val geminiApiKey = settingsRepository.geminiApiKey.first()
            val githubRepository = settingsRepository.githubRepository.first()
            val gcpLocation = settingsRepository.gcpLocation.first()
            val geminiModelName = settingsRepository.geminiModelName.first()

            if (!apiKey.isNullOrBlank()) {
                julesApiClient = JulesApiClient(apiKey)
            }

            if (!geminiApiKey.isNullOrBlank() && !githubRepository.isNullOrBlank() && !gcpLocation.isNullOrBlank() && !geminiModelName.isNullOrBlank()) {
                geminiApiClient = GeminiApiClient(githubRepository, gcpLocation, geminiModelName)
            }
        }
    }

    fun processInput(input: String) {
        val parsedCommand = parseCommand(input)
        _uiState.update { it.copy(output = it.output + "\n> " + input, isLoading = true) }
        viewModelScope.launch {
            val result = executeCommand(parsedCommand)
            _uiState.update { it.copy(output = it.output + "\n" + result, isLoading = false) }
        }
    }

    private suspend fun executeCommand(parsedCommand: ParsedCommand): String {
        return when (parsedCommand.command) {
            "jules" -> executeJulesCommand(parsedCommand.args)
            "gemini" -> executeGeminiCommand(parsedCommand.args)
            else -> "Unknown command: ${parsedCommand.command}"
        }
    }

    private suspend fun executeJulesCommand(args: List<String>): String {
        if (julesApiClient == null) {
            return "Jules API key not configured. Please set it in the settings screen."
        }

        if (args.isEmpty()) {
            return "Please provide a subcommand for the Jules command. Available commands: sources, sessions, send"
        }

        return when (val subcommand = args.first()) {
            "sources" -> {
                try {
                    val sources = julesApiClient?.getSources()
                    sources?.sources?.joinToString("\n") { it.name } ?: "No sources found."
                } catch (e: Exception) {
                    "Error getting sources: ${e.message}"
                }
            }
            "sessions" -> {
                try {
                    val sessions = julesApiClient?.getSessions()
                    sessions?.joinToString("\n") { "${it.id}: ${it.title}" } ?: "No sessions found."
                } catch (e: Exception) {
                    "Error getting sessions: ${e.message}"
                }
            }
            "send" -> {
                if (args.size < 3) {
                    return "Usage: jules send <sessionId> <prompt>"
                }
                val sessionId = args[1]
                val prompt = args.drop(2).joinToString(" ")
                try {
                    julesApiClient?.sendMessage(sessionId, prompt)
                    "Message sent to session $sessionId."
                } catch (e: Exception) {
                    "Error sending message: ${e.message}"
                }
            }
            else -> "Unknown Jules command: $subcommand"
        }
    }

    private suspend fun executeGeminiCommand(args: List<String>): String {
        if (geminiApiClient == null) {
            return "Gemini API key not configured. Please set it in the settings screen."
        }
        if (args.isEmpty()) {
            return "Please provide a prompt for the Gemini command."
        }
        val prompt = args.joinToString(" ")
        return try {
            val response = geminiApiClient?.generateContent(prompt)
            response?.toString() ?: "No response from Gemini."
        } catch (e: Exception) {
            "Error executing Gemini command: ${e.message}"
        }
    }
}
