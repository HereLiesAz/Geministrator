// products/cli/src/main/kotlin/com/gemini/orchestrator/cli/Main.kt
package com.gemini.orchestrator.cli
import com.gemini.orchestrator.adapter.cli.CliConfigStorage
import com.gemini.orchestrator.core.GeminiService
import com.gemini.orchestrator.core.Orchestrator
import com.gemini.orchestrator.core.council.ILogger
import kotlinx.cli.*
import kotlinx.coroutines.runBlocking

class ConsoleLogger : ILogger { override fun log(message: String) { println(message) } }

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) = runBlocking {
    val parser = ArgParser("gemini-orchestrator")
    val configStorage = CliConfigStorage()
    val logger = ConsoleLogger()

    class RunCommand : Subcommand("run", "Run a new workflow") {
        val prompt by argument(ArgType.String, description = "The high-level task for the AI to perform")
        override fun execute() {
            runBlocking {
                val apiKey = getAndValidateApiKey(configStorage, logger)
                if (apiKey == null) {
                    println("❌ Could not obtain a valid API key. Exiting.")
                    return@runBlocking
                }
                val orchestrator = Orchestrator(com.gemini.orchestrator.adapter.cli.CliAdapter(), apiKey, logger, configStorage)
                orchestrator.run(prompt, System.getProperty("user.dir"))
            }
        }
    }

    class ConfigureCommand : Subcommand("config", "Configure settings") {
        val toggleReview by option(ArgType.Boolean, shortName = "r", description = "Toggle pre-commit review")
        val setConcurrency by option(ArgType.Int, shortName = "c", description = "Set concurrency limit")
        val setTokenLimit by option(ArgType.Int, shortName = "t", description = "Set token limit")
        override fun execute() {
            toggleReview?.let {
                val current = configStorage.loadPreCommitReview()
                configStorage.savePreCommitReview(!current)
                println("✅ Pre-commit review set to: ${!current}")
            }
            setConcurrency?.let {
                configStorage.saveConcurrencyLimit(it)
                println("✅ Concurrency limit set to: $it")
            }
            setTokenLimit?.let {
                configStorage.saveTokenLimit(it)
                println("✅ Token limit set to: $it")
            }
        }
    }

    parser.subcommands(RunCommand(), ConfigureCommand())
    parser.parse(args)
}

private suspend fun getAndValidateApiKey(storage: CliConfigStorage, logger: ILogger): String? {
    var apiKey = storage.loadApiKey()
    while (true) {
        if (!apiKey.isNullOrBlank()) {
            if (GeminiService(apiKey, logger, storage, "", "").validateApiKey()) {
                return apiKey
            }
            logger.log("⚠️ Your saved API key is no longer valid.")
        }
        print("Please enter your Gemini API Key: ")
        apiKey = readlnOrNull()
        if (apiKey.isNullOrBlank()) return null
        if (GeminiService(apiKey, logger, storage, "", "").validateApiKey()) {
            storage.saveApiKey(apiKey)
            logger.log("✅ API Key is valid and has been saved.")
            return apiKey
        } else {
            logger.log("❌ The key you entered is invalid. Please try again or press Enter to quit.")
        }
    }
}

// products/android-studio-plugin/src/main/resources/META-INF/plugin.xml
<idea-plugin>
    <id>com.gemini.orchestrator</id>
    <name>Gemini Orchestrator</name>
    <version>1.0.0</version>
    <vendor>Gemini</vendor>
    <description><![CDATA[AI-driven development assistant to automate complex workflows.]]></description>
    <depends>com.intellij.modules.platform</depends>
    <depends>org.jetbrains.kotlin</depends>
    <depends>com.intellij.modules.vcs</depends>
    <depends>com.intellij.diff</depends>
    <extensions defaultExtensionNs="com.intellij">
        <toolWindow id="Gemini Orchestrator" anchor="right" factoryClass="com.gemini.orchestrator.plugin.OrchestratorToolWindowFactory" icon="AllIcons.General.User" />
    </extensions>
    <actions>
        <action id="Gemini.Orchestrator.Run" class="com.gemini.orchestrator.plugin.RunOrchestratorAction" text="Run Gemini Orchestrator" description="Opens the Gemini Orchestrator tool window." icon="AllIcons.Actions.Execute">
            <add-to-group group-id="ToolsMenu" anchor="last"/>
        </action>
    </actions>
</idea-plugin>

// products/android-studio-plugin/src/main/kotlin/com/gemini/orchestrator/plugin/ProgressLogger.kt
package com.gemini.orchestrator.plugin
import com.intellij.openapi.application.ApplicationManager
import javax.swing.JTextArea

class ProgressLogger(private val outputArea: JTextArea) : com.gemini.orchestrator.core.council.ILogger {
    override fun log(message: String) {
        ApplicationManager.getApplication().invokeLater {
            outputArea.append("$message\n")
            outputArea.caretPosition = outputArea.document.length
        }
    }
}

// products/android-studio-plugin/src/main/kotlin/com/gemini/orchestrator/plugin/OrchestratorToolWindowFactory.kt
package com.gemini.orchestrator.plugin
// ... (Final implementation with all UI controls and first-run wizard) ...
