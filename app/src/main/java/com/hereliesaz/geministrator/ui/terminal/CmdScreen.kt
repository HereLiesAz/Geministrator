package com.hereliesaz.geministrator.ui.terminal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.aznavrail.AzButton

@Composable
fun CmdScreen(
    viewModel: TerminalViewModel = hiltViewModel(),
    setLoading: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoading) {
        setLoading(uiState.isLoading)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = uiState.output,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        BasicTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth()
        )
        AzButton(
            onClick = {
                viewModel.processInput(input)
                input = ""
            },
            modifier = Modifier.fillMaxWidth(),
            text = "Submit"
        )
    }
}
