package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalPolicy
import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.RoleDefinitionId
import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.providers.AgentTaskRequest
import com.hereliesaz.geministrator.providers.PromptContext
import com.hereliesaz.geministrator.providers.PromptContextBlock
import com.hereliesaz.geministrator.providers.ProviderArtifact

class WorkflowEngine(
    private val sessionGateway: ManagedSessionGateway,
    roles: Collection<RoleDefinition>,
) {
    private val rolesById: Map<RoleDefinitionId, RoleDefinition> = roles.associateBy { it.id }

    init {
        require(rolesById.size == roles.size) { "Role IDs must be unique" }
    }

    data class DispatchResult(
        val run: WorkflowRun,
        val handles: Map<com.hereliesaz.geministrator.domain.TaskDefinitionId, ManagedSessionHandle>,
    )

    suspend fun dispatchReadyTasks(
        project: Project,
        definition: WorkflowDefinition,
        run: WorkflowRun,
        existingHandles: Map<com.hereliesaz.geministrator.domain.TaskDefinitionId, ManagedSessionHandle> = emptyMap(),
        nowEpochMillis: Long,
    ): DispatchResult {
        val refreshed = WorkflowRunFactory.refreshReadiness(definition, run, nowEpochMillis)
        val activeCount = refreshed.taskRuns.values.count { it.status.isActive() }
        val availableSlots = (definition.concurrencyPolicy.maxConcurrentTasks - activeCount).coerceAtLeast(0)
        if (availableSlots == 0) return DispatchResult(refreshed, existingHandles)

        val definitionsById = definition.tasks.associateBy { it.id }
        var nextRun = refreshed.copy(status = WorkflowRunStatus.Running)
        val handles = existingHandles.toMutableMap()

        val ready = refreshed.taskRuns.values
            .filter { it.status == TaskRunStatus.Ready }
            .take(availableSlots)

        for (taskRun in ready) {
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
            nextRun = nextRun.copy(
                taskRuns = nextRun.taskRuns + (
                    task.id to taskRun.copy(
                        status = if (request.requirePlanApproval) TaskRunStatus.Planning else TaskRunStatus.Running,
                        assignedProviderId = handle.providerId,
                        providerRunId = handle.providerRunId,
                        blockingReason = null,
                    )
                ),
                updatedAtEpochMillis = nowEpochMillis,
            )
        }

        return DispatchResult(nextRun, handles)
    }

    suspend fun reconcile(
        definition: WorkflowDefinition,
        run: WorkflowRun,
        handles: Map<com.hereliesaz.geministrator.domain.TaskDefinitionId, ManagedSessionHandle>,
        nowEpochMillis: Long,
        artifactIdFactory: (TaskRun, ProviderArtifact, Int) -> ArtifactId,
    ): WorkflowRun {
        var nextRun = run

        for ((taskId, handle) in handles) {
            val taskRun = nextRun.taskRuns[taskId] ?: continue
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
        val workflowStatus = when {
            statuses.isNotEmpty() && statuses.all { it == TaskRunStatus.Completed } -> WorkflowRunStatus.Completed
            statuses.any { it == TaskRunStatus.Failed } -> WorkflowRunStatus.Failed
            else -> WorkflowRunStatus.Running
        }

        return nextRun.copy(status = workflowStatus, updatedAtEpochMillis = nowEpochMillis)
    }

    private fun TaskRunStatus.isActive(): Boolean = when (this) {
        TaskRunStatus.Planning,
        TaskRunStatus.AwaitingApproval,
        TaskRunStatus.Running,
        TaskRunStatus.Verifying,
        TaskRunStatus.Retrying,
        -> true
        else -> false
    }
}
