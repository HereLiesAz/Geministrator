package com.hereliesaz.geministrator.di

import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.JulesApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JulesApiModule {

    @Provides
    @Singleton
    fun provideJulesApiClient(settingsRepository: SettingsRepository): JulesApiClient {
        val apiKey = runBlocking { settingsRepository.getApiKey() }
        return JulesApiClient(apiKey ?: "")
    }
}