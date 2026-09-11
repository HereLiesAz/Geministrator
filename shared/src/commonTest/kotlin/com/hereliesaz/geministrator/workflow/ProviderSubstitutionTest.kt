package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.ArtifactKind
import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.ProviderConstraints
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
import kotlin.test.assertNotEquals

/** Two distinct providers satisfy the same role/task contract with identical observable outcomes. */
class ProviderSubstitutionTest {

    private val providerAlpha = AgentProviderId("alpha")
    private val providerBeta = AgentProviderId("beta")

    private val project = Project(
        id = ProjectId("proj"),
        name = "Substitution project",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private val taskId = TaskDefinitionId("work")

    private val definition = WorkflowDefinition(
        id = WorkflowDefinitionId("sub-def"),
        name = "Substitution workflow",
        tasks = listOf(
            TaskDefinition(
                id = taskId,
                name = "Work",
                objective = "Perform the work",
                roleId = BuiltInRoles.ImplementationEngineer.id,
                executor = TaskExecutor.RoleAgent(BuiltInRoles.ImplementationEngineer.id),
            ),
        ),
    )

    @Test
    fun alphaAndBetaProvidersReachIdenticalTerminalOutcome() = runBlocking {
        val alphaResult = runWithProvider(providerAlpha)
        val betaResult = runWithProvider(providerBeta)

        assertEquals(WorkflowRunStatus.Completed, alphaResult.run.status)
        assertEquals(WorkflowRunStatus.Completed, betaResult.run.status)
        assertEquals(TaskRunStatus.Completed, alphaResult.run.taskRuns.getValue(taskId).status)
        assertEquals(TaskRunStatus.Completed, betaResult.run.taskRuns.getValue(taskId).status)
        assertNotEquals(
            alphaResult.run.taskRuns.getValue(taskId).assignedProviderId,
            betaResult.run.taskRuns.getValue(taskId).assignedProviderId,
            "Each run should be assigned to the requested provider",
        )
        assertEquals(
            alphaResult.run.taskRuns.getValue(taskId).artifacts.map { it.kind },
            betaResult.run.taskRuns.getValue(taskId).artifacts.map { it.kind },
            "Artifact shape is identical regardless of provider",
        )
    }

    @Test
    fun providerConstraintChannelsDispatchToTheNamedProvider() = runBlocking {
        val constrainedDefinition = definition.copy(
            tasks = definition.tasks.map { task ->
                task.copy(providerConstraints = ProviderConstraints.RequireProvider(providerBeta))
            },
        )
        val gateway = StubTwoProviderGateway(providerAlpha, providerBeta)
        val coordinator = WorkflowRuntimeCoordinator(
            persistence = InMemoryWorkflowPersistence(),
            engine = WorkflowEngine(gateway, BuiltInRoles.all),
            sessionGateway = gateway,
            executorIntegrations = TaskExecutorIntegrationRegistry.Empty,
        )
        val run = WorkflowRunFactory.create(
            definition = constrainedDefinition,
            workflowRunId = WorkflowRunId("constrained"),
            projectId = project.id,
            objective = "test",
            nowEpochMillis = 1L,
            taskRunIdFactory = { TaskRunId("c-${it.value}") },
        )

        var state = WorkflowRuntimeState(run)
        state = coordinator.cycle(project, constrainedDefinition, state, 10L, ::id)
        state = coordinator.cycle(project, constrainedDefinition, state, 20L, ::id)

        assertEquals(TaskRunStatus.Completed, state.run.taskRuns.getValue(taskId).status)
        assertEquals(providerBeta, state.run.taskRuns.getValue(taskId).assignedProviderId)
    }

    private suspend fun runWithProvider(preferredProviderId: AgentProviderId): WorkflowRuntimeState {
        val gateway = StubTwoProviderGateway(providerAlpha, providerBeta, preferred = preferredProviderId)
        val coordinator = WorkflowRuntimeCoordinator(
            persistence = InMemoryWorkflowPersistence(),
            engine = WorkflowEngine(gateway, BuiltInRoles.all),
            sessionGateway = gateway,
            executorIntegrations = TaskExecutorIntegrationRegistry.Empty,
        )
        val seededDef = definition.copy(
            tasks = definition.tasks.map { task ->
                task.copy(
                    providerConstraints = ProviderConstraints.RequireProvider(preferredProviderId),
                )
            },
        )
        val run = WorkflowRunFactory.create(
            definition = seededDef,
            workflowRunId = WorkflowRunId("run-${preferredProviderId.value}"),
            projectId = project.id,
            objective = "test",
            nowEpochMillis = 1L,
            taskRunIdFactory = { TaskRunId("${preferredProviderId.value}-${it.value}") },
        )
        var state = WorkflowRuntimeState(run)
        state = coordinator.cycle(project, seededDef, state, 10L, ::id)
        state = coordinator.cycle(project, seededDef, state, 20L, ::id)
        return state
    }

    @Suppress("UNUSED_PARAMETER")
    private fun id(taskRun: TaskRun, artifact: ProviderArtifact, index: Int): ArtifactId =
        ArtifactId("${taskRun.id.value}-$index")
}

private class StubTwoProviderGateway(
    private val alpha: AgentProviderId,
    private val beta: AgentProviderId,
    private val preferred: AgentProviderId? = null,
) : ManagedSessionGateway {

    override suspend fun resolveProvider(selection: ProviderSelectionRequest): AgentProviderId {
        val constraint = selection.constraints
        if (constraint is ProviderConstraints.RequireProvider) return constraint.providerId
        return preferred ?: alpha
    }

    override suspend fun createSession(request: ManagedSessionRequest): ManagedSessionHandle {
        val providerId = resolveProvider(request.providerSelection)
        return ManagedSessionHandle(
            taskRunId = request.taskRequest.taskRunId,
            providerId = providerId,
            providerRunId = ProviderRunId("${providerId.value}-run"),
        )
    }

    override suspend fun status(handle: ManagedSessionHandle): ManagedSessionStatus =
        ManagedSessionStatus.Completed

    override suspend fun message(handle: ManagedSessionHandle, message: String) = ProviderActionResult.Accepted

    override suspend fun approvePlan(handle: ManagedSessionHandle) = ProviderActionResult.Accepted

    override suspend fun artifacts(handle: ManagedSessionHandle): List<ProviderArtifact> = listOf(
        ProviderArtifact(
            kind = ArtifactKind.CodeChange,
            label = "Change from ${handle.providerId.value}",
            textContent = "Synthetic code change from ${handle.providerId.value}",
        ),
    )
}
