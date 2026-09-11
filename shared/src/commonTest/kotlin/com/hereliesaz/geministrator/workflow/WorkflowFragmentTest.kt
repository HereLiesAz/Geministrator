package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TestDesignPolicy
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowFragmentId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkflowFragmentTest {

    private val approval = TaskExecutor.HumanApproval()

    private fun task(id: String, vararg deps: String) = TaskDefinition(
        id = TaskDefinitionId(id),
        name = id,
        objective = id,
        roleId = null,
        executor = approval,
        dependsOn = deps.map(::TaskDefinitionId).toSet(),
    )

    private val baseDefinition = WorkflowDefinition(
        id = WorkflowDefinitionId("base"),
        name = "Base",
        tasks = listOf(task("a")),
        testDesignPolicy = TestDesignPolicy.None,
    )

    @Test
    fun fragmentTasksAreAppendedToDefinition() {
        val fragment = WorkflowFragment(
            id = WorkflowFragmentId("frag"),
            name = "Fragment",
            tasks = listOf(task("b"), task("c", "b")),
        )

        val expanded = baseDefinition.withFragment(fragment)

        assertEquals(3, expanded.tasks.size)
        assertTrue(expanded.tasks.any { it.id == TaskDefinitionId("b") })
        assertTrue(expanded.tasks.any { it.id == TaskDefinitionId("c") })
    }

    @Test
    fun connectFromWiresExistingTasksToFragmentEntryPoints() {
        val fragment = WorkflowFragment(
            id = WorkflowFragmentId("frag"),
            name = "Fragment",
            tasks = listOf(task("b"), task("c", "b")),
        )

        val expanded = baseDefinition.withFragment(
            fragment,
            connectFrom = setOf(TaskDefinitionId("a")),
        )

        val bTask = expanded.tasks.single { it.id == TaskDefinitionId("b") }
        assertTrue(TaskDefinitionId("a") in bTask.dependsOn, "Entry b should depend on a")
        val cTask = expanded.tasks.single { it.id == TaskDefinitionId("c") }
        assertTrue(TaskDefinitionId("b") in cTask.dependsOn, "c should depend on b")
    }

    @Test
    fun connectToWiresFragmentExitPointsToDownstreamTask() {
        val downstreamId = TaskDefinitionId("done")
        val base = baseDefinition.copy(tasks = baseDefinition.tasks + task("done"))

        val fragment = WorkflowFragment(
            id = WorkflowFragmentId("frag"),
            name = "Fragment",
            tasks = listOf(task("b"), task("c", "b")),
        )

        val expanded = base.withFragment(
            fragment,
            connectTo = setOf(downstreamId),
        )

        val done = expanded.tasks.single { it.id == downstreamId }
        assertTrue(TaskDefinitionId("c") in done.dependsOn, "done should depend on exit point c")
    }

    @Test
    fun fragmentEntryAndExitPointsAreComputedCorrectly() {
        val fragment = WorkflowFragment(
            id = WorkflowFragmentId("frag"),
            name = "Fragment",
            tasks = listOf(task("entry"), task("mid", "entry"), task("exit", "mid")),
        )

        assertEquals(setOf(TaskDefinitionId("entry")), fragment.entryPoints)
        assertEquals(setOf(TaskDefinitionId("exit")), fragment.exitPoints)
    }

    @Test
    fun fragmentValidatesCorrectlyAfterExpansion() {
        val fragment = WorkflowFragment(
            id = WorkflowFragmentId("frag"),
            name = "Fragment",
            tasks = listOf(task("b", "a")),
        )

        val expanded = baseDefinition.withFragment(fragment)
        val errors = WorkflowGraphValidator.validate(expanded)
        assertTrue(errors.isEmpty(), "Expanded definition should be valid: $errors")
    }
}
