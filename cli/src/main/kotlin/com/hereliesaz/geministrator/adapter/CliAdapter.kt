package com.hereliesaz.geministrator.adapter

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.ExecutionResult
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.core.config.ConfigStorage
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class CliAdapter(
    private val config: ConfigStorage,
    private val logger: ILogger,
) : ExecutionAdapter {
    private val jsonParser = Json { isLenient = true; ignoreUnknownKeys = true }

    override fun execute(command: AbstractCommand, silent: Boolean): ExecutionResult {
        return when (command) {
            is AbstracatCommand.AppendToFile -> try {
                File(command.path).appendText(command.content); ExecutionResult(
                    true,
                    "Appended to ${command.path}"
                )
            } catch (e: IOException) {
                ExecutionResult(false, e.message ?: "Failed to append")
            }

            is AbstractCommand.CreateAndSwitchToBranch -> runCommand(
                listOf(
                    "git",
                    "checkout",
                    "-b",
                    command.branchName
                ), silent = silent
            )

            is AbstractCommand.DeleteBranch -> runCommand(
                listOf(
                    "git",
                    "branch",
                    "-D",
                    command.branchName
                ), silent = silent
            )

            is AbstractCommand.DeleteFile -> try {
                File(command.path).delete(); ExecutionResult(true, "Deleted ${command.path}")
            } catch (e: SecurityException) {
                ExecutionResult(false, "Failed to delete file: ${e.message}")
            }
            is AbstractCommand.DiscardAllChanges -> {
                val resetResult =
                    runCommand(listOf("git", "reset", "--hard", "HEAD"), silent = silent)
                if (!resetResult.isSuccess) return resetResult
                runCommand(listOf("git", "clean", "-fd"), silent = silent)
            }

            is AbstractCommand.GetCurrentBranch -> runCommand(
                listOf(
                    "git",
                    "rev-parse",
                    "--abbrev-ref",
                    "HEAD"
                ), silent = silent
            )
            is AbstractCommand.LogJournalEntry -> try {
                val file = File(".orchestrator/journal.log")
                file.parentFile.mkdirs()
                file.appendText(command.entry)
                ExecutionResult(true, "Logged journal entry")
            } catch (e: IOException) {
                ExecutionResult(false, e.message ?: "Failed to log journal entry")
            }

            is AbstractCommand.MergeBranch -> runCommand(
                listOf("git", "merge", command.branchName),
                silent = silent
            )
            is AbstractCommand.PerformWebSearch -> performWebSearch(command.query)
            is AbstractCommand.ReadFile -> try {
                ExecutionResult(true, "Read file successfully.", File(command.path).readText())
            } catch (e: IOException) {
                ExecutionResult(false, "Failed to read file: ${e.message}")
            }

            is AbstractCommand.RequestClarification -> {
                logger.interactive("\n--- CLARIFICATION REQUIRED ---\n${command.question}")
                val response = logger.prompt("Your response: ") ?: ""
                ExecutionResult(true, "User provided clarification.", response)
            }
            is AbstractCommand.RequestCommitReview -> {
                logger.interactive("\n--- PENDING COMMIT: FINAL REVIEW ---\nProposed Commit Message: \"${command.proposedCommitMessage}\"\n--- STAGED CHANGES ---")
                val diffResult = runCommand(listOf("git", "diff", "--staged"))
                logger.interactive(diffResult.output)
                val response = logger.prompt("Approve and commit these changes? (y/n): ")
                val choice = if (response?.lowercase() == "y") "APPROVE" else "REJECT"
                ExecutionResult(true, "User chose '$choice'", choice)
            }

            is AbstractCommand.RequestUserDecision -> {
                logger.interactive("\n--- USER DECISION REQUIRED ---\n${command.prompt}")
                command.options.forEachIndexed { index, option -> logger.interactive("  ${index + 1}. $option") }
                val choiceStr = logger.prompt("Enter your choice (number): ")
                val choice = choiceStr?.toIntOrNull()
                val selection = choice?.let { command.options.getOrNull(it - 1) } ?: "Cancel"
                ExecutionResult(true, "User chose '$selection'", selection)
            }

            is AbstractCommand.RunShellCommand -> runCommand(
                command.command,
                command.workingDir,
                silent
            )
            is AbstractCommand.RunTests -> {
                val cmd = mutableListOf("./gradlew")
                val task = if (command.module != null) ":${command.module}:test" else "test"
                cmd.add(task)
                command.testName?.let {
                    cmd.add("--tests")
                    cmd.add(it)
                }
                cmd.add("--info")
                runCommand(cmd, ".", silent)
            }

            is AbstractCommand.StageFiles -> runCommand(
                listOf("git", "add") + command.filePaths,
                silent = silent
            )

            is AbstractCommand.SwitchToBranch -> runCommand(
                listOf(
                    "git",
                    "checkout",
                    command.branchName
                ), silent = silent
            )

            is AbstractCommand.WriteFile -> try {
                val file =
                    File(command.path); file.parentFile.mkdirs(); file.writeText(command.content); ExecutionResult(
                    true,
                    "Wrote to ${command.path}"
                )
            } catch (e: IOException) {
                ExecutionResult(false, e.message ?: "Failed to write file")
            }

            is AbstractCommand.Commit -> runCommand(
                listOf("git", "commit", "-m", command.message),
                silent = silent
            )

            is AbstractCommand.DisplayMessage -> {
                logger.interactive("[INFO] ${command.message}"); ExecutionResult(
                    true,
                    "Message displayed"
                )
            }

            is AbstractCommand.PauseAndExit -> TODO()
        }
    }

    private fun runCommand(
        command: List<String>,
        workDir: String = ".",
        silent: Boolean = false,
    ): ExecutionResult {
        if (!silent) {
            logger.info("  [CLI Adapter] Executing: '${command.joinToString(" ")}'")
        }
        return try {
            val process =
                ProcessBuilder(command).directory(File(workDir)).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(120, TimeUnit.SECONDS)
            if (process.exitValue() == 0) ExecutionResult(true, output.ifBlank { "Command executed successfully." }, output)
            else ExecutionResult(false, "Exit code ${process.exitValue()}: $output", output)
        } catch (e: IOException) {
            ExecutionResult(false, e.message ?: "Failed to run shell command")
        } catch (e: InterruptedException) {
            ExecutionResult(false, "Command timed out: ${e.message}")
        }
    }

    private fun performWebSearch(query: String): ExecutionResult {
        val apiKey = config.loadSearchApiKey()
        val engineId = config.loadSearchEngineId()

        if (apiKey.isNullOrBlank() || engineId.isNullOrBlank()) {
            return ExecutionResult(
                false,
                "Web search is not configured. Please use 'geministrator config --search-api-key YOUR_KEY --search-engine-id YOUR_ID' to set it up."
            )
        }

        logger.info("  [CLI Adapter] Performing web search for: '$query'")

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url =
                URI("https://www.googleapis.com/customsearch/v1?key=$apiKey&cx=$engineId&q=$encodedQuery").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            return if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().readText()
                val searchResponse = jsonParser.decodeFromString<SearchResponse>(responseText)
                val summary = searchResponse.items.joinToString("\n---\n") {
                    "Title: ${it.title}\nSnippet: ${it.snippet}\nURL: ${it.link}"
                }.ifBlank { "No relevant search results found." }
                ExecutionResult(true, summary)
            } else {
                val error = connection.errorStream.bufferedReader().readText()
                ExecutionResult(
                    false,
                    "Web search failed with status ${connection.responseCode}: $error"
                )
            }
        } catch (e: Exception) {
            return ExecutionResult(false, "An exception occurred during web search: ${e.message}")
        }
    }
}