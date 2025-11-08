package com.hereliesaz.geministrator.adk

import com.google.adk.annotations.Description
import com.google.adk.annotations.Tool
import com.jules.apiclient.JulesApiClient
import com.jules.apiclient.Session
import com.jules.apiclient.Source
import javax.inject.Inject

class JulesTools @Inject constructor(
    // We will inject a factory or provider to create this client dynamically
    // For now, let's assume it can be provided, or we'll adjust the service.
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
}
