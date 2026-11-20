package com.example.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cached_weather_telemetry")
data class CachedWeatherEntity(
    @PrimaryKey val locationId: String,
    val locationName: String,
    val temperatureC: Double,
    val rainCurrentMm: Double,
    val rain24hMm: Double,
    val rain72hMm: Double,
    val rainfallDurationHours72h: Int,
    val soilMoistureM3M3: Double?,
    val windKmh: Double,
    val weatherLabel: String,
    val fetchedAtLabel: String,
    val telemetryTimestampIso: String,
    val fetchedAtEpochMillis: Long,
    val sourceLabel: String
)

@Entity(
    tableName = "historical_landslides",
    indices = [Index(value = ["corridorId"])]
)
data class HistoricalLandslideEntity(
    @PrimaryKey val id: String,
    val corridorId: String,
    val year: Int,
    val locationLabel: String,
    val triggerSummary: String,
    val severity: String,
    val simulatedDemo: Boolean
)

@Entity(
    tableName = "ml_prediction_logs",
    indices = [Index(value = ["corridorId"]), Index(value = ["evaluatedAtEpochMillis"])]
)
data class PredictionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val corridorId: String,
    val corridorName: String,
    val estimatedProbability: Double?,
    val riskCategory: String,
    val executionMode: String,
    val modelName: String,
    val rainfallIntensityMmHr: Double?,
    val rainfallDurationHours: Double?,
    val soilMoistureM3M3: Double?,
    val slopeAngleDeg: Int,
    val freshnessStatus: String,
    val evaluatedAtEpochMillis: Long
)
