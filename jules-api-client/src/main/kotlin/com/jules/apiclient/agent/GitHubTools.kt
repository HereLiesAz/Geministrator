package com.jules.apiclient.agent

import com.github.apiclient.Comment
import com.github.apiclient.GitHubApiClient
import com.github.apiclient.PullRequest
import kotlinx.coroutines.runBlocking

class GitHubTools(private val apiClient: GitHubApiClient) {
    fun getPullRequests(owner: String, repo: String): List<PullRequest> = runBlocking {
        apiClient.getPullRequests(owner, repo)
    }

    fun getPullRequestDiff(diffUrl: String): String = runBlocking {
        apiClient.getPullRequestDiff(diffUrl)
    }

    fun createComment(owner: String, repo: String, prNumber: Int, comment: String) = runBlocking {
        apiClient.createComment(owner, repo, prNumber, Comment(comment))
    }
}
