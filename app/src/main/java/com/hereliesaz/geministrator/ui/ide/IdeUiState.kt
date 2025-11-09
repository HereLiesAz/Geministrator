package com.hereliesaz.geministrator.ui.ide

data class IdeUiState(
    val filePath: String = "",
    val fileContent: String = "",
    val consoleOutput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)