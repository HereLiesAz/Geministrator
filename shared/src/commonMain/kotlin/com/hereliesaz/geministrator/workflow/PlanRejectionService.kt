package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.RoleDefinitionId
import com.hereliesaz.geministrator.providers.ProviderActionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val planRejectionMutex = Mutex()

class PlanRejectionService(
    private val gateRepository: ApprovalGateRepository,
    private val gateCoordinator: ApprovalGateCoordinator,
    private val sessionGateway: ManagedSessionGateway,
) {
    suspend fun rejectPlan(
        gateId: ApprovalGateId,
        handle: ManagedSessionHandle,
        decidedByRoleId: RoleDefinitionId? = null,
        note: String? = null,
        nowEpochMillis: Long,
    ): ApprovalGate = planRejectionMutex.withLock {
        val gate = requireNotNull(gateRepository.get(gateId)) {
            "Approval gate ${gateId.value} does not exist"
        }
        require(gate.kind == ApprovalGateKind.PlanApproval) {
            "Approval gate ${gateId.value} is not a plan gate"
        }
        require(gate.requiredRoleId == null || gate.requiredRoleId == decidedByRoleId) {
            "Role ${decidedByRoleId?.value ?: "<human>"} is not authorized for gate ${gateId.value}"
        }
        require(gate.status == ApprovalGateStatus.Pending) {
            "Approval gate ${gateId.value} cannot be rejected while ${gate.status}"
        }

        when (val cancellation = sessionGateway.cancel(handle)) {
            ProviderActionResult.Accepted -> Unit
            is ProviderActionResult.Rejected -> error(
                "Provider session ${handle.providerRunId.value} could not be cancelled after plan rejection: " +
                    cancellation.reason.ifBlank { "provider rejected cancellation" },
            )
        }

        gateCoordinator.decide(
            id = gateId,
            approved = false,
            decidedByRoleId = decidedByRoleId,
            note = note ?: "Plan rejected in application",
            nowEpochMillis = nowEpochMillis,
        )
    }
}
