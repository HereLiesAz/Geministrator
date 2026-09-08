package com.hereliesaz.geministrator

import com.hereliesaz.conveyance.h2g2.H2g2WorkflowBand
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowEdge
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowMotion
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowNode
import com.hereliesaz.conveyance.h2g2.H2g2WorkflowState
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRun

internal data class WorkflowMindMapProjection(
    val bands: List<H2g2WorkflowBand>,
    val edges: List<H2g2WorkflowEdge>,
)

internal fun projectWorkflowMindMap(
    definition: WorkflowDefinition,
    run: WorkflowRun,
    roles: Collection<RoleDefinition>,
): WorkflowMindMapProjection {
    val tasksById = definition.tasks.associateBy { it.id }
    val rolesById = roles.associateBy { it.id }
    val depthByTask = mutableMapOf<TaskDefinitionId, Int>()

    fun depth(task: TaskDefinition): Int = depthByTask.getOrPut(task.id) {
        if (task.dependsOn.isEmpty()) {
            0
        } else {
            1 + task.dependsOn.maxOf { dependencyId ->
                tasksById[dependencyId]?.let(::depth) ?: 0
            }
        }
    }

    val nodes = definition.tasks.map { task ->
        val taskRun = run.taskRuns[task.id]
        val role = taskRun?.assignedRoleId?.let(rolesById::get) ?: rolesById[task.roleId]
        val roleName = role?.name ?: task.roleId.value
        val state = taskRun?.status.toH2g2State()
        H2g2WorkflowNode(
            id = task.id.value,
            label = roleName,
            subtitle = task.name,
            hueSeed = roleName,
            motionSeed = roleName,
            motion = roleMotion(roleName),
            state = state,
            progress = taskRun?.displayProgress(),
            detail = buildString {
                append(task.objective)
                taskRun?.assignedProviderId?.let {
                    append(" · Staffed by ")
                    append(it.value)
                }
                taskRun?.progressMessage?.takeIf(String::isNotBlank)?.let {
                    append(" · ")
                    append(it)
                }
                taskRun?.blockingReason?.message?.takeIf(String::isNotBlank)?.let {
                    append(" · ")
                    append(it)
                }
                taskRun?.attempt?.takeIf { it > 1 }?.let {
                    append(" · Attempt ")
                    append(it)
                }
            },
        )
    }

    val nodeById = nodes.associateBy { it.id }
    val bands = definition.tasks
        .groupBy(::depth)
        .toSortedMap()
        .values
        .map { tasks ->
            H2g2WorkflowBand(
                tasks.mapNotNull { nodeById[it.id.value] },
            )
        }

    val edges = definition.tasks.flatMap { task ->
        task.dependsOn.map { dependency ->
            H2g2WorkflowEdge(
                from = dependency.value,
                to = task.id.value,
            )
        }
    }

    return WorkflowMindMapProjection(bands = bands, edges = edges)
}

private fun TaskRunStatus?.toH2g2State(): H2g2WorkflowState = when (this) {
    null,
    TaskRunStatus.Created,
    -> H2g2WorkflowState.Pending
    TaskRunStatus.Blocked -> H2g2WorkflowState.Blocked
    TaskRunStatus.Ready -> H2g2WorkflowState.Ready
    TaskRunStatus.AwaitingApproval,
    TaskRunStatus.Escalated,
    -> H2g2WorkflowState.Gate
    TaskRunStatus.Planning,
    TaskRunStatus.Running,
    TaskRunStatus.Verifying,
    TaskRunStatus.Retrying,
    -> H2g2WorkflowState.Active
    TaskRunStatus.Completed -> H2g2WorkflowState.Complete
    TaskRunStatus.Failed,
    TaskRunStatus.Cancelled,
    -> H2g2WorkflowState.Failed
}

/**
 * Exact provider progress wins. Otherwise this is deliberately a lifecycle plateau, not a claimed
 * percentage of provider work. It gives the node a truthful sense of governed progress even when a
 * provider such as Jules exposes qualitative updates but no numeric fraction.
 */
private fun TaskRun.displayProgress(): Float? = progress ?: when (status) {
    TaskRunStatus.Planning -> .14f
    TaskRunStatus.AwaitingApproval -> .24f
    TaskRunStatus.Running,
    TaskRunStatus.Retrying,
    -> .52f
    TaskRunStatus.Verifying -> .82f
    TaskRunStatus.Completed -> 1f
    else -> null
}

/** Motion belongs to the company position, not whichever provider happens to staff it. */
internal fun roleMotion(role: String): H2g2WorkflowMotion = when (role) {
    "Orchestrator" -> H2g2WorkflowMotion.Orbit
    "Product Manager" -> H2g2WorkflowMotion.Nod
    "Researcher" -> H2g2WorkflowMotion.Float
    "Architect" -> H2g2WorkflowMotion.Pendulum
    "EPA Representative" -> H2g2WorkflowMotion.Hover
    "UX Designer" -> H2g2WorkflowMotion.Sway
    "Implementation Engineer" -> H2g2WorkflowMotion.Scoot
    "Crash Test Dummy" -> H2g2WorkflowMotion.Wag
    "QA Engineer" -> H2g2WorkflowMotion.Skitter
    "Adversarial Reviewer" -> H2g2WorkflowMotion.Shimmy
    "Code Reviewer" -> H2g2WorkflowMotion.Tilt
    "Recovery Engineer" -> H2g2WorkflowMotion.Bob
    "Release Engineer" -> H2g2WorkflowMotion.Pulse
    else -> H2g2WorkflowMotion.Breathe
}
