package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.TaskRunStatus

object TaskRunTransitions {
    private val allowed: Map<TaskRunStatus, Set<TaskRunStatus>> = mapOf(
        TaskRunStatus.Created to setOf(TaskRunStatus.Blocked, TaskRunStatus.Ready, TaskRunStatus.Cancelled),
        TaskRunStatus.Blocked to setOf(TaskRunStatus.Ready, TaskRunStatus.Cancelled),
        TaskRunStatus.Ready to setOf(TaskRunStatus.Planning, TaskRunStatus.Running, TaskRunStatus.Cancelled),
        TaskRunStatus.Planning to setOf(TaskRunStatus.AwaitingApproval, TaskRunStatus.Running, TaskRunStatus.Failed, TaskRunStatus.Cancelled),
        TaskRunStatus.AwaitingApproval to setOf(TaskRunStatus.Running, TaskRunStatus.Retrying, TaskRunStatus.Escalated, TaskRunStatus.Cancelled),
        TaskRunStatus.Running to setOf(TaskRunStatus.Verifying, TaskRunStatus.Completed, TaskRunStatus.Failed, TaskRunStatus.Retrying, TaskRunStatus.Cancelled),
        TaskRunStatus.Verifying to setOf(TaskRunStatus.Completed, TaskRunStatus.Retrying, TaskRunStatus.Failed, TaskRunStatus.Escalated),
        TaskRunStatus.Retrying to setOf(TaskRunStatus.Planning, TaskRunStatus.Running, TaskRunStatus.Escalated, TaskRunStatus.Cancelled),
        TaskRunStatus.Completed to emptySet(),
        TaskRunStatus.Failed to setOf(TaskRunStatus.Retrying, TaskRunStatus.Escalated),
        TaskRunStatus.Escalated to setOf(TaskRunStatus.Retrying, TaskRunStatus.Cancelled),
        TaskRunStatus.Cancelled to emptySet(),
    )

    fun canTransition(from: TaskRunStatus, to: TaskRunStatus): Boolean = to in allowed.getValue(from)

    fun requireAllowed(from: TaskRunStatus, to: TaskRunStatus) {
        require(canTransition(from, to)) { "Illegal task transition: $from -> $to" }
    }
}
