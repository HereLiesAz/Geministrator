package com.hereliesaz.geministrator.data

import android.content.Context
import kotlinx.serialization.json.Json
import javax.inject.Inject

class PromptsRepositoryImpl @Inject constructor(private val context: Context): PromptsRepository {
    override suspend fun getPrompts(): List<Prompt> {
        val jsonString = context.assets.open("prompts.json").bufferedReader().use { it.readText() }
        return Json.decodeFromString(jsonString)
    }
}
