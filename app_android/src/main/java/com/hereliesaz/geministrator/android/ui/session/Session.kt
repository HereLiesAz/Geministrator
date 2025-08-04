package com.hereliesaz.geministrator.android.ui.session

import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel

data class Session(
    val id: Int,
    val title: String,
    val initialPrompt: String,
    val projectUri: Uri,
    val viewModel: SessionViewModel
)