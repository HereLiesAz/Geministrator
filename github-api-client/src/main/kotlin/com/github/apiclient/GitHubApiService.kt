package com.github.apiclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

@Serializable
data class PullRequest(
    val number: Int,
    val title: String,
    val body: String?,
    @SerialName("diff_url") val diffUrl: String
)

@Serializable
data class Comment(
    val body: String
)

interface GitHubApiService {
    @GET("/repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<PullRequest>

    @GET
    suspend fun getPullRequestDiff(
        @Header("Authorization") auth: String,
        @Url url: String
    ): String

    @POST("/repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun createComment(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body comment: Comment
    )
}
