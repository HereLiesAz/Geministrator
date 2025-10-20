package com.jules.apiclient

import com.jules.apiclient.util.TestProperties
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class GeminiApiClientTest {

    private lateinit var apiClient: GeminiApiClient

    @Before
    fun setUp() {
        val projectId = TestProperties.getProperty("gcpProjectId")
        val location = TestProperties.getProperty("gcpLocation")
        val modelName = TestProperties.getProperty("geminiModelName")
        val serviceAccountKeyPath = TestProperties.getProperty("googleApplicationCredentials")

        assumeTrue("GCP credentials are not available or are placeholders, skipping test.",
            !projectId.isNullOrEmpty() && projectId != "placeholder" &&
            !location.isNullOrEmpty() && location != "placeholder" &&
            !modelName.isNullOrEmpty() && modelName != "placeholder" &&
            !serviceAccountKeyPath.isNullOrEmpty() && serviceAccountKeyPath != "placeholder")

        // The Gemini API client uses environment variables for authentication, so we set it here.
        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", serviceAccountKeyPath)

        apiClient = GeminiApiClient(projectId!!, location!!, modelName!!)
    }

    @Test
    fun `generateContent should return a response`() = runTest {
        // When
        val result = apiClient.generateContent("Hello, Gemini!")

        // Then
        assertTrue("The response should have at least one candidate.", result?.candidatesCount!! > 0)
    }
}
