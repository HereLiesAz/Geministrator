package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalPolicy
import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.ProviderConstraints
import com.hereliesaz.geministrator.domain.RetryReason
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.RoleDefinitionId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.domain.effectiveExecutor
import com.hereliesaz.geministrator.events.AgentAssigned
import com.hereliesaz.geministrator.events.ArtifactCreated
import com.hereliesaz.geministrator.events.ExecutorAssigned
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

    init { require(rolesById.size == roles.size) { "Role IDs must be unique" } }

    data class DispatchResult(val run: WorkflowRun, val handles: Map<TaskDefinitionId, ManagedSessionHandle>)

    suspend fun dispatchReadyTasks(project: Project, definition: WorkflowDefinition, run: WorkflowRun, existingHandles: Map<TaskDefinitionId, ManagedSessionHandle> = emptyMap(), nowEpochMillis: Long): DispatchResult {
        if (run.status.isTerminal()) return DispatchResult(run, existingHandles)
        val refreshed = WorkflowRunFactory.refreshReadiness(definition, run, nowEpochMillis)
        val activeCount = refreshed.taskRuns.values.count { it.status.isActive() }
        var remainingSlots = (definition.concurrencyPolicy.maxConcurrentTasks - activeCount).coerceAtLeast(0)
        if (remainingSlots == 0) return DispatchResult(refreshed, existingHandles)
        val definitionsById = definition.tasks.associateBy { it.id }
        var nextRun = refreshed
        val handles = existingHandles.toMutableMap()
        val activeByProvider = refreshed.taskRuns.values.filter { it.status.isActive() && it.assignedProviderId != null }.groupingBy { requireNotNull(it.assignedProviderId) }.eachCount().toMutableMap()
        val dispatchable = refreshed.taskRuns.values.filter { it.status == TaskRunStatus.Ready || it.status == TaskRunStatus.Retrying }
        for (taskRun in dispatchable) {
            if (remainingSlots == 0) break
            val task = requireNotNull(definitionsById[taskRun.taskDefinitionId])
            val executor = taskRun.executor ?: task.effectiveExecutor()
            when (executor) {
                is TaskExecutor.RoleAgent -> {
                    val role = requireNotNull(rolesById[executor.roleId]) { "Role ${executor.roleId.value} is not registered" }
                    require(role.enabled) { "Role ${role.name} is disabled" }
                    val selection = ProviderSelectionRequest(role.preferredProviderId, role.capabilitiesRequired, task.providerConstraints)
                    val providerId = sessionGateway.resolveProvider(selection)
                    val providerLimit = definition.concurrencyPolicy.perProviderLimits[providerId] ?: Int.MAX_VALUE
                    val providerActive = activeByProvider[providerId] ?: 0
                    if (providerActive >= providerLimit) continue
                    val dependencyArtifacts = task.dependsOn.mapNotNull(nextRun.taskRuns::get).flatMap(TaskRun::artifacts)
                    val request = AgentTaskRequest(taskRun.id, task.objective, role.instructions, task.acceptanceCriteria, dependencyArtifacts, project.repository, task.approvalPolicy != ApprovalPolicy.None, PromptContext(listOf(PromptContextBlock("Workflow objective", nextRun.objective), PromptContextBlock("Role", role.instructions)), listOf(PromptContextBlock("Task", task.objective), PromptContextBlock("Attempt", taskRun.attempt.toString())), definition.promptReusePolicy, "${nextRun.id.value}:${role.id.value}"))
                    val handle = sessionGateway.createSession(ManagedSessionRequest(selection.copy(preferredProviderId = providerId, constraints = ProviderConstraints.RequireProvider(providerId)), request))
                    handles[task.id] = handle
                    val startedStatus = if (request.requirePlanApproval) TaskRunStatus.Planning else TaskRunStatus.Running
                    TaskRunTransitions.requireAllowed(taskRun.status, startedStatus)
                    nextRun = nextRun.copy(status = if (nextRun.status == WorkflowRunStatus.AwaitingHuman) nextRun.status else WorkflowRunStatus.Running, taskRuns = nextRun.taskRuns + (task.id to taskRun.copy(status = startedStatus, assignedRoleId = task.roleId ?: role.id, executor = executor, assignedProviderId = handle.providerId, providerRunId = handle.providerRunId, externalRunId = null, blockingReason = null, progress = null, progressMessage = null)), updatedAtEpochMillis = nowEpochMillis)
                    activeByProvider[providerId] = providerActive + 1
                    eventSink.append(AgentAssigned(nextRun.id, task.id, role.id, nowEpochMillis))
                }
                is TaskExecutor.HumanApproval -> {
                    TaskRunTransitions.requireAllowed(taskRun.status, TaskRunStatus.AwaitingApproval)
                    nextRun = nextRun.copy(status = WorkflowRunStatus.AwaitingHuman, taskRuns = nextRun.taskRuns + (task.id to taskRun.copy(status = TaskRunStatus.AwaitingApproval, assignedRoleId = task.roleId, executor = executor, blockingReason = null, progress = null, progressMessage = executor.label)), updatedAtEpochMillis = nowEpochMillis)
                    eventSink.append(HumanDecisionRequired(nextRun.id, task.id, executor.label, nowEpochMillis))
                }
                is TaskExecutor.GitHubAction, is TaskExecutor.TestRunner, is TaskExecutor.Deployment, is TaskExecutor.RepositoryOperation, is TaskExecutor.ExternalService, is TaskExecutor.NestedWorkflow -> continue
            }
            eventSink.append(ExecutorAssigned(nextRun.id, task.id, executor, task.roleId, nowEpochMillis))
            eventSink.append(TaskStarted(nextRun.id, task.id, taskRun.attempt, nowEpochMillis))
            remainingSlots -= 1
        }
        return DispatchResult(nextRun, handles)
    }

    suspend fun completeTask(definition: WorkflowDefinition, run: WorkflowRun, taskDefinitionId: TaskDefinitionId, nowEpochMillis: Long, artifacts: List<ArtifactRef> = emptyList(), externalRunId: String? = null): WorkflowRun {
        require(!run.status.isTerminal()) { "Workflow ${run.id.value} is already ${run.status}" }
        val taskRun = requireNotNull(run.taskRuns[taskDefinitionId]) { "Task run ${taskDefinitionId.value} is missing" }
        TaskRunTransitions.requireAllowed(taskRun.status, TaskRunStatus.Completed)
        eventSink.append(TaskCompleted(run.id, taskDefinitionId, nowEpochMillis))
        var nextRun = run.copy(taskRuns = run.taskRuns + (taskDefinitionId to taskRun.copy(status = TaskRunStatus.Completed, artifacts = if (artifacts.isEmpty()) taskRun.artifacts else mergeArtifacts(taskRun.artifacts, artifacts), externalRunId = externalRunId ?: taskRun.externalRunId, progress = 1f, blockingReason = null)), updatedAtEpochMillis = nowEpochMillis)
        nextRun = WorkflowRunFactory.refreshReadiness(definition, nextRun, nowEpochMillis)
        val status = deriveWorkflowStatus(nextRun)
        if (status == WorkflowRunStatus.Completed) eventSink.append(WorkflowCompleted(nextRun.id, nowEpochMillis))
        return nextRun.copy(status = status)
    }

    suspend fun reconcile(definition: WorkflowDefinition, run: WorkflowRun, handles: Map<TaskDefinitionId, ManagedSessionHandle>, nowEpochMillis: Long, artifactIdFactory: (TaskRun, ProviderArtifact, Int) -> ArtifactId): WorkflowRun {
        if (run.status.isTerminal()) return run
        var nextRun = run
        for ((taskId, handle) in handles) {
            val taskRun = nextRun.taskRuns[taskId] ?: continue
            if (taskRun.status == TaskRunStatus.Completed || taskRun.status == TaskRunStatus.Cancelled) continue
            val previousStatus = taskRun.status
            val status = sessionGateway.status(handle)
            val providerProgress = sessionGateway.progress(handle)
            val durableArtifacts = sessionGateway.artifacts(handle).mapIndexed { index, artifact -> ArtifactRef(artifactIdFactory(taskRun, artifact, index), artifact.kind, taskRun.id, artifact.label, artifact.uri, artifact.textContent, artifact.mediaType, artifact.metadata, nowEpochMillis) }
            val mappedStatus = when (status) { ManagedSessionStatus.Planning -> TaskRunStatus.Planning; ManagedSessionStatus.AwaitingApproval -> TaskRunStatus.AwaitingApproval; ManagedSessionStatus.Running -> TaskRunStatus.Running; ManagedSessionStatus.Completed -> TaskRunStatus.Completed; ManagedSessionStatus.Failed -> TaskRunStatus.Failed; ManagedSessionStatus.Unknown -> taskRun.status }
            val safeStatus = if (mappedStatus == taskRun.status || TaskRunTransitions.canTransition(taskRun.status, mappedStatus)) mappedStatus else taskRun.status
            val mergedArtifacts = mergeArtifacts(taskRun.artifacts, durableArtifacts)
            val previousArtifactIds = taskRun.artifacts.mapTo(mutableSetOf()) { it.id }
            mergedArtifacts.filter { it.id !in previousArtifactIds }.forEach { eventSink.append(ArtifactCreated(nextRun.id, taskId, it, nowEpochMillis)) }
            if (safeStatus == TaskRunStatus.Completed && previousStatus != TaskRunStatus.Completed) eventSink.append(TaskCompleted(nextRun.id, taskId, nowEpochMillis))
            nextRun = nextRun.copy(taskRuns = nextRun.taskRuns + (taskId to taskRun.copy(status = safeStatus, artifacts = mergedArtifacts, progress = if (safeStatus == TaskRunStatus.Completed) 1f else providerProgress?.fraction ?: taskRun.progress, progressMessage = providerProgress?.message ?: taskRun.progressMessage)), updatedAtEpochMillis = nowEpochMillis)
        }
        nextRun = WorkflowRunFactory.refreshReadiness(definition, nextRun, nowEpochMillis)
        val workflowStatus = deriveWorkflowStatus(nextRun)
        if (workflowStatus == WorkflowRunStatus.Completed && run.status != WorkflowRunStatus.Completed) eventSink.append(WorkflowCompleted(nextRun.id, nowEpochMillis))
        return nextRun.copy(status = workflowStatus, updatedAtEpochMillis = nowEpochMillis)
    }

    suspend fun handleFailure(definition: WorkflowDefinition, run: WorkflowRun, taskDefinitionId: TaskDefinitionId, retryReason: RetryReason, reason: String, nowEpochMillis: Long): WorkflowRun {
        require(!run.status.isTerminal()) { "Workflow ${run.id.value} is already ${run.status}" }
        val task = requireNotNull(definition.tasks.firstOrNull { it.id == taskDefinitionId }) { "Task ${taskDefinitionId.value} is not defined" }
        val taskRun = requireNotNull(run.taskRuns[taskDefinitionId]) { "Task run ${taskDefinitionId.value} is missing" }
        val decision = FailurePolicyEvaluator.decide(taskRun, task.retryPolicy, task.escalationPolicy, retryReason)
        return when (decision) {
            is FailureDecision.Retry -> { TaskRunTransitions.requireAllowed(taskRun.status, TaskRunStatus.Retrying); eventSink.append(RetryScheduled(run.id, taskDefinitionId, decision.nextAttempt, reason, nowEpochMillis)); run.copy(status = WorkflowRunStatus.Running, taskRuns = run.taskRuns + (taskDefinitionId to taskRun.copy(status = TaskRunStatus.Retrying, attempt = decision.nextAttempt, assignedProviderId = null, providerRunId = null, externalRunId = null, blockingReason = null, progress = null, progressMessage = null)), updatedAtEpochMillis = nowEpochMillis) }
            FailureDecision.RequireHumanDecision -> { TaskRunTransitions.requireAllowed(taskRun.status, TaskRunStatus.Escalated); eventSink.append(HumanDecisionRequired(run.id, taskDefinitionId, reason, nowEpochMillis)); eventSink.append(TaskEscalated(run.id, taskDefinitionId, reason, occurredAtEpochMillis = nowEpochMillis)); run.copy(status = WorkflowRunStatus.AwaitingHuman, taskRuns = run.taskRuns + (taskDefinitionId to taskRun.copy(status = TaskRunStatus.Escalated)), updatedAtEpochMillis = nowEpochMillis) }
            is FailureDecision.Reassign -> { TaskRunTransitions.requireAllowed(taskRun.status, TaskRunStatus.Retrying); eventSink.append(TaskEscalated(run.id, taskDefinitionId, reason, decision.roleId, nowEpochMillis)); run.copy(status = WorkflowRunStatus.Running, taskRuns = run.taskRuns + (taskDefinitionId to taskRun.copy(status = TaskRunStatus.Retrying, assignedRoleId = decision.roleId, executor = TaskExecutor.RoleAgent(decision.roleId), assignedProviderId = null, providerRunId = null, externalRunId = null, blockingReason = null, progress = null, progressMessage = null)), updatedAtEpochMillis = nowEpochMillis) }
            FailureDecision.FailWorkflow -> { if (taskRun.status != TaskRunStatus.Failed) TaskRunTransitions.requireAllowed(taskRun.status, TaskRunStatus.Failed); eventSink.append(TaskFailed(run.id, taskDefinitionId, reason, nowEpochMillis)); eventSink.append(WorkflowFailed(run.id, reason, nowEpochMillis)); run.copy(status = WorkflowRunStatus.Failed, taskRuns = run.taskRuns + (taskDefinitionId to taskRun.copy(status = TaskRunStatus.Failed)), updatedAtEpochMillis = nowEpochMillis) }
        }
    }

    private fun mergeArtifacts(existing: List<ArtifactRef>, incoming: List<ArtifactRef>): List<ArtifactRef> { val byId = LinkedHashMap<ArtifactId, ArtifactRef>(); existing.forEach { byId[it.id] = it }; incoming.forEach { byId[it.id] = it }; return byId.values.toList() }
    private fun deriveWorkflowStatus(run: WorkflowRun): WorkflowRunStatus { if (run.status.isTerminal()) return run.status; val statuses = run.taskRuns.values.map { it.status }; return when { statuses.isNotEmpty() && statuses.all { it == TaskRunStatus.Completed } -> WorkflowRunStatus.Completed; statuses.any { it == TaskRunStatus.AwaitingApproval || it == TaskRunStatus.Escalated } -> WorkflowRunStatus.AwaitingHuman; else -> WorkflowRunStatus.Running } }
    private fun WorkflowRunStatus.isTerminal() = this == WorkflowRunStatus.Completed || this == WorkflowRunStatus.Failed || this == WorkflowRunStatus.Cancelled
    private fun TaskRunStatus.isActive() = this == TaskRunStatus.Planning || this == TaskRunStatus.AwaitingApproval || this == TaskRunStatus.Running || this == TaskRunStatus.Verifying || this == TaskRunStatus.Retrying
}