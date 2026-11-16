package com.example.securescan.ui.filescanner

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
import okhttp3.MultipartBody
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for the File Scanning module.
 * Handles file upload, polling VirusTotal for analysis results, and local persistence.
 */
@HiltViewModel
class FileScannerViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val _scanResult = MutableStateFlow<UiState<AnalysisResponse>>(UiState.Idle)
    val scanResult: StateFlow<UiState<AnalysisResponse>> = _scanResult

    private val _scanStatus = MutableStateFlow<String?>(null)
    val scanStatus: StateFlow<String?> = _scanStatus

    /**
     * Initiates the file scanning process by uploading the file to VirusTotal.
     */
    fun scanFile(filePart: MultipartBody.Part, file: File) {
        viewModelScope.launch {
            _scanResult.value = UiState.Loading
            _scanStatus.value = "Uploading secure package..."
            
            try {
                val response = repository.uploadFile(filePart)
                if (response.isSuccessful) {
                    val scanId = response.body()?.data?.id
                    if (scanId != null) {
                        pollAnalysis(scanId, file)
                    } else {
                        _scanResult.value = UiState.Error("Upload successful but no analysis ID received")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _scanResult.value = UiState.Error("Upload Error ${response.code()}: $errorBody")
                }
            } catch (e: Exception) {
                _scanResult.value = UiState.Error(e.message ?: "Network error")
            }
        }
    }

    /**
     * Polls the VirusTotal API for completion of the file analysis.
     */
    private suspend fun pollAnalysis(id: String, file: File) {
        var attempts = 0
        val maxAttempts = 60 // Allow up to 120 seconds for file analysis
        
        while (attempts < maxAttempts) {
            _scanStatus.value = when {
                attempts < 10 -> "Cloud sandboxing in progress..."
                attempts < 30 -> "Deep heuristic scanning..."
                else -> "Finalizing threat intelligence..."
            }
            
            try {
                val response = repository.getAnalysis(id)
                if (response.isSuccessful) {
                    val analysis = response.body()
                    val status = analysis?.data?.attributes?.status
                    
                    if (status == "completed") {
                        _scanResult.value = UiState.Success(analysis)
                        saveToHistory(analysis, file)
                        return
                    }
                }
            } catch (_: Exception) {
                // Ignore transient network errors during polling
            }
            
            attempts++
            delay(2000)
        }
        _scanResult.value = UiState.Error("Scan timed out. Large files take longer to process.")
    }

    /**
     * Saves the completed analysis report to the local history database.
     */
    private fun saveToHistory(analysis: AnalysisResponse, file: File) {
        viewModelScope.launch {
            val stats = analysis.data.attributes?.stats
            val scanEntity = ScanEntity(
                type = "FILE",
                target = file.name,
                scanId = analysis.data.id,
                date = System.currentTimeMillis(),
                maliciousCount = stats?.malicious ?: 0,
                suspiciousCount = stats?.suspicious ?: 0,
                harmlessCount = stats?.harmless ?: 0,
                undetectedCount = stats?.undetected ?: 0,
                status = analysis.data.attributes?.status ?: "unknown",
                localFilePath = file.absolutePath,
                fileSize = file.length()
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