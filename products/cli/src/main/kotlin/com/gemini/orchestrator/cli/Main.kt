package com.hereliesaz.GeminiOrchestrator.cli

import com.hereliesaz.GeminiOrchestrator.adapter.cli.CliConfigStorage
import com.hereliesaz.GeminiOrchestrator.core.GeminiService
import com.hereliesaz.GeminiOrchestrator.core.Orchestrator
import com.hereliesaz.GeminiOrchestrator.core.council.ILogger
import kotlinx.cli.*
import kotlinx.coroutines.runBlocking

class ConsoleLogger : ILogger { override fun log(message: String) { println(message) } }

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
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
                val orchestrator = Orchestrator(com.hereliesaz.GeminiOrchestrator.adapter.cli.CliAdapter(), apiKey, logger, configStorage)
                orchestrator.run(prompt, System.getProperty("user.dir"))
            }
        }
    }

    class ConfigureCommand : Subcommand("config", "Configure settings") {
        val toggleReview by option(ArgType.Boolean, shortName = "r", description = "Toggle pre-commit review")
        val setConcurrency by option(ArgType.Int, shortName = "c", description = "Set concurrency limit")
        val setTokenLimit by option(ArgType.Int, shortName = "t", description = "Set token limit")
        override fun execute() {
            var changed = false
            toggleReview?.let {
                val current = configStorage.loadPreCommitReview()
                configStorage.savePreCommitReview(!current)
                println("✅ Pre-commit review set to: ${!current}")
                changed = true
            }
            setConcurrency?.let {
                if (it > 0) {
                    configStorage.saveConcurrencyLimit(it)
                    println("✅ Concurrency limit set to: $it")
                    changed = true
                } else {
                    println("❌ Invalid concurrency limit.")
                }
            }
            setTokenLimit?.let {
                if (it > 1000) {
                    configStorage.saveTokenLimit(it)
                    println("✅ Token limit set to: $it")
                    changed = true
                } else {
                    println("❌ Token limit must be greater than 1000.")
                }
            }
            if (!changed) {
                println("No settings changed. Use flags like -r, -c, or -t to modify settings.")
            }
        }
    }

    parser.subcommands(RunCommand(), ConfigureCommand())
    if (args.isEmpty()) {
        println("Please specify a command: 'run' or 'config'. Use --help for more info.")
    } else {
        parser.parse(args)
    }
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
        print("Please enter your Gemini API Key (or press Enter to quit): ")
        apiKey = readlnOrNull()
        if (apiKey.isNullOrBlank()) return null
        if (GeminiService(apiKey, logger, storage, "", "").validateApiKey()) {
            storage.saveApiKey(apiKey)
            logger.log("✅ API Key is valid and has been saved.")
            return apiKey
        } else {
            logger.log("❌ The key you entered is invalid. Please try again.")
        }
    }
}