package com.hereliesaz.geministrator.adk

import com.google.adk.annotations.Description
import com.google.adk.annotations.Tool
import com.jules.apiclient.JulesApiClient
import com.jules.apiclient.Session
import com.jules.apiclient.Source
import javax.inject.Inject

class JulesTools @Inject constructor(
    private val julesApiClient: JulesApiClient
) {

    @Tool
    @Description("Get a list of all available source code repositories.")
    suspend fun listSources(): List<Source> {
        return julesApiClient.getSources().sources
    }

    @Tool
    @Description("Create a new session for a specific source repository.")
    suspend fun createSession(
        @Description("The ID of the source to use (from listSources).") sourceId: String,
        @Description("A title for the new session.") title: String,
        @Description("The initial prompt or question for the agent.") prompt: String
    ): Session {
        return julesApiClient.createSession(sourceId, title, prompt)
    }

    @Tool
    @Description("Send a message or prompt to an existing session.")
    suspend fun sendMessage(
        @Description("The ID of the session to send a message to.") sessionId: String,
        @Description("The user's message or prompt.") prompt: String
    ) {
        julesApiClient.sendMessage(sessionId, prompt)
    }

    @Tool
    @Description("Get the list of all activities (the conversation history) for a session.")
    suspend fun getActivities(
        @Description("The ID of the session to fetch history for.") sessionId: String
    ) = julesApiClient.getActivities(sessionId)

    @Tool
    @Description("Get the full text content of a single file within a session.")
    suspend fun getFileContent(
        @Description("The ID of the session.") sessionId: String,
        @Description("The full path of the file to read.") filePath: String
    ): String {
        return julesApiClient.getFileContent(sessionId, filePath)
    }

    @Tool
    @Description("Update the content of a file within a session.")
    suspend fun updateFileContent(
        @Description("The ID of the session.") sessionId: String,
        @Description("The full path of the file to write.") filePath: String,
        @Description("The new content to write to the file.") content: String
    ) {
        julesApiClient.updateFileContent(sessionId, filePath, content)
    }

    @Tool
    @Description("Run a file (e.g., a script) within the session's environment.")
    suspend fun runFile(
        @Description("The ID of the session.") sessionId: String,
        @Description("The full path of the file to run.") filePath: String
    ): String {
        return julesApiClient.runFile(sessionId, filePath)
    }

    @Tool
    @Description("Commit all current changes in the session with a given message.")
    suspend fun commitChanges(
        @Description("The ID of the session.") sessionId: String,
        @Description("The commit message.") message: String
    ): String {
        return julesApiClient.commitChanges(sessionId, message)
    }
}