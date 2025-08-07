package com.hereliesaz.geministrator.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File

class PromptManager(private val configDir: File) {

    private val prompts: Map<String, String>
    private val customPromptsFile = File(configDir, "prompts.json")
    private val jsonParser = Json { isLenient = true; ignoreUnknownKeys = true; prettyPrint = true }


    init {
        prompts = if (customPromptsFile.exists()) {
            loadPromptsFromFile(customPromptsFile)
        } else {
            loadDefaultPrompts()
        }
    }

    private fun loadPromptsFromFile(file: File): Map<String, String> {
        return try {
            val json = Json.parseToJsonElement(file.readText()).jsonObject
            json.mapValues { it.value.toString() } // Keep the full JSON object
        } catch (e: Exception) {
            println("[WARNING] Could not parse custom prompts.json. Falling back to defaults. Error: ${e.message}")
            loadDefaultPrompts()
        }
    }

    private fun loadDefaultPrompts(): Map<String, String> {
        val resourceStream = this::class.java.getResourceAsStream("/prompts-jules.json")
            ?: throw IllegalStateException("Default prompts-jules.json not found in resources.")
        return resourceStream.use { stream ->
            val json = Json.parseToJsonElement(stream.bufferedReader().readText()).jsonObject
            json.mapValues { it.value.toString() } // Keep the full JSON object for structured prompts
        }
    }

    fun getPrompt(key: String, replacements: Map<String, String> = emptyMap()): String {
        var prompt = prompts[key] ?: throw IllegalArgumentException("Prompt key '$key' not found.")
        replacements.forEach { (placeholder, value) ->
            prompt = prompt.replace("{{${placeholder}}}", value)
        }
        return prompt
    }

    fun getPromptsAsString(): String {
        return if (customPromptsFile.exists()) {
            customPromptsFile.readText()
        } else {
            this::class.java.getResourceAsStream("/prompts-jules.json")?.bufferedReader()?.readText()
                ?: "{}"
        }
    }

    fun savePromptsFromString(content: String) {
        // Validate it's actual JSON before saving
        try {
            jsonParser.parseToJsonElement(content)
            customPromptsFile.parentFile.mkdirs()
            customPromptsFile.writeText(content)
        } catch (e: Exception) {
            // In a real app, this would throw a specific exception to be caught by the ViewModel
            println("Error saving prompts: Invalid JSON format. ${e.message}")
            throw e
        }
    }

    fun resetToDefaults(): Boolean {
        return if (customPromptsFile.exists()) {
            customPromptsFile.delete()
        } else {
            true // Already using defaults
        }
    }
}