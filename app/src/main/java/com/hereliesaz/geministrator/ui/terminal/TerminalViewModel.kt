package com.hereliesaz.geministrator.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.hereliesaz.geministrator.data.JulesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val julesRepository: JulesRepository
) : ViewModel() {
//    private var geminiApiClient: GeminiApiClient? = null

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
//            val geminiApiKey = settingsRepository.getGeminiApiKey()
//            if (!geminiApiKey.isNullOrBlank()) {
//                geminiApiClient = GeminiApiClient(geminiApiKey)
//            }
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
//            "gemini" -> executeGeminiCommand(parsedCommand.args)
            else -> "Unknown command: ${parsedCommand.command}"
        }
    }

    private suspend fun executeJulesCommand(args: List<String>): String {
        if (args.isEmpty()) {
            return "Please provide a subcommand for the Jules command. Available commands: new, list, pull"
        }

        return when (val subcommand = args.first()) {
            "new" -> {
                if (args.size < 3) {
                    return "Usage: jules new <repo> <prompt>"
                }
                val repo = args[1]
                val prompt = args.drop(2).joinToString(" ")
                try {
                    julesRepository.createSession(repo, prompt)
                } catch (e: Exception) {
                    "Error creating session: ${e.message}"
                }
            }
            "list" -> {
                try {
                    julesRepository.listSessions()
                } catch (e: Exception) {
                    "Error listing sessions: ${e.message}"
                }
            }
            "pull" -> {
                if (args.size < 2) {
                    return "Usage: jules pull <sessionId>"
                }
                val sessionId = args[1]
                try {
                    julesRepository.pull(sessionId)
                } catch (e: Exception) {
                    "Error pulling session: ${e.message}"
                }
            }
            else -> "Unknown Jules command: $subcommand"
        }
    }

//    private suspend fun executeGeminiCommand(args: List<String>): String {
//        val client = geminiApiClient ?: return "Gemini API key not configured. Please set it in the settings screen."
//        if (args.isEmpty()) {
//            return "Please provide a prompt for the Gemini command."
//        }
//        val prompt = args.joinToString(" ")
//        return try {
//            client.generateContent(prompt)
//        } catch (e: Exception) {
//            "Error executing Gemini command: ${e.message}"
//        }
//    }
}
