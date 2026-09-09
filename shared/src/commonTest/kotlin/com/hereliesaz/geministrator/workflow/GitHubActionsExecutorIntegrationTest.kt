package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.ArtifactKind
import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.RepositoryRef
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

class GitHubActionsExecutorIntegrationTest {
    @Test
    fun dispatchPassesRepositoryWorkflowAndResolvedRef() = runBlocking {
        val client = FakeGitHubActionsClient(
            dispatchedRun = GitHubWorkflowRun(
                id = "run-42",
                status = GitHubWorkflowRunStatus.Queued,
                progressMessage = "queued",
            ),
        )
        val integration = GitHubActionsExecutorIntegration(client)
        val context = context(TaskExecutor.GitHubAction(workflow = "ci.yml"))

        val execution = integration.dispatch(context)

        assertEquals(
            GitHubWorkflowDispatchRequest(
                repository = context.project.repository!!,
                workflow = "ci.yml",
                ref = "main",
            ),
            client.lastDispatch,
        )
        assertEquals(TaskRunStatus.Running, execution.status)
        assertEquals("run-42", execution.externalRunId)
        assertEquals("queued", execution.progressMessage)
    }

    @Test
    fun explicitExecutorRefOverridesRepositoryDefault() = runBlocking {
        val client = FakeGitHubActionsClient()
        val integration = GitHubActionsExecutorIntegration(client)
        val context = context(TaskExecutor.GitHubAction(workflow = "release.yml", ref = "release/v2"))

        integration.dispatch(context)

        assertEquals("release/v2", client.lastDispatch?.ref)
    }

    @Test
    fun reconcileMapsCompletedRunAndArtifacts() = runBlocking {
        val base = context(TaskExecutor.GitHubAction("ci.yml"))
        val artifact = ArtifactRef(
            id = ArtifactId("artifact"),
            kind = ArtifactKind.TestResult,
            taskRunId = base.taskRun.id,
            label = "test-results",
            uri = "https://example.invalid/artifact",
            createdAtEpochMillis = 10L,
        )
        val client = FakeGitHubActionsClient(
            reconciledRun = GitHubWorkflowRun(
                id = "run-42",
                status = GitHubWorkflowRunStatus.Completed,
                artifacts = listOf(artifact),
                progressMessage = "complete",
            ),
        )
        val integration = GitHubActionsExecutorIntegration(client)
        val context = base.copy(taskRun = base.taskRun.copy(status = TaskRunStatus.Running, externalRunId = "run-42"))

        val execution = integration.reconcile(context)

        assertEquals(context.project.repository, client.lastRepository)
        assertEquals("run-42", client.lastRunId)
        assertEquals(TaskRunStatus.Completed, execution.status)
        assertEquals(1f, execution.progress)
        assertEquals(listOf(artifact), execution.artifacts)
    }

    @Test
    fun reconcileMapsFailedRun() = runBlocking {
        val client = FakeGitHubActionsClient(
            reconciledRun = GitHubWorkflowRun(
                id = "run-42",
                status = GitHubWorkflowRunStatus.Failed,
                progressMessage = "failed",
            ),
        )
        val integration = GitHubActionsExecutorIntegration(client)
        val base = context(TaskExecutor.GitHubAction("ci.yml"))
        val context = base.copy(taskRun = base.taskRun.copy(status = TaskRunStatus.Running, externalRunId = "run-42"))

        val execution = integration.reconcile(context)

        assertEquals(TaskRunStatus.Failed, execution.status)
        assertEquals("failed", execution.progressMessage)
    }

    private fun context(executor: TaskExecutor.GitHubAction): TaskExecutorContext {
        val taskId = TaskDefinitionId("ci")
        val repository = RepositoryRef(owner = "HereLiesAz", name = "haive", defaultBranch = "main")
        val project = Project(
            id = ProjectId("project"),
            name = "Project",
            repository = repository,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        val task = TaskDefinition(
            id = taskId,
            name = "CI",
            objective = "Run CI",
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
        return TaskExecutorContext(project, definition, run, task, taskRun, executor, 20L)
    }
}

private class FakeGitHubActionsClient(
    private val dispatchedRun: GitHubWorkflowRun = GitHubWorkflowRun("run-1", GitHubWorkflowRunStatus.Running),
    private val reconciledRun: GitHubWorkflowRun = dispatchedRun,
) : GitHubActionsClient {
    var lastDispatch: GitHubWorkflowDispatchRequest? = null
    var lastRepository: RepositoryRef? = null
    var lastRunId: String? = null

    override suspend fun dispatch(request: GitHubWorkflowDispatchRequest): GitHubWorkflowRun {
        lastDispatch = request
        return dispatchedRun
    }

    override suspend fun getRun(repository: RepositoryRef, runId: String): GitHubWorkflowRun {
        lastRepository = repository
        lastRunId = runId
        return reconciledRun
    }
}
