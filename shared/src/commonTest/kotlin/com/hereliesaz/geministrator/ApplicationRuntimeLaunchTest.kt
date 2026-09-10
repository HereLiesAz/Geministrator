package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.domain.ApprovalPolicy
import com.hereliesaz.geministrator.domain.RepositoryRef
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.persistence.InMemoryWorkflowPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApplicationRuntimeLaunchTest {
    @Test
    fun starterLaunchPersistsRepositoryObjectiveAndImplementationPlanGate() = runBlocking {
        val persistence = InMemoryWorkflowPersistence()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val repository = RepositoryRef(
            owner = " HereLiesAz ",
            name = " haive ",
            defaultBranch = " main ",
        )

        try {
            val runtime = ApplicationRuntime.create(
                providers = emptyList(),
                scope = scope,
                persistence = persistence,
            )

            runtime.launchStarterWorkflow(
                projectName = " The Haive ",
                objective = " Ship one complete workflow ",
                repository = repository,
            )

            val live = assertIs<ApplicationRuntimeState.Live>(runtime.state.value)
            val project = persistence.projects.get(live.presentation.run.projectId)
                ?: error("Project was not persisted")
            assertEquals("The Haive", project.name)
            assertEquals(RepositoryRef("HereLiesAz", "haive", "main"), project.repository)
            assertEquals("Ship one complete workflow", live.presentation.run.objective)
            assertEquals(
                ApprovalPolicy.HumanApproval,
                live.presentation.definition.tasks
                    .single { it.id == TaskDefinitionId("implementation") }
                    .approvalPolicy,
            )
        } finally {
            scope.cancel()
        }
    }
}
