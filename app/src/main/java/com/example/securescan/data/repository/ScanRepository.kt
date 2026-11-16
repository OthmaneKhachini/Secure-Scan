package com.example.securescan.data.repository

import com.example.securescan.data.local.ScanDao
import com.example.securescan.data.local.ScanEntity
import com.example.securescan.data.remote.VirusTotalApi
import com.example.securescan.util.Constants
import kotlinx.coroutines.flow.first
import okhttp3.MultipartBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class that handles data operations for security scans.
 * It serves as a mediator between the VirusTotal Remote API and local Room Database.
 *
 * @property api The VirusTotal API service for remote analysis requests.
 * @property scanDao The Data Access Object for local scan history persistence.
 */
@Singleton
class ScanRepository @Inject constructor(
    private val api: VirusTotalApi,
    private val scanDao: ScanDao
) {
    /**
     * Submits a URL to VirusTotal for analysis.
     */
    suspend fun scanUrl(url: String) = api.scanUrl(Constants.API_KEY, url)

    /**
     * Retrieves the analysis report for a specific scan ID.
     */
    suspend fun getAnalysis(id: String) = api.getAnalysis(Constants.API_KEY, id)

    /**
     * Uploads a file to VirusTotal for security analysis.
     */
    suspend fun uploadFile(file: MultipartBody.Part) = api.uploadFile(Constants.API_KEY, file)

    /**
     * Persists a scan record to the local history database.
     */
    suspend fun saveScan(scan: ScanEntity) = scanDao.insertScan(scan)

    /**
     * Returns a Flow of all scan records ordered by date.
     */
    fun getAllScans() = scanDao.getAllScans()

    /**
     * Finds a specific scan record by its VirusTotal analysis ID.
     */
    suspend fun getScanByScanId(scanId: String) = scanDao.getScanByScanId(scanId)

    /**
     * Deletes a specific scan record and its associated local file if it exists.
     */
    suspend fun deleteScan(scan: ScanEntity) {
        scan.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        scanDao.deleteScan(scan)
    }

    /**
     * Purges all scan records from the database and deletes all associated local files.
     */
    suspend fun deleteAllScans() {
        val allScans = scanDao.getAllScans().first()
        allScans.forEach { scan ->
            scan.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
        scanDao.deleteAllScans()
    }
}