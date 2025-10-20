package com.jules.apiclient

import com.jules.apiclient.util.TestProperties
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class JulesApiClientTest {

    private lateinit var apiKey: String
    private lateinit var apiClient: JulesApiClient

    @Before
    fun setUp() {
        apiKey = TestProperties.getProperty("julesApiKey") ?: ""
        assumeTrue("Jules API key is a placeholder, skipping test.", apiKey.isNotEmpty() && apiKey != "placeholder")
        apiClient = JulesApiClient(apiKey)
    }

    @Test
    fun `getSources should return a list of sources`() = runTest {
        // When
        val result = apiClient.getSources()

        // Then
        assertTrue("The sources list should not be empty.", result.sources.isNotEmpty())
    }

    @Test
    fun `getSessions should return a list of sessions`() = runTest {
        // When
        val result = apiClient.getSessions()

        // Then
        assertTrue("The sessions list should not be empty.", result.isNotEmpty())
    }
}
