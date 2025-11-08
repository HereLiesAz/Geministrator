package com.hereliesaz.geministrator.adk

import com.github.apiclient.GitHubApiClient
import com.google.adk.annotations.Description
import com.google.adk.annotations.Tool
import javax.inject.Inject

class GitHubTools @Inject constructor(
    private val gitHubApiClient: GitHubApiClient
) {

    @Tool
    @Description("Get a list of pull requests for a given repository.")
    suspend fun getPullRequests(
        @Description("The owner of the repository (e.g., 'hereliesaz').") owner: String,
        @Description("The name of the repository (e.g., 'geministrator').") repo: String
    ): List<String> {
        // Assuming GitHubApiClient returns a complex object, we simplify it for the agent.
        return gitHubApiClient.getPullRequests(owner, repo).map {
            "PR #${it.number}: ${it.title}"
        }
    }

    @Tool
    @Description("Get the diff (file changes) for a specific pull request.")
    suspend fun getPullRequestDiff(
        @Description("The owner of the repository.") owner: String,
        @Description("The name of the repository.") repo: String,
        @Description("The number of the pull request.") prNumber: Int
    ): String {
        return gitHubApiClient.getPullRequestDiff(owner, repo, prNumber)
    }

    @Tool
    @Description("Create a comment on a pull request.")
    suspend fun createComment(
        @Description("The owner of the repository.") owner: String,
        @Description("The name of the repository.") repo: String,
        @Description("The number of the pull request.") prNumber: Int,
        @Description("The text of the comment to post.") comment: String
    ) {
        gitHubApiClient.createComment(owner, repo, prNumber, comment)
    }
}
