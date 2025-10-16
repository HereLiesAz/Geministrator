package com.hereliesaz.geministrator.service

import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.agent.GitHubTools
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CodeReviewServiceTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var gitHubTools: GitHubTools
    private lateinit var codeReviewService: CodeReviewService

    @Before
    fun setUp() {
        settingsRepository = mockk(relaxed = true)
        gitHubTools = mockk(relaxed = true)
        codeReviewService = CodeReviewService(settingsRepository)
    }

    @Test
    fun `reviewPullRequest should call createComment`() = runTest {
        // Given
        val owner = "test-owner"
        val repo = "test-repo"
        val prNumber = 1
        val userId = "test-user"
        val sessionId = "test-session"
        val geminiModelName = "test-model"
        val githubToken = "test-token"

        coEvery { settingsRepository.geminiModelName } returns flowOf(geminiModelName)
        coEvery { settingsRepository.githubAccessToken } returns flowOf(githubToken)
        coEvery { gitHubTools.getPullRequests(owner, repo) } returns listOf(mockk {
            coEvery { number } returns prNumber
            coEvery { diffUrl } returns "test-url"
        })
        coEvery { gitHubTools.getPullRequestDiff("test-url") } returns "test-diff"
        coEvery { gitHubTools.createComment(owner, repo, prNumber, any()) } returns Unit

        // When
        codeReviewService.reviewPullRequest(owner, repo, prNumber, userId, sessionId)

        // Then
        coVerify(exactly = 1) { gitHubTools.createComment(owner, repo, prNumber, any()) }
    }
}
