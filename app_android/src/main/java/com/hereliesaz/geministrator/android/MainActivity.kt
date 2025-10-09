package com.hereliesaz.geministrator.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.geministrator.android.ui.core.GeministratorNavHost
import com.hereliesaz.geministrator.android.ui.theme.GeministratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeministratorTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val projectViewModel: com.hereliesaz.geministrator.android.ui.project.ProjectViewModel = viewModel()
                    GeministratorNavHost(navController = navController, projectViewModel = projectViewModel, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}