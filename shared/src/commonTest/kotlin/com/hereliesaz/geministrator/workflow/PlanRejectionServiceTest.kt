package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.ProviderRunId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.events.NoOpWorkflowEventSink
import com.hereliesaz.geministrator.providers.ProviderActionResult
import com.hereliesaz.geministrator.providers.ProviderArtifact
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlanRejectionServiceTest {
    @Test
    fun rejectionClaimsGateCancelsProviderAndThenResolvesGate() = runBlocking {
        val repository = RecordingGateRepository(planGate())
        val gateway = RecordingGateway()
        val service = PlanRejectionService(
            gateRepository = repository,
            gateCoordinator = ApprovalGateCoordinator(repository, NoOpWorkflowEventSink),
            sessionGateway = gateway,
        )

        val rejected = service.rejectPlan(
            gateId = GATE_ID,
            handle = HANDLE,
            note = "Nope",
            nowEpochMillis = 10L,
        )

        assertEquals(1, gateway.cancelCalls)
        assertEquals(0, gateway.approveCalls)
        assertEquals(ApprovalGateStatus.Rejected, rejected.status)
        assertEquals("Nope", rejected.decisionNote)
        assertEquals(ApprovalGateStatus.Rejected, repository.get(GATE_ID)?.status)
    }

    @Test
    fun cancellationFailureLeavesDurableApplyingClaimForSafeRecovery() = runBlocking {
        val repository = RecordingGateRepository(planGate())
        val gateway = RecordingGateway(cancelResult = ProviderActionResult.Rejected("still running"))
        val service = PlanRejectionService(
            gateRepository = repository,
            gateCoordinator = ApprovalGateCoordinator(repository, NoOpWorkflowEventSink),
            sessionGateway = gateway,
        )

        assertFailsWith<IllegalStateException> {
            service.rejectPlan(
                gateId = GATE_ID,
                handle = HANDLE,
                note = "Reject it",
                nowEpochMillis = 10L,
            )
        }

        assertEquals(1, gateway.cancelCalls)
        assertEquals(ApprovalGateStatus.Applying, repository.get(GATE_ID)?.status)
        assertEquals("Reject it", repository.get(GATE_ID)?.decisionNote)
    }

    @Test
    fun applyingGateCannotBeContradictedBySecondManualRejection() = runBlocking {
        val repository = RecordingGateRepository(planGate().applying(ApprovalDecisionIntent.Reject, null, "decision in flight"))
        val gateway = RecordingGateway()
        val service = PlanRejectionService(
            gateRepository = repository,
            gateCoordinator = ApprovalGateCoordinator(repository, NoOpWorkflowEventSink),
            sessionGateway = gateway,
        )

        assertFailsWith<IllegalArgumentException> {
            service.rejectPlan(
                gateId = GATE_ID,
                handle = HANDLE,
                nowEpochMillis = 10L,
            )
        }

        assertEquals(0, gateway.cancelCalls)
        assertEquals(ApprovalGateStatus.Applying, repository.get(GATE_ID)?.status)
    }

    private fun planGate() = ApprovalGate(
        id = GATE_ID,
        workflowRunId = WorkflowRunId("run"),
        taskDefinitionId = TaskDefinitionId("implementation"),
        kind = ApprovalGateKind.PlanApproval,
        reason = "Review plan",
        requiresHuman = true,
        createdAtEpochMillis = 1L,
    )

    private companion object {
        val GATE_ID = ApprovalGateId("plan:run:implementation:0")
        val HANDLE = ManagedSessionHandle(
            taskRunId = TaskRunId("task-run"),
            providerId = AgentProviderId("provider"),
            providerRunId = ProviderRunId("provider-run"),
        )
    }
}

private class RecordingGateRepository(initial: ApprovalGate) : ApprovalGateRepository {
    private val gates = mutableMapOf(initial.id to initial)

    override suspend fun put(gate: ApprovalGate) {
        gates[gate.id] = gate
    }

    override suspend fun get(id: ApprovalGateId): ApprovalGate? = gates[id]

    override suspend fun unresolved(workflowRunId: WorkflowRunId): List<ApprovalGate> = gates.values.filter {
        it.workflowRunId == workflowRunId &&
            (it.status == ApprovalGateStatus.Pending || it.status == ApprovalGateStatus.Applying)
    }
}

private class RecordingGateway(
    private val cancelResult: ProviderActionResult = ProviderActionResult.Accepted,
) : ManagedSessionGateway {
    var cancelCalls = 0
    var approveCalls = 0

    override suspend fun resolveProvider(selection: ProviderSelectionRequest): AgentProviderId = AgentProviderId("provider")

    override suspend fun createSession(request: ManagedSessionRequest): ManagedSessionHandle = error("not used")

    override suspend fun status(handle: ManagedSessionHandle): ManagedSessionStatus = ManagedSessionStatus.AwaitingApproval

    override suspend fun message(handle: ManagedSessionHandle, message: String): ProviderActionResult = ProviderActionResult.Accepted

    override suspend fun approvePlan(handle: ManagedSessionHandle): ProviderActionResult {
        approveCalls += 1
        return ProviderActionResult.Accepted
    }

    override suspend fun cancel(handle: ManagedSessionHandle): ProviderActionResult {
        cancelCalls += 1
        return cancelResult
    }

    override suspend fun artifacts(handle: ManagedSessionHandle): List<ProviderArtifact> = emptyList()
}
