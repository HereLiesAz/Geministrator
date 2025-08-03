package com.hereliesaz.geministrator.adapter

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.ExecutionResult
import java.io.File
import java.util.concurrent.TimeUnit

class CliAdapter : ExecutionAdapter {
    override fun execute(command: AbstractCommand): ExecutionResult {
        return when (command) {
            is AbstractCommand.AppendToFile -> try { File(command.path).appendText(command.content); ExecutionResult(true, "Appended to ${command.path}") } catch (e: Exception) { ExecutionResult(false, e.message ?: "Failed to append") }
            is AbstractCommand.CreateAndSwitchToBranch -> runCommand(
                listOf(
                    "git",
                    "checkout",
                    "-b",
                    command.branchName
                )
            )

            is AbstractCommand.DeleteBranch -> runCommand(
                listOf(
                    "git",
                    "branch",
                    "-D",
                    command.branchName
                )
            )
            is AbstractCommand.DeleteFile -> try { File(command.path).delete(); ExecutionResult(true, "Deleted ${command.path}") } catch (e: Exception) { ExecutionResult(false, "Failed to delete file") }
            is AbstractCommand.DiscardAllChanges -> {
                val resetResult = runCommand(listOf("git", "reset", "--hard", "HEAD"))
                if (!resetResult.isSuccess) return resetResult
                runCommand(listOf("git", "clean", "-fd"))
            }

            is AbstractCommand.GetCurrentBranch -> runCommand(
                listOf(
                    "git",
                    "rev-parse",
                    "--abbrev-ref",
                    "HEAD"
                )
            )

            is AbstractCommand.LogJournalEntry -> try {
                val file = File(".orchestrator/journal.log")
                file.parentFile.mkdirs()
                file.appendText(command.entry)
                ExecutionResult(true, "Logged journal entry")
            } catch (e: Exception) {
                ExecutionResult(false, e.message ?: "Failed to log journal entry")
            }

            is AbstractCommand.MergeBranch -> runCommand(listOf("git", "merge", command.branchName))
            is AbstractCommand.PerformWebSearch -> ExecutionResult(true, "Simulated web search for: ${command.query}. Found official documentation.", null)
            is AbstractCommand.ReadFile -> try { ExecutionResult(true, "Read file successfully.", File(command.path).readText()) } catch (e: Exception) { ExecutionResult(false, "Failed to read file: ${e.message}") }
            is AbstractCommand.RequestClarification -> { println("\n--- CLARIFICATION REQUIRED ---\n${command.question}"); print("Your response: "); ExecutionResult(true, "User provided clarification.", readlnOrNull() ?: "") }
            is AbstractCommand.RequestCommitReview -> {
                println("\n--- PENDING COMMIT: FINAL REVIEW ---\nProposed Commit Message: \"${command.proposedCommitMessage}\"\n--- STAGED CHANGES ---");
                val diffResult = runCommand(
                    listOf(
                        "git",
                        "diff",
                        "--staged"
                    )
                ); println(diffResult.output); print("Approve and commit these changes? (y/n): ");
                val choice =
                    if (readlnOrNull()?.lowercase() == "y") "APPROVE" else "REJECT"; ExecutionResult(
                    true,
                    "User chose '$choice'",
                    choice
                )
            }
            is AbstractCommand.RequestUserDecision -> { println("\n--- USER DECISION REQUIRED ---\n${command.prompt}"); command.options.forEachIndexed { index, option -> println("  ${index + 1}. $option") }; print("Enter your choice (number): "); val choice = readlnOrNull()?.toIntOrNull(); val selection = choice?.let { command.options.getOrNull(it - 1) } ?: "Cancel"; ExecutionResult(true, "User chose '$selection'", selection) }
            is AbstractCommand.RunShellCommand -> runCommand(command.command, command.workingDir)
            is AbstractCommand.RunTests -> runCommand(
                listOf("./gradlew", "test", "--info"),
                "."
            ) // Added --info for better diagnostics
            is AbstractCommand.StageFiles -> runCommand(listOf("git", "add") + command.filePaths)
            is AbstractCommand.SwitchToBranch -> runCommand(
                listOf(
                    "git",
                    "checkout",
                    command.branchName
                )
            )
            is AbstractCommand.WriteFile -> try { val file = File(command.path); file.parentFile.mkdirs(); file.writeText(command.content); ExecutionResult(true, "Wrote to ${command.path}") } catch (e: Exception) { ExecutionResult(false, e.message ?: "Failed to write file") }
            is AbstractCommand.Commit -> runCommand(listOf("git", "commit", "-m", command.message))
            is AbstractCommand.DisplayMessage -> { println("[INFO] ${command.message}"); ExecutionResult(true, "Message displayed") }
        }
    }

    private fun runCommand(command: List<String>, workDir: String = "."): ExecutionResult {
        println("  [CLI Adapter] Executing: '${command.joinToString(" ")}'")
        return try {
            val process =
                ProcessBuilder(command).directory(File(workDir)).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(120, TimeUnit.SECONDS)
            if (process.exitValue() == 0) ExecutionResult(true, output.ifBlank { "Command executed successfully." }, output)
            else ExecutionResult(false, "Exit code ${process.exitValue()}: $output", output)
        } catch (e: Exception) { ExecutionResult(false, e.message ?: "Failed to run shell command") }
    }
}