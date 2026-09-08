package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.BlockingReason
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.domain.effectiveExecutor

object WorkflowRunFactory {
    fun create(
        definition: WorkflowDefinition,
        workflowRunId: WorkflowRunId,
        projectId: ProjectId,
        objective: String,
        nowEpochMillis: Long,
        taskRunIdFactory: (TaskDefinitionId) -> TaskRunId,
    ): WorkflowRun {
        WorkflowGraphValidator.requireValid(definition)

        val taskRuns = definition.tasks.associate { task ->
            val status = if (task.dependsOn.isEmpty()) TaskRunStatus.Ready else TaskRunStatus.Blocked
            task.id to TaskRun(
                id = taskRunIdFactory(task.id),
                taskDefinitionId = task.id,
                status = status,
                assignedRoleId = task.roleId,
                executor = task.effectiveExecutor(),
                blockingReason = if (status == TaskRunStatus.Blocked) {
                    BlockingReason(
                        code = "WAITING_FOR_DEPENDENCIES",
                        message = "Waiting for ${task.dependsOn.size} task dependency/dependencies.",
                    )
                } else {
                    null
                },
            )
        }

        return WorkflowRun(
            id = workflowRunId,
            projectId = projectId,
            workflowDefinitionId = definition.id,
            objective = objective,
            status = WorkflowRunStatus.Created,
            taskRuns = taskRuns,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
        )
    }

    fun refreshReadiness(
        definition: WorkflowDefinition,
        run: WorkflowRun,
        nowEpochMillis: Long,
    ): WorkflowRun {
        val definitionsById = definition.tasks.associateBy { it.id }
        val refreshed = run.taskRuns.mapValues { (taskId, taskRun) ->
            if (taskRun.status != TaskRunStatus.Blocked) return@mapValues taskRun

            val task = definitionsById[taskId] ?: return@mapValues taskRun
            val dependencyRuns = task.dependsOn.mapNotNull(run.taskRuns::get)
            val hasFailedDependency = dependencyRuns.any {
                it.status == TaskRunStatus.Failed ||
                    it.status == TaskRunStatus.Escalated ||
                    it.status == TaskRunStatus.Cancelled
            }
            val allCompleted = dependencyRuns.size == task.dependsOn.size &&
                dependencyRuns.all { it.status == TaskRunStatus.Completed }

            when {
                allCompleted -> taskRun.copy(
                    status = TaskRunStatus.Ready,
                    blockingReason = null,
                )
                hasFailedDependency -> taskRun.copy(
                    blockingReason = BlockingReason(
                        code = "DEPENDENCY_FAILED",
                        message = "A dependency did not complete successfully.",
                    ),
                )
                else -> taskRun
            }
        }

        return run.copy(
            taskRuns = refreshed,
            updatedAtEpochMillis = nowEpochMillis,
        )
    }
}
