package com.hereliesaz.geministrator.ui.codereview

import android.app.Application
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.service.CodeReviewService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class CodeReviewViewModelTest {

    private class TestViewModelFactory(
        private val codeReviewService: CodeReviewService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CodeReviewViewModel(codeReviewService) as T
        }
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var codeReviewService: CodeReviewService
    private lateinit var viewModel: CodeReviewViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        codeReviewService = mockk(relaxed = true)
        viewModel = TestViewModelFactory(codeReviewService).create(CodeReviewViewModel::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reviewPullRequest should call codeReviewService`() = runTest {
        // Given
        val owner = "test-owner"
        val repo = "test-repo"
        val prNumber = 1
        val sessionId = "test-session"
        val userId = "test-user"
        coEvery { codeReviewService.reviewPullRequest(owner, repo, prNumber, userId, sessionId) } returns Unit

        // When
        viewModel.reviewPullRequest(owner, repo, prNumber, sessionId, userId)

        // Then
        coVerify(exactly = 1) { codeReviewService.reviewPullRequest(owner, repo, prNumber, userId, sessionId) }
    }
}
