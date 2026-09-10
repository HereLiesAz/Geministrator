package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.RoleDefinitionId
import com.hereliesaz.geministrator.persistence.RepositoryWorkflowEventSink
import com.hereliesaz.geministrator.workflow.ApprovalGateCoordinator
import com.hereliesaz.geministrator.workflow.WorkflowApprovalService

/**
 * Resolves a durable failure-escalation gate from the application boundary and refreshes the
 * published runtime from persistence afterwards. Gate decision, run transition, and audit event
 * are committed atomically by WorkflowPersistence.
 */
suspend fun ApplicationRuntime.decideFailureEscalation(
    gateId: ApprovalGateId,
    approved: Boolean,
    decidedByRoleId: RoleDefinitionId? = null,
    note: String? = null,
    nowEpochMillis: Long,
) {
    val live = state.value as? ApplicationRuntimeState.Live
        ?: error("No active workflow is loaded")
    val presentation = live.presentation
    val gate = requireNotNull(persistence.approvalGates.get(gateId)) {
        "Approval gate ${gateId.value} does not exist"
    }
    require(gate.workflowRunId == presentation.run.id) {
        "Approval gate ${gateId.value} does not belong to the active workflow"
    }

    val service = WorkflowApprovalService(
        gateRepository = persistence.approvalGates,
        gateCoordinator = ApprovalGateCoordinator(
            repository = persistence.approvalGates,
            eventSink = RepositoryWorkflowEventSink(persistence.events),
        ),
        sessionGateway = sessionGateway,
        failureEscalationDecisionStore = persistence,
    )
    service.decideFailureEscalation(
        run = presentation.run,
        gateId = gateId,
        approved = approved,
        decidedByRoleId = decidedByRoleId,
        note = note,
        nowEpochMillis = nowEpochMillis,
    )
    refresh()
}
