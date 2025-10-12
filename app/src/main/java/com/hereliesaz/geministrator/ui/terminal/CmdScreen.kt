package com.hereliesaz.geministrator.ui.terminal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect

@Composable
fun CmdScreen(
    viewModel: TerminalViewModel = viewModel(),
    setLoading: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoading) {
        setLoading(uiState.isLoading)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.processInput(input)
                    input = ""
                }
            ) {
                Text("Submit")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = uiState.output,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
