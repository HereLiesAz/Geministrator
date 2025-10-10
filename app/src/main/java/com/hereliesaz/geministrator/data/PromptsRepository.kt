package com.hereliesaz.geministrator.data

import android.app.Application
import kotlinx.serialization.json.Json
import java.io.IOException

class PromptsRepository(private val application: Application) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getPrompts(): Result<List<Prompt>> {
        return try {
            val jsonString = application.assets.open("prompts.json").bufferedReader().use { it.readText() }
            val promptList = json.decodeFromString<PromptList>(jsonString)
            Result.success(promptList.prompts)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
