package com.hereliesaz.geministrator.core

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.ILogger

sealed class WorkflowStatus {
    data class Success(val commitMessage: String, val successfulSteps: List<String>) : WorkflowStatus()
    data class Failure(val reason: String) : WorkflowStatus()
    data class TestsFailed(val testOutput: String, val successfulSteps: List<String>) : WorkflowStatus()
    data class Paused(val message: String, val successfulSteps: List<String>) : WorkflowStatus()
}

class Manager(private val adapter: ExecutionAdapter, private val logger: ILogger) {
    suspend fun executeWorkflow(workflow: List<AbstractCommand>, prompt: String): WorkflowStatus {
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

            // Intercept PauseAndExit before it reaches any adapter.
            if (command is AbstractCommand.PauseAndExit) {
                logger.info("  [Manager] -> Intercepted PauseAndExit command.")
                return WorkflowStatus.Paused(command.checkInMessage, successfulSteps)
            }

            val result = adapter.execute(command)

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