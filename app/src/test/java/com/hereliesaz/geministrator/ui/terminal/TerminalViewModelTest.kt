package com.hereliesaz.geministrator.ui.terminal

import com.hereliesaz.geministrator.MainDispatcherRule
import com.hereliesaz.geministrator.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TerminalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: TerminalViewModel
    private val mockSettingsRepository: SettingsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        // Default to no keys being set
        coEvery { mockSettingsRepository.getApiKey() } returns null
        coEvery { mockSettingsRepository.getGeminiApiKey() } returns null
        viewModel = TerminalViewModel(mockSettingsRepository)
    }

    @Test
    fun `initial state is correct`() {
        val uiState = viewModel.uiState.value
        assertEquals("", uiState.output)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `processInput with unknown command shows error`() = runTest {
        val command = "foo bar"
        viewModel.processInput(command)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState.output.contains("> foo bar"))
        assertTrue(uiState.output.contains("Unknown command: foo"))
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `processInput with jules command shows error if key not set`() = runTest {
        val command = "jules sources"
        viewModel.processInput(command)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState.output.contains("> jules sources"))
        assertTrue(uiState.output.contains("Jules API key not configured"))
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `processInput with gemini command shows error if key not set`() = runTest {
        val command = "gemini hello"
        viewModel.processInput(command)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState.output.contains("> gemini hello"))
        assertTrue(uiState.output.contains("Gemini API key not configured"))
        assertFalse(uiState.isLoading)
    }
}
