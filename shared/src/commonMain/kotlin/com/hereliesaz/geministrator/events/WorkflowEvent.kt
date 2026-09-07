package com.hereliesaz.geministrator.events

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.RoleDefinitionId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRunId

sealed interface WorkflowEvent {
    val workflowRunId: WorkflowRunId
    val occurredAtEpochMillis: Long
}

data class WorkflowCreated(
    override val workflowRunId: WorkflowRunId,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class TaskBecameReady(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class AgentAssigned(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    val roleId: RoleDefinitionId,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class TaskStarted(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    val attempt: Int,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class ApprovalRequired(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    val gateId: ApprovalGateId? = null,
    val reason: String,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class ApprovalDecisionReceived(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId?,
    val gateId: ApprovalGateId,
    val approved: Boolean,
    val decidedByRoleId: RoleDefinitionId?,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class ArtifactCreated(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    val artifact: ArtifactRef,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class VerificationFailed(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    val reason: String,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class RetryScheduled(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    val nextAttempt: Int,
    val reason: String,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class TaskEscalated(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    val reason: String,
    val reassignedRoleId: RoleDefinitionId? = null,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class TaskCompleted(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class TaskFailed(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId,
    val reason: String,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class HumanDecisionRequired(
    override val workflowRunId: WorkflowRunId,
    val taskDefinitionId: TaskDefinitionId?,
    val reason: String,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class WorkflowCompleted(
    override val workflowRunId: WorkflowRunId,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent

data class WorkflowFailed(
    override val workflowRunId: WorkflowRunId,
    val reason: String,
    override val occurredAtEpochMillis: Long,
) : WorkflowEvent
