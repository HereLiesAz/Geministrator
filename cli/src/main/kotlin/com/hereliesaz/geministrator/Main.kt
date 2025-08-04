package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.adapter.CliAdapter
import com.hereliesaz.geministrator.adapter.CliConfigStorage
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.common.MultiStreamLogger
import com.hereliesaz.geministrator.common.PromptManager
import com.hereliesaz.geministrator.core.Orchestrator
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.coroutines.runBlocking
import java.io.File

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
    val parser = ArgParser("geministrator")
    val configStorage = CliConfigStorage()
    val logger = MultiStreamLogger(configStorage.getConfigDirectory())
    val promptManager = PromptManager(configStorage.getConfigDirectory())
    val adapter = CliAdapter(configStorage, logger)

    class RunCommand : Subcommand("run", "Run a new workflow") {
        val prompt by argument(ArgType.String, description = "The high-level task for the AI to perform")
        val specFile by option(
            ArgType.String,
            fullName = "spec-file",
            description = "Path to a project specification markdown file."
        )

        override fun execute() {
            runBlocking {
                val geminiService = createGeminiService(configStorage, logger, adapter)
                if (geminiService == null) {
                    logger.error("Could not configure a valid authentication method. Exiting.")
                    return@runBlocking
                }

                val specFileContent = specFile?.let {
                    try {
                        File(it).readText()
                    } catch (e: Exception) {
                        logger.error("Could not read spec file at '$it'.", e)
                        null
                    }
                }

                val projectType = determineProjectType(logger)
                val orchestrator =
                    Orchestrator(adapter, logger, configStorage, promptManager, geminiService)
                orchestrator.run(
                    prompt,
                    System.getProperty("user.dir"),
                    projectType,
                    specFileContent
                )
            }
        }
    }

    class ConfigureCommand : Subcommand("config", "Configure settings") {
        val toggleReview by option(ArgType.Boolean, shortName = "r", description = "Toggle pre-commit review")
        val setConcurrency by option(ArgType.Int, shortName = "c", description = "Set concurrency limit")
        val setTokenLimit by option(ArgType.Int, shortName = "t", description = "Set token limit")
        val setSearchApiKey by option(
            ArgType.String,
            fullName = "search-api-key",
            description = "Set Google Custom Search API Key"
        )
        val setSearchEngineId by option(
            ArgType.String,
            fullName = "search-engine-id",
            description = "Set Google Programmable Search Engine ID"
        )
        val resetPrompts by option(
            ArgType.Boolean,
            fullName = "reset-prompts",
            description = "Reset all agent prompts to their default values"
        )
        val setAuthMethod by option(
            ArgType.String,
            fullName = "auth-method",
            description = "Set the preferred authentication method ('adc' or 'apikey')"
        )
        val setFreeTierOnly by option(
            ArgType.Boolean,
            fullName = "free-tier",
            description = "Toggle free tier only mode (enforces ADC auth and free models)"
        )


        override fun execute() {
            toggleReview?.let {
                val current = configStorage.loadPreCommitReview()
                configStorage.savePreCommitReview(!current)
                logger.interactive("SUCCESS: Pre-commit review set to: ${!current}")
            }
            setConcurrency?.let {
                configStorage.saveConcurrencyLimit(it)
                logger.interactive("SUCCESS: Concurrency limit set to: $it")
            }
            setTokenLimit?.let {
                configStorage.saveTokenLimit(it)
                logger.interactive("SUCCESS: Token limit set to: $it")
            }
            setSearchApiKey?.let {
                configStorage.saveSearchApiKey(it)
                logger.interactive("SUCCESS: Search API Key has been saved.")
            }
            setSearchEngineId?.let {
                configStorage.saveSearchEngineId(it)
                logger.interactive("SUCCESS: Search Engine ID has been saved.")
            }
            resetPrompts?.let {
                if (it) {
                    if (promptManager.resetToDefaults()) {
                        logger.interactive("SUCCESS: Custom prompts file deleted. System will use default prompts on next run.")
                    } else {
                        logger.error("Failed to delete custom prompts file.")
                    }
                }
            }
            setAuthMethod?.let {
                val method = it.lowercase()
                if (method == "adc" || method == "apikey") {
                    configStorage.saveAuthMethod(method)
                    logger.interactive("SUCCESS: Default authentication method set to '$method'.")
                } else {
                    logger.error("Invalid authentication method. Please choose 'adc' or 'apikey'.")
                }
            }
            setFreeTierOnly?.let {
                configStorage.saveFreeTierOnly(it)
                logger.interactive("SUCCESS: Free tier only mode set to '$it'.")
            }
        }
    }

    parser.subcommands(RunCommand(), ConfigureCommand())
    parser.parse(args)
}

private suspend fun createGeminiService(
    configStorage: CliConfigStorage,
    logger: ILogger,
    adapter: CliAdapter,
): GeminiService? {
    val freeTierOnly = configStorage.loadFreeTierOnly()
    val authMethod = if (freeTierOnly) "adc" else configStorage.loadAuthMethod()

    val strategicModel: String
    val flashModel: String

    if (freeTierOnly) {
        logger.info("Free Tier Only mode is enabled. Using free models and ADC authentication.")
        strategicModel = "gemini-1.5-pro-latest"
        flashModel = "gemini-1.5-flash-latest"
    } else {
        strategicModel = configStorage.loadModelName("strategic", "gemini-pro")
        flashModel = configStorage.loadModelName("flash", "gemini-1.5-flash-latest")
    }

    val promptManager = PromptManager(configStorage.getConfigDirectory())

    if (authMethod == "adc") {
        val service = GeminiService(
            authMethod = "adc",
            apiKey = "", // Not needed for ADC
            logger = logger,
            config = configStorage,
            strategicModelName = strategicModel,
            flashModelName = flashModel,
            promptManager = promptManager,
            adapter = adapter
        )
        if (service.isAdcAuthReady()) {
            return service
        }
        if (freeTierOnly) {
            logger.error("Free Tier Only mode requires ADC authentication, which failed. Please configure gcloud or disable free tier mode.")
            return null
        }
        logger.info("ADC authentication failed. Checking for API key as fallback...")
    }

    // Fallback to API key or if 'apikey' is the chosen method
    var apiKey = configStorage.loadApiKey()
    while (true) {
        if (!apiKey.isNullOrBlank()) {
            val serviceForValidation =
                GeminiService("apikey", apiKey, logger, configStorage, "", "", null, null)
            if (serviceForValidation.validateApiKey(apiKey)) {
                logger.info("API Key authentication successful.")
                // Save the valid key before returning the service
                configStorage.saveApiKey(apiKey)
                return GeminiService(
                    "apikey",
                    apiKey,
                    logger,
                    configStorage,
                    strategicModel,
                    flashModel,
                    promptManager,
                    adapter
                )
            }
            logger.error("Your saved API key is no longer valid.")
        }
        apiKey = logger.prompt("Please enter your Gemini API Key: ")
        if (apiKey.isNullOrBlank()) return null
        // Validate the newly entered key *before* saving it
    }
}


private fun determineProjectType(logger: ILogger): String {
    logger.interactive("\nWhat type of project are you working on?")
    val options = listOf(
        "Application",
        "Web Service/API",
        "Library/SDK",
        "Automation Script",
        "Website",
        "Other"
    )
    options.forEachIndexed { index, option -> logger.interactive("  ${index + 1}. $option") }
    while (true) {
        val choiceStr = logger.prompt("Enter your choice (number): ")
        val choice = choiceStr?.toIntOrNull()
        if (choice != null && choice in 1..options.size) {
            return options[choice - 1]
        }
        logger.error("Invalid selection. Please enter a number from the list.")
    }
}