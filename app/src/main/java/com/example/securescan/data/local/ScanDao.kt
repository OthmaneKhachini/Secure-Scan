package com.example.securescan.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing security scan records in the local database.
 */
@Dao
interface ScanDao {
    /**
     * Inserts a new scan record. Replaces existing records with the same primary key.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity)

    /**
     * Retrieves all scan records ordered by the most recent date first.
     */
    @Query("SELECT * FROM scans ORDER BY date DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    /**
     * Deletes a specific scan record.
     */
    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    /**
     * Retrieves a single scan record by its VirusTotal analysis identifier.
     */
    @Query("SELECT * FROM scans WHERE scanId = :scanId LIMIT 1")
    suspend fun getScanByScanId(scanId: String): ScanEntity?

    /**
     * Purges all scan records from the database.
     */
    @Query("DELETE FROM scans")
    suspend fun deleteAllScans()
}