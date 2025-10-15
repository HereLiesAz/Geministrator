package com.hereliesaz.geministrator.service

import com.google.adk.runtime.AdkApp
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.agent.createCodeReviewAgent
import com.jules.apiclient.agent.GitHubTools
import kotlinx.coroutines.flow.first
import com.github.apiclient.GitHubApiClient

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
            val adkApp = AdkApp(rootAgent = agent)

            adkApp.registerTools(tools)

            val prs = tools.getPullRequests(owner, repo)
            val pr = prs.find { it.number == prNumber }

            if (pr != null) {
                val diff = tools.getPullRequestDiff(pr.diff_url)
                val response = adkApp.query("Review the following code diff:\n\n$diff")
                val lastResponse = response.last()
                val review = lastResponse.data["text_output"] ?: "No review comments generated."
                tools.createComment(owner, repo, prNumber, review)
            }
        }
    }
}
