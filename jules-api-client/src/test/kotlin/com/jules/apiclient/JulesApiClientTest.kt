package com.jules.apiclient

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.util.Properties

class JulesApiClientTest {

    private lateinit var apiClient: JulesApiClient

    @Before
    fun setUp() {
        val properties = Properties()
        // Tests run from the module's directory, so we need to go up one level to find the root local.properties
        val localPropertiesFile = File("../local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        } else {
            // Fallback for different execution contexts
            val rootProperties = File("local.properties")
            if (rootProperties.exists()) {
                properties.load(FileInputStream(rootProperties))
            } else {
                println("local.properties file not found at ${localPropertiesFile.absolutePath} or the current directory.")
            }
        }
        val apiKey = properties.getProperty("julesApiKey", "placeholder")

        apiClient = JulesApiClient(apiKey)
    }

    @Test
    fun `getSources should return a list of sources`() = runTest {
        // When
        val result = apiClient.getSources()

        // Then
        assert(result.sources.isNotEmpty())
    }

    @Test
    fun `getSessions should return a list of sessions`() = runTest {
        // When
        val result = apiClient.getSessions()

        // Then
        assert(result.isNotEmpty())
    }
}
