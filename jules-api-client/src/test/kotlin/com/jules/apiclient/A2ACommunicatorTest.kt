package com.jules.apiclient

import com.google.cloud.vertexai.api.GenerateContentResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class A2ACommunicatorTest {

    private lateinit var julesApiClient: JulesApiClient
    private lateinit var geminiApiClient: GeminiApiClient
    private lateinit var a2aCommunicator: A2ACommunicator

    @Before
    fun setUp() {
        julesApiClient = mockk(relaxed = true)
        geminiApiClient = mockk(relaxed = true)
        a2aCommunicator = A2ACommunicator(julesApiClient, geminiApiClient)
    }

    @Test
    fun `julesToGemini should call geminiApiClient's generateContent`() = runTest {
        // Given
        val prompt = "Hello, Gemini!"
        val mockResponse: GenerateContentResponse = mockk(relaxed = true)
        coEvery { geminiApiClient.generateContent(prompt) } returns mockResponse

        // When
        a2aCommunicator.julesToGemini(prompt)

        // Then
        coVerify(exactly = 1) { geminiApiClient.generateContent(prompt) }
    }

    @Test
    fun `geminiToJules_sendMessage should call julesApiClient's sendMessage`() = runTest {
        // Given
        val sessionId = "test-session-id"
        val prompt = "Hello, Jules!"
        coEvery { julesApiClient.sendMessage(sessionId, prompt) } returns Unit

        // When
        a2aCommunicator.geminiToJules_sendMessage(sessionId, prompt)

        // Then
        coVerify(exactly = 1) { julesApiClient.sendMessage(sessionId, prompt) }
    }
}
