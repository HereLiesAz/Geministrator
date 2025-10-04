package com.hereliesaz.geministrator.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hereliesaz.geministrator.android.ui.jules.SourceSelectionScreen
import com.hereliesaz.geministrator.android.ui.theme.GeministratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeministratorTheme {
                SourceSelectionScreen()
            }
        }
    }
}