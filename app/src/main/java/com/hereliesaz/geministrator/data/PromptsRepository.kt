package com.hereliesaz.geministrator.data

import android.content.Context
import com.hereliesaz.geministrator.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Prompt(
    val name: String,
    val description: String,
    val template: String
)

@Singleton
class PromptsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    @Throws(Exception::class)
    fun getPrompts(): List<Prompt> {
        val inputStream: InputStream = context.resources.openRawResource(R.raw.prompts)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return Json.decodeFromString<List<Prompt>>(jsonString)
    }
}
