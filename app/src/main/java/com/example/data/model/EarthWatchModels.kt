package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RiskLevel(val label: String, val sortOrder: Int) {
    LOW("LOW", 1),
    MEDIUM("MEDIUM", 2),
    HIGH("HIGH", 3);

    companion object {
        fun safeFrom(raw: String?): RiskLevel =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEDIUM
    }
}

enum class IncidentCategory(val code: String, val displayLabel: String) {
    LANDSLIDE("LANDSLIDE", "Landslide / Mudflow"),
    ROAD_BLOCKAGE("ROAD_BLOCKAGE", "Road Blockage"),
    ROCKFALL("ROCKFALL", "Rockfall / Boulders"),
    WATERLOGGING("WATERLOGGING", "Drainage / Waterlogging"),
    CRACK_SUBSIDENCE("CRACK_SUBSIDENCE", "Road Crack / Subsidence");

    companion object {
        fun safeFrom(raw: String?): IncidentCategory =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: ROAD_BLOCKAGE
    }
}

enum class VerificationStatus(val code: String, val badgeLabel: String) {
    UNVERIFIED("UNVERIFIED", "UNVERIFIED COMMUNITY REPORT"),
    CONFIRMED("CONFIRMED", "CONFIRMED INCIDENT"),
    RESOLVED("RESOLVED", "RESOLVED"),
    DISMISSED("DISMISSED", "DISMISSED");

    companion object {
        fun safeFrom(raw: String?): VerificationStatus =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: UNVERIFIED
    }
}

enum class CorridorOperationalStatus(val code: String, val displayLabel: String) {
    OPEN("OPEN", "Open (Monitored)"),
    CAUTION("CAUTION", "Caution Advised"),
    CLOSURE_REPORTED("CLOSURE_REPORTED", "Unverified Blockage Reported"),
    CLOSED_OFFICIAL("CLOSED_OFFICIAL", "Official Road Closure");

    companion object {
        fun safeFrom(raw: String?): CorridorOperationalStatus =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: CAUTION
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val subscribedCorridors: List<String> = emptyList(),
    val alertsEnabled: Boolean = true,
    val riskThreshold: String = "MEDIUM",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val reporterName: String = "",
    val corridorId: String = "",
    val corridorName: String = "",
    val locationLabel: String = "",
    val incidentType: String = IncidentCategory.LANDSLIDE.code,
    val severity: String = RiskLevel.MEDIUM.label,
    val description: String = "",
    val observedTimeText: String = "",
    val verificationStatus: String = VerificationStatus.UNVERIFIED.code,
    val visibility: String = "PUBLIC",
    val adminNotes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OfficialAdvisory(
    @DocumentId val id: String = "",
    val title: String = "",
    val corridorId: String = "",
    val corridorName: String = "",
    val severity: String = "MEDIUM",
    val sourceType: String = "OFFICIAL_ADVISORY",
    val sourceLabel: String = "",
    val summary: String = "",
    val roadClosed: Boolean = false,
    val published: Boolean = true,
    val simulatedDemo: Boolean = false,
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoadCorridorStatus(
    @DocumentId val id: String = "",
    val corridorName: String = "",
    val status: String = CorridorOperationalStatus.CAUTION.code,
    val statusNote: String = "",
    val verifiedSafety: Boolean = false,
    val active: Boolean = true,
    val simulatedDemo: Boolean = false,
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class NilgirisLocation(
    val id: String,
    val name: String,
    val talukOrArea: String,
    val elevationMeters: Int,
    val latitude: Double,
    val longitude: Double,
    val mapX: Float,
    val mapY: Float
)

data class GeoPointLatLng(
    val latitude: Double,
    val longitude: Double
)

data class MonitoredSpot(
    val id: String,
    val name: String,
    val corridorId: String,
    val elevationMeters: Int,
    val slopeDegrees: Int,
    val latitude: Double,
    val longitude: Double,
    val mapX: Float,
    val mapY: Float,
    val hazardNote: String
)

data class MonitoredCorridor(
    val id: String,
    val name: String,
    val highwayCode: String,
    val fromLocationId: String,
    val toLocationId: String,
    val distanceKm: Int,
    val averageDriveMinutes: Int,
    val maxSlopeDegrees: Int,
    val representativeElevationMeters: Int,
    val hairpinCount: Int,
    val soilAndGeology: String,
    val baselineTerrainScore: Int,
    val historicalLandslideCount: Int,
    val mapPolyline: List<Pair<Float, Float>>,
    val geoPolyline: List<GeoPointLatLng>,
    val alternateCorridorIds: List<String>,
    val monitoredSpots: List<MonitoredSpot>
)

data class HistoricalLandslideRecord(
    val id: String,
    val corridorId: String,
    val year: Int,
    val locationLabel: String,
    val triggerSummary: String,
    val severity: RiskLevel,
    val simulatedDemo: Boolean = true
)

data class LiveWeatherTelemetry(
    val locationId: String,
    val locationName: String,
    val temperatureC: Double,
    val rainCurrentMm: Double,
    val rain24hMm: Double,
    val rain72hMm: Double,
    val rainfallDurationHours72h: Int = 0,
    val soilMoistureM3M3: Double?,
    val windKmh: Double,
    val weatherLabel: String,
    val fetchedAtLabel: String,
    val telemetryTimestampIso: String = "",
    val fetchedAtEpochMillis: Long = System.currentTimeMillis(),
    val isLiveFromApi: Boolean,
    val sourceLabel: String
)

data class ExperimentalRiskEstimate(
    val corridorId: String,
    val corridorName: String,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val rainfallIntensityScore: Int,
    val soilMoistureScore: Int,
    val terrainSlopeScore: Int,
    val historicalFactorScore: Int,
    val contributingFactors: List<String>,
    val telemetryBasedOnLiveWeather: Boolean,
    val experimentalDisclaimer: String
)

enum class PredictionExecutionMode(val badgeLabel: String) {
    CONNECTED_PYTHON_RF_API("CONNECTED PYTHON RANDOM FOREST"),
    DEMONSTRATION_MODE("DEMONSTRATION MODE")
}

enum class DataFreshnessStatus(val label: String) {
    FRESH("FRESH LIVE DATA"),
    AGING("AGING DATA"),
    STALE("STALE TELEMETRY"),
    MISSING("DATA MISSING / DEMO")
}

data class MlBackendHealthStatus(
    val isConnected: Boolean,
    val isValidatedModel: Boolean,
    val endpointUrl: String,
    val modelName: String,
    val modelVersion: String,
    val datasetRows: Int,
    val cvAccuracyMean: Double,
    val statusDetail: String
)

data class MlFactorContribution(
    val featureName: String,
    val importanceWeight: Float,
    val observedValueLabel: String,
    val impactDirection: String
)

data class MlPredictionInputSnapshot(
    val rainfallIntensityMmHr: Double?,
    val rainfallDurationHours: Double?,
    val soilMoistureM3M3: Double?,
    val slopeAngleDeg: Int,
    val terrainElevationMeters: Int,
    val historicalLandslideCount: Int,
    val telemetryTimestampIso: String?,
    val telemetryAgeMinutes: Int?
)

data class DemoScenarioPreset(
    val id: String,
    val title: String,
    val description: String,
    val rainfallIntensityMmHr: Double,
    val rainfallDurationHours: Double,
    val soilMoistureM3M3: Double?
)

data class MlLandslidePredictionResult(
    val corridorId: String,
    val corridorName: String,
    val estimatedProbability: Double?,
    val riskCategory: RiskLevel?,
    val executionMode: PredictionExecutionMode,
    val isValidatedModel: Boolean,
    val modelName: String,
    val modelVersion: String,
    val dataFreshnessStatus: DataFreshnessStatus,
    val freshnessWarning: String,
    val missingDataWarnings: List<String>,
    val contributingFactors: List<MlFactorContribution>,
    val inputSnapshot: MlPredictionInputSnapshot,
    val explanationNote: String
)

data class AiRiskBriefing(
    val corridorId: String,
    val corridorName: String,
    val estimatedRiskCategory: String,
    val summary: String,
    val keyFactors: List<String>,
    val travellerAdvice: String,
    val experimentalDisclaimer: String
)

data class RouteEvaluation(
    val primaryCorridor: MonitoredCorridor,
    val alternateCorridors: List<MonitoredCorridor>,
    val riskEstimate: ExperimentalRiskEstimate?,
    val weatherAtStops: List<LiveWeatherTelemetry>,
    val relatedReports: List<CommunityReport>,
    val relatedAdvisories: List<OfficialAdvisory>,
    val corridorStatus: RoadCorridorStatus?,
    val safetyConfirmed: Boolean,
    val safetyNotice: String
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

enum class LocationPermissionState(val label: String) {
    UNKNOWN("Location Not Requested"),
    DENIED("Permission Denied"),
    DENIED_PERMANENTLY("Permission Blocked in Settings"),
    APPROXIMATE_ONLY("Approximate Location Only"),
    FINE_GRANTED("Precise GPS Active")
}

data class UserLocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedKmh: Float?,
    val bearingDegrees: Float?,
    val timestampMillis: Long,
    val isFinePrecision: Boolean,
    val nearestCorridorId: String,
    val nearestCorridorName: String,
    val distanceToNearestCorridorKm: Double,
    val nearestHazardSpotName: String?,
    val distanceToNearestSpotKm: Double?
)
