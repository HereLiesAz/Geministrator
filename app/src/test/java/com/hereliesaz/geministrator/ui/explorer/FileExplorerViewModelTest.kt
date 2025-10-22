package com.hereliesaz.geministrator.ui.explorer

import com.hereliesaz.geministrator.MainDispatcherRule
import com.hereliesaz.geministrator.ui.project.ProjectUiState
import com.hereliesaz.geministrator.ui.project.ProjectViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
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

    private lateinit var viewModel: FileExplorerViewModel
    private val mockProjectViewModel: ProjectViewModel = mockk(relaxed = true)
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = createTempDir()
        val projectUiState = ProjectUiState(localCachePath = tempDir)
        every { mockProjectViewModel.uiState } returns MutableStateFlow(projectUiState)
        viewModel = FileExplorerViewModel(mockProjectViewModel)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `initial state is correct`() {
        val uiState = viewModel.uiState.value
        assertEquals(tempDir, uiState.projectRoot)
        assertEquals(tempDir, uiState.currentPath)
        assertTrue(uiState.files.isEmpty())
        assertFalse(uiState.canNavigateUp)
    }

    @Test
    fun `navigateTo updates current path and files`() {
        val subDir = File(tempDir, "subDir")
        subDir.mkdir()
        File(subDir, "file.txt").createNewFile()

        viewModel.navigateTo(subDir)

        val uiState = viewModel.uiState.value
        assertEquals(subDir, uiState.currentPath)
        assertEquals(1, uiState.files.size)
        assertEquals("file.txt", uiState.files[0].name)
        assertTrue(uiState.canNavigateUp)
    }

    @Test
    fun `navigateUp updates current path and files`() {
        val subDir = File(tempDir, "subDir")
        subDir.mkdir()
        viewModel.navigateTo(subDir)
        viewModel.navigateUp()

        val uiState = viewModel.uiState.value
        assertEquals(tempDir, uiState.currentPath)
    }

    @Test
    fun `navigateUp does nothing at root`() {
        val initialState = viewModel.uiState.value
        viewModel.navigateUp()
        assertEquals(initialState, viewModel.uiState.value)
    }
}
