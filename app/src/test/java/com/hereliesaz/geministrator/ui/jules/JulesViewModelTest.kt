package com.hereliesaz.geministrator.ui.jules

import com.hereliesaz.geministrator.MainDispatcherRule
import com.hereliesaz.geministrator.data.PromptsRepository
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.GithubRepo
import com.jules.apiclient.JulesApiClient
import com.jules.apiclient.Session
import com.jules.apiclient.Source
import com.jules.apiclient.SourceContext
import com.jules.apiclient.SourceList
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class JulesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: JulesViewModel
    private val mockSettingsRepository: SettingsRepository = mockk()
    private val mockPromptsRepository: PromptsRepository = mockk()
    private val mockApiClient: JulesApiClient = mockk()

    @Before
    fun setUp() {
        coEvery { mockSettingsRepository.apiKey } returns MutableStateFlow("test_api_key")
        coEvery { mockPromptsRepository.getPrompts() } returns Result.success(emptyList())

        viewModel = JulesViewModel(mockSettingsRepository, mockPromptsRepository)
        viewModel.apiClient = mockApiClient // Manually inject the mock client
    }

    @Test
    fun `loadSources updates uiState with sources on success`() = runTest {
        val sources = listOf(Source("1", "github", GithubRepo("test", "repo")))
        coEvery { mockApiClient.getSources() } returns SourceList(sources)

        viewModel.loadSources()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(sources, viewModel.uiState.value.sources)
    }

    @Test
    fun `onSourceSelected updates uiState and shows dialog`() {
        val source = Source("1", "github", GithubRepo("test", "repo"))
        viewModel.onSourceSelected(source)

        val uiState = viewModel.uiState.value
        assertEquals(source, uiState.selectedSource)
        assertTrue(uiState.showCreateSessionDialog)
    }

    @Test
    fun `dismissCreateSessionDialog hides dialog`() {
        viewModel.onSourceSelected(Source("1", "github", GithubRepo("test", "repo"))) // First show the dialog
        viewModel.dismissCreateSessionDialog()

        assertFalse(viewModel.uiState.value.showCreateSessionDialog)
    }

    @Test
    fun `createSession calls apiClient and updates uiState`() = runTest {
        val source = Source("1", "github", GithubRepo("test", "repo"))
        val session = Session("session_1", "Test Session", "Test Session", SourceContext("github", com.jules.apiclient.GithubRepoContext("main")), "Do something")
        viewModel.onSourceSelected(source)

        coEvery { mockApiClient.createSession(any(), source, any(), any()) } returns session

        viewModel.createSession("Test Session", "Do something")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockApiClient.createSession("Do something", source, "Test Session", "[]") }
        assertEquals(session, viewModel.uiState.value.createdSession)
    }

    @Test
    fun `onRoleSelected updates selectedRoles in uiState`() {
        viewModel.onRoleSelected("planner", true)
        assertTrue(viewModel.uiState.value.selectedRoles.contains("planner"))

        viewModel.onRoleSelected("planner", false)
        assertFalse(viewModel.uiState.value.selectedRoles.contains("planner"))
    }
}
