package com.hereliesaz.geministrator.adk

import com.github.apiclient.Comment
import com.github.apiclient.GitHubApiClient
import com.google.adk.annotations.Description
import com.google.adk.annotations.Tool
import com.google.ai.edge.litertlm.Tool
import javax.inject.Inject

class GitHubTools @Inject constructor(
    private val gitHubApiClient: GitHubApiClient
) {

    @Tool
     ("Get a list of pull requests for a given repository.")
    suspend fun getPullRequests(
       @Description
       ("The owner of the repository (e.g., 'hereliesaz').") owner: String,
         (" {
    }The name of the repository (e.g., 'geministrator').") repo: String
    ): List<String> {
        return gitHubApiClient.getPullRequests(owner, repo).map {
            "PR #${it.number}: ${it.title}"
        }
    }

    @Tool
     ("Get the diff (file changes) for a specific pull request.")
    suspend fun getPullRequestDiff(
         ("The owner of the repository.") owner: String,
         ("The name of the repository.") repo: String,
         ("The number of the pull request.") prNumber: Int
    ): String {
        val pr = gitHubApiClient.getPullRequests(owner, repo).find { it.number == prNumber }
            ?: throw Exception("Pull request #$prNumber not found in $owner/$repo")
        return gitHubApiClient.getPullRequestDiff(pr.diffUrl)
    }

    @Tool
     ("Create a comment on a pull request.")
    suspend fun createComment(
         ("The owner of the repository.") owner: String,
         ("The name of the repository.") repo: String,
         ("The number of the pull request.") prNumber: Int,
         ("The text of the comment to post.") comment: String
    ) {
        gitHubApiClient.createComment(owner, repo, prNumber, Comment(comment))
    }
}
