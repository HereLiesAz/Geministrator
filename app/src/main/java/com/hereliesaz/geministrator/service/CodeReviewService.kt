package com.hereliesaz.geministrator.service

import com.github.apiclient.Comment
import com.github.apiclient.GitHubApiClient
import com.jules.apiclient.GeminiApiClient
import kotlinx.coroutines.coroutineScope

class CodeReviewService(
    private val gitHubApiClient: GitHubApiClient,
    private val geminiApiClient: GeminiApiClient
) {

    suspend fun reviewPullRequests(owner: String, repo: String) = coroutineScope {
        val pullRequests = gitHubApiClient.getPullRequests(owner, repo)

        for (pr in pullRequests) {
            val diff = gitHubApiClient.getPullRequestDiff(pr.diffUrl)
            val prompt = "Please review the following code changes and provide feedback:\n\n$diff"
            val review = geminiApiClient.generateContent(prompt)
            val comment = review.text
            if (comment != null) {
                gitHubApiClient.createComment(owner, repo, pr.number, Comment(comment))
            }
        }
    }
}
