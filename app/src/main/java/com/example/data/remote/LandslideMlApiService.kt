package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class LandslideMlHealthResponse(
    val status: String = "",
    @Json(name = "model_loaded") val modelLoaded: Boolean = false,
    @Json(name = "is_validated_model") val isValidatedModel: Boolean = false,
    @Json(name = "model_name") val modelName: String = "",
    @Json(name = "model_version") val modelVersion: String = "",
    @Json(name = "dataset_rows") val datasetRows: Int = 0,
    @Json(name = "cv_accuracy_mean") val cvAccuracyMean: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class LandslideMlPredictionRequest(
    @Json(name = "corridor_id") val corridorId: String,
    @Json(name = "corridor_name") val corridorName: String,
    @Json(name = "rainfall_intensity_mm_hr") val rainfallIntensityMmHr: Double?,
    @Json(name = "rainfall_duration_hours") val rainfallDurationHours: Double?,
    @Json(name = "rainfall_24h_mm") val rainfall24hMm: Double?,
    @Json(name = "rainfall_72h_mm") val rainfall72hMm: Double?,
    @Json(name = "soil_moisture_m3_m3") val soilMoistureM3M3: Double?,
    @Json(name = "slope_angle_deg") val slopeAngleDeg: Double?,
    @Json(name = "terrain_elevation_meters") val terrainElevationMeters: Int?,
    @Json(name = "historical_landslide_count") val historicalLandslideCount: Int?,
    @Json(name = "telemetry_timestamp_iso") val telemetryTimestampIso: String?,
    @Json(name = "telemetry_age_minutes") val telemetryAgeMinutes: Int?
)

@JsonClass(generateAdapter = true)
data class MlFeatureContributionDto(
    @Json(name = "feature_name") val featureName: String = "",
    @Json(name = "importance_weight") val importanceWeight: Float = 0f,
    @Json(name = "observed_value_label") val observedValueLabel: String = "",
    @Json(name = "impact_direction") val impactDirection: String = ""
)

@JsonClass(generateAdapter = true)
data class LandslideMlPredictionResponse(
    @Json(name = "corridor_id") val corridorId: String = "",
    @Json(name = "corridor_name") val corridorName: String = "",
    @Json(name = "estimated_landslide_probability") val estimatedLandslideProbability: Double? = null,
    @Json(name = "risk_category") val riskCategory: String = "UNCONFIRMED",
    @Json(name = "contributing_factors") val contributingFactors: List<MlFeatureContributionDto> = emptyList(),
    @Json(name = "model_name") val modelName: String = "",
    @Json(name = "model_version") val modelVersion: String = "",
    @Json(name = "is_validated_model") val isValidatedModel: Boolean = false,
    @Json(name = "execution_mode") val executionMode: String = "",
    @Json(name = "data_freshness_status") val dataFreshnessStatus: String = "MISSING",
    @Json(name = "freshness_warning") val freshnessWarning: String = "",
    @Json(name = "missing_data_warnings") val missingDataWarnings: List<String> = emptyList(),
    @Json(name = "explanation_note") val explanationNote: String = ""
)

interface LandslideMlApi {

    @GET("health")
    suspend fun getHealth(): LandslideMlHealthResponse

    @POST("api/v1/predict/landslide-risk")
    suspend fun predictLandslideRisk(
        @Body request: LandslideMlPredictionRequest
    ): LandslideMlPredictionResponse

    companion object {
        fun normalizeBaseUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return ""
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }

        fun create(rawBaseUrl: String): LandslideMlApi {
            val normalized = normalizeBaseUrl(rawBaseUrl)
            require(normalized.startsWith("http://") || normalized.startsWith("https://")) {
                "ML backend URL must start with http:// or https://"
            }
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .writeTimeout(4, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(LandslideMlApi::class.java)
        }
    }
}
