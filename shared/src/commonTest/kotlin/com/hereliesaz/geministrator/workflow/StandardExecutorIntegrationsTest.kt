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

class StandardExecutorIntegrationsTest {
    @Test
    fun testRunnerDispatchAndReconcileUseExternalRunId() = runBlocking {
        val client = FakeTestRunnerClient()
        val integration = TestRunnerExecutorIntegration(client)
        val context = context(TaskExecutor.TestRunner("./gradlew test"))

        val started = integration.dispatch(context)
        assertEquals("./gradlew test", client.command)
        assertEquals(TaskRunStatus.Running, started.status)
        assertEquals("test-1", started.externalRunId)

        val runningContext = context.copy(taskRun = context.taskRun.copy(status = TaskRunStatus.Running, externalRunId = "test-1"))
        val completed = integration.reconcile(runningContext)
        assertEquals("test-1", client.runId)
        assertEquals(TaskRunStatus.Completed, completed.status)
        assertEquals(1f, completed.progress)
    }

    @Test
    fun deploymentDispatchUsesEnvironment() = runBlocking {
        val client = FakeDeploymentClient()
        val integration = DeploymentExecutorIntegration(client)
        val context = context(TaskExecutor.Deployment("production"))

        val started = integration.dispatch(context)

        assertEquals("production", client.environment)
        assertEquals("deploy-1", started.externalRunId)
        assertEquals(TaskRunStatus.Running, started.status)
    }

    private fun context(executor: TaskExecutor): TaskExecutorContext {
        val taskId = TaskDefinitionId("task")
        val project = Project(
            id = ProjectId("project"),
            name = "Project",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        val task = TaskDefinition(
            id = taskId,
            name = "Task",
            objective = "Execute",
            roleId = null,
            executor = executor,
        )
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

private class FakeTestRunnerClient : TestRunnerClient {
    var command: String? = null
    var runId: String? = null

    override suspend fun start(project: Project, command: String?): ExternalExecutionRun {
        this.command = command
        return ExternalExecutionRun("test-1", ExternalExecutionStatus.Running)
    }

    override suspend fun getRun(project: Project, runId: String): ExternalExecutionRun {
        this.runId = runId
        return ExternalExecutionRun(runId, ExternalExecutionStatus.Completed)
    }
}

private class FakeDeploymentClient : DeploymentClient {
    var environment: String? = null

    override suspend fun deploy(project: Project, environment: String): ExternalExecutionRun {
        this.environment = environment
        return ExternalExecutionRun("deploy-1", ExternalExecutionStatus.Running)
    }

    override suspend fun getRun(project: Project, runId: String): ExternalExecutionRun =
        ExternalExecutionRun(runId, ExternalExecutionStatus.Completed)
}
