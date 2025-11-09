package com.hereliesaz.geministrator.di

import android.content.Context
import com.hereliesaz.geministrator.adk.BrowserTools
import com.hereliesaz.geministrator.adk.FileSystemTools
import com.hereliesaz.geministrator.adk.GitHubTools
import com.hereliesaz.geministrator.adk.JulesTools
import com.hereliesaz.geministrator.adk.TerminalTools
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdkModule {

}