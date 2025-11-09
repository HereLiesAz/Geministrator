package com.hereliesaz.geministrator.di

import android.content.Context
import com.github.apiclient.GitHubApiClient
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.GoogleGenerativeAI
import com.hereliesaz.geministrator.data.A2ACommunicator
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.JulesApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideJulesApiClient(settingsRepository: SettingsRepository): JulesApiClient {
        val apiKey = runBlocking { settingsRepository.julesApiKey.first() }
        return JulesApiClient(apiKey = apiKey ?: "")
    }

    @Provides
    @Singleton
    fun provideGithubApiClient(): GitHubApiClient {
        // TODO: This client will also need authentication
        return GitHubApiClient()
    }

    /**
     * Provides the A2A Communicator.
     * This is the correct way to interact with the remote ADK.
     */
    @Provides
    @Singleton
    fun provideA2ACommunicator(settingsRepository: SettingsRepository): A2ACommunicator {
        return A2ACommunicator(settingsRepository)
    }

    /**
     * Provides a basic Gemini model for any on-device, non-agent tasks
     * (like the old "decompose" feature).
     */
    @Provides
    @Singleton
    fun provideGenerativeModel(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): GenerativeModel {
        val geminiApiKey = runBlocking { settingsRepository.geminiApiKey.first() }
        return GoogleGenerativeAI(context, geminiApiKey ?: "").generativeModel("gemini-pro")
    }
}