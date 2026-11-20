package com.example.data.repository

import android.content.Context
import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.local.db.CachedWeatherEntity
import com.example.data.local.db.EarthWatchSqlDatabase
import com.example.data.local.db.HistoricalLandslideEntity
import com.example.data.local.db.PredictionHistoryEntity
import com.example.data.model.HistoricalLandslideRecord
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.MlLandslidePredictionResult
import com.example.data.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalSqlCacheRepository(
    private val db: EarthWatchSqlDatabase
) {
    constructor(context: Context) : this(EarthWatchSqlDatabase.getInstance(context))

    suspend fun ensureHistoricalCatalogSeeded() = withContext(Dispatchers.IO) {
        val dao = db.historicalLandslideDao()
        if (dao.countAll() == 0) {
            dao.insertAll(
                NilgirisReferenceCatalog.simulatedHistoricalRecords.map { rec ->
                    HistoricalLandslideEntity(
                        id = rec.id,
                        corridorId = rec.corridorId,
                        year = rec.year,
                        locationLabel = rec.locationLabel,
                        triggerSummary = rec.triggerSummary,
                        severity = rec.severity.label,
                        simulatedDemo = rec.simulatedDemo
                    )
                }
            )
        }
    }

    fun observeHistoricalRecordsByCorridor(corridorId: String): Flow<List<HistoricalLandslideRecord>> {
        return db.historicalLandslideDao().observeByCorridor(corridorId).map { rows ->
            rows.map { row ->
                HistoricalLandslideRecord(
                    id = row.id,
                    corridorId = row.corridorId,
                    year = row.year,
                    locationLabel = row.locationLabel,
                    triggerSummary = row.triggerSummary,
                    severity = RiskLevel.safeFrom(row.severity),
                    simulatedDemo = row.simulatedDemo
                )
            }
        }
    }

    suspend fun cacheWeatherTelemetry(telemetryList: List<LiveWeatherTelemetry>) = withContext(Dispatchers.IO) {
        if (telemetryList.isEmpty()) return@withContext
        db.weatherCacheDao().upsertAll(
            telemetryList.map { w ->
                CachedWeatherEntity(
                    locationId = w.locationId,
                    locationName = w.locationName,
                    temperatureC = w.temperatureC,
                    rainCurrentMm = w.rainCurrentMm,
                    rain24hMm = w.rain24hMm,
                    rain72hMm = w.rain72hMm,
                    rainfallDurationHours72h = w.rainfallDurationHours72h,
                    soilMoistureM3M3 = w.soilMoistureM3M3,
                    windKmh = w.windKmh,
                    weatherLabel = w.weatherLabel,
                    fetchedAtLabel = w.fetchedAtLabel,
                    telemetryTimestampIso = w.telemetryTimestampIso,
                    fetchedAtEpochMillis = w.fetchedAtEpochMillis,
                    sourceLabel = w.sourceLabel
                )
            }
        )
    }

    suspend fun loadCachedWeatherFallback(): List<LiveWeatherTelemetry> = withContext(Dispatchers.IO) {
        db.weatherCacheDao().getCachedWeatherOnce().map { c ->
            LiveWeatherTelemetry(
                locationId = c.locationId,
                locationName = c.locationName,
                temperatureC = c.temperatureC,
                rainCurrentMm = c.rainCurrentMm,
                rain24hMm = c.rain24hMm,
                rain72hMm = c.rain72hMm,
                rainfallDurationHours72h = c.rainfallDurationHours72h,
                soilMoistureM3M3 = c.soilMoistureM3M3,
                windKmh = c.windKmh,
                weatherLabel = c.weatherLabel,
                fetchedAtLabel = "${c.fetchedAtLabel} (SQL Cache)",
                telemetryTimestampIso = c.telemetryTimestampIso,
                fetchedAtEpochMillis = c.fetchedAtEpochMillis,
                isLiveFromApi = false,
                sourceLabel = "Room SQLite Cache (${c.sourceLabel})"
            )
        }
    }

    fun observeRecentPredictionLogs(limit: Int = 10): Flow<List<PredictionHistoryEntity>> =
        db.predictionHistoryDao().observeRecentLogs(limit)

    suspend fun recordMlPrediction(result: MlLandslidePredictionResult) = withContext(Dispatchers.IO) {
        if (result.estimatedProbability == null || result.riskCategory == null) return@withContext
        db.predictionHistoryDao().insertLog(
            PredictionHistoryEntity(
                corridorId = result.corridorId,
                corridorName = result.corridorName,
                estimatedProbability = result.estimatedProbability,
                riskCategory = result.riskCategory.label,
                executionMode = result.executionMode.badgeLabel,
                modelName = result.modelName,
                rainfallIntensityMmHr = result.inputSnapshot.rainfallIntensityMmHr,
                rainfallDurationHours = result.inputSnapshot.rainfallDurationHours,
                soilMoistureM3M3 = result.inputSnapshot.soilMoistureM3M3,
                slopeAngleDeg = result.inputSnapshot.slopeAngleDeg,
                freshnessStatus = result.dataFreshnessStatus.label,
                evaluatedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearPredictionLogs() = withContext(Dispatchers.IO) {
        db.predictionHistoryDao().clearAll()
    }
}
