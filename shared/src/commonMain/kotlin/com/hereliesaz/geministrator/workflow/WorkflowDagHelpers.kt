package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId

/**
 * Returns copies of [branches] each with [from] added to their [TaskDefinition.dependsOn].
 * The underlying DAG is unchanged — fan-out is native to the model; this is a convenience
 * for authoring several tasks that share a single upstream dependency.
 */
fun fanOut(from: TaskDefinitionId, branches: List<TaskDefinition>): List<TaskDefinition> =
    branches.map { it.copy(dependsOn = it.dependsOn + from) }

/**
 * Returns a copy of [into] with all of [from] added to its [TaskDefinition.dependsOn].
 * Fan-in is native to the model; this is a convenience for a single downstream task that
 * waits on multiple upstream tasks simultaneously.
 */
fun fanIn(from: List<TaskDefinitionId>, into: TaskDefinition): TaskDefinition =
    into.copy(dependsOn = into.dependsOn + from)
