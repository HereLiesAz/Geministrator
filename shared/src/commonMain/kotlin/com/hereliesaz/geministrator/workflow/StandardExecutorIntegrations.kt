package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId

data class ExternalExecutionRun(
    val id: String,
    val status: ExternalExecutionStatus,
    val artifacts: List<ArtifactRef> = emptyList(),
    val progress: Float? = null,
    val message: String? = null,
) {
    init {
        require(progress == null || progress in 0f..1f) { "Execution progress must be normalized 0f..1f" }
    }
}

enum class ExternalExecutionStatus {
    Queued,
    Running,
    Verifying,
    Completed,
    Failed,
}

interface TestRunnerClient {
    suspend fun start(project: Project, command: String?): ExternalExecutionRun
    suspend fun getRun(project: Project, runId: String): ExternalExecutionRun
}

class TestRunnerExecutorIntegration(
    private val client: TestRunnerClient,
) : TaskExecutorIntegration {
    override fun supports(executor: TaskExecutor): Boolean = executor is TaskExecutor.TestRunner

    override suspend fun dispatch(context: TaskExecutorContext): TaskExecutorExecution {
        val executor = context.executor as TaskExecutor.TestRunner
        return client.start(context.project, executor.command).toTaskExecution()
    }

    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution {
        val runId = requireExternalRunId(context, "Test runner")
        return client.getRun(context.project, runId).toTaskExecution()
    }
}

interface DeploymentClient {
    suspend fun deploy(project: Project, environment: String): ExternalExecutionRun
    suspend fun getRun(project: Project, runId: String): ExternalExecutionRun
}

class DeploymentExecutorIntegration(
    private val client: DeploymentClient,
) : TaskExecutorIntegration {
    override fun supports(executor: TaskExecutor): Boolean = executor is TaskExecutor.Deployment

    override suspend fun dispatch(context: TaskExecutorContext): TaskExecutorExecution {
        val executor = context.executor as TaskExecutor.Deployment
        return client.deploy(context.project, executor.environment).toTaskExecution()
    }

    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution {
        val runId = requireExternalRunId(context, "Deployment")
        return client.getRun(context.project, runId).toTaskExecution()
    }
}

interface RepositoryOperationClient {
    suspend fun start(project: Project, operation: String): ExternalExecutionRun
    suspend fun getRun(project: Project, runId: String): ExternalExecutionRun
}

class RepositoryOperationExecutorIntegration(
    private val client: RepositoryOperationClient,
) : TaskExecutorIntegration {
    override fun supports(executor: TaskExecutor): Boolean = executor is TaskExecutor.RepositoryOperation

    override suspend fun dispatch(context: TaskExecutorContext): TaskExecutorExecution {
        val executor = context.executor as TaskExecutor.RepositoryOperation
        return client.start(context.project, executor.operation).toTaskExecution()
    }

    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution {
        val runId = requireExternalRunId(context, "Repository operation")
        return client.getRun(context.project, runId).toTaskExecution()
    }
}

interface ExternalServiceClient {
    suspend fun start(project: Project, service: String, operation: String?): ExternalExecutionRun
    suspend fun getRun(project: Project, service: String, runId: String): ExternalExecutionRun
}

class ExternalServiceExecutorIntegration(
    private val client: ExternalServiceClient,
) : TaskExecutorIntegration {
    override fun supports(executor: TaskExecutor): Boolean = executor is TaskExecutor.ExternalService

    override suspend fun dispatch(context: TaskExecutorContext): TaskExecutorExecution {
        val executor = context.executor as TaskExecutor.ExternalService
        return client.start(context.project, executor.service, executor.operation).toTaskExecution()
    }

    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution {
        val executor = context.executor as TaskExecutor.ExternalService
        val runId = requireExternalRunId(context, "External service")
        return client.getRun(context.project, executor.service, runId).toTaskExecution()
    }
}

interface NestedWorkflowClient {
    suspend fun start(
        project: Project,
        workflowDefinitionId: WorkflowDefinitionId,
        objective: String,
    ): ExternalExecutionRun

    suspend fun getRun(project: Project, runId: String): ExternalExecutionRun
}

class NestedWorkflowExecutorIntegration(
    private val client: NestedWorkflowClient,
) : TaskExecutorIntegration {
    override fun supports(executor: TaskExecutor): Boolean = executor is TaskExecutor.NestedWorkflow

    override suspend fun dispatch(context: TaskExecutorContext): TaskExecutorExecution {
        val executor = context.executor as TaskExecutor.NestedWorkflow
        return client.start(
            project = context.project,
            workflowDefinitionId = executor.workflowDefinitionId,
            objective = context.task.objective,
        ).toTaskExecution()
    }

    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution {
        val runId = requireExternalRunId(context, "Nested workflow")
        return client.getRun(context.project, runId).toTaskExecution()
    }
}

private fun requireExternalRunId(context: TaskExecutorContext, label: String): String =
    requireNotNull(context.taskRun.externalRunId) {
        "$label task ${context.task.id.value} is missing its external run ID"
    }

internal fun ExternalExecutionRun.toTaskExecution(): TaskExecutorExecution = TaskExecutorExecution(
    status = when (status) {
        ExternalExecutionStatus.Queued,
        ExternalExecutionStatus.Running,
        -> TaskRunStatus.Running
        ExternalExecutionStatus.Verifying -> TaskRunStatus.Verifying
        ExternalExecutionStatus.Completed -> TaskRunStatus.Completed
        ExternalExecutionStatus.Failed -> TaskRunStatus.Failed
    },
    externalRunId = id,
    artifacts = artifacts,
    progress = progress ?: if (status == ExternalExecutionStatus.Completed) 1f else null,
    progressMessage = message,
)
