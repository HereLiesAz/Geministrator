package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalPolicy
import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.RetryReason
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.RoleDefinitionId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.events.AgentAssigned
import com.hereliesaz.geministrator.events.ArtifactCreated
import com.hereliesaz.geministrator.events.HumanDecisionRequired
import com.hereliesaz.geministrator.events.NoOpWorkflowEventSink
import com.hereliesaz.geministrator.events.RetryScheduled
import com.hereliesaz.geministrator.events.TaskCompleted
import com.hereliesaz.geministrator.events.TaskEscalated
import com.hereliesaz.geministrator.events.TaskFailed
import com.hereliesaz.geministrator.events.TaskStarted
import com.hereliesaz.geministrator.events.WorkflowCompleted
import com.hereliesaz.geministrator.events.WorkflowEventSink
import com.hereliesaz.geministrator.events.WorkflowFailed
import com.hereliesaz.geministrator.providers.AgentTaskRequest
import com.hereliesaz.geministrator.providers.PromptContext
import com.hereliesaz.geministrator.providers.PromptContextBlock
import com.hereliesaz.geministrator.providers.ProviderArtifact

class WorkflowEngine(
    private val sessionGateway: ManagedSessionGateway,
    roles: Collection<RoleDefinition>,
    private val eventSink: WorkflowEventSink = NoOpWorkflowEventSink,
) {
    private val rolesById: Map<RoleDefinitionId, RoleDefinition> = roles.associateBy { it.id }

    init {
        require(rolesById.size == roles.size) { "Role IDs must be unique" }
    }

    data class DispatchResult(
        val run: WorkflowRun,
        val handles: Map<TaskDefinitionId, ManagedSessionHandle>,
    )

    suspend fun dispatchReadyTasks(
        project: Project,
        definition: WorkflowDefinition,
        run: WorkflowRun,
        existingHandles: Map<TaskDefinitionId, ManagedSessionHandle> = emptyMap(),
        nowEpochMillis: Long,
    ): DispatchResult {
        val refreshed = WorkflowRunFactory.refreshReadiness(definition, run, nowEpochMillis)
        val activeCount = refreshed.taskRuns.values.count { it.status.isActive() }
        val availableSlots = (definition.concurrencyPolicy.maxConcurrentTasks - activeCount).coerceAtLeast(0)
        if (availableSlots == 0) return DispatchResult(refreshed, existingHandles)

        val definitionsById = definition.tasks.associateBy { it.id }
        var nextRun = refreshed.copy(status = WorkflowRunStatus.Running)
        val handles = existingHandles.toMutableMap()

        val dispatchable = refreshed.taskRuns.values
            .filter { it.status == TaskRunStatus.Ready || it.status == TaskRunStatus.Retrying }
            .take(availableSlots)

        for (taskRun in dispatchable) {
            val task = requireNotNull(definitionsById[taskRun.taskDefinitionId])
            val role = requireNotNull(rolesById[task.roleId]) {
                "Role ${task.roleId.value} is not registered"
            }
            require(role.enabled) { "Role ${role.name} is disabled" }

            val dependencyArtifacts = task.dependsOn
                .mapNotNull(nextRun.taskRuns::get)
                .flatMap(TaskRun::artifacts)

            val request = AgentTaskRequest(
                taskRunId = taskRun.id,
                objective = task.objective,
                roleInstructions = role.instructions,
                acceptanceCriteria = task.acceptanceCriteria,
                contextArtifacts = dependencyArtifacts,
                repository = project.repository,
                requirePlanApproval = task.approvalPolicy != ApprovalPolicy.None,
                promptContext = PromptContext(
                    stablePrefix = listOf(
                        PromptContextBlock("Workflow objective", nextRun.objective),
                        PromptContextBlock("Role", role.instructions),
                    ),
                    dynamicContext = listOf(
                        PromptContextBlock("Task", task.objective),
                        PromptContextBlock("Attempt", taskRun.attempt.toString()),
                    ),
                    reusePolicy = definition.promptReusePolicy,
                    cacheNamespace = "${nextRun.id.value}:${role.id.value}",
                ),
            )

            val handle = sessionGateway.createSession(
                ManagedSessionRequest(
                    providerSelection = ProviderSelectionRequest(
                        preferredProviderId = role.preferredProviderId,
                        requiredCapabilities = role.capabilitiesRequired,
                        constraints = task.providerConstraints,
                    ),
                    taskRequest = request,
                ),
            )

            handles[task.id] = handle
            val startedStatus = if (request.requirePlanApproval) TaskRunStatus.Planning else TaskRunStatus.Running
            nextRun = nextRun.copy(
                taskRuns = nextRun.taskRuns + (
                    task.id to taskRun.copy(
                        status = startedStatus,
                        assignedRoleId = role.id,
                        assignedProviderId = handle.providerId,
                        providerRunId = handle.providerRunId,
                        blockingReason = null,
                    )
                ),
                updatedAtEpochMillis = nowEpochMillis,
            )
            eventSink.append(
                AgentAssigned(
                    workflowRunId = nextRun.id,
                    taskDefinitionId = task.id,
                    roleId = role.id,
                    occurredAtEpochMillis = nowEpochMillis,
                ),
            )
            eventSink.append(
                TaskStarted(
                    workflowRunId = nextRun.id,
                    taskDefinitionId = task.id,
                    attempt = taskRun.attempt,
                    occurredAtEpochMillis = nowEpochMillis,
                ),
            )
        }

        return DispatchResult(nextRun, handles)
    }

    suspend fun reconcile(
        definition: WorkflowDefinition,
        run: WorkflowRun,
        handles: Map<TaskDefinitionId, ManagedSessionHandle>,
        nowEpochMillis: Long,
        artifactIdFactory: (TaskRun, ProviderArtifact, Int) -> ArtifactId,
    ): WorkflowRun {
        var nextRun = run

        for ((taskId, handle) in handles) {
            val taskRun = nextRun.taskRuns[taskId] ?: continue
            val previousStatus = taskRun.status
            val status = sessionGateway.status(handle)
            val providerArtifacts = sessionGateway.artifacts(handle)
            val durableArtifacts = providerArtifacts.mapIndexed { index, artifact ->
                ArtifactRef(
                    id = artifactIdFactory(taskRun, artifact, index),
                    kind = artifact.kind,
                    taskRunId = taskRun.id,
                    label = artifact.label,
                    uri = artifact.uri,
                    textContent = artifact.textContent,
                    mediaType = artifact.mediaType,
                    metadata = artifact.metadata,
                    createdAtEpochMillis = nowEpochMillis,
                )
            }

            val mappedStatus = when (status) {
                ManagedSessionStatus.Planning -> TaskRunStatus.Planning
                ManagedSessionStatus.AwaitingApproval -> TaskRunStatus.AwaitingApproval
                ManagedSessionStatus.Running -> TaskRunStatus.Running
                ManagedSessionStatus.Completed -> TaskRunStatus.Completed
                ManagedSessionStatus.Failed -> TaskRunStatus.Failed
                ManagedSessionStatus.Unknown -> taskRun.status
            }

            val previousArtifactIds = taskRun.artifacts.mapTo(mutableSetOf()) { it.id }
            durableArtifacts
                .filter { it.id !in previousArtifactIds }
                .forEach { artifact ->
                    eventSink.append(
                        ArtifactCreated(
                            workflowRunId = nextRun.id,
                            taskDefinitionId = taskId,
                            artifact = artifact,
                            occurredAtEpochMillis = nowEpochMillis,
                        ),
                    )
                }

            if (mappedStatus == TaskRunStatus.Completed && previousStatus != TaskRunStatus.Completed) {
                eventSink.append(
                    TaskCompleted(
                        workflowRunId = nextRun.id,
                        taskDefinitionId = taskId,
                        occurredAtEpochMillis = nowEpochMillis,
                    ),
                )
            }

            nextRun = nextRun.copy(
                taskRuns = nextRun.taskRuns + (
                    taskId to taskRun.copy(
                        status = mappedStatus,
                        artifacts = durableArtifacts,
                    )
                ),
                updatedAtEpochMillis = nowEpochMillis,
            )
        }

        nextRun = WorkflowRunFactory.refreshReadiness(definition, nextRun, nowEpochMillis)
        val statuses = nextRun.taskRuns.values.map { it.status }
        val workflowStatus = if (statuses.isNotEmpty() && statuses.all { it == TaskRunStatus.Completed }) {
            WorkflowRunStatus.Completed
        } else {
            WorkflowRunStatus.Running
        }

        if (workflowStatus == WorkflowRunStatus.Completed && run.status != WorkflowRunStatus.Completed) {
            eventSink.append(
                WorkflowCompleted(
                    workflowRunId = nextRun.id,
                    occurredAtEpochMillis = nowEpochMillis,
                ),
            )
        }

        return nextRun.copy(status = workflowStatus, updatedAtEpochMillis = nowEpochMillis)
    }

    suspend fun handleFailure(
        definition: WorkflowDefinition,
        run: WorkflowRun,
        taskDefinitionId: TaskDefinitionId,
        retryReason: RetryReason,
        reason: String,
        nowEpochMillis: Long,
    ): WorkflowRun {
        val task = requireNotNull(definition.tasks.firstOrNull { it.id == taskDefinitionId }) {
            "Task ${taskDefinitionId.value} is not defined"
        }
        val taskRun = requireNotNull(run.taskRuns[taskDefinitionId]) {
            "Task run ${taskDefinitionId.value} is missing"
        }
        val decision = FailurePolicyEvaluator.decide(
            taskRun = taskRun,
            retryPolicy = task.retryPolicy,
            escalationPolicy = task.escalationPolicy,
            reason = retryReason,
        )

        return when (decision) {
            is FailureDecision.Retry -> {
                eventSink.append(
                    RetryScheduled(
                        workflowRunId = run.id,
                        taskDefinitionId = taskDefinitionId,
                        nextAttempt = decision.nextAttempt,
                        reason = reason,
                        occurredAtEpochMillis = nowEpochMillis,
                    ),
                )
                run.copy(
                    status = WorkflowRunStatus.Running,
                    taskRuns = run.taskRuns + (
                        taskDefinitionId to taskRun.copy(
                            status = TaskRunStatus.Retrying,
                            attempt = decision.nextAttempt,
                            assignedProviderId = null,
                            providerRunId = null,
                            artifacts = emptyList(),
                            blockingReason = null,
                        )
                    ),
                    updatedAtEpochMillis = nowEpochMillis,
                )
            }

            FailureDecision.RequireHumanDecision -> {
                eventSink.append(
                    HumanDecisionRequired(
                        workflowRunId = run.id,
                        taskDefinitionId = taskDefinitionId,
                        reason = reason,
                        occurredAtEpochMillis = nowEpochMillis,
                    ),
                )
                eventSink.append(
                    TaskEscalated(
                        workflowRunId = run.id,
                        taskDefinitionId = taskDefinitionId,
                        reason = reason,
                        occurredAtEpochMillis = nowEpochMillis,
                    ),
                )
                run.copy(
                    status = WorkflowRunStatus.AwaitingHuman,
                    taskRuns = run.taskRuns + (taskDefinitionId to taskRun.copy(status = TaskRunStatus.Escalated)),
                    updatedAtEpochMillis = nowEpochMillis,
                )
            }

            is FailureDecision.Reassign -> {
                eventSink.append(
                    TaskEscalated(
                        workflowRunId = run.id,
                        taskDefinitionId = taskDefinitionId,
                        reason = reason,
                        reassignedRoleId = decision.roleId,
                        occurredAtEpochMillis = nowEpochMillis,
                    ),
                )
                run.copy(
                    status = WorkflowRunStatus.Running,
                    taskRuns = run.taskRuns + (
                        taskDefinitionId to taskRun.copy(
                            status = TaskRunStatus.Retrying,
                            assignedRoleId = decision.roleId,
                            assignedProviderId = null,
                            providerRunId = null,
                            artifacts = emptyList(),
                            blockingReason = null,
                        )
                    ),
                    updatedAtEpochMillis = nowEpochMillis,
                )
            }

            FailureDecision.FailWorkflow -> {
                eventSink.append(
                    TaskFailed(
                        workflowRunId = run.id,
                        taskDefinitionId = taskDefinitionId,
                        reason = reason,
                        occurredAtEpochMillis = nowEpochMillis,
                    ),
                )
                eventSink.append(
                    WorkflowFailed(
                        workflowRunId = run.id,
                        reason = reason,
                        occurredAtEpochMillis = nowEpochMillis,
                    ),
                )
                run.copy(
                    status = WorkflowRunStatus.Failed,
                    taskRuns = run.taskRuns + (taskDefinitionId to taskRun.copy(status = TaskRunStatus.Failed)),
                    updatedAtEpochMillis = nowEpochMillis,
                )
            }
        }
    }

    private fun TaskRunStatus.isActive(): Boolean = when (this) {
        TaskRunStatus.Planning,
        TaskRunStatus.AwaitingApproval,
        TaskRunStatus.Running,
        TaskRunStatus.Verifying,
        -> true
        else -> false
    }
}
