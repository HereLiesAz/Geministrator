package com.hereliesaz.geministrator.ui.ide

import android.app.Application
import com.hereliesaz.geministrator.data.GeminiApiClient
import com.hereliesaz.geministrator.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var geminiApiClient: GeminiApiClient
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        geminiApiClient = mockk(relaxed = true)
        viewModel = SearchViewModel(settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `performSearch should call geminiApiClient`() = runTest {
        // Given
        val query = "test-query"
        viewModel.onSearchQueryChange(query)
        val mockResponse = mockk<com.google.genai.protocol.GenerateContentResponse>(relaxed = true)
        coEvery { geminiApiClient.generateContent(any()) } returns mockResponse
        coEvery { settingsRepository.geminiApiKey } returns flowOf("test-key")
        viewModel = SearchViewModel(settingsRepository)


        // When
        viewModel.performSearch()

        // Then
        coVerify(exactly = 1) { geminiApiClient.generateContent(any()) }
    }
}
