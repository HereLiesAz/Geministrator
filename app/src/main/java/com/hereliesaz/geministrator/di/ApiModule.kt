package com.hereliesaz.geministrator.di

import android.content.Context
import com.github.apiclient.GitHubApiClient
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.JulesApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
        val apiKey = runBlocking { settingsRepository.apiKey.first() }
        return JulesApiClient(apiKey = apiKey ?: "")
    }

    @Provides
    @Singleton
    fun provideGithubApiClient(settingsRepository: SettingsRepository): GitHubApiClient {
        val accessToken = runBlocking { settingsRepository.githubAccessToken.first() }
        return GitHubApiClient(accessToken = accessToken ?: "")
    }

}