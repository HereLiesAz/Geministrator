package com.hereliesaz.geministrator.providers.jules

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject

fun interface JulesApiKeyProvider {
    suspend fun getApiKey(): String
}

interface JulesApi {
    suspend fun listSources(): List<JulesSource>
    suspend fun getSession(sessionName: String): JulesSession
    suspend fun createSession(request: JulesCreateSessionRequest): JulesSession
    suspend fun listActivities(sessionName: String): List<JulesActivity>
    suspend fun sendMessage(sessionName: String, message: String)
    suspend fun approvePlan(sessionName: String)
    suspend fun deleteSession(sessionName: String)
}

class JulesRestApi(
    private val apiKeyProvider: JulesApiKeyProvider,
    private val baseUrl: String = "https://jules.googleapis.com/v1alpha",
    private val client: HttpClient = HttpClient {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000L
            connectTimeoutMillis = 10_000L
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )
        }
    },
) : JulesApi {

    override suspend fun listSources(): List<JulesSource> {
        val result = mutableListOf<JulesSource>()
        var pageToken: String? = null
        do {
            val response = client.get("$baseUrl/sources") {
                authenticate()
                url {
                    parameters.append("pageSize", "100")
                    pageToken?.let { parameters.append("pageToken", it) }
                }
            }.body<JulesListSourcesResponse>()
            result += response.sources
            pageToken = response.nextPageToken
        } while (!pageToken.isNullOrBlank())
        return result
    }

    override suspend fun getSession(sessionName: String): JulesSession =
        client.get("$baseUrl/$sessionName") {
            authenticate()
        }.body()

    override suspend fun createSession(request: JulesCreateSessionRequest): JulesSession =
        client.post("$baseUrl/sessions") {
            authenticate()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun listActivities(sessionName: String): List<JulesActivity> {
        val result = mutableListOf<JulesActivity>()
        var pageToken: String? = null
        do {
            val response = client.get("$baseUrl/$sessionName/activities") {
                authenticate()
                url {
                    parameters.append("pageSize", "100")
                    pageToken?.let { parameters.append("pageToken", it) }
                }
            }.body<JulesListActivitiesResponse>()
            result += response.activities
            pageToken = response.nextPageToken
        } while (!pageToken.isNullOrBlank())
        return result
    }

    override suspend fun sendMessage(sessionName: String, message: String) {
        client.post("$baseUrl/$sessionName:sendMessage") {
            authenticate()
            contentType(ContentType.Application.Json)
            setBody(JulesSendMessageRequest(prompt = message))
        }
    }

    override suspend fun approvePlan(sessionName: String) {
        client.post("$baseUrl/$sessionName:approvePlan") {
            authenticate()
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {})
        }
    }

    override suspend fun deleteSession(sessionName: String) {
        client.delete("$baseUrl/$sessionName") {
            authenticate()
        }
    }

    private suspend fun io.ktor.client.request.HttpRequestBuilder.authenticate() {
        header("x-goog-api-key", apiKeyProvider.getApiKey())
    }
}
