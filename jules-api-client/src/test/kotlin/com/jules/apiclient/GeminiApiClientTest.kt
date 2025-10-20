package com.jules.apiclient

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import java.io.File
import java.io.FileInputStream
import java.util.Properties

class GeminiApiClientTest {

    @get:Rule
    val environmentVariables = EnvironmentVariables()

    private lateinit var apiClient: GeminiApiClient

    @Before
    fun setUp() {
        val properties = Properties()
        val localPropertiesFile = File("../local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        } else {
            println("local.properties file not found at ${localPropertiesFile.absolutePath}")
        }
        val projectId = properties.getProperty("gcpProjectId")
        val location = properties.getProperty("gcpLocation")
        val modelName = properties.getProperty("geminiModelName")

        val serviceAccountKeyPath = properties.getProperty("googleApplicationCredentials")

        environmentVariables.set("GOOGLE_APPLICATION_CREDENTIALS", serviceAccountKeyPath)

        apiClient = GeminiApiClient(projectId, location, modelName)
    }

    @Test
    fun `generateContent should return a response`() = runTest {
        // When
        val result = apiClient.generateContent("Hello, Gemini!")

        // Then
        assert(result?.candidatesCount!! > 0)
    }
}
