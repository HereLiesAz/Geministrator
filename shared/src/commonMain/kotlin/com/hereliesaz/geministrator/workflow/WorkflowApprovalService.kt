package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.RoleDefinitionId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
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
        decidedByRoleId: RoleDefinitionId,
        note: String?,
        nowEpochMillis: Long,
    ): ApprovalGate {
        val gate = requireNotNull(gateRepository.get(gateId)) {
            "Approval gate ${gateId.value} does not exist"
        }
        require(gate.kind == ApprovalGateKind.PlanApproval) {
            "Approval gate ${gateId.value} is not a plan gate"
        }
        require(gate.status == ApprovalGateStatus.Pending) {
            "Approval gate ${gateId.value} is already resolved"
        }
        require(gate.requiredRoleId == null || gate.requiredRoleId == decidedByRoleId) {
            "Role ${decidedByRoleId.value} is not authorized for gate ${gateId.value}"
        }

        // The provider side effect is the authority on whether the plan was actually accepted.
        // Keep the local gate Pending until that call returns, then persist exactly one matching
        // decision/event. If the call throws, no contradictory local approval is recorded.
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

    suspend fun decideFailureEscalation(
        run: WorkflowRun,
        gateId: ApprovalGateId,
        approved: Boolean,
        decidedByRoleId: RoleDefinitionId?,
        note: String?,
        nowEpochMillis: Long,
    ): WorkflowRun {
        require(run.status != WorkflowRunStatus.Completed && run.status != WorkflowRunStatus.Failed && run.status != WorkflowRunStatus.Cancelled) {
            "Workflow ${run.id.value} is already ${run.status}"
        }
        val gate = requireNotNull(gateRepository.get(gateId)) { "Approval gate ${gateId.value} does not exist" }
        require(gate.workflowRunId == run.id) { "Approval gate ${gateId.value} belongs to another workflow" }
        require(gate.kind == ApprovalGateKind.FailureEscalation) { "Approval gate ${gateId.value} is not a failure escalation gate" }
        require(gate.status == ApprovalGateStatus.Pending) { "Approval gate ${gateId.value} is already resolved" }
        require(gate.requiredRoleId == null || gate.requiredRoleId == decidedByRoleId) {
            "Role ${decidedByRoleId?.value ?: "<human>"} is not authorized for gate ${gateId.value}"
        }
        val taskDefinitionId = requireNotNull(gate.taskDefinitionId) { "Failure escalation gate ${gateId.value} has no task" }
        val taskRun = requireNotNull(run.taskRuns[taskDefinitionId]) { "Task run ${taskDefinitionId.value} is missing" }
        require(taskRun.status == TaskRunStatus.Escalated) {
            "Task ${taskDefinitionId.value} is ${taskRun.status}, not Escalated"
        }

        gateCoordinator.decide(
            id = gateId,
            approved = approved,
            decidedByRoleId = decidedByRoleId,
            note = note,
            nowEpochMillis = nowEpochMillis,
        )

        return if (approved) {
            TaskRunTransitions.requireAllowed(TaskRunStatus.Escalated, TaskRunStatus.Retrying)
            run.copy(
                status = WorkflowRunStatus.Running,
                taskRuns = run.taskRuns + (
                    taskDefinitionId to taskRun.copy(
                        status = TaskRunStatus.Retrying,
                        attempt = taskRun.attempt + 1,
                        assignedProviderId = null,
                        providerRunId = null,
                        externalRunId = null,
                        blockingReason = null,
                        progress = null,
                        progressMessage = note ?: "Failure escalation approved; retry scheduled",
                    )
                ),
                updatedAtEpochMillis = nowEpochMillis,
            )
        } else {
            TaskRunTransitions.requireAllowed(TaskRunStatus.Escalated, TaskRunStatus.Cancelled)
            run.copy(
                status = WorkflowRunStatus.Failed,
                taskRuns = run.taskRuns + (
                    taskDefinitionId to taskRun.copy(
                        status = TaskRunStatus.Cancelled,
                        blockingReason = null,
                        progressMessage = note ?: "Failure escalation rejected",
                    )
                ),
                updatedAtEpochMillis = nowEpochMillis,
            )
        }
    }
}
