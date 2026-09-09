package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.EnvironmentPlanningPolicy
import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TestDesignPolicy
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId

object StarterWorkflowFactory {
    fun create(
        id: WorkflowDefinitionId,
        objective: String,
    ): WorkflowDefinition {
        val implementationId = TaskDefinitionId("implementation")
        val verificationId = TaskDefinitionId("verification")
        val reviewId = TaskDefinitionId("review")
        val releaseId = TaskDefinitionId("release-approval")

        return WorkflowDefinition(
            id = id,
            name = objective.take(80),
            description = "Starter workflow created from the live runtime empty state.",
            tasks = listOf(
                TaskDefinition(
                    id = implementationId,
                    name = "Implement objective",
                    objective = objective,
                    roleId = BuiltInRoles.ImplementationEngineer.id,
                    executor = TaskExecutor.RoleAgent(BuiltInRoles.ImplementationEngineer.id),
                    environmentPlanningPolicy = EnvironmentPlanningPolicy.NotRequired,
                ),
                TaskDefinition(
                    id = verificationId,
                    name = "Verify objective",
                    objective = "Independently verify the implementation satisfies the objective: $objective",
                    roleId = BuiltInRoles.QaEngineer.id,
                    executor = TaskExecutor.RoleAgent(BuiltInRoles.QaEngineer.id),
                    dependsOn = setOf(implementationId),
                    environmentPlanningPolicy = EnvironmentPlanningPolicy.NotRequired,
                ),
                TaskDefinition(
                    id = reviewId,
                    name = "Review implementation",
                    objective = "Independently review the verified implementation for correctness and unintended side effects.",
                    roleId = BuiltInRoles.CodeReviewer.id,
                    executor = TaskExecutor.RoleAgent(BuiltInRoles.CodeReviewer.id),
                    dependsOn = setOf(verificationId),
                    environmentPlanningPolicy = EnvironmentPlanningPolicy.NotRequired,
                ),
                TaskDefinition(
                    id = releaseId,
                    name = "Release approval",
                    objective = "Approve the verified and reviewed result for release.",
                    roleId = BuiltInRoles.ReleaseEngineer.id,
                    executor = TaskExecutor.HumanApproval("Approve release"),
                    dependsOn = setOf(reviewId),
                    environmentPlanningPolicy = EnvironmentPlanningPolicy.NotRequired,
                ),
            ),
            testDesignPolicy = TestDesignPolicy.BeforeAndAfterImplementation,
        )
    }
}
