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
        logger.info("Manager starting workflow with ${workflow.size} steps.")
        if (workflow.isEmpty()) {
            logger.info("WARNING: Manager received an empty workflow. Nothing to do.")
            return WorkflowStatus.Success("No changes made.", emptyList())
        }

        val successfulSteps = mutableListOf<String>()
        for (command in workflow) {
            val commandName = command::class.simpleName ?: "UnknownCommand"
            logger.info("---")
            logger.info("  [Manager] -> Delegating command: $commandName")
            val result = adapter.execute(command)

            // If the adapter executes PauseAndExit, the application will terminate,
            // and the code below this line will not be reached.
            if (command is AbstractCommand.PauseAndExit) {
                // This return is effectively unreachable but satisfies the compiler.
                return WorkflowStatus.Success("Workflow paused by user.", successfulSteps)
            }

            if (!result.isSuccess) {
                val reason = "Execution of $commandName failed: ${result.output}"
                logger.error("  ERROR: $reason")

                // If tests failed, we return a specific status for self-correction.
                if (command is AbstractCommand.RunTests) {
                    logger.error("  TESTS FAILED!")
                    return WorkflowStatus.TestsFailed(result.output, successfulSteps)
                }

                return WorkflowStatus.Failure(reason)
            }
            logger.info("  SUCCESS: ${result.output}")
            successfulSteps.add(commandName)
        }

        logger.info("---")
        logger.info("Manager completed workflow successfully.")
        return WorkflowStatus.Success(prompt, successfulSteps)
    }
}