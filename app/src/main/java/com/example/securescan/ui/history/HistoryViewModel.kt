package com.example.securescan.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.map
import com.example.securescan.data.local.ScanEntity
import com.example.securescan.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Security Intelligence history screen.
 * Manages the list of past scans and local storage management operations.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    /**
     * Observable list of all scan records.
     */
    val allScans = repository.getAllScans().asLiveData()

    /**
     * Aggregated metrics about the application's local file storage usage.
     */
    val storageInfo = allScans.map { scans ->
        val fileScans = scans.filter { it.type == "FILE" }
        val totalSize = fileScans.sumOf { it.fileSize }
        StorageInfo(
            fileCount = fileScans.size,
            totalSizeBytes = totalSize,
            lastFileName = fileScans.firstOrNull()?.target ?: "None"
        )
    }

    /**
     * Removes a specific security log and its associated local file.
     */
    fun deleteScan(scan: ScanEntity) {
        viewModelScope.launch {
            repository.deleteScan(scan)
        }
    }

    /**
     * Batch removal of multiple security logs.
     */
    fun deleteScans(scans: List<ScanEntity>) {
        viewModelScope.launch {
            scans.forEach { repository.deleteScan(it) }
        }
    }

    /**
     * Purges all intelligence data and local cache files.
     */
    fun deleteAllScans() {
        viewModelScope.launch {
            repository.deleteAllScans()
        }
    }
}

/**
 * Encapsulates storage management information for the history dashboard.
 */
data class StorageInfo(
    val fileCount: Int,
    val totalSizeBytes: Long,
    val lastFileName: String
)