package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowDefinition

sealed interface WorkflowValidationError {
    data class DuplicateTaskId(val taskId: TaskDefinitionId) : WorkflowValidationError
    data class MissingDependency(
        val taskId: TaskDefinitionId,
        val missingDependencyId: TaskDefinitionId,
    ) : WorkflowValidationError
    data class SelfDependency(val taskId: TaskDefinitionId) : WorkflowValidationError
    data class Cycle(val taskIds: Set<TaskDefinitionId>) : WorkflowValidationError
}

object WorkflowGraphValidator {
    fun validate(definition: WorkflowDefinition): List<WorkflowValidationError> {
        val errors = mutableListOf<WorkflowValidationError>()
        val grouped = definition.tasks.groupBy { it.id }
        grouped.filterValues { it.size > 1 }.keys.forEach {
            errors += WorkflowValidationError.DuplicateTaskId(it)
        }

        val knownIds = grouped.keys
        definition.tasks.forEach { task ->
            task.dependsOn.forEach { dependency ->
                when {
                    dependency == task.id -> errors += WorkflowValidationError.SelfDependency(task.id)
                    dependency !in knownIds -> errors += WorkflowValidationError.MissingDependency(task.id, dependency)
                }
            }
        }

        if (errors.any { it is WorkflowValidationError.DuplicateTaskId }) {
            return errors
        }

        val cycleNodes = detectCycleNodes(definition)
        if (cycleNodes.isNotEmpty()) {
            errors += WorkflowValidationError.Cycle(cycleNodes)
        }

        return errors
    }

    fun requireValid(definition: WorkflowDefinition) {
        val errors = validate(definition)
        require(errors.isEmpty()) {
            "Invalid workflow graph: ${errors.joinToString()}"
        }
    }

    private fun detectCycleNodes(definition: WorkflowDefinition): Set<TaskDefinitionId> {
        val dependencies = definition.tasks.associate { it.id to it.dependsOn }
        val visiting = mutableSetOf<TaskDefinitionId>()
        val visited = mutableSetOf<TaskDefinitionId>()
        val cycleNodes = linkedSetOf<TaskDefinitionId>()

        fun visit(taskId: TaskDefinitionId, path: MutableList<TaskDefinitionId>) {
            if (taskId in visited) return
            if (taskId in visiting) {
                val cycleStart = path.indexOf(taskId)
                if (cycleStart >= 0) {
                    cycleNodes += path.subList(cycleStart, path.size)
                }
                cycleNodes += taskId
                return
            }

            visiting += taskId
            path += taskId
            dependencies[taskId].orEmpty()
                .filter { it in dependencies }
                .forEach { visit(it, path) }
            path.removeAt(path.lastIndex)
            visiting -= taskId
            visited += taskId
        }

        dependencies.keys.forEach { visit(it, mutableListOf()) }
        return cycleNodes
    }
}
