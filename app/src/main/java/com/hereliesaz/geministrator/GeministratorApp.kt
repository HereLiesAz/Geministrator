package com.hereliesaz.geministrator

import android.app.Application
import com.google.firebase.FirebaseApp
import com.hereliesaz.geministrator.agent.GeministratorAgent
import com.hereliesaz.geministrator.data.SettingsRepository

class GeministratorApp : Application() {
    companion object {
        lateinit var agent: GeministratorAgent
            private set
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val settingsRepository = SettingsRepository(this)
        agent = GeministratorAgent(settingsRepository)
    }
}
