package com.example.securescan.di

import android.content.Context
import androidx.room.Room
import com.example.securescan.data.local.AppDatabase
import com.example.securescan.data.local.ScanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "securescan_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideScanDao(database: AppDatabase): ScanDao {
        return database.scanDao()
    }
}