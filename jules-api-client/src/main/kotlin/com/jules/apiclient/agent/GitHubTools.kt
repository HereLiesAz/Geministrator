package com.jules.apiclient.agent

import com.github.apiclient.Comment
import com.github.apiclient.GitHubApiClient
import com.github.apiclient.PullRequest
import com.google.adk.tools.Tool
import com.google.adk.tools.ToolFunction

class GitHubTools(private val apiClient: GitHubApiClient) : Tool {
    @ToolFunction
    suspend fun getPullRequests(owner: String, repo: String): List<PullRequest> {
        return apiClient.getPullRequests(owner, repo)
    }

    @ToolFunction
    suspend fun getPullRequestDiff(diffUrl: String): String {
        return apiClient.getPullRequestDiff(diffUrl)
    }

    @ToolFunction
    suspend fun createComment(owner: String, repo: String, prNumber: Int, comment: String) {
        apiClient.createComment(owner, repo, prNumber, Comment(comment))
    }
}
