package com.hereliesaz.geministrator.ui.ide

import androidx.lifecycle.SavedStateHandle
import com.hereliesaz.geministrator.MainDispatcherRule
import com.hereliesaz.geministrator.data.HistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class ActivityStreamViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var historyRepository: HistoryRepository
    private lateinit var viewModel: ActivityStreamViewModel
    private val sessionId = UUID.randomUUID()

    @Before
    fun setup() {
        historyRepository = mockk(relaxed = true)
        val savedStateHandle = SavedStateHandle(mapOf("sessionId" to sessionId.toString()))
        viewModel = ActivityStreamViewModel(
            historyRepository = historyRepository,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `when session history is loaded then uiState is updated`() = runTest {
        val message = "Test message"
        coEvery { historyRepository.getSessionHistory(sessionId) } returns flowOf(listOf(message))

        viewModel.loadSessionHistory()

        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.messages.size)
        assertEquals(message, uiState.messages[0])
    }

    @Test
    fun `when a message is sent then it is added to the history`() = runTest {
        val message = "New message"
        viewModel.onMessageSent(message)

        coVerify { historyRepository.addMessageToHistory(sessionId, message) }
    }
}
