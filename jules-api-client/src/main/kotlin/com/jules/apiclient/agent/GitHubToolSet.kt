package com.jules.apiclient.agent

import com.google.adk.tools.FunctionTool
import com.github.apiclient.GitHubApiClient

class GitHubToolSet(gitHubApiClient: GitHubApiClient) {
    private val tools = GitHubTools(gitHubApiClient)
    val getPullRequests = FunctionTool.create(
        tools.javaClass,
        "getPullRequests"
    )
    val getPullRequestDiff = FunctionTool.create(
        tools.javaClass,
        "getPullRequestDiff"
    )
    val createComment = FunctionTool.create(
        tools.javaClass,
        "createComment"
    )
}
