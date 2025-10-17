package com.hereliesaz.geministrator.ui.project

import android.app.Application
import android.net.Uri
import com.hereliesaz.geministrator.data.GitManager
import com.hereliesaz.geministrator.data.cloneRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@ExperimentalCoroutinesApi
class ProjectViewModelTest {

    private class TestViewModelFactory(
        private val projectManager: ProjectManager,
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectViewModel(projectManager, application) as T
        }
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var projectManager: ProjectManager
    private lateinit var viewModel: ProjectViewModel

    @Before
    fun setUp() {
        mockkStatic("com.hereliesaz.geministrator.data.GitManagerKt")
        Dispatchers.setMain(testDispatcher)
        projectManager = mockk(relaxed = true) {
            coEvery { getProjectFolderUri() } returns null
        }
        val application: Application = mockk(relaxed = true)
        viewModel = TestViewModelFactory(projectManager, application).create(ProjectViewModel::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cloneProject should call cloneRepository`() = runTest {
        // Given
        val url = "test-url"
        val file: File = mockk(relaxed = true)
        coEvery { com.hereliesaz.geministrator.data.cloneRepository(url, any()) } returns Result.success(file)

        // When
        viewModel.cloneProject(url)

        // Then
        coVerify(exactly = 1) { com.hereliesaz.geministrator.data.cloneRepository(url, any()) }
    }
}
