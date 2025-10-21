package com.hereliesaz.geministrator.ui.explorer

import com.hereliesaz.geministrator.MainDispatcherRule
import com.hereliesaz.geministrator.ui.project.ProjectUiState
import com.hereliesaz.geministrator.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.util.ViewModelFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class FileExplorerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var projectViewModel: ProjectViewModel
    private lateinit var viewModel: FileExplorerViewModel
    private lateinit var tempDir: File

    @Before
    fun setup() {
        tempDir = createTempDir()
        projectViewModel = mockk(relaxed = true)
        every { projectViewModel.uiState } returns MutableStateFlow(
            ProjectUiState(
                localCachePath = tempDir
            )
        )

        viewModel = ViewModelFactory {
            FileExplorerViewModel(
                projectViewModel = projectViewModel
            )
        }.create(FileExplorerViewModel::class.java)
    }

    @Test
    fun `when a directory is loaded then uiState is updated`() {
        val file = File(tempDir, "test.txt")
        file.createNewFile()

        viewModel.navigateTo(tempDir)

        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.files.size)
        assertEquals(file, uiState.files[0])
    }

    @Test
    fun `when navigating up then uiState is updated`() {
        val subDir = File(tempDir, "sub")
        subDir.mkdir()

        viewModel.navigateTo(subDir)
        viewModel.navigateUp()

        val uiState = viewModel.uiState.value
        assertEquals(tempDir, uiState.currentPath)
    }

    private fun createTempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "testDir")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        dir.mkdir()
        return dir
    }
}
