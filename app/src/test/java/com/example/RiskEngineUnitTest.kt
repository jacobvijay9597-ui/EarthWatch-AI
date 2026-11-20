package com.example

import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.location.UserLocationTracker
import com.example.data.model.DataFreshnessStatus
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.PredictionExecutionMode
import com.example.data.model.RiskLevel
import com.example.data.remote.LandslideMlApi
import com.example.data.remote.LandslideMlHealthResponse
import com.example.data.remote.LandslideMlPredictionRequest
import com.example.data.remote.LandslideMlPredictionResponse
import com.example.data.remote.MlFeatureContributionDto
import com.example.data.repository.LandslidePredictionRepository
import com.example.data.repository.RiskEngine
import com.example.ui.navigation.EarthWatchDestination
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RiskEngineUnitTest {

    @Test
    fun earthWatchDestinations_allEntriesNonNullWithRoutes() {
        assertEquals(5, EarthWatchDestination.entries.size)
        EarthWatchDestination.entries.forEach { dest ->
            assertNotNull(dest)
            assertTrue(dest.route.isNotBlank())
            assertTrue(dest.label.isNotBlank())
        }
    }

    @Test
    fun estimateCorridorRisk_returnsNullWhenLiveWeatherMissing() {
        val corridor = NilgirisReferenceCatalog.corridorById("mettupalayam_coonoor")
        val estimate = RiskEngine.estimateCorridorRisk(corridor, emptyMap())
        assertNull(estimate)
    }

    @Test
    fun estimateCorridorRisk_handlesNullableSoilMoistureWithoutFabricating() {
        val corridor = NilgirisReferenceCatalog.corridorById("mettupalayam_coonoor")
        val weatherWithoutSoil = mapOf(
            "mettupalayam" to LiveWeatherTelemetry(
                locationId = "mettupalayam",
                locationName = "Mettupalayam",
                temperatureC = 24.0,
                rainCurrentMm = 6.0,
                rain24hMm = 30.0,
                rain72hMm = 60.0,
                rainfallDurationHours72h = 18,
                soilMoistureM3M3 = null,
                windKmh = 12.0,
                weatherLabel = "Moderate Rain",
                fetchedAtLabel = "Now",
                isLiveFromApi = true,
                sourceLabel = "Open-Meteo"
            )
        )
        val est = RiskEngine.estimateCorridorRisk(corridor, weatherWithoutSoil)
        assertNotNull(est)
        assertEquals(0, est!!.soilMoistureScore)
        assertTrue(est.contributingFactors.any { it.contains("Unavailable", ignoreCase = true) })
    }

    @Test
    fun estimateCorridorRisk_increasesWithHeavyRainAndSoilMoisture() {
        val corridor = NilgirisReferenceCatalog.corridorById("mettupalayam_coonoor")

        val dryWeather = mapOf(
            "mettupalayam" to LiveWeatherTelemetry(
                locationId = "mettupalayam",
                locationName = "Mettupalayam",
                temperatureC = 28.0,
                rainCurrentMm = 0.0,
                rain24hMm = 0.0,
                rain72hMm = 1.0,
                soilMoistureM3M3 = 0.16,
                windKmh = 6.0,
                weatherLabel = "Clear Sky",
                fetchedAtLabel = "Now",
                isLiveFromApi = true,
                sourceLabel = "Open-Meteo"
            )
        )
        val stormWeather = mapOf(
            "mettupalayam" to LiveWeatherTelemetry(
                locationId = "mettupalayam",
                locationName = "Mettupalayam",
                temperatureC = 20.0,
                rainCurrentMm = 12.0,
                rain24hMm = 75.0,
                rain72hMm = 140.0,
                soilMoistureM3M3 = 0.45,
                windKmh = 24.0,
                weatherLabel = "Heavy Rain",
                fetchedAtLabel = "Now",
                isLiveFromApi = true,
                sourceLabel = "Open-Meteo"
            )
        )

        val dryEst = RiskEngine.estimateCorridorRisk(corridor, dryWeather)
        val stormEst = RiskEngine.estimateCorridorRisk(corridor, stormWeather)

        assertNotNull(dryEst)
        assertNotNull(stormEst)
        assertTrue(stormEst!!.riskScore > dryEst!!.riskScore)
        assertEquals(RiskLevel.HIGH, stormEst.riskLevel)
        assertTrue(stormEst.experimentalDisclaimer.contains("EXPERIMENTAL ESTIMATE"))
    }

    @Test
    fun findCorridorsBetween_returnsKotagiriAlternateForMettupalayamOoty() {
        val (primary, alts) = NilgirisReferenceCatalog.findCorridorsBetween("mettupalayam", "ooty")
        assertEquals("mettupalayam_coonoor", primary.id)
        assertTrue(alts.any { it.id == "mettupalayam_kotagiri_ooty" })
    }

    @Test
    fun findCorridorsBetween_coimbatoreToOotyHasGpsPolylineAndSpots() {
        val (primary, alts) = NilgirisReferenceCatalog.findCorridorsBetween("coimbatore", "ooty")
        assertEquals("coimbatore_ooty", primary.id)
        assertTrue(primary.geoPolyline.size >= 8)
        assertEquals(11.0168, primary.geoPolyline.first().latitude, 0.001)
        assertEquals(11.4102, primary.geoPolyline.last().latitude, 0.001)
        assertTrue(primary.monitoredSpots.size >= 3)
        assertTrue(alts.any { it.id == "mettupalayam_kotagiri_ooty" })
    }

    @Test
    fun estimateCorridorRisk_coimbatoreToOotyIncludesCoonoorGhatTelemetry() {
        val corridor = NilgirisReferenceCatalog.corridorById("coimbatore_ooty")
        val coonoorOnlyStorm = mapOf(
            "coonoor" to LiveWeatherTelemetry(
                locationId = "coonoor",
                locationName = "Coonoor",
                temperatureC = 18.0,
                rainCurrentMm = 14.0,
                rain24hMm = 80.0,
                rain72hMm = 150.0,
                soilMoistureM3M3 = 0.44,
                windKmh = 20.0,
                weatherLabel = "Heavy Rain",
                fetchedAtLabel = "Now",
                isLiveFromApi = true,
                sourceLabel = "Open-Meteo"
            )
        )
        val est = RiskEngine.estimateCorridorRisk(corridor, coonoorOnlyStorm)
        assertNotNull(est)
        assertEquals(RiskLevel.HIGH, est!!.riskLevel)
    }

    @Test
    fun isPlausibleGoogleMapsKey_rejectsStubsAndAcceptsValidFormat() {
        assertTrue(!com.example.ui.components.isPlausibleGoogleMapsKey(""))
        assertTrue(!com.example.ui.components.isPlausibleGoogleMapsKey("YOUR_GOOGLE_MAPS_API_KEY"))
        assertTrue(!com.example.ui.components.isPlausibleGoogleMapsKey("123456789"))
        assertTrue(com.example.ui.components.isPlausibleGoogleMapsKey("AIzaSyDummyValidFormatKeyForTesting123456"))
    }

    @Test
    fun mlRepository_usesConnectedPythonRfApiWhenAvailableAndValidated() = runTest {
        val corridor = NilgirisReferenceCatalog.corridorById("coimbatore_ooty")
        val nowMillis = 1_700_000_000_000L
        var capturedRequest: LandslideMlPredictionRequest? = null

        val fakeApi = object : LandslideMlApi {
            override suspend fun getHealth(): LandslideMlHealthResponse =
                LandslideMlHealthResponse(
                    status = "ok",
                    modelLoaded = true,
                    isValidatedModel = true,
                    modelName = "RandomForestClassifier",
                    modelVersion = "rf-2025.1",
                    datasetRows = 60,
                    cvAccuracyMean = 0.916
                )

            override suspend fun predictLandslideRisk(request: LandslideMlPredictionRequest): LandslideMlPredictionResponse {
                capturedRequest = request
                return LandslideMlPredictionResponse(
                    corridorId = request.corridorId,
                    corridorName = request.corridorName,
                    estimatedLandslideProbability = 0.81,
                    riskCategory = "HIGH",
                    contributingFactors = listOf(
                        MlFeatureContributionDto("Rainfall Intensity", 0.31f, "21.0 mm/h", "Elevating Risk"),
                        MlFeatureContributionDto("Slope Angle", 0.22f, "33°", "Elevating Risk")
                    ),
                    modelName = "RandomForestClassifier",
                    modelVersion = "rf-2025.1",
                    isValidatedModel = true,
                    executionMode = "CONNECTED_PYTHON_RF_API",
                    dataFreshnessStatus = "FRESH",
                    freshnessWarning = "Telemetry is fresh.",
                    missingDataWarnings = emptyList(),
                    explanationNote = "Validated Python scikit-learn Random Forest output."
                )
            }
        }

        val repo = LandslidePredictionRepository(
            apiFactory = { fakeApi },
            clockMillisProvider = { nowMillis }
        )
        val liveWeather = listOf(
            LiveWeatherTelemetry(
                locationId = "coonoor",
                locationName = "Coonoor",
                temperatureC = 17.5,
                rainCurrentMm = 21.0,
                rain24hMm = 92.0,
                rain72hMm = 178.0,
                rainfallDurationHours72h = 34,
                soilMoistureM3M3 = 0.43,
                windKmh = 18.0,
                weatherLabel = "Heavy Rain",
                fetchedAtLabel = "Just now",
                telemetryTimestampIso = "2025-11-20T09:00",
                fetchedAtEpochMillis = nowMillis - 5 * 60_000L,
                isLiveFromApi = true,
                sourceLabel = "Open-Meteo"
            )
        )

        val health = repo.checkBackendHealth("http://10.0.2.2:8000/")
        assertTrue(health.isConnected)
        assertTrue(health.isValidatedModel)
        assertEquals(60, health.datasetRows)

        val result = repo.evaluateCorridorRisk(
            corridor = corridor,
            weatherList = liveWeather,
            backendBaseUrl = "http://10.0.2.2:8000/"
        )

        assertEquals(PredictionExecutionMode.CONNECTED_PYTHON_RF_API, result.executionMode)
        assertTrue(result.isValidatedModel)
        assertEquals(0.81, result.estimatedProbability ?: 0.0, 0.0001)
        assertEquals(RiskLevel.HIGH, result.riskCategory)
        assertEquals(DataFreshnessStatus.FRESH, result.dataFreshnessStatus)
        assertEquals(2, result.contributingFactors.size)
        assertEquals(34.0, capturedRequest?.rainfallDurationHours ?: 0.0, 0.01)
        assertEquals(0.43, capturedRequest?.soilMoistureM3M3 ?: 0.0, 0.001)
    }

    @Test
    fun mlRepository_entersDemonstrationModeAndFlagsMissingSoilWhenApiOffline() = runTest {
        val corridor = NilgirisReferenceCatalog.corridorById("mettupalayam_coonoor")
        val nowMillis = 1_700_000_000_000L

        val offlineApi = object : LandslideMlApi {
            override suspend fun getHealth(): LandslideMlHealthResponse {
                throw IOException("Connection refused")
            }

            override suspend fun predictLandslideRisk(request: LandslideMlPredictionRequest): LandslideMlPredictionResponse {
                throw IOException("Connection refused")
            }
        }

        val repo = LandslidePredictionRepository(
            apiFactory = { offlineApi },
            clockMillisProvider = { nowMillis }
        )
        val staleWeatherMissingSoil = listOf(
            LiveWeatherTelemetry(
                locationId = "coonoor",
                locationName = "Coonoor",
                temperatureC = 19.0,
                rainCurrentMm = 15.0,
                rain24hMm = 68.0,
                rain72hMm = 120.0,
                rainfallDurationHours72h = 26,
                soilMoistureM3M3 = null,
                windKmh = 15.0,
                weatherLabel = "Rain",
                fetchedAtLabel = "3h ago",
                telemetryTimestampIso = "2025-11-20T06:00",
                fetchedAtEpochMillis = nowMillis - 180 * 60_000L,
                isLiveFromApi = true,
                sourceLabel = "Open-Meteo"
            )
        )

        val result = repo.evaluateCorridorRisk(
            corridor = corridor,
            weatherList = staleWeatherMissingSoil,
            backendBaseUrl = "http://10.0.2.2:8000/"
        )

        assertEquals(PredictionExecutionMode.DEMONSTRATION_MODE, result.executionMode)
        assertFalse(result.isValidatedModel)
        assertEquals(DataFreshnessStatus.STALE, result.dataFreshnessStatus)
        assertNull(result.inputSnapshot.soilMoistureM3M3)
        assertTrue(result.missingDataWarnings.any { it.contains("Soil moisture telemetry unavailable", ignoreCase = true) })
        assertTrue(result.missingDataWarnings.any { it.contains("Demonstration Mode", ignoreCase = true) })
        assertNotNull(result.estimatedProbability)
    }

    @Test
    fun mlRepository_withholdsPredictionWhenLiveWeatherMissingAndNoDemoSelected() = runTest {
        val corridor = NilgirisReferenceCatalog.corridorById("coimbatore_ooty")
        val repo = LandslidePredictionRepository(
            apiFactory = { error("Should not call API without weather data") }
        )

        val result = repo.evaluateCorridorRisk(
            corridor = corridor,
            weatherList = emptyList(),
            backendBaseUrl = "http://10.0.2.2:8000/",
            demoScenarioOverride = null
        )

        assertEquals(PredictionExecutionMode.DEMONSTRATION_MODE, result.executionMode)
        assertFalse(result.isValidatedModel)
        assertNull(result.estimatedProbability)
        assertNull(result.riskCategory)
        assertEquals(DataFreshnessStatus.MISSING, result.dataFreshnessStatus)
        assertTrue(result.missingDataWarnings.isNotEmpty())
    }

    @Test
    fun mlRepository_demoScenarioProducesDeterministicNonRandomOutput() = runTest {
        val corridor = NilgirisReferenceCatalog.corridorById("coimbatore_ooty")
        val preset = NilgirisReferenceCatalog.demoScenarios.first()
        val repo = LandslidePredictionRepository(
            apiFactory = { error("Demo scenario does not fabricate live API telemetry") }
        )

        val firstRun = repo.evaluateCorridorRisk(
            corridor = corridor,
            weatherList = emptyList(),
            backendBaseUrl = "http://10.0.2.2:8000/",
            demoScenarioOverride = preset
        )
        val secondRun = repo.evaluateCorridorRisk(
            corridor = corridor,
            weatherList = emptyList(),
            backendBaseUrl = "http://10.0.2.2:8000/",
            demoScenarioOverride = preset
        )

        assertEquals(PredictionExecutionMode.DEMONSTRATION_MODE, firstRun.executionMode)
        assertEquals(firstRun.estimatedProbability, secondRun.estimatedProbability)
        assertEquals(firstRun.riskCategory, secondRun.riskCategory)
        assertTrue(firstRun.missingDataWarnings.any { it.contains("Demonstration Scenario", ignoreCase = true) })
    }

    @Test
    fun userLocationTracker_computesHaversineAndNearestHazardSpot() {
        val samePointKm = UserLocationTracker.haversineDistanceKm(11.3530, 76.7959, 11.3530, 76.7959)
        assertEquals(0.0, samePointKm, 0.001)

        val cbeToOotyKm = UserLocationTracker.haversineDistanceKm(11.0168, 76.9558, 11.4102, 76.6950)
        assertTrue(cbeToOotyKm in 45.0..60.0)

        // Position near Burliar / Marappalam along the Coimbatore -> Coonoor -> Ooty ghat road
        val snapshot = UserLocationTracker.buildUserLocationSnapshot(
            latitude = 11.3455,
            longitude = 76.8350,
            accuracyMeters = 8.5f,
            speedMetersPerSec = 10.0f,
            bearingDegrees = 310f,
            timestampMillis = 1_700_000_000_000L,
            isFinePrecision = true
        )

        assertTrue(snapshot.isFinePrecision)
        assertEquals(8.5f, snapshot.accuracyMeters, 0.01f)
        assertEquals(36.0f, snapshot.speedKmh ?: 0f, 0.1f)
        assertTrue(snapshot.distanceToNearestCorridorKm < 0.2)
        assertNotNull(snapshot.nearestHazardSpotName)
        assertTrue((snapshot.distanceToNearestSpotKm ?: 99.0) < 2.5)
    }

    @Test
    fun userLocationTracker_projectsNilgirisBoundsAndRejectsOutOfSectorCoords() {
        val insideCoonoor = UserLocationTracker.projectLatLngToNilgirisSector(11.3530, 76.7959)
        assertNotNull(insideCoonoor)
        assertTrue(insideCoonoor!!.first in 0.0f..1.0f)
        assertTrue(insideCoonoor.second in 0.0f..1.0f)

        val outsideChennai = UserLocationTracker.projectLatLngToNilgirisSector(13.0827, 80.2707)
        assertNull(outsideChennai)
    }
}

