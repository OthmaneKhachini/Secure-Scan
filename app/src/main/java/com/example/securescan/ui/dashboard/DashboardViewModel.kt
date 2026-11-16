package com.example.securescan.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.example.securescan.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 * Provides high-level security metrics and recent activity data.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val allScans = repository.getAllScans().asLiveData()

    /**
     * Calculated statistics based on the total scan history.
     */
    val stats = allScans.map { scans ->
        val total = scans.size
        val malicious = scans.count { it.maliciousCount > 0 }
        val safeUrls = scans.count { it.type == "URL" && it.maliciousCount == 0 }
        val safeFiles = scans.count { it.type == "FILE" && it.maliciousCount == 0 }
        
        DashboardStats(total, malicious, safeUrls, safeFiles)
    }
    
    /**
     * The most recent 5 security logs for the dashboard feed.
     */
    val recentActivity = allScans.map { it.take(5) }
}

/**
 * Data class representing the summarized metrics for the dashboard overview.
 */
data class DashboardStats(
    val total: Int,
    val threats: Int,
    val safeUrls: Int,
    val safeFiles: Int
)