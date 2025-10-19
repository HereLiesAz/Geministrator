package com.hereliesaz.geministrator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@ExperimentalCoroutinesApi
class SettingsRepositoryTest {
    @get:Rule
    val mockkRule = io.mockk.junit4.MockKRule(this)

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        mockkStatic(Context::dataStore)
        val context = mockk<Context>(relaxed = true)
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { temporaryFolder.newFile("test_settings.preferences_pb") }
        )
        every { context.dataStore } returns testDataStore
        settingsRepository = SettingsRepository(context)
    }

    @Test
    fun `save and retrieve api key`() = testScope.runTest {
        // Given
        val apiKey = "test-api-key"

        // When
        settingsRepository.saveApiKey(apiKey)

        // Then
        val retrievedApiKey = settingsRepository.apiKey.first()
        assert(retrievedApiKey == apiKey)
    }
}
