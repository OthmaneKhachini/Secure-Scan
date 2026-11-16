package com.example.securescan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing a security scan result.
 */
@Entity(tableName = "scans")
data class ScanEntity(
    /** Unique identifier for the database record. */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    
    /** The type of scan: "URL" or "FILE". */
    val type: String,
    
    /** The target of the scan (URL string or File Name). */
    val target: String,
    
    /** The VirusTotal analysis identifier. */
    val scanId: String,
    
    /** Timestamp of when the scan was performed. */
    val date: Long,
    
    /** Number of security engines that flagged the item as malicious. */
    val maliciousCount: Int,
    
    /** Number of security engines that flagged the item as suspicious. */
    val suspiciousCount: Int,
    
    /** Number of security engines that flagged the item as harmless. */
    val harmlessCount: Int,
    
    /** Number of security engines that did not detect any threat. */
    val undetectedCount: Int,
    
    /** The current status of the analysis (e.g., "completed"). */
    val status: String,
    
    /** Local path to the file if it was a file scan, used for management. */
    val localFilePath: String? = null,
    
    /** Size of the file in bytes. */
    val fileSize: Long = 0L
)