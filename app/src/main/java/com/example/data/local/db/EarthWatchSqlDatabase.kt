package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedWeatherEntity::class,
        HistoricalLandslideEntity::class,
        PredictionHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EarthWatchSqlDatabase : RoomDatabase() {
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun historicalLandslideDao(): HistoricalLandslideDao
    abstract fun predictionHistoryDao(): PredictionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: EarthWatchSqlDatabase? = null

        fun getInstance(context: Context): EarthWatchSqlDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EarthWatchSqlDatabase::class.java,
                    "earthwatch_local_sql.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
