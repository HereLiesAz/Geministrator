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

    suspend fun getSources(): SourceList {
        return service.getSources(apiKey)
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
        return service.createSession(apiKey, request)
    }

    suspend fun getSessions(): List<Session> {
        return service.getSessions(apiKey)
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