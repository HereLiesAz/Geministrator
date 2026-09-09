package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.ProviderRunId
import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.persistence.InMemoryWorkflowPersistence
import com.hereliesaz.geministrator.providers.ProviderActionResult
import com.hereliesaz.geministrator.providers.ProviderArtifact
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MixedExecutorWorkflowTest {
    @Test
    fun mixedAgentActionApprovalAndDeploymentShareOneDagWithoutFakeRoles() {
        val fixture = definition()

        assertTrue(WorkflowGraphValidator.validate(fixture.definition).isEmpty())

        val run = WorkflowRunFactory.create(
            definition = fixture.definition,
            workflowRunId = WorkflowRunId("run"),
            projectId = ProjectId("project"),
            objective = "Ship",
            nowEpochMillis = 1L,
            taskRunIdFactory = { TaskRunId("run-${it.value}") },
        )

        assertIs<TaskExecutor.RoleAgent>(run.taskRuns.getValue(fixture.implement).executor)
        assertIs<TaskExecutor.GitHubAction>(run.taskRuns.getValue(fixture.verify).executor)
        assertIs<TaskExecutor.HumanApproval>(run.taskRuns.getValue(fixture.approve).executor)
        assertIs<TaskExecutor.Deployment>(run.taskRuns.getValue(fixture.deploy).executor)
        assertEquals(null, run.taskRuns.getValue(fixture.verify).assignedRoleId)
        assertEquals(null, run.taskRuns.getValue(fixture.deploy).assignedRoleId)
        assertEquals(TaskRunStatus.Ready, run.taskRuns.getValue(fixture.implement).status)
        assertEquals(TaskRunStatus.Blocked, run.taskRuns.getValue(fixture.verify).status)
    }

    @Test
    fun liveRuntimeAdvancesAgentActionApprovalAndDeploymentToCompletion() = runBlocking {
        val fixture = definition()
        val project = Project(
            id = ProjectId("project"),
            name = "Project",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        val initialRun = WorkflowRunFactory.create(
            definition = fixture.definition,
            workflowRunId = WorkflowRunId("run-live"),
            projectId = project.id,
            objective = "Ship",
            nowEpochMillis = 1L,
            taskRunIdFactory = { TaskRunId("live-${it.value}") },
        )
        val gateway = CompletingMixedGateway()
        val integration = CompletingSystemIntegration()
        val persistence = InMemoryWorkflowPersistence()
        val engine = WorkflowEngine(gateway, BuiltInRoles.all)
        val coordinator = WorkflowRuntimeCoordinator(
            persistence = persistence,
            engine = engine,
            sessionGateway = gateway,
            executorIntegrations = TaskExecutorIntegrationRegistry(listOf(integration)),
        )
        var state = WorkflowRuntimeState(initialRun)

        state = coordinator.cycle(project, fixture.definition, state, 10L, ::artifactId)
        assertEquals(TaskRunStatus.Running, state.run.taskRuns.getValue(fixture.implement).status)

        state = coordinator.cycle(project, fixture.definition, state, 20L, ::artifactId)
        assertEquals(TaskRunStatus.Completed, state.run.taskRuns.getValue(fixture.implement).status)
        assertEquals(TaskRunStatus.Running, state.run.taskRuns.getValue(fixture.verify).status)

        state = coordinator.cycle(project, fixture.definition, state, 30L, ::artifactId)
        assertEquals(TaskRunStatus.Completed, state.run.taskRuns.getValue(fixture.verify).status)
        assertEquals(TaskRunStatus.AwaitingApproval, state.run.taskRuns.getValue(fixture.approve).status)
        assertEquals(WorkflowRunStatus.AwaitingHuman, state.run.status)

        val approvedRun = engine.completeHumanApprovalTask(
            definition = fixture.definition,
            run = state.run,
            taskDefinitionId = fixture.approve,
            nowEpochMillis = 40L,
        )
        state = WorkflowRuntimeState(approvedRun, state.handles)
        assertEquals(TaskRunStatus.Ready, state.run.taskRuns.getValue(fixture.deploy).status)

        state = coordinator.cycle(project, fixture.definition, state, 50L, ::artifactId)
        assertEquals(TaskRunStatus.Running, state.run.taskRuns.getValue(fixture.deploy).status)

        state = coordinator.cycle(project, fixture.definition, state, 60L, ::artifactId)
        assertEquals(TaskRunStatus.Completed, state.run.taskRuns.getValue(fixture.deploy).status)
        assertEquals(WorkflowRunStatus.Completed, state.run.status)
        assertEquals(1, integration.githubDispatches)
        assertEquals(1, integration.deploymentDispatches)
        assertEquals(WorkflowRunStatus.Completed, persistence.runs.get(state.run.id)?.status)
    }

    @Test
    fun validatorRejectsTaskWithNeitherResponsibilityNorExecutor() {
        val definition = WorkflowDefinition(
            id = WorkflowDefinitionId("invalid"),
            name = "Invalid",
            tasks = listOf(
                TaskDefinition(
                    id = TaskDefinitionId("orphan"),
                    name = "Orphan",
                    objective = "Cannot execute",
                    roleId = null,
                ),
            ),
        )

        assertIs<WorkflowValidationError.MissingExecutor>(WorkflowGraphValidator.validate(definition).single())
    }

    private fun definition(): MixedDefinition {
        val implement = TaskDefinitionId("implement")
        val verify = TaskDefinitionId("verify")
        val approve = TaskDefinitionId("approve")
        val deploy = TaskDefinitionId("deploy")
        return MixedDefinition(
            WorkflowDefinition(
                id = WorkflowDefinitionId("mixed"),
                name = "Mixed executor workflow",
                tasks = listOf(
                    TaskDefinition(
                        id = implement,
                        name = "Implement",
                        objective = "Implement the change",
                        roleId = BuiltInRoles.ImplementationEngineer.id,
                        executor = TaskExecutor.RoleAgent(BuiltInRoles.ImplementationEngineer.id),
                    ),
                    TaskDefinition(
                        id = verify,
                        name = "CI",
                        objective = "Run repository CI",
                        roleId = null,
                        executor = TaskExecutor.GitHubAction("ci.yml"),
                        dependsOn = setOf(implement),
                    ),
                    TaskDefinition(
                        id = approve,
                        name = "Release approval",
                        objective = "Approve release evidence",
                        roleId = BuiltInRoles.ReleaseEngineer.id,
                        executor = TaskExecutor.HumanApproval("Approve release"),
                        dependsOn = setOf(verify),
                    ),
                    TaskDefinition(
                        id = deploy,
                        name = "Deploy",
                        objective = "Deploy the verified build",
                        roleId = null,
                        executor = TaskExecutor.Deployment("production"),
                        dependsOn = setOf(approve),
                    ),
                ),
            ),
            implement,
            verify,
            approve,
            deploy,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun artifactId(taskRun: TaskRun, artifact: ProviderArtifact, index: Int): ArtifactId =
        ArtifactId("${taskRun.id.value}-$index")

    private data class MixedDefinition(
        val definition: WorkflowDefinition,
        val implement: TaskDefinitionId,
        val verify: TaskDefinitionId,
        val approve: TaskDefinitionId,
        val deploy: TaskDefinitionId,
    )
}

private class CompletingMixedGateway : ManagedSessionGateway {
    override suspend fun resolveProvider(selection: ProviderSelectionRequest) = AgentProviderId("provider")

    override suspend fun createSession(request: ManagedSessionRequest) = ManagedSessionHandle(
        taskRunId = request.taskRequest.taskRunId,
        providerId = AgentProviderId("provider"),
        providerRunId = ProviderRunId("provider-run"),
    )

    override suspend fun status(handle: ManagedSessionHandle) = ManagedSessionStatus.Completed

    override suspend fun message(handle: ManagedSessionHandle, message: String) = ProviderActionResult.Accepted

    override suspend fun approvePlan(handle: ManagedSessionHandle) = ProviderActionResult.Accepted

    override suspend fun artifacts(handle: ManagedSessionHandle): List<ProviderArtifact> = emptyList()
}

private class CompletingSystemIntegration : TaskExecutorIntegration {
    var githubDispatches = 0
    var deploymentDispatches = 0

    override fun supports(executor: TaskExecutor): Boolean =
        executor is TaskExecutor.GitHubAction || executor is TaskExecutor.Deployment

    override suspend fun dispatch(context: TaskExecutorContext): TaskExecutorExecution {
        val id = when (context.executor) {
            is TaskExecutor.GitHubAction -> {
                githubDispatches += 1
                "github-run"
            }
            is TaskExecutor.Deployment -> {
                deploymentDispatches += 1
                "deploy-run"
            }
            else -> error("Unsupported executor")
        }
        return TaskExecutorExecution(TaskRunStatus.Running, externalRunId = id)
    }

    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution =
        TaskExecutorExecution(
            status = TaskRunStatus.Completed,
            externalRunId = context.taskRun.externalRunId,
            progress = 1f,
        )
}
