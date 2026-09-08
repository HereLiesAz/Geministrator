package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.ProviderRunId
import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.events.ApprovalDecisionReceived
import com.hereliesaz.geministrator.events.InMemoryWorkflowEventSink
import com.hereliesaz.geministrator.providers.ProviderActionResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkflowApprovalServiceTest {
    @Test
    fun architectApprovalDrivesProviderPlanApprovalAndAuditEvent() = runBlocking {
        val taskId = TaskDefinitionId("task")
        val definition = WorkflowDefinition(
            id = WorkflowDefinitionId("wf"),
            name = "Workflow",
            tasks = listOf(
                TaskDefinition(
                    id = taskId,
                    name = "Task",
                    objective = "Do it",
                    roleId = BuiltInRoles.ImplementationEngineer.id,
                ),
            ),
        )
        val run = WorkflowRunFactory.create(
            definition = definition,
            workflowRunId = WorkflowRunId("run"),
            projectId = ProjectId("project"),
            objective = "Objective",
            nowEpochMillis = 0L,
            taskRunIdFactory = { TaskRunId("task-run") },
        )
        val repository = ApprovalMemoryRepository()
        val events = InMemoryWorkflowEventSink()
        val gateway = ApprovalGateway()
        val service = WorkflowApprovalService(
            gateRepository = repository,
            gateCoordinator = ApprovalGateCoordinator(repository, events),
            sessionGateway = gateway,
        )
        val gate = service.ensurePlanGate(
            run = run,
            taskDefinitionId = taskId,
            gateIdFactory = { ApprovalGateId("gate") },
            nowEpochMillis = 10L,
        )
        val decided = service.approvePlan(
            gateId = gate.id,
            handle = ManagedSessionHandle(
                taskRunId = TaskRunId("task-run"),
                providerId = com.hereliesaz.geministrator.domain.AgentProviderId("jules"),
                providerRunId = ProviderRunId("session"),
            ),
            decidedByRoleId = BuiltInRoles.Architect.id,
            note = "Plan is sound",
            nowEpochMillis = 20L,
        )

        assertEquals(ApprovalGateStatus.Approved, decided.status)
        assertTrue(gateway.approved)
        assertTrue(events.snapshot().any { it is ApprovalDecisionReceived && it.approved })
    }
}

private class ApprovalMemoryRepository : ApprovalGateRepository {
    private val values = mutableMapOf<ApprovalGateId, ApprovalGate>()
    override suspend fun put(gate: ApprovalGate) { values[gate.id] = gate }
    override suspend fun get(id: ApprovalGateId): ApprovalGate? = values[id]
    override suspend fun unresolved(workflowRunId: WorkflowRunId): List<ApprovalGate> =
        values.values.filter { it.workflowRunId == workflowRunId && it.status == ApprovalGateStatus.Pending }
}

private class ApprovalGateway : ManagedSessionGateway {
    var approved: Boolean = false
    override suspend fun createSession(request: ManagedSessionRequest): ManagedSessionHandle = error("not used")
    override suspend fun status(handle: ManagedSessionHandle): ManagedSessionStatus = ManagedSessionStatus.AwaitingApproval
    override suspend fun message(handle: ManagedSessionHandle, message: String): ProviderActionResult = ProviderActionResult.Accepted
    override suspend fun approvePlan(handle: ManagedSessionHandle): ProviderActionResult {
        approved = true
        return ProviderActionResult.Accepted
    }
    override suspend fun artifacts(handle: ManagedSessionHandle) = emptyList<com.hereliesaz.geministrator.providers.ProviderArtifact>()
}
