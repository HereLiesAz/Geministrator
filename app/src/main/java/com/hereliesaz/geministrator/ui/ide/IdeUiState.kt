package com.hereliesaz.geministrator.ui.ide

import java.io.File

data class TextInsertion(val text: String, val line: Int, val column: Int)

data class IdeUiState(
    val currentFile: File? = null,
    val fileContent: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCommitDialog: Boolean = false,
    val commitMessage: String = "",
    val messages: List<String> = emptyList(),
    val textInsertion: TextInsertion? = null
)
