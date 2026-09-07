package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.providers.ProviderActionResult

class WorkflowApprovalService(
    private val gateRepository: ApprovalGateRepository,
    private val gateCoordinator: ApprovalGateCoordinator,
    private val sessionGateway: ManagedSessionGateway,
) {
    suspend fun ensurePlanGate(
        run: WorkflowRun,
        taskDefinitionId: TaskDefinitionId,
        gateIdFactory: (TaskDefinitionId) -> ApprovalGateId,
        nowEpochMillis: Long,
    ): ApprovalGate {
        val existing = gateRepository.unresolved(run.id).firstOrNull {
            it.taskDefinitionId == taskDefinitionId && it.kind == ApprovalGateKind.PlanApproval
        }
        if (existing != null) return existing

        return gateCoordinator.open(
            id = gateIdFactory(taskDefinitionId),
            workflowRunId = run.id,
            taskDefinitionId = taskDefinitionId,
            kind = ApprovalGateKind.PlanApproval,
            reason = "Provider plan requires independent approval before execution.",
            requiredRoleId = BuiltInRoles.Architect.id,
            nowEpochMillis = nowEpochMillis,
        )
    }

    suspend fun approvePlan(
        gateId: ApprovalGateId,
        handle: ManagedSessionHandle,
        decidedByRoleId: com.hereliesaz.geministrator.domain.RoleDefinitionId,
        note: String?,
        nowEpochMillis: Long,
    ): ApprovalGate {
        val gate = requireNotNull(gateRepository.get(gateId)) { "Approval gate ${gateId.value} does not exist" }
        require(gate.kind == ApprovalGateKind.PlanApproval) { "Approval gate ${gateId.value} is not a plan gate" }
        require(gate.requiredRoleId == null || gate.requiredRoleId == decidedByRoleId) {
            "Role ${decidedByRoleId.value} is not authorized for gate ${gateId.value}"
        }

        return when (val providerResult = sessionGateway.approvePlan(handle)) {
            ProviderActionResult.Accepted -> gateCoordinator.decide(
                id = gateId,
                approved = true,
                decidedByRoleId = decidedByRoleId,
                note = note,
                nowEpochMillis = nowEpochMillis,
            )
            is ProviderActionResult.Rejected -> gateCoordinator.decide(
                id = gateId,
                approved = false,
                decidedByRoleId = decidedByRoleId,
                note = providerResult.reason,
                nowEpochMillis = nowEpochMillis,
            )
        }
    }
}
