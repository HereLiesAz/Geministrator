package com.jules.apiclient

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.util.Properties
import java.io.FileInputStream

class JulesApiClientTest {

    private var apiKey: String? = null
    private lateinit var client: JulesApiClient

    @Before
    fun setUp() {
        val properties = Properties()
        try {
            val fileInputStream = FileInputStream("../local.properties")
            properties.load(fileInputStream)
            apiKey = properties.getProperty("julesApiKey")
        } catch (e: Exception) {
            // Ignore: The test will be skipped by the assumeTrue check
        }
        assumeTrue("Jules API key not found in local.properties, skipping test.", apiKey != null && apiKey != "placeholder")
        client = JulesApiClient(apiKey!!)
    }

    @Test
    fun `getSources returns a list of sources`() = runBlocking {
        val sources = client.getSources()
        assert(sources.sources.isNotEmpty())
    }

    @Test
    fun `createSession, sendMessage, and getActivities`() = runBlocking {
        // First, get a source to create a session with
        val sources = client.getSources()
        val source = sources.sources.first()

        // Create a session
        val session = client.createSession("Test prompt", source, "Test Session", "[]")
        assert(session.id.isNotEmpty())

        // Send a message
        client.sendMessage(session.id, "Hello, Jules!")

        // Get activities
        val activities = client.getActivities(session.id)
        assert(activities.activities.isNotEmpty())
    }
}
