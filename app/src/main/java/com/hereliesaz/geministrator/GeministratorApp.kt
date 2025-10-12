package com.hereliesaz.geministrator

import android.app.Application
import com.google.firebase.FirebaseApp

class GeministratorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
