package com.example.securescan.ui.urlscanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securescan.data.local.ScanEntity
import com.example.securescan.data.model.AnalysisResponse
import com.example.securescan.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for URL scanning operations.
 * Manages the submission, polling, and results of URL analysis requests.
 */
@HiltViewModel
class UrlScannerViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val _scanResult = MutableStateFlow<UiState<AnalysisResponse>>(UiState.Idle)
    val scanResult: StateFlow<UiState<AnalysisResponse>> = _scanResult

    private val _scanStatus = MutableStateFlow<String?>(null)
    val scanStatus: StateFlow<String?> = _scanStatus

    /**
     * Submits a URL for remote VirusTotal analysis.
     */
    fun scanUrl(url: String) {
        viewModelScope.launch {
            _scanResult.value = UiState.Loading
            _scanStatus.value = "Initiating scan..."
            
            try {
                val response = repository.scanUrl(url)
                if (response.isSuccessful) {
                    val scanId = response.body()?.data?.id
                    if (scanId != null) {
                        pollAnalysis(scanId, url)
                    } else {
                        _scanResult.value = UiState.Error("No analysis ID received")
                    }
                } else {
                    _scanResult.value = UiState.Error("Remote scan request failed")
                }
            } catch (e: Exception) {
                _scanResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    /**
     * Periodically requests the status of a scan until it reaches "completed".
     */
    private suspend fun pollAnalysis(id: String, url: String) {
        var attempts = 0
        val maxAttempts = 30 
        
        while (attempts < maxAttempts) {
            _scanStatus.value = when {
                attempts < 5 -> "Connecting to analysis node..."
                attempts < 15 -> "Processing heuristics..."
                else -> "Finalizing intelligence report..."
            }
            
            try {
                val response = repository.getAnalysis(id)
                if (response.isSuccessful) {
                    val analysis = response.body()
                    val status = analysis?.data?.attributes?.status
                    
                    if (status == "completed") {
                        _scanResult.value = UiState.Success(analysis)
                        saveToHistory(analysis, url)
                        return
                    }
                }
            } catch (_: Exception) {}
            
            attempts++
            delay(2000)
        }
        _scanResult.value = UiState.Error("Scan timed out")
    }

    private fun saveToHistory(analysis: AnalysisResponse, url: String) {
        viewModelScope.launch {
            val stats = analysis.data.attributes?.stats
            val scanEntity = ScanEntity(
                type = "URL",
                target = url,
                scanId = analysis.data.id,
                date = System.currentTimeMillis(),
                maliciousCount = stats?.malicious ?: 0,
                suspiciousCount = stats?.suspicious ?: 0,
                harmlessCount = stats?.harmless ?: 0,
                undetectedCount = stats?.undetected ?: 0,
                status = analysis.data.attributes?.status ?: "unknown"
            )
            repository.saveScan(scanEntity)
        }
    }

    fun resetState() {
        _scanResult.value = UiState.Idle
        _scanStatus.value = null
    }

    sealed class UiState<out T> {
        object Idle : UiState<Nothing>()
        object Loading : UiState<Nothing>()
        data class Success<T>(val data: T) : UiState<T>()
        data class Error(val message: String) : UiState<Nothing>()
    }
}