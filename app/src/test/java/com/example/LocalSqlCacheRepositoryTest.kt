package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.EarthWatchSqlDatabase
import com.example.data.model.DataFreshnessStatus
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.MlLandslidePredictionResult
import com.example.data.model.MlPredictionInputSnapshot
import com.example.data.model.PredictionExecutionMode
import com.example.data.model.RiskLevel
import com.example.data.repository.LocalSqlCacheRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalSqlCacheRepositoryTest {

    private lateinit var db: EarthWatchSqlDatabase
    private lateinit var repo: LocalSqlCacheRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EarthWatchSqlDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LocalSqlCacheRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `cacheWeatherTelemetry persists and marks fallback as non-live SQL cache`() = runBlocking {
        val sample = LiveWeatherTelemetry(
            locationId = "coonoor",
            locationName = "Coonoor",
            temperatureC = 17.5,
            rainCurrentMm = 6.2,
            rain24hMm = 34.0,
            rain72hMm = 82.0,
            rainfallDurationHours72h = 20,
            soilMoistureM3M3 = 0.36,
            windKmh = 14.0,
            weatherLabel = "Moderate Rain",
            fetchedAtLabel = "20 Nov 07:45 IST",
            telemetryTimestampIso = "2026-11-20T07:45",
            fetchedAtEpochMillis = 1_763_650_000_000L,
            isLiveFromApi = true,
            sourceLabel = "Open-Meteo Live"
        )
        repo.cacheWeatherTelemetry(listOf(sample))

        val fallback = repo.loadCachedWeatherFallback()
        assertEquals(1, fallback.size)
        assertEquals("coonoor", fallback.first().locationId)
        assertFalse(fallback.first().isLiveFromApi)
        assertTrue(fallback.first().sourceLabel.contains("Room SQLite Cache"))
    }

    @Test
    fun `historical catalog seeds and queries by corridor`() = runBlocking {
        repo.ensureHistoricalCatalogSeeded()
        val records = repo.observeHistoricalRecordsByCorridor("mettupalayam_coonoor").first()
        assertTrue(records.isNotEmpty())
        assertTrue(records.all { it.corridorId == "mettupalayam_coonoor" })
    }

    @Test
    fun `recordMlPrediction inserts and clearPredictionLogs empties table`() = runBlocking {
        val result = MlLandslidePredictionResult(
            corridorId = "coimbatore_ooty",
            corridorName = "Coimbatore – Mettupalayam – Coonoor – Ooty",
            estimatedProbability = 0.72,
            riskCategory = RiskLevel.HIGH,
            executionMode = PredictionExecutionMode.CONNECTED_PYTHON_RF_API,
            isValidatedModel = true,
            modelName = "Random Forest",
            modelVersion = "rf-v1.0.0",
            dataFreshnessStatus = DataFreshnessStatus.FRESH,
            freshnessWarning = "Fresh",
            missingDataWarnings = emptyList(),
            contributingFactors = emptyList(),
            inputSnapshot = MlPredictionInputSnapshot(
                rainfallIntensityMmHr = 12.0,
                rainfallDurationHours = 22.0,
                soilMoistureM3M3 = 0.39,
                slopeAngleDeg = 32,
                terrainElevationMeters = 1350,
                historicalLandslideCount = 11,
                telemetryTimestampIso = "2026-11-20T07:45",
                telemetryAgeMinutes = 1
            ),
            explanationNote = "Test"
        )
        repo.recordMlPrediction(result)
        val logs = repo.observeRecentPredictionLogs().first()
        assertEquals(1, logs.size)
        assertEquals("HIGH", logs.first().riskCategory)

        repo.clearPredictionLogs()
        val emptyLogs = repo.observeRecentPredictionLogs().first()
        assertTrue(emptyLogs.isEmpty())
    }
}
