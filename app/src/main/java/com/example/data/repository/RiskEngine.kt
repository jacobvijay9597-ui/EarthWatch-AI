package com.example.data.repository

import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.MonitoredCorridor
import com.example.data.model.RiskLevel
import java.util.Locale
import kotlin.math.roundToInt

object RiskEngine {

    fun estimateCorridorRisk(
        corridor: MonitoredCorridor,
        weatherByLocationId: Map<String, LiveWeatherTelemetry>
    ): ExperimentalRiskEstimate? {
        val stationIds = if (corridor.id == "coimbatore_ooty") {
            listOf("coimbatore", "mettupalayam", "coonoor", "ooty")
        } else {
            listOf(corridor.fromLocationId, corridor.toLocationId)
        }
        val telemetry = stationIds.mapNotNull { weatherByLocationId[it] }
        if (telemetry.isEmpty()) {
            // Never invent live weather telemetry if live data has not loaded.
            return null
        }

        val maxRainCurrent = telemetry.maxOf { it.rainCurrentMm }
        val maxRain24h = telemetry.maxOf { it.rain24hMm }
        val maxRain72h = telemetry.maxOf { it.rain72hMm }
        val maxSoil: Double? = telemetry.mapNotNull { it.soilMoistureM3M3 }.maxOrNull()

        // 1. Rainfall intensity & duration score (0..35)
        val rainIntensityPart = (maxRainCurrent * 2.5).coerceAtMost(12.0)
        val rain24hPart = (maxRain24h * 0.35).coerceAtMost(13.0)
        val rain72hPart = (maxRain72h * 0.12).coerceAtMost(10.0)
        val rainfallScore = (rainIntensityPart + rain24hPart + rain72hPart).roundToInt().coerceIn(0, 35)

        // 2. Soil moisture score (0..25), omitted (0) when sensor reading is unavailable
        val soilScore = maxSoil?.let {
            (((it - 0.12) / 0.34) * 25.0).roundToInt().coerceIn(0, 25)
        } ?: 0

        // 3. Terrain & slope vulnerability (0..25)
        val terrainScore = corridor.baselineTerrainScore.coerceIn(5, 25)

        // 4. Historical landslide frequency score (0..15)
        val historyScore = (corridor.historicalLandslideCount * 1.5).roundToInt().coerceIn(2, 15)

        val totalScore = (rainfallScore + soilScore + terrainScore + historyScore).coerceIn(0, 100)
        val riskLevel = when {
            totalScore >= 62 -> RiskLevel.HIGH
            totalScore >= 38 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val factors = buildList {
            add(
                String.format(
                    Locale.US,
                    "Live Rainfall: %.1f mm/h current, %.1f mm (24h), %.1f mm (72h cumulative)",
                    maxRainCurrent, maxRain24h, maxRain72h
                )
            )
            add(
                if (maxSoil != null) {
                    String.format(
                        Locale.US,
                        "Surface Soil Moisture (0–7cm): %.3f m³/m³",
                        maxSoil
                    )
                } else {
                    "Surface Soil Moisture (0–7cm): Unavailable from Open-Meteo (not fabricated)"
                }
            )
            add(
                "Terrain & Slope: Max ${corridor.maxSlopeDegrees}° gradient across ${corridor.hairpinCount} hairpin bends (${corridor.soilAndGeology})"
            )
            add(
                "Historical Baseline (Simulated Demo Reference): ${corridor.historicalLandslideCount} documented slope events along ${corridor.highwayCode}"
            )
        }

        return ExperimentalRiskEstimate(
            corridorId = corridor.id,
            corridorName = corridor.name,
            riskScore = totalScore,
            riskLevel = riskLevel,
            rainfallIntensityScore = rainfallScore,
            soilMoistureScore = soilScore,
            terrainSlopeScore = terrainScore,
            historicalFactorScore = historyScore,
            contributingFactors = factors,
            telemetryBasedOnLiveWeather = true,
            experimentalDisclaimer = "EXPERIMENTAL ESTIMATE: Heuristic model combining live Open-Meteo rainfall/soil data with corridor terrain metadata. Not an official GSI/NDMA geological alert."
        )
    }

    fun estimateAllCorridors(
        weatherList: List<LiveWeatherTelemetry>
    ): List<ExperimentalRiskEstimate> {
        if (weatherList.isEmpty()) return emptyList()
        val map = weatherList.associateBy { it.locationId }
        return NilgirisReferenceCatalog.corridors.mapNotNull { corridor ->
            estimateCorridorRisk(corridor, map)
        }
    }
}
