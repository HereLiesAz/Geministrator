package com.hereliesaz.geministrator.apis

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.util.Properties
import java.io.FileInputStream

class GeminiApiClientTest {

    private var apiKey: String? = null
    private lateinit var client: GeminiApiClient

    @Before
    fun setUp() {
        val properties = Properties()
        try {
            val fileInputStream = FileInputStream("../local.properties")
            properties.load(fileInputStream)
            apiKey = properties.getProperty("geminiApiKey")
        } catch (e: Exception) {
            // Ignore: The test will be skipped by the assumeTrue check
        }
        assumeTrue("Gemini API key not found in local.properties, skipping test.", apiKey != null && apiKey != "placeholder")
        client = GeminiApiClient(apiKey!!)
    }

    @Test
    fun `generateContent returns a non-empty string`() = runBlocking {
        val prompt = "Hello, Gemini!"
        val response = client.generateContent(prompt)
        assert(response.isNotEmpty())
    }
}
