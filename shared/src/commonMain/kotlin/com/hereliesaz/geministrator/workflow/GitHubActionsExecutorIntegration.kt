package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.RepositoryRef
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRunStatus

data class GitHubWorkflowDispatchRequest(
    val repository: RepositoryRef,
    val workflow: String,
    val ref: String?,
)

data class GitHubWorkflowRun(
    val id: String,
    val status: GitHubWorkflowRunStatus,
    val artifacts: List<ArtifactRef> = emptyList(),
    val progressMessage: String? = null,
)

enum class GitHubWorkflowRunStatus {
    Queued,
    Running,
    Completed,
    Failed,
}

interface GitHubActionsClient {
    suspend fun dispatch(request: GitHubWorkflowDispatchRequest): GitHubWorkflowRun
    suspend fun getRun(repository: RepositoryRef, runId: String): GitHubWorkflowRun
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
        val run = client.dispatch(
            GitHubWorkflowDispatchRequest(
                repository = repository,
                workflow = executor.workflow,
                ref = executor.ref ?: repository.defaultBranch,
            ),
        )
        return run.toExecution()
    }

    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution {
        val repository = requireNotNull(context.project.repository) {
            "GitHub Action executor requires a project repository"
        }
        val runId = requireNotNull(context.taskRun.externalRunId) {
            "GitHub Action task ${context.task.id.value} is missing its external run ID"
        }
        return client.getRun(repository, runId).toExecution()
    }

    private fun GitHubWorkflowRun.toExecution(): TaskExecutorExecution = TaskExecutorExecution(
        status = when (status) {
            GitHubWorkflowRunStatus.Queued,
            GitHubWorkflowRunStatus.Running,
            -> TaskRunStatus.Running
            GitHubWorkflowRunStatus.Completed -> TaskRunStatus.Completed
            GitHubWorkflowRunStatus.Failed -> TaskRunStatus.Failed
        },
        externalRunId = id,
        artifacts = artifacts,
        progress = when (status) {
            GitHubWorkflowRunStatus.Completed -> 1f
            else -> null
        },
        progressMessage = progressMessage,
    )
}
