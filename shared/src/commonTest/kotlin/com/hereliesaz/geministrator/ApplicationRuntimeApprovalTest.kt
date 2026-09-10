package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.domain.AgentCapability
import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.ApprovalPolicy
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
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.events.ApprovalDecisionReceived
import com.hereliesaz.geministrator.persistence.InMemoryWorkflowPersistence
import com.hereliesaz.geministrator.providers.AgentCapabilities
import com.hereliesaz.geministrator.providers.AgentEvent
import com.hereliesaz.geministrator.providers.AgentProvider
import com.hereliesaz.geministrator.providers.AgentRunHandle
import com.hereliesaz.geministrator.providers.AgentTaskRequest
import com.hereliesaz.geministrator.providers.ProviderActionResult
import com.hereliesaz.geministrator.workflow.ApprovalGateStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApplicationRuntimeApprovalTest {
    @Test
    fun humanApprovalCompletesGatePersistsAndPublishes() = runBlocking {
        val persistence = InMemoryWorkflowPersistence()
        val project = project()
        val taskId = TaskDefinitionId("approve")
        val executor = TaskExecutor.HumanApproval("Approve release")
        val definition = WorkflowDefinition(
            id = WorkflowDefinitionId("definition"),
            name = "Release",
            tasks = listOf(
                TaskDefinition(
                    id = taskId,
                    name = "Release approval",
                    objective = "Approve release",
                    roleId = null,
                    executor = executor,
                ),
            ),
        )
        val run = WorkflowRun(
            id = WorkflowRunId("run"),
            projectId = project.id,
            workflowDefinitionId = definition.id,
            objective = "Ship",
            status = WorkflowRunStatus.AwaitingHuman,
            taskRuns = mapOf(
                taskId to TaskRun(
                    id = TaskRunId("task-run"),
                    taskDefinitionId = taskId,
                    status = TaskRunStatus.AwaitingApproval,
                    assignedRoleId = null,
                    executor = executor,
                ),
            ),
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        persistence.projects.put(project)
        persistence.definitions.put(definition)
        persistence.runs.put(run)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        try {
            val runtime = ApplicationRuntime.create(
                providers = emptyList(),
                scope = scope,
                persistence = persistence,
            )

            runtime.approveTask(taskId)

            val live = assertIs<ApplicationRuntimeState.Live>(runtime.state.value)
            assertEquals(WorkflowRunStatus.Completed, live.presentation.run.status)
            assertEquals(TaskRunStatus.Completed, live.presentation.run.taskRuns.getValue(taskId).status)
            assertEquals(WorkflowRunStatus.Completed, persistence.runs.get(run.id)?.status)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun providerPlanApprovalUsesDurableGateResumesSameSessionPersistsAndPublishes() = runBlocking {
        val fixture = providerFixture(ApprovalProvider())
        try {
            fixture.runtime.approveTask(fixture.taskId)

            assertTrue(fixture.provider.approved)
            val live = assertIs<ApplicationRuntimeState.Live>(fixture.runtime.state.value)
            assertEquals(WorkflowRunStatus.Running, live.presentation.run.status)
            assertEquals(TaskRunStatus.Running, live.presentation.run.taskRuns.getValue(fixture.taskId).status)
            assertEquals(
                ProviderRunId("remote-run"),
                live.presentation.run.taskRuns.getValue(fixture.taskId).providerRunId,
            )
            assertEquals(
                TaskRunStatus.Running,
                fixture.persistence.runs.get(fixture.run.id)?.taskRuns?.get(fixture.taskId)?.status,
            )

            val gateId = planGateId(fixture.run, fixture.taskId)
            assertEquals(
                ApprovalGateStatus.Approved,
                fixture.persistence.approvalGates.get(gateId)?.status,
            )
            val decisions = fixture.persistence.events.forRun(fixture.run.id)
                .filterIsInstance<ApprovalDecisionReceived>()
            assertEquals(1, decisions.size)
            assertTrue(decisions.single().approved)
            assertEquals(gateId, decisions.single().gateId)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun providerCompletingDuringApprovalPassesThroughApprovedGateBeforeCompletion() = runBlocking {
        val fixture = providerFixture(ApprovalProvider(completeDuringApproval = true))
        try {
            fixture.runtime.approveTask(fixture.taskId)

            val live = assertIs<ApplicationRuntimeState.Live>(fixture.runtime.state.value)
            assertEquals(WorkflowRunStatus.Completed, live.presentation.run.status)
            assertEquals(
                TaskRunStatus.Completed,
                live.presentation.run.taskRuns.getValue(fixture.taskId).status,
            )
            assertEquals(
                WorkflowRunStatus.Completed,
                fixture.persistence.runs.get(fixture.run.id)?.status,
            )
            val gateId = planGateId(fixture.run, fixture.taskId)
            assertEquals(
                ApprovalGateStatus.Approved,
                fixture.persistence.approvalGates.get(gateId)?.status,
            )
        } finally {
            fixture.scope.cancel()
        }
    }

    private suspend fun providerFixture(provider: ApprovalProvider): ProviderFixture {
        val persistence = InMemoryWorkflowPersistence()
        val project = project()
        val taskId = TaskDefinitionId("implement")
        val executor = TaskExecutor.RoleAgent(BuiltInRoles.ImplementationEngineer.id)
        val definition = WorkflowDefinition(
            id = WorkflowDefinitionId("provider-definition"),
            name = "Implementation",
            tasks = listOf(
                TaskDefinition(
                    id = taskId,
                    name = "Implement",
                    objective = "Implement change",
                    roleId = BuiltInRoles.ImplementationEngineer.id,
                    executor = executor,
                    approvalPolicy = ApprovalPolicy.HumanApproval,
                ),
            ),
        )
        val run = WorkflowRun(
            id = WorkflowRunId("provider-run"),
            projectId = project.id,
            workflowDefinitionId = definition.id,
            objective = "Ship",
            status = WorkflowRunStatus.AwaitingHuman,
            taskRuns = mapOf(
                taskId to TaskRun(
                    id = TaskRunId("provider-task-run"),
                    taskDefinitionId = taskId,
                    status = TaskRunStatus.AwaitingApproval,
                    assignedRoleId = BuiltInRoles.ImplementationEngineer.id,
                    assignedProviderId = provider.id,
                    providerRunId = ProviderRunId("remote-run"),
                    executor = executor,
                ),
            ),
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        persistence.projects.put(project)
        persistence.definitions.put(definition)
        persistence.runs.put(run)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val runtime = ApplicationRuntime.create(
            providers = listOf(provider),
            scope = scope,
            persistence = persistence,
        )
        return ProviderFixture(
            persistence = persistence,
            provider = provider,
            project = project,
            definition = definition,
            run = run,
            taskId = taskId,
            scope = scope,
            runtime = runtime,
        )
    }

    private fun planGateId(run: WorkflowRun, taskId: TaskDefinitionId) = ApprovalGateId(
        "plan:${run.id.value}:${taskId.value}:${run.taskRuns.getValue(taskId).attempt}",
    )

    private fun project() = Project(
        id = ProjectId("project"),
        name = "Project",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private data class ProviderFixture(
        val persistence: InMemoryWorkflowPersistence,
        val provider: ApprovalProvider,
        val project: Project,
        val definition: WorkflowDefinition,
        val run: WorkflowRun,
        val taskId: TaskDefinitionId,
        val scope: CoroutineScope,
        val runtime: ApplicationRuntime,
    )
}

private class ApprovalProvider(
    private val completeDuringApproval: Boolean = false,
) : AgentProvider {
    override val id = AgentProviderId("approval-provider")
    var approved = false

    override suspend fun capabilities() = AgentCapabilities(
        supported = setOf(
            AgentCapability.RepositoryRead,
            AgentCapability.RepositoryWrite,
            AgentCapability.PlanApproval,
        ),
    )

    override suspend fun start(request: AgentTaskRequest): AgentRunHandle =
        AgentRunHandle(ProviderRunId("remote-run"))

    override fun observe(runId: ProviderRunId): Flow<AgentEvent> = if (completeDuringApproval) {
        flow {
            emit(AgentEvent.Completed(runId))
        }
    } else {
        emptyFlow()
    }

    override suspend fun sendMessage(
        runId: ProviderRunId,
        message: String,
    ) = ProviderActionResult.Accepted

    override suspend fun approvePlan(runId: ProviderRunId): ProviderActionResult {
        approved = true
        if (completeDuringApproval) delay(50L)
        return ProviderActionResult.Accepted
    }

    override suspend fun cancel(runId: ProviderRunId) = ProviderActionResult.Accepted
}
