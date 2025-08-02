// adapters/adapter-cli/src/main/kotlin/com/gemini/orchestrator/adapter/cli/CliConfigStorage.kt
package com.gemini.orchestrator.adapter.cli
import com.gemini.orchestrator.core.config.ConfigStorage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class CliConfigStorage : ConfigStorage {
    private val configFile = File(System.getProperty("user.home"), ".gemini-orchestrator/config.properties")
    private val properties = Properties()
    private val KEY_API = "GEMINI_API_KEY"
    private val KEY_REVIEW = "PRE_COMMIT_REVIEW"
    private val KEY_CONCURRENCY = "CONCURRENCY_LIMIT"
    private val KEY_TOKEN_LIMIT = "TOKEN_LIMIT"
    private val KEY_MODEL_STRATEGIC = "MODEL_STRATEGIC"
    private val KEY_MODEL_FLASH = "MODEL_FLASH"

    init {
        configFile.parentFile.mkdirs()
        if (configFile.exists()) { FileInputStream(configFile).use { properties.load(it) } }
    }

    private fun saveProperties() { FileOutputStream(configFile).use { properties.store(it, "Gemini Orchestrator Configuration") } }
    override fun saveApiKey(apiKey: String) { properties.setProperty(KEY_API, apiKey); saveProperties() }
    override fun loadApiKey(): String? = properties.getProperty(KEY_API)
    override fun savePreCommitReview(enabled: Boolean) { properties.setProperty(KEY_REVIEW, enabled.toString()); saveProperties() }
    override fun loadPreCommitReview(): Boolean = properties.getProperty(KEY_REVIEW, "true").toBoolean()
    override fun saveConcurrencyLimit(limit: Int) { properties.setProperty(KEY_CONCURRENCY, limit.toString()); saveProperties() }
    override fun loadConcurrencyLimit(): Int = properties.getProperty(KEY_CONCURRENCY, "2").toIntOrNull() ?: 2
    override fun saveTokenLimit(limit: Int) { properties.setProperty(KEY_TOKEN_LIMIT, limit.toString()); saveProperties() }
    override fun loadTokenLimit(): Int = properties.getProperty(KEY_TOKEN_LIMIT, "500000").toIntOrNull() ?: 500000
    override fun saveModelName(type: String, name: String) { properties.setProperty(if (type == "strategic") KEY_MODEL_STRATEGIC else KEY_MODEL_FLASH, name); saveProperties() }
    override fun loadModelName(type: String, default: String): String = properties.getProperty(if (type == "strategic") KEY_MODEL_STRATEGIC else KEY_MODEL_FLASH, default)
}

// adapters/adapter-cli/src/main/kotlin/com/gemini/orchestrator/adapter/cli/CliAdapter.kt
package com.gemini.orchestrator.adapter.cli
import com.gemini.orchestrator.common.*
import java.io.File
import java.util.concurrent.TimeUnit

class CliAdapter : ExecutionAdapter {
    override fun execute(command: AbstractCommand): ExecutionResult {
        return when (command) {
            is AbstractCommand.AppendToFile -> try { File(command.path).appendText(command.content); ExecutionResult(true, "Appended to ${command.path}") } catch (e: Exception) { ExecutionResult(false, e.message ?: "Failed to append") }
            is AbstractCommand.CreateAndSwitchToBranch -> runCommand("git checkout -b ${command.branchName}")
            is AbstractCommand.DeleteBranch -> runCommand("git branch -D ${command.branchName}")
            is AbstractCommand.DeleteFile -> try { File(command.path).delete(); ExecutionResult(true, "Deleted ${command.path}") } catch (e: Exception) { ExecutionResult(false, "Failed to delete file") }
            is AbstractCommand.DiscardAllChanges -> runCommand("git reset --hard HEAD && git clean -fd")
            is AbstractCommand.GetCurrentBranch -> runCommand("git rev-parse --abbrev-ref HEAD")
            is AbstractCommand.LogJournalEntry -> runCommand("echo '${command.entry}' >> .orchestrator/journal.log")
            is AbstractCommand.MergeBranch -> runCommand("git merge ${command.branchName}")
            is AbstractCommand.PerformWebSearch -> ExecutionResult(true, "Simulated web search for: ${command.query}. Found official documentation.", null)
            is AbstractCommand.ReadFile -> try { ExecutionResult(true, "Read file successfully.", File(command.path).readText()) } catch (e: Exception) { ExecutionResult(false, "Failed to read file: ${e.message}") }
            is AbstractCommand.RequestClarification -> { println("\n--- CLARIFICATION REQUIRED ---\n${command.question}"); print("Your response: "); ExecutionResult(true, "User provided clarification.", readlnOrNull() ?: "") }
            is AbstractCommand.RequestCommitReview -> { println("\n--- PENDING COMMIT: FINAL REVIEW ---\nProposed Commit Message: \"${command.proposedCommitMessage}\"\n--- STAGED CHANGES ---"); val diffResult = runCommand("git diff --staged"); println(diffResult.output); print("Approve and commit these changes? (y/n): "); val choice = if (readlnOrNull()?.lowercase() == "y") "APPROVE" else "REJECT"; ExecutionResult(true, "User chose '$choice'", choice) }
            is AbstractCommand.RequestUserDecision -> { println("\n--- USER DECISION REQUIRED ---\n${command.prompt}"); command.options.forEachIndexed { index, option -> println("  ${index + 1}. $option") }; print("Enter your choice (number): "); val choice = readlnOrNull()?.toIntOrNull(); val selection = choice?.let { command.options.getOrNull(it - 1) } ?: "Cancel"; ExecutionResult(true, "User chose '$selection'", selection) }
            is AbstractCommand.RunShellCommand -> runCommand(command.command, command.workingDir)
            is AbstractCommand.RunTests -> runCommand("./gradlew test", ".")
            is AbstractCommand.StageFiles -> runCommand("git add ${command.filePaths.joinToString(" ")}")
            is AbstractCommand.SwitchToBranch -> runCommand("git checkout ${command.branchName}")
            is AbstractCommand.WriteFile -> try { val file = File(command.path); file.parentFile.mkdirs(); file.writeText(command.content); ExecutionResult(true, "Wrote to ${command.path}") } catch (e: Exception) { ExecutionResult(false, e.message ?: "Failed to write file") }
            is AbstractCommand.Commit -> runCommand("git commit -m \"${command.message}\"")
            is AbstractCommand.DisplayMessage -> { println("[INFO] ${command.message}"); ExecutionResult(true, "Message displayed") }
        }
    }

    private fun runCommand(command: String, workDir: String = "."): ExecutionResult {
        println("  [CLI Adapter] Executing: '$command'")
        return try {
            val process = ProcessBuilder("sh", "-c", command).directory(File(workDir)).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(120, TimeUnit.SECONDS)
            if (process.exitValue() == 0) ExecutionResult(true, output.ifBlank { "Command executed successfully." }, output)
            else ExecutionResult(false, "Exit code ${process.exitValue()}: $output", output)
        } catch (e: Exception) { ExecutionResult(false, e.message ?: "Failed to run shell command") }
    }
}

// adapters/adapter-android-studio/src/main/kotlin/com/gemini/orchestrator/adapter/as/PluginConfigStorage.kt
package com.gemini.orchestrator.adapter.as
import com.gemini.orchestrator.core.config.ConfigStorage
import com.intellij.ide.util.PropertiesComponent

class PluginConfigStorage : ConfigStorage {
    private val props = PropertiesComponent.getInstance()
    private val ID_API_KEY = "com.gemini.orchestrator.apiKey"
    private val ID_REVIEW_ENABLED = "com.gemini.orchestrator.reviewEnabled"
    private val ID_CONCURRENCY_LIMIT = "com.gemini.orchestrator.concurrencyLimit"
    private val ID_TOKEN_LIMIT = "com.gemini.orchestrator.tokenLimit"
    private val ID_MODEL_STRATEGIC = "com.gemini.orchestrator.modelStrategic"
    private val ID_MODEL_FLASH = "com.gemini.orchestrator.modelFlash"

    override fun saveApiKey(apiKey: String) = props.setValue(ID_API_KEY, apiKey)
    override fun loadApiKey(): String? = props.getValue(ID_API_KEY)
    override fun savePreCommitReview(enabled: Boolean) = props.setValue(ID_REVIEW_ENABLED, enabled, true)
    override fun loadPreCommitReview(): Boolean = props.getBoolean(ID_REVIEW_ENABLED, true)
    override fun saveConcurrencyLimit(limit: Int) = props.setValue(ID_CONCURRENCY_LIMIT, limit, 2)
    override fun loadConcurrencyLimit(): Int = props.getInt(ID_CONCURRENCY_LIMIT, 2)
    override fun saveTokenLimit(limit: Int) = props.setValue(ID_TOKEN_LIMIT, limit, 500000)
    override fun loadTokenLimit(): Int = props.getInt(ID_TOKEN_LIMIT, 500000)
    override fun saveModelName(type: String, name: String) = props.setValue(if (type == "strategic") ID_MODEL_STRATEGIC else ID_MODEL_FLASH, name)
    override fun loadModelName(type: String, default: String): String = props.getValue(if (type == "strategic") ID_MODEL_STRATEGIC else ID_MODEL_FLASH, default)
}

// adapters/adapter-android-studio/src/main/kotlin/com/gemini/orchestrator/adapter/as/AndroidStudioAdapter.kt
package com.gemini.orchestrator.adapter.as
import com.gemini.orchestrator.common.*
import com.gemini.orchestrator.core.council.ILogger
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import java.io.File

class AndroidStudioAdapter(private val project: Project, private val logger: ILogger) : ExecutionAdapter {
    override fun execute(command: AbstractCommand): ExecutionResult {
        // This would contain the full implementation using the IntelliJ SDK for each command.
        // For brevity, only a few key implementations are shown.
        return when (command) {
            is AbstractCommand.ReadFile -> {
                var result: ExecutionResult? = null
                ApplicationManager.getApplication().runReadAction {
                    try {
                        val file = VfsUtil.findFileByIoFile(File(project.basePath, command.path), false)
                        val content = file?.let { VfsUtil.loadText(it) } ?: ""
                        result = ExecutionResult(true, "Read file.", content)
                    } catch (e: Exception) { result = ExecutionResult(false, "Failed to read file: ${e.message}") }
                }
                result!!
            }
            is AbstractCommand.RequestCommitReview -> {
                var decision = "REJECT"
                ApplicationManager.getApplication().invokeAndWait {
                    // This is a simplified version. A real implementation would be more complex.
                    val diffManager = DiffManager.getInstance()
                    // diffManager.showDiff(project, createDiffRequest(command))
                    val userChoice = Messages.showYesNoDialog(project, "Approve changes for commit?", "Final Review", "Approve", "Reject", null)
                    if (userChoice == Messages.YES) decision = "APPROVE"
                }
                ExecutionResult(true, "User chose '$decision'", decision)
            }
            else -> ExecutionResult(true, "Simulated execution of ${command::class.simpleName}", null)
        }
    }
}
