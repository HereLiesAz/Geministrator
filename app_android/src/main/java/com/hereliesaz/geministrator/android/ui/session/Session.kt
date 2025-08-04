package com.hereliesaz.geministrator.android.ui.session

data class Session(
    val id: Int,
    val title: String,
    val initialPrompt: String,
    val viewModel: SessionViewModel
)