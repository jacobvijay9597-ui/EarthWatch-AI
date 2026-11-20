package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM cached_weather_telemetry ORDER BY locationName ASC")
    fun observeCachedWeather(): Flow<List<CachedWeatherEntity>>

    @Query("SELECT * FROM cached_weather_telemetry ORDER BY locationName ASC")
    suspend fun getCachedWeatherOnce(): List<CachedWeatherEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedWeatherEntity>)
}

@Dao
interface HistoricalLandslideDao {
    @Query("SELECT * FROM historical_landslides WHERE corridorId = :corridorId ORDER BY year DESC")
    fun observeByCorridor(corridorId: String): Flow<List<HistoricalLandslideEntity>>

    @Query("SELECT COUNT(*) FROM historical_landslides")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HistoricalLandslideEntity>)
}

@Dao
interface PredictionHistoryDao {
    @Query("SELECT * FROM ml_prediction_logs ORDER BY evaluatedAtEpochMillis DESC LIMIT :limit")
    fun observeRecentLogs(limit: Int = 12): Flow<List<PredictionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: PredictionHistoryEntity)

    @Query("DELETE FROM ml_prediction_logs")
    suspend fun clearAll()
}
