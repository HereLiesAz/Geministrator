package com.hereliesaz.geministrator.data

import android.app.Application
import android.content.res.AssetManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class PromptsRepositoryTest {

    private lateinit var application: Application
    private lateinit var assetManager: AssetManager
    private lateinit var promptsRepository: PromptsRepository

    @Before
    fun setUp() {
        application = mockk(relaxed = true)
        assetManager = mockk(relaxed = true)
        coEvery { application.assets } returns assetManager
        promptsRepository = PromptsRepository(application)
    }

    @Test
    fun `getPrompts should return a list of prompts`() = runTest {
        // Given
        val jsonString = """
            {
              "prompts": [
                {
                  "name": "Test Prompt 1",
                  "prompt": "This is a test prompt.",
                  "tags": ["test", "prompt"]
                },
                {
                  "name": "Test Prompt 2",
                  "prompt": "This is another test prompt.",
                  "tags": ["test", "another"]
                }
              ]
            }
        """.trimIndent()
        val inputStream = ByteArrayInputStream(jsonString.toByteArray())
        coEvery { assetManager.open("prompts.json") } returns inputStream

        // When
        val result = promptsRepository.getPrompts()

        // Then
        assert(result.isSuccess)
        val prompts = result.getOrNull()
        assert(prompts != null)
        assert(prompts!!.size == 2)
        assert(prompts[0].name == "Test Prompt 1")
    }
}
