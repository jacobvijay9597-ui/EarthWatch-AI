package com.example.data.repository

import android.util.Log
import com.example.data.model.DataFreshnessStatus
import com.example.data.model.DemoScenarioPreset
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.MlBackendHealthStatus
import com.example.data.model.MlFactorContribution
import com.example.data.model.MlLandslidePredictionResult
import com.example.data.model.MlPredictionInputSnapshot
import com.example.data.model.MonitoredCorridor
import com.example.data.model.PredictionExecutionMode
import com.example.data.model.RiskLevel
import com.example.data.remote.LandslideMlApi
import com.example.data.remote.LandslideMlPredictionRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private const val ML_REPO_TAG = "LandslidePredictionRepo"

class LandslidePredictionRepository(
    private val apiFactory: (String) -> LandslideMlApi = { LandslideMlApi.create(it) },
    private val clockMillisProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun checkBackendHealth(rawBaseUrl: String): MlBackendHealthStatus =
        withContext(Dispatchers.IO) {
            val normalized = LandslideMlApi.normalizeBaseUrl(rawBaseUrl)
            if (normalized.isBlank() || (!normalized.startsWith("http://") && !normalized.startsWith("https://"))) {
                return@withContext MlBackendHealthStatus(
                    isConnected = false,
                    isValidatedModel = false,
                    endpointUrl = rawBaseUrl,
                    modelName = "Not connected",
                    modelVersion = "unavailable",
                    datasetRows = 0,
                    cvAccuracyMean = 0.0,
                    statusDetail = "Enter a valid HTTP/HTTPS URL for the Python Random Forest API."
                )
            }
            try {
                val health = apiFactory(normalized).getHealth()
                MlBackendHealthStatus(
                    isConnected = health.modelLoaded,
                    isValidatedModel = health.isValidatedModel && health.modelLoaded,
                    endpointUrl = normalized,
                    modelName = health.modelName.ifBlank { "Python Random Forest" },
                    modelVersion = health.modelVersion.ifBlank { "unknown" },
                    datasetRows = health.datasetRows,
                    cvAccuracyMean = health.cvAccuracyMean,
                    statusDetail = if (health.modelLoaded && health.isValidatedModel) {
                        "Connected to Python Random Forest API (${health.datasetRows} training records)."
                    } else {
                        "Backend reachable, but trained Random Forest artifact is not loaded."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                safeLog("Python ML backend unreachable at $normalized: ${e.message}")
                MlBackendHealthStatus(
                    isConnected = false,
                    isValidatedModel = false,
                    endpointUrl = normalized,
                    modelName = "Offline",
                    modelVersion = "unavailable",
                    datasetRows = 0,
                    cvAccuracyMean = 0.0,
                    statusDetail = "Python Random Forest API unreachable (${e.javaClass.simpleName}). Demonstration Mode active."
                )
            }
        }

    suspend fun evaluateCorridorRisk(
        corridor: MonitoredCorridor,
        weatherList: List<LiveWeatherTelemetry>,
        backendBaseUrl: String,
        demoScenarioOverride: DemoScenarioPreset? = null
    ): MlLandslidePredictionResult = withContext(Dispatchers.IO) {
        val weatherMap = weatherList.associateBy { it.locationId }
        val stationIds = if (corridor.id == "coimbatore_ooty") {
            listOf("coimbatore", "mettupalayam", "coonoor", "ooty")
        } else {
            listOf(corridor.fromLocationId, corridor.toLocationId)
        }
        val stationTelemetry = stationIds.mapNotNull { weatherMap[it] }

        if (demoScenarioOverride != null) {
            val snapshot = MlPredictionInputSnapshot(
                rainfallIntensityMmHr = demoScenarioOverride.rainfallIntensityMmHr,
                rainfallDurationHours = demoScenarioOverride.rainfallDurationHours,
                soilMoistureM3M3 = demoScenarioOverride.soilMoistureM3M3,
                slopeAngleDeg = corridor.maxSlopeDegrees,
                terrainElevationMeters = corridor.representativeElevationMeters,
                historicalLandslideCount = corridor.historicalLandslideCount,
                telemetryTimestampIso = "DEMO_PRESET",
                telemetryAgeMinutes = null
            )
            val missing = buildList {
                if (demoScenarioOverride.soilMoistureM3M3 == null) {
                    add("Soil moisture omitted in this demonstration scenario; ensemble excludes saturation term.")
                }
                add("Using curated Demonstration Scenario '${demoScenarioOverride.title}' — NOT live Open-Meteo weather.")
            }
            return@withContext runDeterministicDemonstrationEnsemble(
                corridor = corridor,
                snapshot = snapshot,
                freshnessStatus = DataFreshnessStatus.MISSING,
                freshnessWarning = "DEMONSTRATION SCENARIO ACTIVE: Curated non-live inputs loaded for model inspection.",
                missingWarnings = missing,
                explanationNote = "${demoScenarioOverride.description} Switch back to Live Telemetry mode when Open-Meteo & Python ML API are active."
            )
        }

        if (stationTelemetry.isEmpty()) {
            val emptySnapshot = MlPredictionInputSnapshot(
                rainfallIntensityMmHr = null,
                rainfallDurationHours = null,
                soilMoistureM3M3 = null,
                slopeAngleDeg = corridor.maxSlopeDegrees,
                terrainElevationMeters = corridor.representativeElevationMeters,
                historicalLandslideCount = corridor.historicalLandslideCount,
                telemetryTimestampIso = null,
                telemetryAgeMinutes = null
            )
            return@withContext MlLandslidePredictionResult(
                corridorId = corridor.id,
                corridorName = corridor.name,
                estimatedProbability = null,
                riskCategory = null,
                executionMode = PredictionExecutionMode.DEMONSTRATION_MODE,
                isValidatedModel = false,
                modelName = "Prediction Withheld (Insufficient Data)",
                modelVersion = "N/A",
                dataFreshnessStatus = DataFreshnessStatus.MISSING,
                freshnessWarning = "Live Open-Meteo rainfall & soil telemetry unavailable. Cannot run real-time prediction without fabricated weather.",
                missingDataWarnings = listOf(
                    "Missing live rainfall intensity (mm/h) and 72h rainfall duration.",
                    "Missing live surface soil moisture (0–7cm).",
                    "Sync live weather telemetry or load a clearly labelled [DEMO] scenario."
                ),
                contributingFactors = emptyList(),
                inputSnapshot = emptySnapshot,
                explanationNote = "DEMONSTRATION MODE: Required meteorological data is unavailable. No random numbers or fabricated weather values were generated."
            )
        }

        val maxRainIntensity = stationTelemetry.maxOf { it.rainCurrentMm }
        val maxRainDuration = stationTelemetry.maxOf { it.rainfallDurationHours72h }.toDouble()
        val maxRain24h = stationTelemetry.maxOf { it.rain24hMm }
        val maxRain72h = stationTelemetry.maxOf { it.rain72hMm }
        val availableSoilReadings = stationTelemetry.mapNotNull { it.soilMoistureM3M3 }
        val maxSoilMoisture: Double? = availableSoilReadings.maxOrNull()

        val oldestFetchEpoch = stationTelemetry.minOf { it.fetchedAtEpochMillis }
        val ageMinutes = ((clockMillisProvider() - oldestFetchEpoch) / 60_000L).toInt().coerceAtLeast(0)
        val latestTimestampIso = stationTelemetry.firstNotNullOfOrNull {
            it.telemetryTimestampIso.takeIf(String::isNotBlank)
        }

        val freshnessStatus = when {
            ageMinutes > 120 -> DataFreshnessStatus.STALE
            ageMinutes > 45 -> DataFreshnessStatus.AGING
            else -> DataFreshnessStatus.FRESH
        }
        val freshnessWarning = when (freshnessStatus) {
            DataFreshnessStatus.FRESH -> "Live Open-Meteo telemetry synced ${ageMinutes}m ago (${latestTimestampIso ?: "recent"})."
            DataFreshnessStatus.AGING -> "Telemetry is ${ageMinutes}m old; refresh before ascending ghat roads."
            DataFreshnessStatus.STALE -> "STALE TELEMETRY: Data is ${ageMinutes}m old. Sync live weather for current storm risk."
            DataFreshnessStatus.MISSING -> "Telemetry timestamp unavailable."
        }

        val missingWarnings = buildList {
            if (maxSoilMoisture == null) {
                add("Soil moisture telemetry unavailable from Open-Meteo for ${corridor.name}; handled without fabricating sensor values.")
            }
            add("Historical landslide frequency uses curated Nilgiris reference records pending official GSI archive feed.")
        }

        val snapshot = MlPredictionInputSnapshot(
            rainfallIntensityMmHr = maxRainIntensity,
            rainfallDurationHours = maxRainDuration,
            soilMoistureM3M3 = maxSoilMoisture,
            slopeAngleDeg = corridor.maxSlopeDegrees,
            terrainElevationMeters = corridor.representativeElevationMeters,
            historicalLandslideCount = corridor.historicalLandslideCount,
            telemetryTimestampIso = latestTimestampIso,
            telemetryAgeMinutes = ageMinutes
        )

        val normalizedUrl = LandslideMlApi.normalizeBaseUrl(backendBaseUrl)
        if (normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
            try {
                val response = apiFactory(normalizedUrl).predictLandslideRisk(
                    LandslideMlPredictionRequest(
                        corridorId = corridor.id,
                        corridorName = corridor.name,
                        rainfallIntensityMmHr = maxRainIntensity,
                        rainfallDurationHours = maxRainDuration,
                        rainfall24hMm = maxRain24h,
                        rainfall72hMm = maxRain72h,
                        soilMoistureM3M3 = maxSoilMoisture,
                        slopeAngleDeg = corridor.maxSlopeDegrees.toDouble(),
                        terrainElevationMeters = corridor.representativeElevationMeters,
                        historicalLandslideCount = corridor.historicalLandslideCount,
                        telemetryTimestampIso = latestTimestampIso,
                        telemetryAgeMinutes = ageMinutes
                    )
                )

                val mappedProb = response.estimatedLandslideProbability
                if (response.isValidatedModel && mappedProb != null) {
                    val mappedCategory = RiskLevel.safeFrom(response.riskCategory)
                    val mappedFactors = response.contributingFactors.map { dto ->
                        MlFactorContribution(
                            featureName = dto.featureName,
                            importanceWeight = dto.importanceWeight,
                            observedValueLabel = dto.observedValueLabel,
                            impactDirection = dto.impactDirection
                        )
                    }
                    val combinedMissing = (response.missingDataWarnings + missingWarnings).distinct()
                    val apiFreshness = when (response.dataFreshnessStatus.uppercase(Locale.US)) {
                        "FRESH" -> DataFreshnessStatus.FRESH
                        "AGING" -> DataFreshnessStatus.AGING
                        "STALE" -> DataFreshnessStatus.STALE
                        else -> freshnessStatus
                    }
                    return@withContext MlLandslidePredictionResult(
                        corridorId = corridor.id,
                        corridorName = corridor.name,
                        estimatedProbability = mappedProb.coerceIn(0.0, 1.0),
                        riskCategory = mappedCategory,
                        executionMode = PredictionExecutionMode.CONNECTED_PYTHON_RF_API,
                        isValidatedModel = true,
                        modelName = response.modelName.ifBlank { "Python Random Forest Classifier" },
                        modelVersion = response.modelVersion.ifBlank { "rf-v1.0.0" },
                        dataFreshnessStatus = apiFreshness,
                        freshnessWarning = response.freshnessWarning.ifBlank { freshnessWarning },
                        missingDataWarnings = combinedMissing,
                        contributingFactors = mappedFactors,
                        inputSnapshot = snapshot,
                        explanationNote = response.explanationNote
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                safeLog("Python ML backend unavailable; entering Demonstration Mode: ${e.message}")
            }
        }

        val offlineMissing = buildList {
            add("External Python Random Forest API not connected; displaying unvalidated Demonstration Mode output.")
            addAll(missingWarnings)
        }

        return@withContext runDeterministicDemonstrationEnsemble(
            corridor = corridor,
            snapshot = snapshot,
            freshnessStatus = freshnessStatus,
            freshnessWarning = freshnessWarning,
            missingWarnings = offlineMissing,
            explanationNote = "DEMONSTRATION MODE: Computed from real Open-Meteo telemetry using deterministic reference decision trees because the trained Python Random Forest API is offline."
        )
    }

    /**
     * Deterministic multi-tree ensemble used ONLY in clearly labelled Demonstration Mode when the
     * external Python scikit-learn Random Forest API is offline/unvalidated. Uses zero Random() calls.
     */
    internal fun runDeterministicDemonstrationEnsemble(
        corridor: MonitoredCorridor,
        snapshot: MlPredictionInputSnapshot,
        freshnessStatus: DataFreshnessStatus,
        freshnessWarning: String,
        missingWarnings: List<String>,
        explanationNote: String
    ): MlLandslidePredictionResult {
        val rainInt = snapshot.rainfallIntensityMmHr ?: 0.0
        val rainDur = snapshot.rainfallDurationHours ?: 0.0
        val soil = snapshot.soilMoistureM3M3
        val slope = snapshot.slopeAngleDeg.toDouble()
        val elev = snapshot.terrainElevationMeters.toDouble()
        val hist = snapshot.historicalLandslideCount.toDouble()

        // 5 deterministic decision-tree leaf probabilities mirroring Nilgiris RF splits
        val tree1 = when {
            rainInt >= 18.0 && rainDur >= 24.0 -> 0.88
            rainInt >= 8.0 && rainDur >= 16.0 -> 0.58
            rainDur >= 20.0 -> 0.42
            else -> 0.14
        }
        val tree2 = when {
            soil == null -> if (rainInt >= 12.0 && slope >= 26.0) 0.62 else 0.28
            soil >= 0.40 && slope >= 25.0 -> 0.86
            soil >= 0.30 && rainDur >= 14.0 -> 0.54
            else -> 0.16
        }
        val tree3 = when {
            slope >= 30.0 && rainInt >= 10.0 -> 0.84
            slope >= 24.0 && hist >= 6.0 && rainDur >= 12.0 -> 0.60
            slope >= 28.0 -> 0.36
            else -> 0.15
        }
        val tree4 = when {
            elev in 600.0..2100.0 && rainInt >= 15.0 -> 0.82
            elev in 900.0..2200.0 && rainDur >= 18.0 -> 0.56
            else -> 0.18
        }
        val tree5 = when {
            hist >= 8.0 && (rainInt >= 12.0 || (soil ?: 0.0) >= 0.38) -> 0.85
            hist >= 5.0 && rainDur >= 16.0 -> 0.52
            else -> 0.17
        }

        val probability = ((tree1 + tree2 + tree3 + tree4 + tree5) / 5.0).coerceIn(0.05, 0.95)
        val riskCategory = when {
            probability >= 0.65 -> RiskLevel.HIGH
            probability >= 0.35 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val contributions = listOf(
            MlFactorContribution(
                featureName = "Rainfall Intensity",
                importanceWeight = 0.27f,
                observedValueLabel = String.format(Locale.US, "%.1f mm/h", rainInt),
                impactDirection = if (rainInt >= 8.0) "Elevating Risk" else "Within Baseline"
            ),
            MlFactorContribution(
                featureName = "Rainfall Duration",
                importanceWeight = 0.22f,
                observedValueLabel = String.format(Locale.US, "%.0f hrs (72h window)", rainDur),
                impactDirection = if (rainDur >= 18.0) "Elevating Risk" else "Within Baseline"
            ),
            MlFactorContribution(
                featureName = "Soil Moisture (0–7cm)",
                importanceWeight = 0.19f,
                observedValueLabel = soil?.let { String.format(Locale.US, "%.3f m³/m³", it) }
                    ?: "Unavailable (Not fabricated)",
                impactDirection = when {
                    soil == null -> "Missing (Omitted)"
                    soil >= 0.32 -> "Elevating Risk"
                    else -> "Within Baseline"
                }
            ),
            MlFactorContribution(
                featureName = "Slope Angle",
                importanceWeight = 0.14f,
                observedValueLabel = "${snapshot.slopeAngleDeg}° max gradient",
                impactDirection = if (slope >= 24.0) "Elevating Risk" else "Moderate Terrain"
            ),
            MlFactorContribution(
                featureName = "Historical Landslide Records",
                importanceWeight = 0.11f,
                observedValueLabel = "${snapshot.historicalLandslideCount} demo events",
                impactDirection = if (hist >= 6.0) "Elevating Risk" else "Moderate History"
            ),
            MlFactorContribution(
                featureName = "Terrain Elevation",
                importanceWeight = 0.07f,
                observedValueLabel = "${snapshot.terrainElevationMeters} m",
                impactDirection = if (elev in 600.0..2100.0) "Escarpment Belt" else "Foothill / Plateau"
            )
        )

        return MlLandslidePredictionResult(
            corridorId = corridor.id,
            corridorName = corridor.name,
            estimatedProbability = probability,
            riskCategory = riskCategory,
            executionMode = PredictionExecutionMode.DEMONSTRATION_MODE,
            isValidatedModel = false,
            modelName = "Demonstration Tree Ensemble (Unvalidated)",
            modelVersion = "demo-rf-fallback",
            dataFreshnessStatus = freshnessStatus,
            freshnessWarning = freshnessWarning,
            missingDataWarnings = missingWarnings,
            contributingFactors = contributions,
            inputSnapshot = snapshot,
            explanationNote = explanationNote
        )
    }

    private fun safeLog(message: String) {
        try {
            Log.i(ML_REPO_TAG, message)
        } catch (_: RuntimeException) {
            // Android Log stub is unmocked in plain JVM unit tests.
        }
    }
}
