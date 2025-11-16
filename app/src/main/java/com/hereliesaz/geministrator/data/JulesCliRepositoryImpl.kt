package com.hereliesaz.geministrator.data

import com.hereliesaz.julesapisdk.Activity
import com.hereliesaz.julesapisdk.Session
import com.hereliesaz.julesapisdk.Source
import com.jules.cliclient.JulesCliClient
import javax.inject.Inject

class JulesCliRepositoryImpl @Inject constructor(
    private val julesCliClient: JulesCliClient
) : JulesRepository {

    override suspend fun getSources(): List<Source> {
        // Not supported by the CLI, return an empty list
        return emptyList()
    }

    override suspend fun createSession(prompt: String, source: Source, title: String): Session {
        val output = julesCliClient.newSession(source.name, prompt)
        val sessionId = parseSessionId(output)
        return Session(id = sessionId, title = title, prompt = prompt, source = source)
    }

    override suspend fun getSession(sessionId: String): Session {
        // Not supported by the CLI, return a placeholder.
        return Session(id = sessionId)
    }

    override suspend fun listSessions(): List<Session> {
        val output = julesCliClient.listSessions()
        return parseSessions(output)
    }

    override suspend fun approvePlan(sessionId: String) {
        // Not supported by the CLI, do nothing.
    }

    override suspend fun listActivities(sessionId: String): List<Activity> {
        // Not supported by the CLI, return an empty list.
        return emptyList()
    }

    override suspend fun sendMessage(sessionId: String, prompt: String) {
        // Not supported by the CLI, do nothing.
    }

    private fun parseSessionId(output: String): String {
        val regex = Regex("Session ID: (\\d+)")
        val matchResult = regex.find(output)
        return matchResult?.groups?.get(1)?.value ?: throw IllegalArgumentException("Could not parse session ID from output: $output")
    }

    private fun parseSessions(output: String): List<Session> {
        val regex = Regex("(\\d+): (.+)")
        return output.lines().mapNotNull { line ->
            val matchResult = regex.find(line)
            if (matchResult != null) {
                val (id, title) = matchResult.destructured
                Session(id = id, title = title)
            } else {
                null
            }
        }
    }
}
