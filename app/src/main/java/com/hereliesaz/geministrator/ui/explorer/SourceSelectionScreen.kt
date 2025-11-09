package com.hereliesaz.geministrator.ui.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.JulesRepository
import com.jules.apiclient.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceSelectionUiState(
    val sources: List<Source> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SourceSelectionViewModel @Inject constructor(
    private val julesRepository: JulesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SourceSelectionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSources()
    }

    private fun loadSources() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val sources = julesRepository.getSources()
                _uiState.update {
                    it.copy(
                        sources = sources,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun retry() {
        loadSources()
    }
}