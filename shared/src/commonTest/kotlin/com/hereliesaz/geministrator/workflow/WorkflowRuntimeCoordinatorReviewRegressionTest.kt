package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.ApprovalPolicy
import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.EscalationPolicy
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.ProviderRunId
import com.hereliesaz.geministrator.domain.RetryPolicy
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
import com.hereliesaz.geministrator.events.ApprovalDecisionReceived
import com.hereliesaz.geministrator.persistence.InMemoryWorkflowPersistence
import com.hereliesaz.geministrator.persistence.WorkflowPersistence
import com.hereliesaz.geministrator.providers.ProviderActionResult
import com.hereliesaz.geministrator.providers.ProviderArtifact
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkflowRuntimeCoordinatorReviewRegressionTest {
    @Test
    fun idleAwaitingHumanRepairsMissingFailureEscalationGate() = runBlocking {
        val taskId = TaskDefinitionId("escalated")
        val executor = TaskExecutor.TestRunner("verify")
        val definition = definition(
            TaskDefinition(
                id = taskId,
                name = "Escalated",
                objective = "Needs a decision",
                roleId = null,
                executor = executor,
                retryPolicy = RetryPolicy(maxAttempts = 1),
                escalationPolicy = EscalationPolicy.RequireHumanDecision,
            ),
        )
        val run = run(
            definition,
            WorkflowRunStatus.AwaitingHuman,
            mapOf(taskId to taskRun(taskId, TaskRunStatus.Escalated, executor)),
        )
        val fixture = fixture()

        val result = fixture.coordinator.cycle(
            project = project(),
            definition = definition,
            state = WorkflowRuntimeState(run),
            nowEpochMillis = 20L,
            artifactIdFactory = ::artifactId,
        )

        assertEquals(run, result.run)
        val gate = fixture.persistence.approvalGates.get(
            ApprovalGateId("failure:${run.id.value}:${taskId.value}:1"),
        )
        assertNotNull(gate)
        assertEquals(ApprovalGateStatus.Pending, gate.status)
        assertEquals(ApprovalGateKind.FailureEscalation, gate.kind)
    }

    @Test
    fun idleAwaitingHumanRecoversIndependentSystemExecutorBeforeReturning() = runBlocking {
        val approvalId = TaskDefinitionId("approval")
        val systemId = TaskDefinitionId("system")
        val approvalExecutor = TaskExecutor.HumanApproval("Approve")
        val systemExecutor = TaskExecutor.TestRunner("verify")
        val definition = definition(
            TaskDefinition(
                id = approvalId,
                name = "Approval",
                objective = "Approve",
                roleId = null,
                executor = approvalExecutor,
                approvalPolicy = ApprovalPolicy.HumanApproval,
            ),
            TaskDefinition(
                id = systemId,
                name = "System",
                objective = "Run independently",
                roleId = null,
                executor = systemExecutor,
            ),
        )
        val run = run(
            definition,
            WorkflowRunStatus.AwaitingHuman,
            linkedMapOf(
                approvalId to taskRun(approvalId, TaskRunStatus.AwaitingApproval, approvalExecutor),
                systemId to taskRun(systemId, TaskRunStatus.Blocked, systemExecutor).copy(
                    blockingReason = com.hereliesaz.geministrator.domain.BlockingReason(
                        WorkflowRunFactory.EXECUTOR_INTEGRATION_UNAVAILABLE,
                        "Unavailable",
                    ),
                ),
            ),
        )
        val integration = IdlessSystemIntegration(dispatchStatus = TaskRunStatus.Completed)
        val fixture = fixture(TaskExecutorIntegrationRegistry(listOf(integration)))

        val result = fixture.coordinator.cycle(
            project = project(),
            definition = definition,
            state = WorkflowRuntimeState(run),
            nowEpochMillis = 20L,
            artifactIdFactory = ::artifactId,
        )

        assertEquals(1, integration.dispatchCount)
        assertEquals(TaskRunStatus.Completed, result.run.taskRuns.getValue(systemId).status)
        assertEquals(TaskRunStatus.AwaitingApproval, result.run.taskRuns.getValue(approvalId).status)
        assertEquals(WorkflowRunStatus.AwaitingHuman, result.run.status)
    }

    @Test
    fun idlessRunningSystemExecutorIsReconciled() = runBlocking {
        val taskId = TaskDefinitionId("idless")
        val executor = TaskExecutor.TestRunner("verify")
        val definition = definition(
            TaskDefinition(
                id = taskId,
                name = "ID-less executor",
                objective = "Finish locally",
                roleId = null,
                executor = executor,
            ),
        )
        val run = run(
            definition,
            WorkflowRunStatus.Running,
            mapOf(taskId to taskRun(taskId, TaskRunStatus.Running, executor)),
        )
        val integration = IdlessSystemIntegration(reconcileStatus = TaskRunStatus.Completed)
        val fixture = fixture(TaskExecutorIntegrationRegistry(listOf(integration)))

        val result = fixture.coordinator.cycle(
            project = project(),
            definition = definition,
            state = WorkflowRuntimeState(run),
            nowEpochMillis = 20L,
            artifactIdFactory = ::artifactId,
        )

        assertEquals(1, integration.reconcileCount)
        assertEquals(TaskRunStatus.Completed, result.run.taskRuns.getValue(taskId).status)
        assertEquals(WorkflowRunStatus.Completed, result.run.status)
    }

    @Test
    fun terminalSiblingFailureRejectsAndCancelsPriorEscalation() = runBlocking {
        val escalatedId = TaskDefinitionId("escalate-first")
        val terminalId = TaskDefinitionId("fail-second")
        val executor = TaskExecutor.TestRunner("verify")
        val definition = definition(
            TaskDefinition(
                id = escalatedId,
                name = "Escalate",
                objective = "Escalate on failure",
                roleId = null,
                executor = executor,
                retryPolicy = RetryPolicy(maxAttempts = 1),
                escalationPolicy = EscalationPolicy.RequireHumanDecision,
            ),
            TaskDefinition(
                id = terminalId,
                name = "Fail",
                objective = "Fail workflow",
                roleId = null,
                executor = executor,
                retryPolicy = RetryPolicy(maxAttempts = 1),
                escalationPolicy = EscalationPolicy.FailWorkflow,
            ),
        )
        val run = run(
            definition,
            WorkflowRunStatus.Running,
            linkedMapOf(
                escalatedId to taskRun(escalatedId, TaskRunStatus.Running, executor),
                terminalId to taskRun(terminalId, TaskRunStatus.Running, executor),
            ),
        )
        val integration = IdlessSystemIntegration(reconcileStatus = TaskRunStatus.Failed)
        val fixture = fixture(TaskExecutorIntegrationRegistry(listOf(integration)))

        val result = fixture.coordinator.cycle(
            project = project(),
            definition = definition,
            state = WorkflowRuntimeState(run),
            nowEpochMillis = 20L,
            artifactIdFactory = ::artifactId,
        )

        assertEquals(WorkflowRunStatus.Failed, result.run.status)
        assertEquals(TaskRunStatus.Cancelled, result.run.taskRuns.getValue(escalatedId).status)
        assertEquals(TaskRunStatus.Failed, result.run.taskRuns.getValue(terminalId).status)
        val gateId = ApprovalGateId("failure:${run.id.value}:${escalatedId.value}:1")
        assertEquals(ApprovalGateStatus.Rejected, fixture.persistence.approvalGates.get(gateId)?.status)
        assertTrue(
            fixture.persistence.approvalGates.unresolved(run.id)
                .none { it.kind == ApprovalGateKind.FailureEscalation },
        )
        val decisions = fixture.persistence.events.forRun(run.id)
            .filterIsInstance<ApprovalDecisionReceived>()
        assertEquals(1, decisions.size)
        assertEquals(false, decisions.single().approved)
    }

    @Test
    fun humanEscalationDecisionWinsAtomicRaceWithTerminalSiblingCleanup() = runBlocking {
        val escalatedId = TaskDefinitionId("escalate-first")
        val terminalId = TaskDefinitionId("fail-second")
        val executor = TaskExecutor.TestRunner("verify")
        val definition = definition(
            TaskDefinition(
                id = escalatedId,
                name = "Escalate",
                objective = "Escalate on failure",
                roleId = null,
                executor = executor,
                retryPolicy = RetryPolicy(maxAttempts = 1),
                escalationPolicy = EscalationPolicy.RequireHumanDecision,
            ),
            TaskDefinition(
                id = terminalId,
                name = "Fail",
                objective = "Fail workflow",
                roleId = null,
                executor = executor,
                retryPolicy = RetryPolicy(maxAttempts = 1),
                escalationPolicy = EscalationPolicy.FailWorkflow,
            ),
        )
        val run = run(
            definition,
            WorkflowRunStatus.Running,
            linkedMapOf(
                escalatedId to taskRun(escalatedId, TaskRunStatus.Running, executor),
                terminalId to taskRun(terminalId, TaskRunStatus.Running, executor),
            ),
        )
        val persistence = HumanDecisionWinsPersistence()
        val integration = IdlessSystemIntegration(reconcileStatus = TaskRunStatus.Failed)
        val fixture = fixture(
            integrations = TaskExecutorIntegrationRegistry(listOf(integration)),
            persistence = persistence,
        )

        val result = fixture.coordinator.cycle(
            project = project(),
            definition = definition,
            state = WorkflowRuntimeState(run),
            nowEpochMillis = 20L,
            artifactIdFactory = ::artifactId,
        )

        val gateId = ApprovalGateId("failure:${run.id.value}:${escalatedId.value}:1")
        assertEquals(ApprovalGateStatus.Approved, persistence.approvalGates.get(gateId)?.status)
        assertEquals(WorkflowRunStatus.Running, result.run.status)
        assertEquals(TaskRunStatus.Retrying, result.run.taskRuns.getValue(escalatedId).status)
        val decisions = persistence.events.forRun(run.id).filterIsInstance<ApprovalDecisionReceived>()
        assertEquals(1, decisions.size)
        assertTrue(decisions.single().approved)
    }

    private fun fixture(
        integrations: TaskExecutorIntegrationRegistry = TaskExecutorIntegrationRegistry.Empty,
        persistence: WorkflowPersistence = InMemoryWorkflowPersistence(),
    ): Fixture {
        val gateway = ReviewNoopGateway()
        val engine = WorkflowEngine(gateway, emptyList())
        return Fixture(
            persistence,
            WorkflowRuntimeCoordinator(persistence, engine, gateway, integrations),
        )
    }

    private fun project() = Project(
        id = ProjectId("project"),
        name = "Project",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun definition(vararg tasks: TaskDefinition) = WorkflowDefinition(
        id = WorkflowDefinitionId("review-regressions"),
        name = "Review regressions",
        tasks = tasks.toList(),
        testDesignPolicy = TestDesignPolicy.None,
    )

    private fun run(
        definition: WorkflowDefinition,
        status: WorkflowRunStatus,
        taskRuns: Map<TaskDefinitionId, TaskRun>,
    ) = WorkflowRun(
        id = WorkflowRunId("review-run"),
        projectId = ProjectId("project"),
        workflowDefinitionId = definition.id,
        objective = "Ship",
        status = status,
        taskRuns = taskRuns,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun taskRun(
        taskId: TaskDefinitionId,
        status: TaskRunStatus,
        executor: TaskExecutor,
    ) = TaskRun(
        id = TaskRunId("task-run-${taskId.value}"),
        taskDefinitionId = taskId,
        status = status,
        assignedRoleId = null,
        executor = executor,
    )

    @Suppress("UNUSED_PARAMETER")
    private fun artifactId(
        taskRun: TaskRun,
        artifact: ProviderArtifact,
        index: Int,
    ) = ArtifactId("${taskRun.id.value}:${index}")

    private data class Fixture(
        val persistence: WorkflowPersistence,
        val coordinator: WorkflowRuntimeCoordinator,
    )
}

private class ReviewNoopGateway : ManagedSessionGateway {
    override suspend fun resolveProvider(selection: ProviderSelectionRequest) = AgentProviderId("provider")

    override suspend fun createSession(request: ManagedSessionRequest) = ManagedSessionHandle(
        request.taskRequest.taskRunId,
        AgentProviderId("provider"),
        ProviderRunId("provider-run"),
    )

    override suspend fun status(handle: ManagedSessionHandle) = ManagedSessionStatus.Unknown

    override suspend fun message(
        handle: ManagedSessionHandle,
        message: String,
    ) = ProviderActionResult.Accepted

    override suspend fun approvePlan(handle: ManagedSessionHandle) = ProviderActionResult.Accepted

    override suspend fun artifacts(handle: ManagedSessionHandle): List<ProviderArtifact> = emptyList()
}

private class IdlessSystemIntegration(
    private val dispatchStatus: TaskRunStatus = TaskRunStatus.Running,
    private val reconcileStatus: TaskRunStatus = TaskRunStatus.Completed,
) : TaskExecutorIntegration {
    var dispatchCount = 0
    var reconcileCount = 0

    override fun supports(executor: TaskExecutor): Boolean = executor is TaskExecutor.TestRunner

    override suspend fun dispatch(context: TaskExecutorContext): TaskExecutorExecution {
        dispatchCount += 1
        return TaskExecutorExecution(
            status = dispatchStatus,
            externalRunId = null,
            progressMessage = "dispatched without external ID",
        )
    }

    override suspend fun reconcile(context: TaskExecutorContext): TaskExecutorExecution {
        reconcileCount += 1
        return TaskExecutorExecution(
            status = reconcileStatus,
            externalRunId = null,
            progressMessage = "reconciled without external ID",
        )
    }
}

private class HumanDecisionWinsPersistence(
    private val delegate: InMemoryWorkflowPersistence = InMemoryWorkflowPersistence(),
) : WorkflowPersistence {
    private var injectedDecision = false

    override val projects = delegate.projects
    override val definitions = delegate.definitions
    override val runs = delegate.runs
    override val events = delegate.events
    override val roles = delegate.roles
    override val artifacts = delegate.artifacts
    override val approvalGates = delegate.approvalGates

    override suspend fun commitFailureEscalationDecision(
        commit: FailureEscalationDecisionCommit,
    ): Boolean {
        if (!injectedDecision) {
            injectedDecision = true
            val gate = requireNotNull(delegate.approvalGates.get(commit.expectedGateId))
            val taskId = requireNotNull(gate.taskDefinitionId)
            val terminalTask = requireNotNull(commit.nextRun.taskRuns[taskId])
            val humanRun = commit.nextRun.copy(
                status = WorkflowRunStatus.Running,
                taskRuns = commit.nextRun.taskRuns + (
                    taskId to terminalTask.copy(
                        status = TaskRunStatus.Retrying,
                        attempt = terminalTask.attempt + 1,
                        blockingReason = null,
                        progress = null,
                        progressMessage = "Human approved retry",
                    )
                ),
                updatedAtEpochMillis = commit.nextRun.updatedAtEpochMillis + 1,
            )
            val humanEvent = ApprovalDecisionReceived(
                workflowRunId = commit.nextRun.id,
                taskDefinitionId = taskId,
                gateId = gate.id,
                approved = true,
                decidedByRoleId = null,
                occurredAtEpochMillis = commit.nextRun.updatedAtEpochMillis + 1,
            )
            check(
                delegate.commitFailureEscalationDecision(
                    FailureEscalationDecisionCommit(
                        expectedGateId = gate.id,
                        decidedGate = gate.approve(null, "Human approved retry", humanEvent.occurredAtEpochMillis),
                        nextRun = humanRun,
                        decisionEvent = humanEvent,
                    ),
                ),
            )
        }
        return delegate.commitFailureEscalationDecision(commit)
    }
}
