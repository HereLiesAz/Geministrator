package com.hereliesaz.geministrator.data

import android.content.Context
import android.content.res.Resources
import com.hereliesaz.geministrator.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class PromptsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: PromptsRepository

    @Before
    fun setup() {
        context = mockk()
        val resources: Resources = mockk()
        every { context.resources } returns resources
        repository = PromptsRepository(context)
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
        every { context.resources.openRawResource(R.raw.prompts) } returns inputStream

        val prompts = repository.getPrompts()

        assertEquals(1, prompts.size)
        assertEquals("test", prompts[0].name)
    }

    @Test
    fun `when prompts file is not found then an error is thrown`() {
        every { context.resources.openRawResource(R.raw.prompts) } throws Exception("File not found")

        assertThrows(Exception::class.java) {
            repository.getPrompts()
        }
    }
}
