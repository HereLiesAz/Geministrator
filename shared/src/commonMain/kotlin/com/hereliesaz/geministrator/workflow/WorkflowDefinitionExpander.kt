package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ArtifactKind
import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TestDesignPolicy
import com.hereliesaz.geministrator.domain.WorkflowDefinition

object WorkflowDefinitionExpander {
    fun expand(definition: WorkflowDefinition): WorkflowDefinition {
        WorkflowGraphValidator.requireValid(definition)

        val includePreCodeTests = definition.testDesignPolicy == TestDesignPolicy.BeforeImplementation ||
            definition.testDesignPolicy == TestDesignPolicy.BeforeAndAfterImplementation

        if (!includePreCodeTests) return definition

        val expanded = buildList {
            definition.tasks.forEach { task ->
                if (task.roleId != BuiltInRoles.ImplementationEngineer.id) {
                    add(task)
                    return@forEach
                }

                val testTaskId = TaskDefinitionId("${task.id.value}--pre-code-tests")
                require(definition.tasks.none { it.id == testTaskId }) {
                    "Cannot inject pre-code test task because ${testTaskId.value} already exists"
                }

                add(
                    TaskDefinition(
                        id = testTaskId,
                        name = "Pre-code tests: ${task.name}",
                        objective = "Derive the verification contract for '${task.name}' from approved specifications, architecture, acceptance criteria, and concepts without inspecting implementation code.",
                        roleId = BuiltInRoles.CrashTestDummy.id,
                        dependsOn = task.dependsOn,
                        acceptanceCriteria = task.acceptanceCriteria,
                        requiredArtifacts = setOf(
                            ArtifactKind.AcceptanceTestPlan,
                            ArtifactKind.BehavioralTest,
                            ArtifactKind.ContractTest,
                            ArtifactKind.FailureScenario,
                        ),
                        approvalPolicy = task.approvalPolicy,
                        retryPolicy = task.retryPolicy,
                        escalationPolicy = task.escalationPolicy,
                        providerConstraints = task.providerConstraints,
                        environmentPlanningPolicy = task.environmentPlanningPolicy,
                    ),
                )

                add(
                    task.copy(
                        dependsOn = task.dependsOn + testTaskId,
                    ),
                )
            }
        }

        return definition.copy(tasks = expanded).also(WorkflowGraphValidator::requireValid)
    }
}
