package com.jules.apiclient.agent

import com.github.apiclient.Comment
import com.github.apiclient.GitHubApiClient
import com.github.apiclient.PullRequest
import com.google.adk.tools.Tool
import com.google.adk.tools.annotations.ToolFunction
import kotlinx.coroutines.runBlocking

class GitHubTools(private val apiClient: GitHubApiClient) : Tool {
    @ToolFunction
    fun getPullRequests(owner: String, repo: String): List<PullRequest> = runBlocking {
        apiClient.getPullRequests(owner, repo)
    }

    @ToolFunction
    fun getPullRequestDiff(diffUrl: String): String = runBlocking {
        apiClient.getPullRequestDiff(diffUrl)
    }

    @ToolFunction
    fun createComment(owner: String, repo: String, prNumber: Int, comment: String) = runBlocking {
        apiClient.createComment(owner, repo, prNumber, Comment(comment))
    }
}
