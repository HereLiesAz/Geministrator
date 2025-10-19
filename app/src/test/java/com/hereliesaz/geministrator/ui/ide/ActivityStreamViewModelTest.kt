package com.hereliesaz.geministrator.ui.ide

import android.app.Application
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.JulesApiClient
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@ExperimentalCoroutinesApi
class ActivityStreamViewModelTest {

    private class TestViewModelFactory(
        private val julesApiClient: JulesApiClient,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActivityStreamViewModel(julesApiClient, settingsRepository) as T
        }
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var julesApiClient: JulesApiClient
    private lateinit var viewModel: ActivityStreamViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        julesApiClient = mockk(relaxed = true)
        coEvery { settingsRepository.apiKey } returns flowOf("test-api-key")
        viewModel = TestViewModelFactory(julesApiClient, settingsRepository).create(ActivityStreamViewModel::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadActivities should call julesApiClient`() = runTest {
        // Given
        val sessionId = "test-session"
        coEvery { julesApiClient.getActivities(sessionId) } returns mockk(relaxed = true)

        // When
        viewModel.loadActivities(sessionId)

        // Then
        coVerify(exactly = 1) { julesApiClient.getActivities(sessionId) }
    }
}
