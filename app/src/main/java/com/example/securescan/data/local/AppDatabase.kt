package com.example.securescan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Main database class for the SecureScan application.
 */
@Database(entities = [ScanEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    /**
     * @return The Data Access Object for scans.
     */
    abstract fun scanDao(): ScanDao
}