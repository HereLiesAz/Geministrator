package com.hereliesaz.geministrator.core

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.ILogger

sealed class WorkflowStatus {
    data class Success(val commitMessage: String, val successfulSteps: List<String>) : WorkflowStatus()
    data class Failure(val reason: String) : WorkflowStatus()
    data class TestsFailed(val testOutput: String, val successfulSteps: List<String>) : WorkflowStatus()
}

class Manager(private val adapter: ExecutionAdapter, private val logger: ILogger) {
    fun executeWorkflow(workflow: List<AbstractCommand>, prompt: String): WorkflowStatus {
        logger.log("Manager starting workflow with ${workflow.size} steps.")
        if (workflow.isEmpty()) {
            logger.log("WARNING: Manager received an empty workflow. Nothing to do.")
            return WorkflowStatus.Success("No changes made.", emptyList())
        }

        val successfulSteps = mutableListOf<String>()
        for (command in workflow) {
            val commandName = command::class.simpleName ?: "UnknownCommand"
            logger.log("---")
            logger.log("  [Manager] -> Delegating command: $commandName")
            val result = adapter.execute(command)

            if (!result.isSuccess) {
                val reason = "Execution of $commandName failed: ${result.output}"
                logger.log("  ERROR: $reason")

                // If tests failed, we return a specific status for self-correction.
                if (command is AbstractCommand.RunTests) {
                    logger.log("  TESTS FAILED!")
                    return WorkflowStatus.TestsFailed(result.output, successfulSteps)
                }

                return WorkflowStatus.Failure(reason)
            }
            logger.log("  SUCCESS: ${result.output}")
            successfulSteps.add(commandName)
        }

        logger.log("---")
        logger.log("Manager completed workflow successfully.")
        return WorkflowStatus.Success(prompt, successfulSteps)
    }
}