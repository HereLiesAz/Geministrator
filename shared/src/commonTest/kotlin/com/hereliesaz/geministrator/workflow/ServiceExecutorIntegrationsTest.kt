package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.TestDesignPolicy
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceExecutorIntegrationsTest {
    @Test
    fun repositoryOperationDispatchesAndCompletes() = runBlocking {
        val client = RecordingRepositoryClient()
        val integration = RepositoryOperationExecutorIntegration(client)
        val context = context(TaskExecutor.RepositoryOperation("open-pull-request"))

        val started = integration.dispatch(context)
        assertEquals("open-pull-request", client.operation)
        assertEquals("repo-1", started.externalRunId)

        val completed = integration.reconcile(
            context.copy(taskRun = context.taskRun.copy(status = TaskRunStatus.Running, externalRunId = "repo-1")),
        )
        assertEquals(TaskRunStatus.Completed, completed.status)
        assertEquals("repo-1", client.runId)
    }

    @Test
    fun externalServiceCarriesServiceAndOperation() = runBlocking {
        val client = RecordingExternalServiceClient()
        val integration = ExternalServiceExecutorIntegration(client)
        val context = context(TaskExecutor.ExternalService("vercel", "deploy-preview"))

        val started = integration.dispatch(context)

        assertEquals("vercel", client.service)
        assertEquals("deploy-preview", client.operation)
        assertEquals(TaskRunStatus.Running, started.status)
    }

    @Test
    fun nestedWorkflowCarriesDefinitionId() = runBlocking {
        val client = RecordingNestedWorkflowClient()
        val integration = NestedWorkflowExecutorIntegration(client)
        val nestedId = WorkflowDefinitionId("child-workflow")
        val context = context(TaskExecutor.NestedWorkflow(nestedId))

        val started = integration.dispatch(context)

        assertEquals(nestedId, client.workflowDefinitionId)
        assertEquals("nested-1", started.externalRunId)
    }

    private fun context(executor: TaskExecutor): TaskExecutorContext {
        val taskId = TaskDefinitionId("task")
        val project = Project(ProjectId("project"), "Project", createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L)
        val task = TaskDefinition(taskId, "Task", "Execute", roleId = null, executor = executor)
        val definition = WorkflowDefinition(
            id = WorkflowDefinitionId("definition"),
            name = "Workflow",
            tasks = listOf(task),
            testDesignPolicy = TestDesignPolicy.None,
        )
        val taskRun = TaskRun(
            id = TaskRunId("task-run"),
            taskDefinitionId = taskId,
            status = TaskRunStatus.Ready,
            assignedRoleId = null,
            executor = executor,
        )
        val run = WorkflowRun(
            id = WorkflowRunId("run"),
            projectId = project.id,
            workflowDefinitionId = definition.id,
            objective = "Ship",
            status = WorkflowRunStatus.Running,
            taskRuns = mapOf(taskId to taskRun),
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        return TaskExecutorContext(project, definition, run, task, taskRun, executor, 2L)
    }
}

private class RecordingRepositoryClient : RepositoryOperationClient {
    var operation: String? = null
    var runId: String? = null
    override suspend fun start(project: Project, operation: String): ExternalExecutionRun {
        this.operation = operation
        return ExternalExecutionRun("repo-1", ExternalExecutionStatus.Running)
    }
    override suspend fun getRun(project: Project, runId: String): ExternalExecutionRun {
        this.runId = runId
        return ExternalExecutionRun(runId, ExternalExecutionStatus.Completed)
    }
}

private class RecordingExternalServiceClient : ExternalServiceClient {
    var service: String? = null
    var operation: String? = null
    override suspend fun start(project: Project, service: String, operation: String?): ExternalExecutionRun {
        this.service = service
        this.operation = operation
        return ExternalExecutionRun("service-1", ExternalExecutionStatus.Running)
    }
    override suspend fun getRun(project: Project, runId: String): ExternalExecutionRun =
        ExternalExecutionRun(runId, ExternalExecutionStatus.Completed)
}

private class RecordingNestedWorkflowClient : NestedWorkflowClient {
    var workflowDefinitionId: WorkflowDefinitionId? = null
    override suspend fun start(project: Project, workflowDefinitionId: WorkflowDefinitionId, targetProjectId: ProjectId?): ExternalExecutionRun {
        this.workflowDefinitionId = workflowDefinitionId
        return ExternalExecutionRun("nested-1", ExternalExecutionStatus.Running)
    }
    override suspend fun getRun(project: Project, runId: String): ExternalExecutionRun =
        ExternalExecutionRun(runId, ExternalExecutionStatus.Completed)
}
