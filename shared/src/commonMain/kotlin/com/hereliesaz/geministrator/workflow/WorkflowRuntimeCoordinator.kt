package com.hereliesaz.geministrator.workflow

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
import com.hereliesaz.geministrator.persistence.WorkflowPersistence
import com.hereliesaz.geministrator.providers.ProviderArtifact

data class WorkflowRuntimeState(
    val run: WorkflowRun,
    val handles: Map<TaskDefinitionId, ManagedSessionHandle> = emptyMap(),
)

class WorkflowRuntimeCoordinator(
    private val persistence: WorkflowPersistence,
    private val engine: WorkflowEngine,
    private val sessionGateway: ManagedSessionGateway,
    private val executorIntegrations: TaskExecutorIntegrationRegistry = TaskExecutorIntegrationRegistry.Empty,
) {
    suspend fun persist(project: Project, definition: WorkflowDefinition, state: WorkflowRuntimeState) {
        persistence.projects.put(project)
        persistence.definitions.put(definition)
        persistence.runs.put(state.run)
        state.run.taskRuns.values.flatMap(TaskRun::artifacts).forEach { persistence.artifacts.put(it) }
    }

    suspend fun resume(workflowRunId: WorkflowRunId): WorkflowRuntimeState {
        val run = requireNotNull(persistence.runs.get(workflowRunId)) {
            "Workflow run ${workflowRunId.value} was not found"
        }
        val handles = buildMap {
            run.taskRuns.forEach { (taskDefinitionId, taskRun) ->
                val providerId = taskRun.assignedProviderId ?: return@forEach
                val providerRunId = taskRun.providerRunId ?: return@forEach
                if (!taskRun.status.canReconnect()) return@forEach
                val handle = ManagedSessionHandle(taskRun.id, providerId, providerRunId)
                sessionGateway.reconnect(handle, taskRun.status.toManagedStatus())
                put(taskDefinitionId, handle)
            }
        }
        return WorkflowRuntimeState(run, handles)
    }

    suspend fun cycle(
        project: Project,
        definition: WorkflowDefinition,
        state: WorkflowRuntimeState,
        nowEpochMillis: Long,
        artifactIdFactory: (TaskRun, ProviderArtifact, Int) -> ArtifactId,
    ): WorkflowRuntimeState {
        if (state.run.status == WorkflowRunStatus.AwaitingHuman && state.handles.isEmpty()) return state

        var nextRun = engine.reconcile(definition, state.run, state.handles, nowEpochMillis, artifactIdFactory)
        nextRun = preserveArtifactTimestamps(state.run, nextRun)
        nextRun = reconcileSystemExecutors(project, definition, nextRun, nowEpochMillis)
        nextRun = refreshAfterSystemExecution(definition, nextRun, nowEpochMillis)

        val newlyFailedTaskIds = nextRun.taskRuns.filter { (taskId, taskRun) ->
            taskRun.status == TaskRunStatus.Failed && state.run.taskRuns[taskId]?.status != TaskRunStatus.Failed
        }.keys
        for (taskId in newlyFailedTaskIds) {
            nextRun = engine.handleFailure(
                definition,
                nextRun,
                taskId,
                RetryReason.ProviderFailure,
                "Executor failed",
                nowEpochMillis,
            )
        }

        if (state.run.status == WorkflowRunStatus.AwaitingHuman ||
            nextRun.taskRuns.values.any { it.status == TaskRunStatus.AwaitingApproval || it.status == TaskRunStatus.Escalated }
        ) nextRun = nextRun.copy(status = WorkflowRunStatus.AwaitingHuman)

        var nextHandles = state.handles.filterKeys { taskId ->
            nextRun.taskRuns[taskId]?.status !in setOf(
                TaskRunStatus.Completed,
                TaskRunStatus.Failed,
                TaskRunStatus.Cancelled,
                TaskRunStatus.Retrying,
                TaskRunStatus.Escalated,
            )
        }

        nextRun = blockUnavailableSystemExecutors(definition, nextRun)
        var nextState = WorkflowRuntimeState(nextRun, nextHandles)
        persist(project, definition, nextState)
        if (nextRun.status.isTerminal() || nextRun.status == WorkflowRunStatus.AwaitingHuman) return nextState

        nextRun = dispatchSystemExecutors(project, definition, nextRun, nowEpochMillis)
        nextRun = refreshAfterSystemExecution(definition, nextRun, nowEpochMillis)
        if (nextRun.status.isTerminal()) {
            nextState = WorkflowRuntimeState(nextRun, nextHandles)
            persist(project, definition, nextState)
            return nextState
        }

        val dispatched = engine.dispatchReadyTasks(project, definition, nextRun, nextHandles, nowEpochMillis)
        nextRun = dispatched.run
        nextHandles = dispatched.handles.filterKeys { nextRun.taskRuns[it]?.status?.isActiveProviderStatus() == true }
        nextState = WorkflowRuntimeState(nextRun, nextHandles)
        persist(project, definition, nextState)
        return nextState
    }

    private suspend fun dispatchSystemExecutors(
        project: Project,
        definition: WorkflowDefinition,
        run: WorkflowRun,
        nowEpochMillis: Long,
    ): WorkflowRun {
        var nextRun = run
        val tasks = definition.tasks.associateBy { it.id }
        for ((taskId, taskRun) in run.taskRuns) {
            val task = tasks[taskId] ?: continue
            val executor = taskRun.executor ?: task.effectiveExecutor()
            if (!executor.isSystemExecutor()) continue
            if (taskRun.status != TaskRunStatus.Ready && taskRun.status != TaskRunStatus.Retrying) continue
            val integration = executorIntegrations.integrationFor(executor) ?: continue
            val execution = integration.dispatch(
                TaskExecutorContext(project, definition, nextRun, task, taskRun, executor, nowEpochMillis),
            )
            nextRun = applyExecution(nextRun, taskId, taskRun, executor, execution, nowEpochMillis)
        }
        return nextRun
    }

    private suspend fun reconcileSystemExecutors(
        project: Project,
        definition: WorkflowDefinition,
        run: WorkflowRun,
        nowEpochMillis: Long,
    ): WorkflowRun {
        var nextRun = run
        val tasks = definition.tasks.associateBy { it.id }
        for ((taskId, taskRun) in run.taskRuns) {
            if (taskRun.status != TaskRunStatus.Running && taskRun.status != TaskRunStatus.Verifying) continue
            val task = tasks[taskId] ?: continue
            val executor = taskRun.executor ?: task.effectiveExecutor()
            if (!executor.isSystemExecutor()) continue
            val integration = executorIntegrations.integrationFor(executor) ?: continue
            val execution = integration.reconcile(
                TaskExecutorContext(project, definition, nextRun, task, taskRun, executor, nowEpochMillis),
            )
            nextRun = applyExecution(nextRun, taskId, taskRun, executor, execution, nowEpochMillis)
        }
        return nextRun
    }

    private fun refreshAfterSystemExecution(
        definition: WorkflowDefinition,
        run: WorkflowRun,
        nowEpochMillis: Long,
    ): WorkflowRun {
        val refreshed = WorkflowRunFactory.refreshReadiness(definition, run, nowEpochMillis)
        return if (refreshed.taskRuns.isNotEmpty() && refreshed.taskRuns.values.all { it.status == TaskRunStatus.Completed }) {
            refreshed.copy(status = WorkflowRunStatus.Completed, updatedAtEpochMillis = nowEpochMillis)
        } else {
            refreshed
        }
    }

    private fun applyExecution(
        run: WorkflowRun,
        taskId: TaskDefinitionId,
        previous: TaskRun,
        executor: TaskExecutor,
        execution: TaskExecutorExecution,
        nowEpochMillis: Long,
    ): WorkflowRun = run.copy(
        taskRuns = run.taskRuns + (taskId to previous.copy(
            status = execution.status,
            executor = executor,
            assignedProviderId = null,
            providerRunId = null,
            externalRunId = execution.externalRunId ?: previous.externalRunId,
            artifacts = if (execution.artifacts.isEmpty()) previous.artifacts else execution.artifacts,
            blockingReason = null,
            progress = execution.progress ?: if (execution.status == TaskRunStatus.Completed) 1f else previous.progress,
            progressMessage = execution.progressMessage ?: previous.progressMessage,
        )),
        updatedAtEpochMillis = nowEpochMillis,
    )

    private fun preserveArtifactTimestamps(previous: WorkflowRun, reconciled: WorkflowRun): WorkflowRun {
        val previousArtifacts = previous.taskRuns.values.flatMap(TaskRun::artifacts).associateBy { it.id }
        if (previousArtifacts.isEmpty()) return reconciled
        return reconciled.copy(taskRuns = reconciled.taskRuns.mapValues { (_, taskRun) ->
            taskRun.copy(artifacts = taskRun.artifacts.map { artifact ->
                previousArtifacts[artifact.id]?.let { artifact.copy(createdAtEpochMillis = it.createdAtEpochMillis) } ?: artifact
            })
        })
    }

    private fun blockUnavailableSystemExecutors(definition: WorkflowDefinition, run: WorkflowRun): WorkflowRun {
        val definitions = definition.tasks.associateBy { it.id }
        var changed = false
        val taskRuns = run.taskRuns.mapValues { (taskId, taskRun) ->
            val executor = taskRun.executor ?: definitions[taskId]?.executor
            val dispatchable = taskRun.status == TaskRunStatus.Ready || taskRun.status == TaskRunStatus.Retrying || taskRun.status == TaskRunStatus.Running
            if (dispatchable && executor != null && executor.isSystemExecutor() && !executorIntegrations.isAvailable(executor)) {
                changed = true
                taskRun.copy(
                    status = TaskRunStatus.Blocked,
                    blockingReason = BlockingReason(
                        WorkflowRunFactory.EXECUTOR_INTEGRATION_UNAVAILABLE,
                        "${executor.displayLabel()} is not available in this runtime.",
                    ),
                    progress = null,
                    progressMessage = null,
                )
            } else taskRun
        }
        return if (changed) run.copy(taskRuns = taskRuns) else run
    }

    private fun TaskExecutor.displayLabel(): String = when (this) {
        is TaskExecutor.GitHubAction -> "GitHub Action executor"
        is TaskExecutor.TestRunner -> "Test runner executor"
        is TaskExecutor.Deployment -> "Deployment executor"
        is TaskExecutor.RepositoryOperation -> "Repository operation executor"
        is TaskExecutor.ExternalService -> "External service executor"
        is TaskExecutor.NestedWorkflow -> "Nested workflow executor"
        else -> "System executor"
    }

    private fun TaskRunStatus.isActiveProviderStatus() = this in setOf(TaskRunStatus.Planning, TaskRunStatus.AwaitingApproval, TaskRunStatus.Running, TaskRunStatus.Verifying)
    private fun TaskRunStatus.canReconnect() = isActiveProviderStatus()
    private fun TaskRunStatus.toManagedStatus() = when (this) {
        TaskRunStatus.Planning -> ManagedSessionStatus.Planning
        TaskRunStatus.AwaitingApproval -> ManagedSessionStatus.AwaitingApproval
        TaskRunStatus.Running, TaskRunStatus.Verifying -> ManagedSessionStatus.Running
        TaskRunStatus.Completed -> ManagedSessionStatus.Completed
        TaskRunStatus.Failed -> ManagedSessionStatus.Failed
        else -> ManagedSessionStatus.Unknown
    }
    private fun WorkflowRunStatus.isTerminal() = this in setOf(WorkflowRunStatus.Completed, WorkflowRunStatus.Failed, WorkflowRunStatus.Cancelled)
}
