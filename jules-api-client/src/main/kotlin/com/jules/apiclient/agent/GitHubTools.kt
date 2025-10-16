package com.jules.apiclient.agent

import com.github.apiclient.Comment
import com.github.apiclient.GitHubApiClient
import com.github.apiclient.PullRequest
import com.google.genai.types.EnterpriseWebSearch
import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.GoogleMaps
import com.google.genai.types.GoogleSearch
import com.google.genai.types.GoogleSearchRetrieval
import com.google.genai.types.Retrieval
import com.google.genai.types.Tool
import com.google.genai.types.ToolCodeExecution
import com.google.genai.types.UrlContext
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Method
import java.util.Optional

class GitHubTools(private val apiClient: GitHubApiClient) : Tool() {
    @ToolFunction
    fun getPullRequests(owner: String, repo: String): List<PullRequest> = runBlocking {
        apiClient.getPullRequests(owner, repo)
    }

    annotation class ToolFunction

    @ToolFunction
    fun getPullRequestDiff(diffUrl: String): String = runBlocking {
        apiClient.getPullRequestDiff(diffUrl)
    }

    @ToolFunction
    fun createComment(owner: String, repo: String, prNumber: Int, comment: String) = runBlocking {
        apiClient.createComment(owner, repo, prNumber, Comment(comment))
    }

    override fun functionDeclarations(): Optional<List<FunctionDeclaration?>?>? {
        TODO("Not yet implemented")
    }

    override fun retrieval(): Optional<Retrieval?>? {
        TODO("Not yet implemented")
    }

    override fun googleSearch(): Optional<GoogleSearch?>? {
        TODO("Not yet implemented")
    }

    override fun googleSearchRetrieval(): Optional<GoogleSearchRetrieval?>? {
        TODO("Not yet implemented")
    }

    override fun enterpriseWebSearch(): Optional<EnterpriseWebSearch?>? {
        TODO("Not yet implemented")
    }

    override fun googleMaps(): Optional<GoogleMaps?>? {
        TODO("Not yet implemented")
    }

    override fun urlContext(): Optional<UrlContext?>? {
        TODO("Not yet implemented")
    }

    override fun functions(): Optional<List<Method?>?>? {
        TODO("Not yet implemented")
    }

    override fun codeExecution(): Optional<ToolCodeExecution?>? {
        TODO("Not yet implemented")
    }

    override fun toBuilder(): Builder? {
        TODO("Not yet implemented")
    }
}
