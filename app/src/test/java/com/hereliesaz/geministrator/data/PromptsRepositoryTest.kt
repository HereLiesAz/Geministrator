package com.hereliesaz.geministrator.data

import android.app.Application
import com.hereliesaz.geministrator.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class PromptsRepositoryTest {

    private lateinit var application: Application
    private lateinit var repository: PromptsRepository

    @Before
    fun setup() {
        application = mockk()
        repository = PromptsRepository(application)
    }

    @Test
    fun `when prompts are loaded successfully then a list of prompts is returned`() {
        val json = """
            [
                {
                    "name": "test",
                    "description": "test description",
                    "template": "test template"
                }
            ]
        """.trimIndent()
        val inputStream = ByteArrayInputStream(json.toByteArray())
        every { application.resources.openRawResource(R.raw.prompts) } returns inputStream

        val result = repository.getPrompts()

        assertTrue(result.isSuccess)
        val prompts = result.getOrNull()
        assertEquals(1, prompts?.size)
        assertEquals("test", prompts?.get(0)?.name)
    }

    @Test
    fun `when prompts file is not found then an error is returned`() {
        every { application.resources.openRawResource(R.raw.prompts) } throws Exception("File not found")

        val result = repository.getPrompts()

        assertTrue(result.isFailure)
    }
}
