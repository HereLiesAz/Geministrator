package com.hereliesaz.geministrator.ui.settings

import android.app.Application
import com.hereliesaz.geministrator.data.PromptsRepository
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@ExperimentalCoroutinesApi
class RolesSettingsViewModelTest {

    private class TestViewModelFactory(
        private val promptsRepository: PromptsRepository,
        private val settingsRepository: SettingsRepository,
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RolesSettingsViewModel(promptsRepository, settingsRepository, application) as T
        }
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var promptsRepository: PromptsRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: RolesSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        promptsRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        val application = mockk<Application>(relaxed = true)
        coEvery { settingsRepository.enabledRoles } returns flowOf(emptySet())
        viewModel = TestViewModelFactory(promptsRepository, settingsRepository, application).create(RolesSettingsViewModel::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onRoleEnabledChanged should call settingsRepository`() = runTest {
        // Given
        val roleName = "test-role"
        val isEnabled = true
        coEvery { settingsRepository.saveEnabledRoles(any()) } returns Unit

        // When
        viewModel.onRoleEnabledChanged(roleName, isEnabled)

        // Then
        coVerify(exactly = 1) { settingsRepository.saveEnabledRoles(any()) }
    }
}
