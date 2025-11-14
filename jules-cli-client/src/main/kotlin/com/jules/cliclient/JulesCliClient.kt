package com.jules.cliclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class JulesCliClient {

    suspend fun newSession(repo: String, prompt: String): String = withContext(Dispatchers.IO) {
        val command = listOf("jules", "remote", "new", "--repo", repo, "--session", prompt)
        val process = ProcessBuilder(command).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText()
        process.waitFor()
        output
    }

    suspend fun listSessions(): String = withContext(Dispatchers.IO) {
        val command = listOf("jules", "remote", "list", "--session")
        val process = ProcessBuilder(command).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText()
        process.waitFor()
        output
    }

    suspend fun pull(sessionId: String): String = withContext(Dispatchers.IO) {
        val command = listOf("jules", "remote", "pull", "--session", sessionId)
        val process = ProcessBuilder(command).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText()
        process.waitFor()
        output
    }
}
