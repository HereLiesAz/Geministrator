package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalPolicy
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import kotlin.test.Test
import kotlin.test.assertEquals

class StarterWorkflowFactoryTest {
    @Test
    fun implementationRequiresHumanPlanApproval() {
        val definition = StarterWorkflowFactory.create(
            id = WorkflowDefinitionId("starter"),
            objective = "Implement the objective",
        )

        assertEquals(
            ApprovalPolicy.HumanApproval,
            definition.tasks.single { it.id == TaskDefinitionId("implementation") }.approvalPolicy,
        )
    }

    @Test
    fun injectedVerificationTasksDoNotInheritImplementationPlanGate() {
        val definition = StarterWorkflowFactory.create(
            id = WorkflowDefinitionId("starter"),
            objective = "Implement the objective",
        )

        val expanded = WorkflowDefinitionExpander.expand(definition)

        assertEquals(
            ApprovalPolicy.None,
            expanded.tasks.single { it.id == TaskDefinitionId("implementation--pre-code-tests") }.approvalPolicy,
        )
        assertEquals(
            ApprovalPolicy.HumanApproval,
            expanded.tasks.single { it.id == TaskDefinitionId("implementation") }.approvalPolicy,
        )
        assertEquals(
            ApprovalPolicy.None,
            expanded.tasks.single { it.id == TaskDefinitionId("implementation--post-code-tests") }.approvalPolicy,
        )
    }
}
