package com.jules.apiclient

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("activityType")
sealed interface Activity {
    val name: String
}

@Serializable
@SerialName("USER_MESSAGE")
data class UserMessageActivity(
    override val name: String,
    val prompt: String
) : Activity

@Serializable
@SerialName("AGENT_RESPONSE")
data class AgentResponseActivity(
    override val name: String,
    val response: String
) : Activity

@Serializable
@SerialName("TOOL_CALL")
data class ToolCallActivity(
    override val name: String,
    val toolName: String,
    val args: String
) : Activity

@Serializable
@SerialName("TOOL_OUTPUT")
data class ToolOutputActivity(
    override val name: String,
    val toolName: String,
    val output: String
) : Activity

@Serializable
@SerialName("PLAN")
data class PlanActivity(
    override val name: String,
    val plan: String,
    val approved: Boolean
) : Activity


@Serializable
data class ActivityList(
    val activities: List<Activity>
)

@Serializable
data class CreateSessionRequest(
    val prompt: String,
    val sourceContext: SourceContext,
    val title: String,
    val requirePlanApproval: Boolean = false,
    val roles: String? = null
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