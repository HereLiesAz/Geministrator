package com.hereliesaz.geministrator.service

import com.google.adk.runner.InMemoryRunner
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.agent.createCodeReviewAgent
import com.jules.apiclient.agent.GitHubTools
import com.jules.apiclient.agent.GitHubToolSet
import kotlinx.coroutines.flow.first
import com.github.apiclient.GitHubApiClient
import com.google.genai.types.Content
import com.google.genai.types.Part
import java.util.Optional

class CodeReviewService(
    private val settingsRepository: SettingsRepository,
) {
    suspend fun reviewPullRequest(owner: String, repo: String, prNumber: Int, userId: String, sessionId: String) {
        val geminiModelName = settingsRepository.geminiModelName.first()
        val githubToken = settingsRepository.githubAccessToken.first()

        if (geminiModelName != null && githubToken != null) {
            val gitHubApiClient = GitHubApiClient(githubToken)
            val toolSet = GitHubToolSet(gitHubApiClient)
            val agent = createCodeReviewAgent(geminiModelName, toolSet)
            val runner = InMemoryRunner(agent)

            val prs = gitHubApiClient.getPullRequests(owner, repo)
            val pr = prs.find { it.number == prNumber }

            if (pr != null) {
                val diff = gitHubApiClient.getPullRequestDiff(pr.diffUrl)
                val userMsg = Content.fromParts(Part.fromText("Review the following code diff:\n\n$diff"))
                val eventStream = runner.runAsync(userId, sessionId, userMsg)
                var review = "No review comments generated."
                eventStream.blockingForEach { event ->
                    if (event.finalResponse() && event.content().isPresent) {
                        event.content().get().parts().flatMap { parts ->
                            if (parts.isEmpty()) Optional.empty() else Optional.of(parts.get(0))
                        }.flatMap(Part::text).ifPresent { text ->
                            review = text
                        }
                    }
                }
                gitHubApiClient.createComment(owner, repo, prNumber, com.github.apiclient.Comment(review))
            }
        }
    }
}
