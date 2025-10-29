package com.hereliesaz.geministrator.ui.ide

import java.io.File

data class IdeUiState(
    val currentFile: File? = null,
    val fileContent: String? = null,
    // val editor: Editor? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCommitDialog: Boolean = false,
    val commitMessage: String = "",
    val messages: List<String> = emptyList(),
)
