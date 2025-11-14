package com.hereliesaz.geministrator.di

import com.github.apiclient.GitHubApiClient
import com.jules.cliclient.JulesCliClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideJulesCliClient(): JulesCliClient {
        return JulesCliClient()
    }

    @Provides
    @Singleton
    fun provideGithubApiClient(): GitHubApiClient {
        // TODO: This client will also need authentication
        return GitHubApiClient()
    }
}
