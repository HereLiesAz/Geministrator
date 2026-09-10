package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.RepositoryRef
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubRestActionsClientTest {
    @Test
    fun dispatchAcceptsNoContentAndDiscoversNewWorkflowRun() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        var runsReads = 0
        val engine = MockEngine { request ->
            requests += request
            when {
                request.method == HttpMethod.Post -> respond(
                    content = "",
                    status = HttpStatusCode.NoContent,
                )
                request.url.encodedPath.endsWith("/runs") -> {
                    runsReads += 1
                    val content = if (runsReads == 1) {
                        """{"workflow_runs":[{"id":41,"status":"completed","conclusion":"success"}]}"""
                    } else {
                        """{"workflow_runs":[{"id":42,"status":"queued","conclusion":null},{"id":41,"status":"completed","conclusion":"success"}]}"""
                    }
                    respond(
                        content = content,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> error("Unexpected request: ${request.method} ${request.url}")
            }
        }
        val client = GitHubRestActionsClient(
            httpClient = HttpClient(engine),
            tokenProvider = GitHubTokenProvider { "token-123" },
            baseUrl = "https://api.github.test",
        )

        val run = client.dispatch(
            GitHubWorkflowDispatchRequest(
                repository = RepositoryRef("HereLiesAz", "haive", "main"),
                workflow = "ci.yml",
                ref = "main",
            ),
        )

        assertEquals("42", run.id)
        assertEquals(GitHubWorkflowRunStatus.Queued, run.status)
        assertEquals(3, requests.size)
        val dispatch = requests.single { it.method == HttpMethod.Post }
        assertEquals("/repos/HereLiesAz/haive/actions/workflows/ci.yml/dispatches", dispatch.url.encodedPath)
        assertEquals("Bearer token-123", dispatch.headers[HttpHeaders.Authorization])
        assertEquals("2026-03-10", dispatch.headers["X-GitHub-Api-Version"])
    }

    @Test
    fun getRunMapsSuccessAndNonExpiredArtifacts() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            when {
                request.url.encodedPath.endsWith("/artifacts") -> respond(
                    content = """{"artifacts":[{"id":7,"name":"results","expired":false,"archive_download_url":"https://api.github.test/artifacts/7.zip"},{"id":8,"name":"old","expired":true,"archive_download_url":"https://api.github.test/artifacts/8.zip"}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> respond(
                    content = """{"id":42,"status":"completed","conclusion":"success"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val client = GitHubRestActionsClient(
            httpClient = HttpClient(engine),
            tokenProvider = GitHubTokenProvider { "token-123" },
            baseUrl = "https://api.github.test",
        )

        val run = client.getRun(RepositoryRef("HereLiesAz", "haive", "main"), "42")

        assertEquals(GitHubWorkflowRunStatus.Completed, run.status)
        assertEquals("success", run.progressMessage)
        assertEquals(listOf(GitHubWorkflowArtifact("7", "results", "https://api.github.test/artifacts/7.zip")), run.artifacts)
        assertEquals(2, requests.size)
        assertTrue(requests.all { it.method == HttpMethod.Get })
    }

    @Test
    fun getRunMapsCompletedNonSuccessToFailure() = runBlocking {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/artifacts")) {
                respond(
                    content = """{"artifacts":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond(
                    content = """{"id":42,"status":"completed","conclusion":"failure"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val client = GitHubRestActionsClient(
            httpClient = HttpClient(engine),
            tokenProvider = GitHubTokenProvider { "token" },
            baseUrl = "https://api.github.test",
        )

        val run = client.getRun(RepositoryRef("HereLiesAz", "haive", "main"), "42")

        assertEquals(GitHubWorkflowRunStatus.Failed, run.status)
    }
}
