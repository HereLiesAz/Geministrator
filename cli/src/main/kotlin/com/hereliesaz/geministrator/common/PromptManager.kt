package com.hereliesaz.geministrator.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File

class PromptManager(private val configDir: File) {

    private val prompts: Map<String, String>

    init {
        val customPromptsFile = File(configDir, "prompts.json")
        prompts = if (customPromptsFile.exists()) {
            loadPromptsFromFile(customPromptsFile)
        } else {
            loadDefaultPrompts()
        }
    }

    private fun loadPromptsFromFile(file: File): Map<String, String> {
        return try {
            val json = Json.parseToJsonElement(file.readText()).jsonObject
            json.mapValues { it.value.toString().trim('"') }
        } catch (e: Exception) {
            println("[WARNING] Could not parse custom prompts.json. Falling back to defaults. Error: ${e.message}")
            loadDefaultPrompts()
        }
    }

    private fun loadDefaultPrompts(): Map<String, String> {
        val resourceStream = this::class.java.getResourceAsStream("/prompts.json")
            ?: throw IllegalStateException("Default prompts.json not found in resources.")
        val json = Json.parseToJsonElement(resourceStream.bufferedReader().readText()).jsonObject
        return json.mapValues { it.value.toString().trim('"') }
    }

    fun getPrompt(key: String, replacements: Map<String, String> = emptyMap()): String {
        var prompt = prompts[key] ?: throw IllegalArgumentException("Prompt key '$key' not found.")
        replacements.forEach { (placeholder, value) ->
            prompt = prompt.replace("{{${placeholder}}}", value)
        }
        return prompt
    }

    fun resetToDefaults(): Boolean {
        val customPromptsFile = File(configDir, "prompts.json")
        return if (customPromptsFile.exists()) {
            customPromptsFile.delete()
        } else {
            true // Already using defaults
        }
    }
}