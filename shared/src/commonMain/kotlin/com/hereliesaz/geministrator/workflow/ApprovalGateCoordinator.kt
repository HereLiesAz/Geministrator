package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.RoleDefinitionId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.events.ApprovalDecisionReceived
import com.hereliesaz.geministrator.events.ApprovalRequired
import com.hereliesaz.geministrator.events.HumanDecisionRequired
import com.hereliesaz.geministrator.events.WorkflowEventSink
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ApprovalGateCoordinator(
    private val repository: ApprovalGateRepository,
    private val eventSink: WorkflowEventSink,
) {
    private val mutationMutex = Mutex()

    suspend fun open(
        id: ApprovalGateId,
        workflowRunId: WorkflowRunId,
        taskDefinitionId: TaskDefinitionId?,
        kind: ApprovalGateKind,
        reason: String,
        requiredRoleId: RoleDefinitionId? = null,
        requiresHuman: Boolean = false,
        nowEpochMillis: Long,
    ): ApprovalGate = mutationMutex.withLock {
        val existing = repository.get(id)
        if (existing != null) return@withLock existing

        val gate = ApprovalGate(
            id = id,
            workflowRunId = workflowRunId,
            taskDefinitionId = taskDefinitionId,
            kind = kind,
            reason = reason,
            requiredRoleId = requiredRoleId,
            requiresHuman = requiresHuman,
            createdAtEpochMillis = nowEpochMillis,
        )
        repository.put(gate)

        if (taskDefinitionId != null) {
            eventSink.append(
                ApprovalRequired(
                    workflowRunId = workflowRunId,
                    taskDefinitionId = taskDefinitionId,
                    gateId = id,
                    reason = reason,
                    occurredAtEpochMillis = nowEpochMillis,
                ),
            )
        }
        if (requiresHuman) {
            eventSink.append(
                HumanDecisionRequired(
                    workflowRunId = workflowRunId,
                    taskDefinitionId = taskDefinitionId,
                    reason = reason,
                    occurredAtEpochMillis = nowEpochMillis,
                ),
            )
        }
        gate
    }

    suspend fun decide(
        id: ApprovalGateId,
        approved: Boolean,
        decidedByRoleId: RoleDefinitionId?,
        note: String?,
        nowEpochMillis: Long,
    ): ApprovalGate = mutationMutex.withLock {
        val current = requireNotNull(repository.get(id)) {
            "Approval gate ${id.value} does not exist"
        }
        require(current.status == ApprovalGateStatus.Pending) {
            "Approval gate ${id.value} is already resolved"
        }

        val decided = if (approved) {
            current.approve(decidedByRoleId, note, nowEpochMillis)
        } else {
            current.reject(decidedByRoleId, note, nowEpochMillis)
        }
        repository.put(decided)
        eventSink.append(
            ApprovalDecisionReceived(
                workflowRunId = decided.workflowRunId,
                taskDefinitionId = decided.taskDefinitionId,
                gateId = decided.id,
                approved = approved,
                decidedByRoleId = decidedByRoleId,
                occurredAtEpochMillis = nowEpochMillis,
            ),
        )
        decided
    }
}
