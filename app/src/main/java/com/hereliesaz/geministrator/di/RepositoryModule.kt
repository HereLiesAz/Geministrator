package com.hereliesaz.geministrator.di

import android.content.Context
import com.hereliesaz.geministrator.data.GitManager
import com.hereliesaz.geministrator.data.GithubRepository
import com.hereliesaz.geministrator.data.GithubRepositoryImpl
import com.hereliesaz.geministrator.data.HistoryRepository
import com.hereliesaz.geministrator.data.HistoryRepositoryImpl
import com.hereliesaz.geministrator.data.JulesRepository
import com.hereliesaz.geministrator.data.JulesCliRepositoryImpl
import com.hereliesaz.geministrator.data.PromptsRepository
import com.hereliesaz.geministrator.data.PromptsRepositoryImpl
import com.hereliesaz.geministrator.data.SessionRepository
import com.hereliesaz.geministrator.data.SessionRepositoryImpl
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.data.SettingsRepositoryImpl
import com.jules.cliclient.JulesCliClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideHistoryRepository(): HistoryRepository {
        return HistoryRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun providePromptsRepository(
        @ApplicationContext context: Context
    ): PromptsRepository {
        return PromptsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideJulesRepository(
        julesCliClient: JulesCliClient
    ): JulesRepository {
        return JulesCliRepositoryImpl(julesCliClient)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        julesRepository: JulesRepository
    ): SessionRepository {
        return SessionRepositoryImpl(julesRepository)
    }

    @Provides
    @Singleton
    fun provideGithubRepository(): GithubRepository {
        return GithubRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideGitManager(
        @ApplicationContext context: Context
    ): GitManager {
        return GitManager(context)
    }
}
