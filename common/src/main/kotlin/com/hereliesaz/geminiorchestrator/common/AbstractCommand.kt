package com.hereliesaz.geminiorchestrator.common

/**
 * The complete, final set of universal commands for the agent system.
 */
sealed interface AbstractCommand {
    // File System
    data class WriteFile(val path: String, val content: String) : AbstractCommand
    data class ReadFile(val path: String) : AbstractCommand
    data class AppendToFile(val path: String, val content: String) : AbstractCommand
    data class DeleteFile(val path: String) : AbstractCommand
    data class LogJournalEntry(val entry: String) : AbstractCommand

    // Execution
    data class RunShellCommand(val command: String, val workingDir: String = ".") : AbstractCommand
    data class RunTests(val module: String?, val testName: String?) : AbstractCommand

    // Version Control
    object GetCurrentBranch : AbstractCommand
    data class CreateAndSwitchToBranch(val branchName: String) : AbstractCommand
    data class SwitchToBranch(val branchName: String) : AbstractCommand
    data class MergeBranch(val branchName: String, val strategy: String = "") : AbstractCommand
    data class DeleteBranch(val branchName: String) : AbstractCommand
    data class StageFiles(val filePaths: List<String>) : AbstractCommand
    data class Commit(val message: String) : AbstractCommand
    object DiscardAllChanges : AbstractCommand

    // UI & User Interaction
    data class DisplayMessage(val message: String) : AbstractCommand
    data class RequestUserDecision(val prompt: String, val options: List<String>) : AbstractCommand
    data class RequestCommitReview(val proposedCommitMessage: String) : AbstractCommand
    data class RequestClarification(val question: String) : AbstractCommand

    // External Tools
    data class PerformWebSearch(val query: String) : AbstractCommand
}
