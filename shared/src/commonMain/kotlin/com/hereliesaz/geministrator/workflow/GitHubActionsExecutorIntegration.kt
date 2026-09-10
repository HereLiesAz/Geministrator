package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.ArtifactKind
import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.RepositoryRef
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRunStatus
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
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
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class GitHubWorkflowDispatchRequest(val repository: RepositoryRef, val workflow: String, val ref: String)
data class GitHubWorkflowArtifact(val id: String, val name: String, val archiveDownloadUrl: String?)
data class GitHubWorkflowRun(val id: String, val status: GitHubWorkflowRunStatus, val artifacts: List<GitHubWorkflowArtifact> = emptyList(), val progressMessage: String? = null)
enum class GitHubWorkflowRunStatus { Queued, Running, Completed, Failed }
fun interface GitHubTokenProvider { suspend fun getToken(): String }

interface GitHubActionsClient {
    suspend fun dispatch(request: GitHubWorkflowDispatchRequest): GitHubWorkflowRun
    suspend fun getRun(repository: RepositoryRef, runId: String): GitHubWorkflowRun
}

class GitHubRestActionsClient(
    private val tokenProvider: GitHubTokenProvider,
    private val httpClient: HttpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000L
            connectTimeoutMillis = 10_000L
        }
    },
    private val baseUrl: String = "https://api.github.com",
    private val json: Json = Json { ignoreUnknownKeys = true },
) : GitHubActionsClient {
    override suspend fun dispatch(request: GitHubWorkflowDispatchRequest): GitHubWorkflowRun {
        val token = requireToken()
        val previousRunIds = listWorkflowRuns(request.repository, request.workflow, request.ref, token).map { it.id }.toSet()
        val repository = request.repository
        httpClient.post("$baseUrl/repos/${repository.owner.encodeURLPathPart()}/${repository.name.encodeURLPathPart()}/actions/workflows/${request.workflow.encodeURLPathPart()}/dispatches") {
            githubHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DispatchBody(ref = request.ref)))
        }.requireSuccess("dispatch GitHub Actions workflow ${request.workflow}")

        repeat(DISPATCH_LOOKUP_ATTEMPTS) { attempt ->
            val run = listWorkflowRuns(repository, request.workflow, request.ref, token).firstOrNull { it.id !in previousRunIds }
            if (run != null) return run.toWorkflowRun()
            if (attempt < DISPATCH_LOOKUP_ATTEMPTS - 1) delay(DISPATCH_LOOKUP_DELAY_MILLIS)
        }
        error("GitHub accepted workflow dispatch ${request.workflow} but its new workflow_dispatch run did not appear for ref ${request.ref}")
    }

    override suspend fun getRun(repository: RepositoryRef, runId: String): GitHubWorkflowRun {
        val token = requireToken()
        val baseRepositoryUrl = "$baseUrl/repos/${repository.owner.encodeURLPathPart()}/${repository.name.encodeURLPathPart()}"
        val runResponse = httpClient.get("$baseRepositoryUrl/actions/runs/${runId.encodeURLPathPart()}") { githubHeaders(token) }
        runResponse.requireSuccess("read GitHub Actions run $runId")
        val run = json.decodeFromString<RunResponse>(runResponse.bodyAsText())
        val artifactsResponse = httpClient.get("$baseRepositoryUrl/actions/runs/${runId.encodeURLPathPart()}/artifacts") { githubHeaders(token) }
        artifactsResponse.requireSuccess("read artifacts for GitHub Actions run $runId")
        val artifacts = json.decodeFromString<ArtifactsResponse>(artifactsResponse.bodyAsText()).artifacts.filterNot(ArtifactResponse::expired).map { artifact ->
            GitHubWorkflowArtifact(artifact.id.toString(), artifact.name, artifact.archiveDownloadUrl)
        }
        return run.toWorkflowRun(artifacts)
    }

    private suspend fun listWorkflowRuns(repository: RepositoryRef, workflow: String, ref: String, token: String): List<RunResponse> {
        val url = "$baseUrl/repos/${repository.owner.encodeURLPathPart()}/${repository.name.encodeURLPathPart()}/actions/workflows/${workflow.encodeURLPathPart()}/runs"
        val response = httpClient.get(url) {
            githubHeaders(token)
            url {
                parameters.append("event", "workflow_dispatch")
                parameters.append("branch", ref)
                parameters.append("per_page", "20")
            }
        }
        response.requireSuccess("list GitHub Actions workflow runs for $workflow")
        return json.decodeFromString<RunsResponse>(response.bodyAsText()).workflowRuns
    }

    private fun RunResponse.toWorkflowRun(artifacts: List<GitHubWorkflowArtifact> = emptyList()) = GitHubWorkflowRun(
        id = id.toString(),
        status = toStatus(),
        artifacts = artifacts,
        progressMessage = conclusion ?: status,
    )

    private suspend fun requireToken(): String = tokenProvider.getToken().trim().also { require(it.isNotEmpty()) { "GitHub Actions token is not configured" } }
    private fun HttpRequestBuilder.githubHeaders(token: String) { header(HttpHeaders.Accept, "application/vnd.github+json"); header(HttpHeaders.Authorization, "Bearer $token"); header("X-GitHub-Api-Version", API_VERSION) }
    private suspend fun HttpResponse.requireSuccess(operation: String) { if (status.value in 200..299) return; val responseBody = bodyAsText().take(500); error("Unable to $operation: HTTP ${status.value}${responseBody.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}") }
    private fun RunResponse.toStatus(): GitHubWorkflowRunStatus = when (status) { "completed" -> if (conclusion == "success") GitHubWorkflowRunStatus.Completed else GitHubWorkflowRunStatus.Failed; "queued", "waiting", "pending", "requested" -> GitHubWorkflowRunStatus.Queued; else -> GitHubWorkflowRunStatus.Running }

    @Serializable private data class DispatchBody(val ref: String)
    @Serializable private data class RunsResponse(@SerialName("workflow_runs") val workflowRuns: List<RunResponse> = emptyList())
    @Serializable private data class RunResponse(val id: Long, val status: String, val conclusion: String? = null)
    @Serializable private data class ArtifactsResponse(val artifacts: List<ArtifactResponse> = emptyList())
    @Serializable private data class ArtifactResponse(val id: Long, val name: String, val expired: Boolean = false, @SerialName("archive_download_url") val archiveDownloadUrl: String? = null)

    private companion object {
        const val API_VERSION = "2026-03-10"
        const val DISPATCH_LOOKUP_ATTEMPTS = 10
        const val DISPATCH_LOOKUP_DELAY_MILLIS = 500L
    }
}

class GitHubActionsExecutorIntegration(private val client: GitHubActionsClient) : TaskExecutorIntegration {
    override fun supports(executor: TaskExecutor): Boolean = executor is TaskExecutor.GitHubAction
    override suspend fun dispatch(context: TaskExecutorContext): TaskExecutorExecution {
        val executor = context.executor as TaskExecutor.GitHubAction
        val repository = requireNotNull(context.project.repository) { "GitHub Action executor requires a project repository" }
        val ref = executor.ref ?: repository.defaultBranch
        require(!ref.isNullOrBlank()) { "GitHub Action executor requires an explicit ref or repository default branch" }
        return client.dispatch(GitHubWorkflowDispatchRequest(repository, executor.workflow, ref)).toExecution(context)
    }
    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution {
        val repository = requireNotNull(context.project.repository) { "GitHub Action executor requires a project repository" }
        val runId = requireNotNull(context.taskRun.externalRunId) { "GitHub Action task ${context.task.id.value} is missing its external run ID" }
        return client.getRun(repository, runId).toExecution(context)
    }
    private fun GitHubWorkflowRun.toExecution(context: TaskExecutorContext) = TaskExecutorExecution(
        status = when (status) { GitHubWorkflowRunStatus.Queued, GitHubWorkflowRunStatus.Running -> TaskRunStatus.Running; GitHubWorkflowRunStatus.Completed -> TaskRunStatus.Completed; GitHubWorkflowRunStatus.Failed -> TaskRunStatus.Failed },
        externalRunId = id,
        artifacts = artifacts.map { artifact -> ArtifactRef(ArtifactId("${context.taskRun.id.value}:github-action:${context.taskRun.attempt}:${artifact.id}"), ArtifactKind.CommandOutput, context.taskRun.id, artifact.name, uri = artifact.archiveDownloadUrl, createdAtEpochMillis = context.nowEpochMillis) },
        progress = if (status == GitHubWorkflowRunStatus.Completed) 1f else null,
        progressMessage = progressMessage,
    )
}
