package com.hereliesaz.geministrator.ui.explorer

import com.hereliesaz.geministrator.ui.project.ProjectViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@ExperimentalCoroutinesApi
class FileExplorerViewModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var projectViewModel: ProjectViewModel
    private lateinit var viewModel: FileExplorerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        projectViewModel = mockk(relaxed = true)
        val root = temporaryFolder.newFolder("root")
        every { projectViewModel.uiState } returns MutableStateFlow(
            com.hereliesaz.geministrator.ui.project.ProjectUiState(localCachePath = root)
        )
        viewModel = FileExplorerViewModel(projectViewModel)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `navigateTo should update the current path`() = runTest {
        // Given
        val newDir = temporaryFolder.newFolder("root", "newDir")

        // When
        viewModel.navigateTo(newDir)

        // Then
        assert(viewModel.uiState.value.currentPath == newDir)
    }

    @Test
    fun `navigateUp should update the current path`() = runTest {
        // Given
        val newDir = temporaryFolder.newFolder("root", "newDir")
        viewModel.navigateTo(newDir)

        // When
        viewModel.navigateUp()

        // Then
        assert(viewModel.uiState.value.currentPath == viewModel.uiState.value.projectRoot)
    }
}
