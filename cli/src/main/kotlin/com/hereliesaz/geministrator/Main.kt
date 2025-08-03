package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.adapter.cli.CliConfigStorage
import com.hereliesaz.geministrator.cli.CliAdapter
import com.hereliesaz.geministrator.core.GeminiService
import com.hereliesaz.geministrator.core.Orchestrator
import com.hereliesaz.geministrator.core.config.ConfigStorage
import com.hereliesaz.geministrator.core.council.ILogger
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.coroutines.runBlocking

class ConsoleLogger : ILogger { override fun log(message: String) { println(message) } }

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) = runBlocking {
    val parser = ArgParser("geministrator")
    val configStorage = CliConfigStorage()
    val logger = ConsoleLogger()

    class RunCommand : Subcommand("run", "Run a new workflow") {
        val prompt by argument(ArgType.String, description = "The high-level task for the AI to perform")
        override fun execute() {
            runBlocking {
                val apiKey = getAndValidateApiKey(configStorage, logger)
                if (apiKey == null) {
                    println("ERROR: Could not obtain a valid API key. Exiting.")

                } else {
                val orchestrator = Orchestrator(CliAdapter(), apiKey, logger, configStorage)
                orchestrator.run(prompt, System.getProperty("user.dir"))
                }
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
                println("SUCCESS: Pre-commit review set to: ${!current}")
            }
            setConcurrency?.let {
                configStorage.saveConcurrencyLimit(it)
                println("SUCCESS: Concurrency limit set to: $it")
            }
            setTokenLimit?.let {
                configStorage.saveTokenLimit(it)
                println("SUCCESS: Token limit set to: $it")
            }
        }
    }

    parser.subcommands(RunCommand(), ConfigureCommand())
    parser.parse(args)
}

private suspend fun getAndValidateApiKey(storage: ConfigStorage, logger: ILogger): String? {
    var apiKey = storage.loadApiKey()
    while (true) {
        if (!apiKey.isNullOrBlank()) {
            if (GeminiService(apiKey, logger, storage, "", "").validateApiKey()) {
                return apiKey
            }
            logger.log("WARNING: Your saved API key is no longer valid.")
        }
        print("Please enter your Gemini API Key: ")
        apiKey = readlnOrNull()
        if (apiKey.isNullOrBlank()) return null
        if (GeminiService(apiKey, logger, storage, "", "").validateApiKey()) {
            storage.saveApiKey(apiKey)
            logger.log("SUCCESS: API Key is valid and has been saved.")
            return apiKey
        } else {
            logger.log("ERROR: The key you entered is invalid. Please try again or press Enter to quit.")
        }
    }
}