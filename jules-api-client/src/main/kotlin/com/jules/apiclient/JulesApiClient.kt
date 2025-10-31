package com.jules.apiclient

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class JulesApiClient(private val apiKey: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://jules.googleapis.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val service = retrofit.create(JulesApiService::class.java)

    suspend fun getSources(): List<Source> {
        val allSources = mutableListOf<Source>()
        var pageToken: String? = null
        do {
            val sourceList = service.getSources(apiKey, pageToken)
            allSources.addAll(sourceList.sources)
            pageToken = sourceList.nextPageToken
        } while (pageToken != null)
        return allSources
    }

    suspend fun createSession(prompt: String, source: Source, title: String, roles: String): Session {
        val request = CreateSessionRequest(
            prompt = prompt,
            sourceContext = SourceContext(
                source = source.name,
                githubRepoContext = GithubRepoContext(startingBranch = "main")
            ),
            title = title,
            roles = roles
        )
        return service.createSession(apiKey, request)
    }

    suspend fun getSessions(): List<Session> {
        return service.getSessions(apiKey)
    }

    suspend fun getSession(sessionId: String): Session {
        return service.getSession(apiKey, sessionId)
    }

    suspend fun nextTurn(sessionId: String, prompt: String): Turn {
        val request = NextTurnRequest(prompt)
        return service.nextTurn(apiKey, sessionId, request)
    }

    suspend fun approvePlan(sessionId: String) {
        service.approvePlan(apiKey, sessionId)
    }

    suspend fun getActivities(sessionId: String): ActivityList {
        return service.getActivities(apiKey, sessionId)
    }

    suspend fun sendMessage(sessionId: String, prompt: String) {
        val request = SendMessageRequest(prompt)
        service.sendMessage(apiKey, sessionId, request)
    }
}