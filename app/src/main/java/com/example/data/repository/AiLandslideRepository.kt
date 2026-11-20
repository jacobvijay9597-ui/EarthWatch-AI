package com.example.data.repository

import android.util.Log
import com.example.data.model.AiRiskBriefing
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.MonitoredCorridor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "AiLandslideRepo"

class AiLandslideRepository {

    private val structuredSchema = Schema.obj(
        properties = mapOf(
            "estimatedRiskCategory" to Schema.enumeration(
                values = listOf("LOW", "MEDIUM", "HIGH"),
                description = "Experimental landslide risk tier based on supplied telemetry"
            ),
            "summary" to Schema.string(
                description = "Concise 2-sentence explanation of the experimental risk assessment"
            ),
            "keyFactors" to Schema.array(
                items = Schema.string(),
                description = "3 to 4 bullet factors analyzing rainfall, soil moisture, slope, and history"
            ),
            "travellerAdvice" to Schema.string(
                description = "Practical mountain travel safety guidance without claiming unverified roads are safe"
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = structuredSchema
            }
        )
    }

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are EarthWatch AI Traveller Safety Assistant for the Nilgiris hill district (Ooty, Coonoor, Mettupalayam, Kotagiri, Kalhatti, Gudalur) in Tamil Nadu.
                    STRICT RULES:
                    1. Never invent live weather measurements or fabricate road closures. Only cite telemetry explicitly supplied in the prompt.
                    2. Always clarify that AI risk assessments are EXPERIMENTAL ESTIMATES and not official government/GSI alerts.
                    3. Never recommend an unverified road as definitively safe; advise checking official District Collectorate/Police advisories.
                    4. Keep answers concise, structured, and helpful for travellers.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generateExperimentalBriefing(
        corridor: MonitoredCorridor,
        estimate: ExperimentalRiskEstimate,
        weatherStops: List<LiveWeatherTelemetry>
    ): Result<AiRiskBriefing> {
        val weatherSummary = weatherStops.joinToString("\n") { w ->
            val soilStr = w.soilMoistureM3M3?.let { String.format(java.util.Locale.US, "%.3f m3/m3", it) } ?: "unavailable"
            "- ${w.locationName}: temp=${w.temperatureC}°C, currentRain=${w.rainCurrentMm}mm/h, rain24h=${w.rain24hMm}mm, rain72h=${w.rain72hMm}mm, rainDuration72h=${w.rainfallDurationHours72h}h, soilMoisture=$soilStr (${w.weatherLabel})"
        }
        val prompt = """
            Analyze landslide risk for the Nilgiris road corridor below using ONLY the observed telemetry provided.
            Corridor: ${corridor.name} (${corridor.highwayCode})
            Distance: ${corridor.distanceKm} km | Hairpins: ${corridor.hairpinCount} | Max Slope: ${corridor.maxSlopeDegrees}°
            Geology: ${corridor.soilAndGeology}
            Simulated Historical Records Count: ${corridor.historicalLandslideCount}
            Heuristic Experimental Score: ${estimate.riskScore}/100 (${estimate.riskLevel.label})
            Observed Weather Telemetry (Open-Meteo):
            $weatherSummary

            Provide a grounded experimental factor breakdown and cautious traveller advice. Never claim unverified roads are guaranteed safe.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
            val json = JSONObject(rawJson)
            val category = json.optString("estimatedRiskCategory", estimate.riskLevel.label)
            val summary = json.optString("summary", "Experimental assessment completed.")
            val advice = json.optString("travellerAdvice", "Verify official Nilgiris road advisories before departure.")
            val factorsArray = json.optJSONArray("keyFactors")
            val factors = if (factorsArray != null) {
                (0 until factorsArray.length()).map { factorsArray.getString(it) }
            } else {
                estimate.contributingFactors
            }

            Result.success(
                AiRiskBriefing(
                    corridorId = corridor.id,
                    corridorName = corridor.name,
                    estimatedRiskCategory = category,
                    summary = summary,
                    keyFactors = factors,
                    travellerAdvice = advice,
                    experimentalDisclaimer = "AI EXPERIMENTAL ESTIMATE (Gemini 3.8 Flash) — For situational awareness only; verify official road advisories."
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Structured AI briefing timed out", e)
            Result.failure(Exception("AI analysis timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Structured AI briefing failed", e)
            Result.failure(e)
        }
    }

    fun streamTravellerAssistant(
        userQuestion: String,
        telemetryContext: String
    ): Flow<String> = flow {
        val fullPrompt = """
            Current Live Nilgiris Telemetry Context:
            $telemetryContext

            Traveller Question: $userQuestion
        """.trimIndent()

        try {
            withTimeout(60_000L) {
                val builder = StringBuilder()
                assistantModel.generateContentStream(fullPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        builder.append(piece)
                        emit(builder.toString())
                    }
                }
                if (builder.isEmpty()) {
                    throw IllegalStateException("AI assistant returned an empty response.")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Streaming AI assistant timed out", e)
            throw Exception("AI assistant timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Streaming AI assistant failed", e)
            throw e
        }
    }
}
