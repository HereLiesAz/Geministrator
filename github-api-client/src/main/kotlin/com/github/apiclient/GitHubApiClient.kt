package com.github.apiclient

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class GitHubApiClient(private val accessToken: String) {

    private val json = Json(Json.Default) { this.ignoreUnknownKeys = true }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val service: GitHubApiService = retrofit.create(GitHubApiService::class.java)

    private val authHeader = "Bearer $accessToken"

    suspend fun getPullRequests(owner: String, repo: String): List<PullRequest> {
        return service.getPullRequests(authHeader, owner, repo)
    }

    suspend fun getPullRequestDiff(url: String): String {
        return service.getPullRequestDiff(authHeader, url)
    }

    suspend fun createComment(owner: String, repo: String, issueNumber: Int, comment: Comment) {
        service.createComment(authHeader, owner, repo, issueNumber, comment)
    }
}
