package com.jules.apiclient

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class Source(
    val name: String,
    val id: String,
    val githubRepo: GithubRepo? = null
)

@Serializable
data class GithubRepo(
    val owner: String,
    val repo: String
)

@Serializable
data class SourceList(
    val sources: List<Source>,
    val nextPageToken: String? = null
)

@Serializable
data class Session(
    val name: String,
    val id: String,
    val title: String,
    val sourceContext: SourceContext,
    val prompt: String
)

@Serializable
data class SourceContext(
    val source: String,
    val githubRepoContext: GithubRepoContext
)

@Serializable
data class GithubRepoContext(
    val startingBranch: String
)

@Serializable
data class Activity(
    val name: String,
    // ... other activity fields
)

@Serializable
data class ActivityList(
    val activities: List<Activity>
)

@Serializable
data class CreateSessionRequest(
    val prompt: String,
    val sourceContext: SourceContext,
    val title: String,
    val requirePlanApproval: Boolean = false
)

@Serializable
data class SendMessageRequest(
    val prompt: String
)

interface JulesApiService {
    @GET("v1alpha/sources")
    suspend fun getSources(@Header("X-Goog-Api-Key") apiKey: String): SourceList

    @POST("v1alpha/sessions")
    suspend fun createSession(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Body request: CreateSessionRequest
    ): Session

    @GET("v1alpha/sessions")
    suspend fun getSessions(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Query("pageSize") pageSize: Int = 10
    ): List<Session>

    @POST("v1alpha/sessions/{sessionId}:approvePlan")
    suspend fun approvePlan(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Path("sessionId") sessionId: String
    )

    @GET("v1alpha/sessions/{sessionId}/activities")
    suspend fun getActivities(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Path("sessionId") sessionId: String,
        @Query("pageSize") pageSize: Int = 30
    ): ActivityList

    @POST("v1alpha/sessions/{sessionId}:sendMessage")
    suspend fun sendMessage(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Path("sessionId") sessionId: String,
        @Body request: SendMessageRequest
    )
}