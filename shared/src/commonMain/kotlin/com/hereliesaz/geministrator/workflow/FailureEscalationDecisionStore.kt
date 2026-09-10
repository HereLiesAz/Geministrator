package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.events.ApprovalDecisionReceived

data class FailureEscalationDecisionCommit(
    val expectedGateId: ApprovalGateId,
    val decidedGate: ApprovalGate,
    val nextRun: WorkflowRun,
    val decisionEvent: ApprovalDecisionReceived,
)

fun interface FailureEscalationDecisionStore {
    /**
     * Atomically commits a pending failure-escalation gate decision, the corresponding workflow
     * run transition, and its audit event. Returns false if the gate is no longer pending.
     */
    suspend fun commitFailureEscalationDecision(
        commit: FailureEscalationDecisionCommit,
    ): Boolean
}
