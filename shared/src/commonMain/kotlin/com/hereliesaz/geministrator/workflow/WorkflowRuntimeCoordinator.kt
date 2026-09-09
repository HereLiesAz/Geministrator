package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.BlockingReason
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.RetryReason
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.domain.effectiveExecutor
import com.hereliesaz.geministrator.persistence.RepositoryWorkflowEventSink
import com.hereliesaz.geministrator.persistence.WorkflowPersistence
import com.hereliesaz.geministrator.providers.ProviderArtifact
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class WorkflowRuntimeState(val run: WorkflowRun, val handles: Map<TaskDefinitionId, ManagedSessionHandle> = emptyMap())

class WorkflowRuntimeCoordinator(
    private val persistence: WorkflowPersistence,
    private val engine: WorkflowEngine,
    private val sessionGateway: ManagedSessionGateway,
    private val executorIntegrations: TaskExecutorIntegrationRegistry = TaskExecutorIntegrationRegistry.Empty,
) {
    private val gateCoordinator = ApprovalGateCoordinator(persistence.approvalGates, RepositoryWorkflowEventSink(persistence.events))
    private val cycleMutex = Mutex()

    suspend fun persist(project: Project, definition: WorkflowDefinition, state: WorkflowRuntimeState) { persistence.projects.put(project); persistence.definitions.put(definition); persistence.runs.put(state.run); state.run.taskRuns.values.flatMap(TaskRun::artifacts).forEach { persistence.artifacts.put(it) } }

    suspend fun resume(workflowRunId: WorkflowRunId): WorkflowRuntimeState {
        val run = requireNotNull(persistence.runs.get(workflowRunId)) { "Workflow run ${workflowRunId.value} was not found" }
        val handles = buildMap { run.taskRuns.forEach { (taskDefinitionId, taskRun) -> val providerId = taskRun.assignedProviderId ?: return@forEach; val providerRunId = taskRun.providerRunId ?: return@forEach; if (!taskRun.status.canReconnect()) return@forEach; val handle = ManagedSessionHandle(taskRun.id, providerId, providerRunId); sessionGateway.reconnect(handle, taskRun.status.toManagedStatus()); put(taskDefinitionId, handle) } }
        return WorkflowRuntimeState(run, handles)
    }

    suspend fun cycle(project: Project, definition: WorkflowDefinition, state: WorkflowRuntimeState, nowEpochMillis: Long, artifactIdFactory: (TaskRun, ProviderArtifact, Int) -> ArtifactId): WorkflowRuntimeState = cycleMutex.withLock {
        val persistedRun = persistence.runs.get(state.run.id)
        val authoritativeState = if (persistedRun != null && persistedRun.updatedAtEpochMillis > state.run.updatedAtEpochMillis) {
            WorkflowRuntimeState(persistedRun, mergeHandles(state.handles, persistedRun))
        } else {
            state
        }
        cycleLocked(project, definition, authoritativeState, nowEpochMillis, artifactIdFactory)
    }

    private suspend fun cycleLocked(project: Project, definition: WorkflowDefinition, state: WorkflowRuntimeState, nowEpochMillis: Long, artifactIdFactory: (TaskRun, ProviderArtifact, Int) -> ArtifactId): WorkflowRuntimeState {
        if (state.run.status.isTerminal()) return state
        var nextRun = recoverAvailableSystemExecutors(definition, state.run, nowEpochMillis)
        nextRun = engine.reconcile(definition, nextRun, state.handles, nowEpochMillis, artifactIdFactory)
        nextRun = preserveArtifactTimestamps(state.run, nextRun)
        nextRun = reconcileSystemExecutors(project, definition, nextRun, nowEpochMillis)
        nextRun = refreshAfterSystemExecution(definition, nextRun, nowEpochMillis)
        val newlyFailedTaskIds = nextRun.taskRuns.filter { (taskId, taskRun) -> taskRun.status == TaskRunStatus.Failed && state.run.taskRuns[taskId]?.status != TaskRunStatus.Failed }.keys
        for (taskId in newlyFailedTaskIds) {
            nextRun = engine.handleFailure(definition, nextRun, taskId, RetryReason.ProviderFailure, "Executor failed", nowEpochMillis)
            if (nextRun.taskRuns[taskId]?.status == TaskRunStatus.Escalated) ensureFailureEscalationGate(nextRun, taskId, "Executor failed", nowEpochMillis)
        }
        ensureMissingFailureEscalationGates(nextRun, nowEpochMillis)
        if (nextRun.taskRuns.values.any { it.status == TaskRunStatus.AwaitingApproval || it.status == TaskRunStatus.Escalated }) nextRun = nextRun.copy(status = WorkflowRunStatus.AwaitingHuman)
        var nextHandles = state.handles.filterKeys { taskId -> nextRun.taskRuns[taskId]?.status !in setOf(TaskRunStatus.Completed, TaskRunStatus.Failed, TaskRunStatus.Cancelled, TaskRunStatus.Retrying, TaskRunStatus.Escalated) }
        nextRun = blockUnavailableSystemExecutors(definition, nextRun)
        var nextState = WorkflowRuntimeState(nextRun, nextHandles); persist(project, definition, nextState)
        if (nextRun.status.isTerminal()) return nextState
        nextRun = dispatchSystemExecutors(project, definition, nextRun, nowEpochMillis); nextRun = refreshAfterSystemExecution(definition, nextRun, nowEpochMillis)
        if (nextRun.status.isTerminal()) { nextState = WorkflowRuntimeState(nextRun, nextHandles); persist(project, definition, nextState); return nextState }
        val dispatched = engine.dispatchReadyTasks(project, definition, nextRun, nextHandles, nowEpochMillis); nextRun = dispatched.run; nextHandles = dispatched.handles.filterKeys { nextRun.taskRuns[it]?.status?.isActiveProviderStatus() == true }; nextState = WorkflowRuntimeState(nextRun, nextHandles); persist(project, definition, nextState); return nextState
    }

    private suspend fun mergeHandles(existing: Map<TaskDefinitionId, ManagedSessionHandle>, run: WorkflowRun): Map<TaskDefinitionId, ManagedSessionHandle> = buildMap {
        putAll(existing.filterKeys { taskId -> run.taskRuns[taskId]?.status?.canReconnect() == true })
        run.taskRuns.forEach { (taskDefinitionId, taskRun) ->
            if (containsKey(taskDefinitionId) || !taskRun.status.canReconnect()) return@forEach
            val providerId = taskRun.assignedProviderId ?: return@forEach
            val providerRunId = taskRun.providerRunId ?: return@forEach
            val handle = ManagedSessionHandle(taskRun.id, providerId, providerRunId)
            sessionGateway.reconnect(handle, taskRun.status.toManagedStatus())
            put(taskDefinitionId, handle)
        }
    }

    private suspend fun ensureMissingFailureEscalationGates(run: WorkflowRun, now: Long) { for ((taskId, taskRun) in run.taskRuns) if (taskRun.status == TaskRunStatus.Escalated) ensureFailureEscalationGate(run, taskId, taskRun.progressMessage ?: "Task failure requires a human decision.", now) }
    private suspend fun ensureFailureEscalationGate(run: WorkflowRun, taskId: TaskDefinitionId, reason: String, now: Long) {
        val existing = persistence.approvalGates.unresolved(run.id).firstOrNull { it.taskDefinitionId == taskId && it.kind == ApprovalGateKind.FailureEscalation }
        if (existing != null) return
        val taskRun = requireNotNull(run.taskRuns[taskId])
        gateCoordinator.open(ApprovalGateId("failure:${run.id.value}:${taskId.value}:${taskRun.attempt}"), run.id, taskId, ApprovalGateKind.FailureEscalation, reason, requiresHuman = true, nowEpochMillis = now)
    }

    private suspend fun dispatchSystemExecutors(project: Project, definition: WorkflowDefinition, run: WorkflowRun, now: Long): WorkflowRun { var next = run; val tasks = definition.tasks.associateBy { it.id }; for ((id, tr) in run.taskRuns) { val task = tasks[id] ?: continue; val ex = tr.executor ?: task.effectiveExecutor(); if (!ex.isSystemExecutor() || (tr.status != TaskRunStatus.Ready && tr.status != TaskRunStatus.Retrying)) continue; val integration = executorIntegrations.integrationFor(ex) ?: continue; next = applyExecution(next, id, tr, ex, integration.dispatch(TaskExecutorContext(project, definition, next, task, tr, ex, now)), now) }; return next }
    private suspend fun reconcileSystemExecutors(project: Project, definition: WorkflowDefinition, run: WorkflowRun, now: Long): WorkflowRun { var next = run; val tasks = definition.tasks.associateBy { it.id }; for ((id, tr) in run.taskRuns) { if (tr.status != TaskRunStatus.Running && tr.status != TaskRunStatus.Verifying) continue; val task = tasks[id] ?: continue; val ex = tr.executor ?: task.effectiveExecutor(); if (!ex.isSystemExecutor()) continue; val integration = executorIntegrations.integrationFor(ex) ?: continue; next = applyExecution(next, id, tr, ex, integration.reconcile(TaskExecutorContext(project, definition, next, task, tr, ex, now)), now) }; return next }
    private fun refreshAfterSystemExecution(definition: WorkflowDefinition, run: WorkflowRun, now: Long): WorkflowRun { val r = WorkflowRunFactory.refreshReadiness(definition, run, now); return if (r.taskRuns.isNotEmpty() && r.taskRuns.values.all { it.status == TaskRunStatus.Completed }) r.copy(status = WorkflowRunStatus.Completed, updatedAtEpochMillis = now) else r }
    private fun applyExecution(run: WorkflowRun, id: TaskDefinitionId, previous: TaskRun, executor: TaskExecutor, execution: TaskExecutorExecution, now: Long): WorkflowRun { if (execution.status != previous.status) TaskRunTransitions.requireAllowed(previous.status, execution.status); return run.copy(taskRuns = run.taskRuns + (id to previous.copy(status = execution.status, executor = executor, assignedProviderId = null, providerRunId = null, externalRunId = execution.externalRunId ?: previous.externalRunId, artifacts = if (execution.artifacts.isEmpty()) previous.artifacts else execution.artifacts, blockingReason = null, progress = execution.progress ?: if (execution.status == TaskRunStatus.Completed) 1f else previous.progress, progressMessage = execution.progressMessage ?: previous.progressMessage)), updatedAtEpochMillis = now) }
    private fun preserveArtifactTimestamps(previous: WorkflowRun, reconciled: WorkflowRun): WorkflowRun { val p = previous.taskRuns.values.flatMap(TaskRun::artifacts).associateBy { it.id }; if (p.isEmpty()) return reconciled; return reconciled.copy(taskRuns = reconciled.taskRuns.mapValues { (_, tr) -> tr.copy(artifacts = tr.artifacts.map { a -> p[a.id]?.let { a.copy(createdAtEpochMillis = it.createdAtEpochMillis) } ?: a }) }) }
    private fun recoverAvailableSystemExecutors(definition: WorkflowDefinition, run: WorkflowRun, now: Long): WorkflowRun { val defs = definition.tasks.associateBy { it.id }; var changed = false; val runs = run.taskRuns.mapValues { (id, tr) -> if (tr.status != TaskRunStatus.Blocked || tr.blockingReason?.code != WorkflowRunFactory.EXECUTOR_INTEGRATION_UNAVAILABLE) return@mapValues tr; val ex = tr.executor ?: defs[id]?.executor ?: return@mapValues tr; if (!ex.isSystemExecutor() || !executorIntegrations.isAvailable(ex)) return@mapValues tr; TaskRunTransitions.requireAllowed(TaskRunStatus.Blocked, TaskRunStatus.Ready); changed = true; tr.copy(status = TaskRunStatus.Ready, blockingReason = null) }; return if (changed) run.copy(taskRuns = runs, updatedAtEpochMillis = now) else run }
    private fun blockUnavailableSystemExecutors(definition: WorkflowDefinition, run: WorkflowRun): WorkflowRun { val defs = definition.tasks.associateBy { it.id }; var changed = false; val runs = run.taskRuns.mapValues { (id, tr) -> val ex = tr.executor ?: defs[id]?.executor; val dispatchable = tr.status == TaskRunStatus.Ready || tr.status == TaskRunStatus.Retrying || tr.status == TaskRunStatus.Running; if (dispatchable && ex != null && ex.isSystemExecutor() && !executorIntegrations.isAvailable(ex)) { changed = true; tr.copy(status = TaskRunStatus.Blocked, blockingReason = BlockingReason(WorkflowRunFactory.EXECUTOR_INTEGRATION_UNAVAILABLE, "${ex.displayLabel()} is not available in this runtime."), progress = null, progressMessage = null) } else tr }; return if (changed) run.copy(taskRuns = runs) else run }
    private fun TaskExecutor.displayLabel(): String = when (this) { is TaskExecutor.GitHubAction -> "GitHub Action executor"; is TaskExecutor.TestRunner -> "Test runner executor"; is TaskExecutor.Deployment -> "Deployment executor"; is TaskExecutor.RepositoryOperation -> "Repository operation executor"; is TaskExecutor.ExternalService -> "External service executor"; is TaskExecutor.NestedWorkflow -> "Nested workflow executor"; else -> "System executor" }
    private fun TaskRunStatus.isActiveProviderStatus() = this in setOf(TaskRunStatus.Planning, TaskRunStatus.AwaitingApproval, TaskRunStatus.Running, TaskRunStatus.Verifying)
    private fun TaskRunStatus.canReconnect() = isActiveProviderStatus()
    private fun TaskRunStatus.toManagedStatus() = when (this) { TaskRunStatus.Planning -> ManagedSessionStatus.Planning; TaskRunStatus.AwaitingApproval -> ManagedSessionStatus.AwaitingApproval; TaskRunStatus.Running, TaskRunStatus.Verifying -> ManagedSessionStatus.Running; TaskRunStatus.Completed -> ManagedSessionStatus.Completed; TaskRunStatus.Failed -> ManagedSessionStatus.Failed; else -> ManagedSessionStatus.Unknown }
    private fun WorkflowRunStatus.isTerminal() = this in setOf(WorkflowRunStatus.Completed, WorkflowRunStatus.Failed, WorkflowRunStatus.Cancelled)
}