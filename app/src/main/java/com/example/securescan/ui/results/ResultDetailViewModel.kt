package com.example.securescan.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securescan.data.model.AnalysisResponse
import com.example.securescan.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultDetailViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val _analysisResult = MutableStateFlow<UiState<AnalysisResponse>>(UiState.Idle)
    val analysisResult: StateFlow<UiState<AnalysisResponse>> = _analysisResult

    fun fetchAnalysis(id: String) {
        viewModelScope.launch {
            _analysisResult.value = UiState.Loading
            try {
                val response = repository.getAnalysis(id)
                if (response.isSuccessful) {
                    val analysis = response.body()
                    if (analysis != null) {
                        _analysisResult.value = UiState.Success(analysis)
                    } else {
                        _analysisResult.value = UiState.Error("Analysis not found")
                    }
                } else {
                    _analysisResult.value = UiState.Error("Error: ${response.message()}")
                }
            } catch (e: Exception) {
                _analysisResult.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteScan(id: String) {
        viewModelScope.launch {
            repository.getScanByScanId(id)?.let { entity ->
                repository.deleteScan(entity)
            }
        }
    }

    sealed class UiState<out T> {
        object Idle : UiState<Nothing>()
        object Loading : UiState<Nothing>()
        data class Success<T>(val data: T) : UiState<T>()
        data class Error(val message: String) : UiState<Nothing>()
    }
}