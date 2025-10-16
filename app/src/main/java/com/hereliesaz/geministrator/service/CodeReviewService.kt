package com.hereliesaz.geministrator.service

import com.google.adk.runner.InMemoryRunner
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.agent.createCodeReviewAgent
import com.jules.apiclient.agent.GitHubTools
import kotlinx.coroutines.flow.first
import com.github.apiclient.GitHubApiClient
import com.google.genai.types.Content
import com.google.genai.types.Part

class CodeReviewService(
    private val settingsRepository: SettingsRepository,
) {
    suspend fun reviewPullRequest(owner: String, repo: String, prNumber: Int) {
        val geminiModelName = settingsRepository.geminiModelName.first()
        val githubToken = settingsRepository.githubAccessToken.first()

        if (geminiModelName != null && githubToken != null) {
            val agent = createCodeReviewAgent(geminiModelName)
            val gitHubApiClient = GitHubApiClient(githubToken)
            val tools = GitHubTools(gitHubApiClient)
            val runner = InMemoryRunner(agent)

            val prs = tools.getPullRequests(owner, repo)
            val pr = prs.find { it.number == prNumber }

            if (pr != null) {
                val diff = tools.getPullRequestDiff(pr.diffUrl)
                val userMsg = Content.fromParts(Part.fromText("Review the following code diff:\n\n$diff"))
                val response = runner.runAsync("user123", "session123", userMsg).last()
                val review = response.content.parts[0].text ?: "No review comments generated."
                tools.createComment(owner, repo, prNumber, review)
            }
        }
    }
}
