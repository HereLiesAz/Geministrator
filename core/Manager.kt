package com.hereliesaz.geministrator.core

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.core.council.ILogger

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
                return WorkflowStatus.Failure(reason)
            }
            logger.log("  SUCCESS: ${result.output}")
            successfulSteps.add(commandName)

            if (command is AbstractCommand.WriteFile) {
                logger.log("  [Manager] -> Auto-running tests after file modification...")
                val testResult = adapter.execute(AbstractCommand.RunTests(null, null))
                if (!testResult.isSuccess) {
                    logger.log("  TESTS FAILED!")
                    return WorkflowStatus.TestsFailed(testResult.output, successfulSteps)
                }
                logger.log("  All tests passed after modification.")
            }
        }

        logger.log("---")
        logger.log("Manager completed workflow successfully.")
        return WorkflowStatus.Success(prompt, successfulSteps)
    }
}