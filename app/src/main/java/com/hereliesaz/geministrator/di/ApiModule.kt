package com.hereliesaz.geministrator.di

import android.content.Context
import com.github.apiclient.GitHubApiClient
import com.google.adk.AdkApp
import com.google.adk.provider.GeminiModelProvider
import com.google.ai.client.generativeai.GenerativeModel
import com.hereliesaz.geministrator.adk.GitHubTools
import com.hereliesaz.geministrator.adk.JulesTools
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

    /**
     * Provides the raw GitHub API client.
     */
    @Provides
    @Singleton
    fun provideGithubApiClient(): GitHubApiClient {
        return GitHubApiClient()
    }

    /**
     * Provides the ADK Tool wrapper for GitHub.
     */
    @Provides
    @Singleton
    fun provideGithubTools(apiClient: GitHubApiClient): GitHubTools {
        return GitHubTools(apiClient)
    }

    /**
     * Provides the raw Jules API client.
     * This is complex because it requires an API key that must be read
     * from DataStore (via SettingsRepository) at startup.
     */
    @Provides
    @Singleton
    fun provideJulesApiClient(settingsRepository: SettingsRepository): JulesApiClient {
        // Warning: This blocks the main thread on startup to get the key.
        // This is necessary for Hilt's synchronous @Provides.
        // A better long-term solution would be a custom factory or provider.
        val apiKey = runBlocking { settingsRepository.julesApiKey.first() }
        return JulesApiClient(apiKey = apiKey ?: "")
    }

    /**
     * Provides the ADK Tool wrapper for Jules.
     */
    @Provides
    @Singleton
    fun provideJulesTools(apiClient: JulesApiClient): JulesTools {
        return JulesTools(apiClient)
    }

    /**
     * Provides the GenerativeModel for the ADK.
     * This also requires an API key from settings.
     */
    @Provides
    @Singleton
    fun provideGenerativeModel(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): GenerativeModel {
        val geminiApiKey = runBlocking { settingsRepository.geminiApiKey.first() }
        return GeminiModelProvider.create(
            context = context,
            apiKey = geminiApiKey ?: ""
        )
    }

    /**
     * Provides the central ADK App runner.
     */
    @Provides
    @Singleton
    fun provideAdkApp(
        model: GenerativeModel,
        julesTools: JulesTools,
        gitHubTools: GitHubTools
    ): AdkApp {
        return AdkApp.builder(model)
            .addTool(julesTools)
            .addTool(gitHubTools)
            .build()
    }
}
