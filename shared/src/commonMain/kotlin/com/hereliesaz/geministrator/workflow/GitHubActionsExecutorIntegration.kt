package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.ArtifactKind
import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.RepositoryRef
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRunStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class GitHubWorkflowDispatchRequest(
    val repository: RepositoryRef,
    val workflow: String,
    val ref: String,
)

data class GitHubWorkflowArtifact(
    val id: String,
    val name: String,
    val archiveDownloadUrl: String?,
)

data class GitHubWorkflowRun(
    val id: String,
    val status: GitHubWorkflowRunStatus,
    val artifacts: List<GitHubWorkflowArtifact> = emptyList(),
    val progressMessage: String? = null,
)

enum class GitHubWorkflowRunStatus {
    Queued,
    Running,
    Completed,
    Failed,
}

fun interface GitHubTokenProvider {
    suspend fun getToken(): String
}

interface GitHubActionsClient {
    suspend fun dispatch(request: GitHubWorkflowDispatchRequest): GitHubWorkflowRun
    suspend fun getRun(repository: RepositoryRef, runId: String): GitHubWorkflowRun
}

class GitHubRestActionsClient(
    private val httpClient: HttpClient,
    private val tokenProvider: GitHubTokenProvider,
    private val baseUrl: String = "https://api.github.com",
    private val json: Json = Json { ignoreUnknownKeys = true },
) : GitHubActionsClient {
    override suspend fun dispatch(request: GitHubWorkflowDispatchRequest): GitHubWorkflowRun {
        val repository = request.repository
        val token = requireToken()
        val response = httpClient.post(
            "$baseUrl/repos/${repository.owner.encodeURLPathPart()}/${repository.name.encodeURLPathPart()}/actions/workflows/${request.workflow.encodeURLPathPart()}/dispatches",
        ) {
            githubHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DispatchBody(ref = request.ref)))
        }
        response.requireSuccess("dispatch GitHub Actions workflow ${request.workflow}")
        val body = json.decodeFromString<DispatchResponse>(response.bodyAsText())
        return GitHubWorkflowRun(
            id = body.workflowRunId.toString(),
            status = GitHubWorkflowRunStatus.Queued,
            progressMessage = body.htmlUrl ?: body.runUrl,
        )
    }

    override suspend fun getRun(repository: RepositoryRef, runId: String): GitHubWorkflowRun {
        val token = requireToken()
        val baseRepositoryUrl = "$baseUrl/repos/${repository.owner.encodeURLPathPart()}/${repository.name.encodeURLPathPart()}"
        val runResponse = httpClient.get("$baseRepositoryUrl/actions/runs/${runId.encodeURLPathPart()}") {
            githubHeaders(token)
        }
        runResponse.requireSuccess("read GitHub Actions run $runId")
        val run = json.decodeFromString<RunResponse>(runResponse.bodyAsText())

        val artifactsResponse = httpClient.get("$baseRepositoryUrl/actions/runs/${runId.encodeURLPathPart()}/artifacts") {
            githubHeaders(token)
        }
        artifactsResponse.requireSuccess("read artifacts for GitHub Actions run $runId")
        val artifacts = json.decodeFromString<ArtifactsResponse>(artifactsResponse.bodyAsText())
            .artifacts
            .filterNot(ArtifactResponse::expired)
            .map { artifact ->
                GitHubWorkflowArtifact(
                    id = artifact.id.toString(),
                    name = artifact.name,
                    archiveDownloadUrl = artifact.archiveDownloadUrl,
                )
            }

        return GitHubWorkflowRun(
            id = run.id.toString(),
            status = run.toStatus(),
            artifacts = artifacts,
            progressMessage = run.conclusion ?: run.status,
        )
    }

    private suspend fun requireToken(): String = tokenProvider.getToken().trim().also { token ->
        require(token.isNotEmpty()) { "GitHub Actions token is not configured" }
    }

    private fun HttpRequestBuilder.githubHeaders(token: String) {
        header(HttpHeaders.Accept, "application/vnd.github+json")
        header(HttpHeaders.Authorization, "Bearer $token")
        header("X-GitHub-Api-Version", API_VERSION)
    }

    private suspend fun HttpResponse.requireSuccess(operation: String) {
        if (status.value in 200..299) return
        val responseBody = bodyAsText().take(500)
        error("Unable to $operation: HTTP ${status.value}${responseBody.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}")
    }

    private fun RunResponse.toStatus(): GitHubWorkflowRunStatus = when (status) {
        "completed" -> if (conclusion == "success") GitHubWorkflowRunStatus.Completed else GitHubWorkflowRunStatus.Failed
        "queued", "waiting", "pending", "requested" -> GitHubWorkflowRunStatus.Queued
        else -> GitHubWorkflowRunStatus.Running
    }

    @Serializable
    private data class DispatchBody(val ref: String)

    @Serializable
    private data class DispatchResponse(
        @SerialName("workflow_run_id") val workflowRunId: Long,
        @SerialName("run_url") val runUrl: String? = null,
        @SerialName("html_url") val htmlUrl: String? = null,
    )

    @Serializable
    private data class RunResponse(
        val id: Long,
        val status: String,
        val conclusion: String? = null,
    )

    @Serializable
    private data class ArtifactsResponse(
        val artifacts: List<ArtifactResponse> = emptyList(),
    )

    @Serializable
    private data class ArtifactResponse(
        val id: Long,
        val name: String,
        val expired: Boolean = false,
        @SerialName("archive_download_url") val archiveDownloadUrl: String? = null,
    )

    private companion object {
        const val API_VERSION = "2026-03-10"
    }
}

class GitHubActionsExecutorIntegration(
    private val client: GitHubActionsClient,
) : TaskExecutorIntegration {
    override fun supports(executor: TaskExecutor): Boolean = executor is TaskExecutor.GitHubAction

    override suspend fun dispatch(context: TaskExecutorContext): TaskExecutorExecution {
        val executor = context.executor as TaskExecutor.GitHubAction
        val repository = requireNotNull(context.project.repository) {
            "GitHub Action executor requires a project repository"
        }
        val ref = executor.ref ?: repository.defaultBranch
        require(!ref.isNullOrBlank()) {
            "GitHub Action executor requires an explicit ref or repository default branch"
        }
        val run = client.dispatch(
            GitHubWorkflowDispatchRequest(
                repository = repository,
                workflow = executor.workflow,
                ref = ref,
            ),
        )
        return run.toExecution(context)
    }

    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution {
        val repository = requireNotNull(context.project.repository) {
            "GitHub Action executor requires a project repository"
        }
        val runId = requireNotNull(context.taskRun.externalRunId) {
            "GitHub Action task ${context.task.id.value} is missing its external run ID"
        }
        return client.getRun(repository, runId).toExecution(context)
    }

    private fun GitHubWorkflowRun.toExecution(context: TaskExecutorContext): TaskExecutorExecution = TaskExecutorExecution(
        status = when (status) {
            GitHubWorkflowRunStatus.Queued,
            GitHubWorkflowRunStatus.Running,
            -> TaskRunStatus.Running
            GitHubWorkflowRunStatus.Completed -> TaskRunStatus.Completed
            GitHubWorkflowRunStatus.Failed -> TaskRunStatus.Failed
        },
        externalRunId = id,
        artifacts = artifacts.map { artifact ->
            ArtifactRef(
                id = ArtifactId("${context.taskRun.id.value}:github-action:${artifact.id}"),
                kind = ArtifactKind.CommandOutput,
                taskRunId = context.taskRun.id,
                label = artifact.name,
                uri = artifact.archiveDownloadUrl,
                createdAtEpochMillis = context.nowEpochMillis,
            )
        },
        progress = when (status) {
            GitHubWorkflowRunStatus.Completed -> 1f
            else -> null
        },
        progressMessage = progressMessage,
    )
}
