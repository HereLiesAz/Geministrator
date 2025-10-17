package com.hereliesaz.geministrator.ui.codereview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.service.CodeReviewService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CodeReviewUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val reviewResult: String? = null
)

class CodeReviewViewModel(
    private val codeReviewService: CodeReviewService
) : ViewModel() {
    private val _uiState = MutableStateFlow(CodeReviewUiState())
    val uiState = _uiState.asStateFlow()

    fun reviewPullRequest(owner: String, repo: String, prNumber: Int, sessionId: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                codeReviewService.reviewPullRequest(owner, repo, prNumber, userId, sessionId)
                _uiState.update { it.copy(isLoading = false, reviewResult = "Review complete!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
