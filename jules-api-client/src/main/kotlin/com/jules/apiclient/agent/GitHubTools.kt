package com.jules.apiclient.agent

import com.github.apiclient.Comment
import com.github.apiclient.GitHubApiClient
import com.github.apiclient.PullRequest

class GitHubTools(private val gitHubApiClient: GitHubApiClient) {

    suspend fun getPullRequests(owner: String, repo: String): List<PullRequest> {
        /**
         * Retrieves a list of pull requests for a given repository.
         *
         * @param owner The owner of the repository.
         * @param repo The name of the repository.
         * @return A list of pull requests.
         */
        return gitHubApiClient.getPullRequests(owner, repo)
    }

    suspend fun getPullRequestDiff(url: String): String {
        /**
         * Retrieves the diff for a given pull request.
         *
         * @param url The URL of the pull request diff.
         * @return The diff of the pull request.
         */
        return gitHubApiClient.getPullRequestDiff(url)
    }

    suspend fun createComment(owner: String, repo: String, issueNumber: Int, body: String) {
        /**
         * Creates a comment on a given pull request.
         *
         * @param owner The owner of the repository.
         * @param repo The name of the repository.
         * @param issueNumber The number of the pull request.
         * @param body The body of the comment.
         */
        gitHubApiClient.createComment(owner, repo, issueNumber, Comment(body))
    }
}
