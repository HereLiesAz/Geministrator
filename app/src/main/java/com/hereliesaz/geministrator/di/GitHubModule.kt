package com.hereliesaz.geministrator.di

import com.github.apiclient.GitHubApiClient
import com.hereliesaz.geministrator.data.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GitHubModule {

    @Provides
    @Singleton
    fun provideGitHubApiClient(settingsRepository: SettingsRepository): GitHubApiClient {
        val apiKey = runBlocking { settingsRepository.getApiKey() }
        return GitHubApiClient(apiKey ?: "")
    }
}