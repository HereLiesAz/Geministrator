package com.jules.cliclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException

class JulesCliClient {

    private suspend fun runCommand(command: List<String>): String = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(command).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = reader.readText()
            val error = errorReader.readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IOException("Command failed with exit code $exitCode: $error")
            }
            output
        } catch (e: IOException) {
            throw IOException("Failed to run command: ${command.joinToString(" ")}", e)
        }
    }

    suspend fun newSession(repo: String, prompt: String): String {
        val command = listOf("jules", "remote", "new", "--repo", repo, "--session", prompt)
        return runCommand(command)
    }

    suspend fun listSessions(): String {
        val command = listOf("jules", "remote", "list", "--session")
        return runCommand(command)
    }

    suspend fun pull(sessionId: String): String {
        val command = listOf("jules", "remote", "pull", "--session", sessionId)
        return runCommand(command)
    }
}
