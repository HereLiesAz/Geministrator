package com.jules.apiclient

import com.hereliesaz.julesapisdk.JulesClient
import com.hereliesaz.julesapisdk.JulesApiException
import com.hereliesaz.julesapisdk.CreateSessionRequest
import com.hereliesaz.julesapisdk.Source
import com.hereliesaz.julesapisdk.Session
import com.hereliesaz.julesapisdk.Activity
import com.hereliesaz.julesapisdk.SourceContext
import com.hereliesaz.julesapisdk.GithubRepoContext

class JulesApiClient(private val apiKey: String) {

    private val client = JulesClient(apiKey)

    suspend fun getSources(): List<Source> {
        val allSources = mutableListOf<Source>()
        var pageToken: String? = null
        do {
            val sourceList = client.listSources(pageToken = pageToken)
            sourceList.sources?.let { allSources.addAll(it) }
            pageToken = sourceList.nextPageToken
        } while (pageToken != null)
        return allSources
    }

    suspend fun createSession(prompt: String, source: Source, title: String): Session {
        val request = CreateSessionRequest(
            prompt = prompt,
            sourceContext = SourceContext(
                source = source.name,
                githubRepoContext = GithubRepoContext(startingBranch = "main")
            ),
            title = title
        )
        return client.createSession(request)
    }

    suspend fun getSessions(): List<Session> {
        val allSessions = mutableListOf<Session>()
        var pageToken: String? = null
        do {
            val sessionList = client.listSessions(pageToken = pageToken)
            sessionList.sessions?.let { allSessions.addAll(it) }
            pageToken = sessionList.nextPageToken
        } while (pageToken != null)
        return allSessions
    }

    suspend fun getSession(sessionId: String): Session {
        return client.getSession(sessionId)
    }

    suspend fun approvePlan(sessionId: String) {
        client.approvePlan(sessionId)
    }

    suspend fun getActivities(sessionId: String): List<Activity> {
        val allActivities = mutableListOf<Activity>()
        var pageToken: String? = null
        do {
            val activityList = client.listActivities(sessionId, pageToken = pageToken)
            activityList.activities?.let { allActivities.addAll(it) }
            pageToken = activityList.nextPageToken
        } while (pageToken != null)
        return allActivities
    }

    suspend fun sendMessage(sessionId: String, prompt: String) {
        client.sendMessage(sessionId, prompt)
    }
}
