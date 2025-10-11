package com.hereliesaz.geministrator.ui.authentication

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
                    viewModel.onSignInResult(intent)
                }
            }
        }
    )

    LaunchedEffect(key1 = uiState.signInIntentSender) {
        uiState.signInIntentSender?.let { intentSender ->
            launcher.launch(
                IntentSenderRequest.Builder(
                    intentSender
                ).build()
            )
            viewModel.resetState()
        }
    }

    LaunchedEffect(key1 = uiState.isSignInSuccessful) {
        if (uiState.isSignInSuccessful) {
            Toast.makeText(context, "Sign in successful", Toast.LENGTH_LONG).show()
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    LaunchedEffect(key1 = uiState.signInError) {
        uiState.signInError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { viewModel.onSignInClick() }) {
            Text("Login with Google")
        }
    }
}
