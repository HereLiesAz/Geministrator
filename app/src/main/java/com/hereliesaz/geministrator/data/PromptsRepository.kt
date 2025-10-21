package com.hereliesaz.geministrator.data

import android.app.Application
import com.hereliesaz.geministrator.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

@Serializable
data class Prompt(
    val name: String,
    val description: String,
    val template: String
)

class PromptsRepository(private val application: Application) {
    fun getPrompts(): Result<List<Prompt>> {
        return try {
            val inputStream: InputStream = application.resources.openRawResource(R.raw.prompts)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val prompts = Json.decodeFromString<List<Prompt>>(jsonString)
            Result.success(prompts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
