package com.hereliesaz.geministrator.adapter

import com.hereliesaz.geministrator.common.ExecutionResult
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.core.config.ConfigStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.io.IOException

class GitHubManager(
    private val config: ConfigStorage,
    private val logger: ILogger,
) {
    private suspend fun getAuthenticatedClient(): GitHub? {
        val token = config.loadGitHubToken()
        if (token.isNullOrBlank()) {
            logger.error("GitHub token not configured. Please run `geministrator config --github-token YOUR_TOKEN`.")
            return null
        }
        return GitHubBuilder().withOAuthToken(token).build()
    }

    suspend fun createPullRequest(
        repoName: String, // e.g., "user/repo"
        title: String,
        headBranch: String,
        baseBranch: String,
    ): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val github = getAuthenticatedClient()
                ?: return@withContext ExecutionResult(
                    false,
                    "GitHub client could not be authenticated."
                )

            logger.info("Creating pull request in '$repoName' from '$headBranch' to '$baseBranch'.")

            val repo = github.getRepository(repoName)
            val pr = repo.createPullRequest(
                title,
                headBranch,
                baseBranch,
                "PR created by Geministrator."
            )

            logger.info("Successfully created PR: ${pr.htmlUrl}")
            ExecutionResult(
                true,
                "Pull request created successfully: ${pr.htmlUrl}",
                pr.htmlUrl.toString()
            )
        } catch (e: IOException) {
            logger.error("Failed to create pull request: ${e.message}", e)
            ExecutionResult(false, "Failed to create pull request: ${e.message}")
        }
    }

    suspend fun getIssueDetails(repoName: String, issueNumber: Int): ExecutionResult =
        withContext(Dispatchers.IO) {
            try {
                val github = getAuthenticatedClient()
                    ?: return@withContext ExecutionResult(
                        false,
                        "GitHub client could not be authenticated."
                    )

                logger.info("Fetching details for issue #$issueNumber from '$repoName'.")

                val repo = github.getRepository(repoName)
                val issue = repo.getIssue(issueNumber)

                val details = StringBuilder()
                details.append("--- GITHUB ISSUE #$issueNumber: ${issue.title} ---\n")
                details.append("State: ${issue.state}\n")
                details.append("Author: ${issue.user.login}\n")
                details.append("Labels: ${issue.labels.joinToString(", ") { it.name }}\n\n")
                details.append("--- BODY ---\n${issue.body}\n\n")

                val comments = issue.comments
                if (comments.isNotEmpty()) {
                    details.append("--- COMMENTS ---\n")
                    comments.forEach { comment ->
                        details.append("Comment by ${comment.user.login}:\n${comment.body}\n---\n")
                    }
                }

                ExecutionResult(true, "Successfully fetched issue details.", details.toString())
            } catch (e: IOException) {
                logger.error("Failed to fetch issue #$issueNumber: ${e.message}", e)
                ExecutionResult(false, "Failed to fetch issue: ${e.message}")
            }
        }
}