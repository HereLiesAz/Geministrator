package com.jules.apiclient

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.FileInputStream
import java.util.Properties

class JulesApiClientTest {

    private lateinit var apiClient: JulesApiClient

    @Before
    fun setUp() {
        val properties = Properties()
        val localPropertiesFile = File("../local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }
        val apiKey = properties.getProperty("apiKey")
        apiClient = JulesApiClient(apiKey)
    }

    @Test
    fun `getSources should return a list of sources`() = runTest {
        // When
        val result = apiClient.getSources()

        // Then
        assert(result.sources.isNotEmpty())
    }
}
