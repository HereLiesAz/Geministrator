package com.hereliesaz.geministrator.ui.project

import com.hereliesaz.geministrator.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class ProjectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: ProjectViewModel

    @Before
    fun setUp() {
        viewModel = ProjectViewModel()
    }

    @Test
    fun `initial state is correct`() {
        val uiState = viewModel.uiState.value
        assertEquals("", uiState.projectName)
        assertEquals("", uiState.description)
        assertEquals("", uiState.cloneUrl)
        assertEquals(null, uiState.localCachePath)
    }

    @Test
    fun `onProjectNameChange updates projectName`() {
        val newProjectName = "Test Project"
        viewModel.onProjectNameChange(newProjectName)
        assertEquals(newProjectName, viewModel.uiState.value.projectName)
    }

    @Test
    fun `onDescriptionChange updates description`() {
        val newDescription = "Test Description"
        viewModel.onDescriptionChange(newDescription)
        assertEquals(newDescription, viewModel.uiState.value.description)
    }

    @Test
    fun `onCloneUrlChange updates cloneUrl`() {
        val newCloneUrl = "https://github.com/test/repo.git"
        viewModel.onCloneUrlChange(newCloneUrl)
        assertEquals(newCloneUrl, viewModel.uiState.value.cloneUrl)
    }

    @Test
    fun `onCacheDirRetrieved updates localCachePath`() {
        val newCacheDir = File("/tmp/cache")
        viewModel.onCacheDirRetrieved(newCacheDir)
        assertEquals(newCacheDir, viewModel.uiState.value.localCachePath)
    }
}
