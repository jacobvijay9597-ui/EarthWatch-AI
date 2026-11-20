package com.example.data.repository

import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.NilgirisLocation
import com.example.data.remote.OpenMeteoWeatherApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRepository(
    apiProvider: () -> OpenMeteoWeatherApi = { OpenMeteoWeatherApi.create() }
) {
    private val api: OpenMeteoWeatherApi by lazy(apiProvider)

    suspend fun fetchAllNilgirisWeather(): Result<List<LiveWeatherTelemetry>> =
        withContext(Dispatchers.IO) {
            try {
                val results = coroutineScope {
                    NilgirisReferenceCatalog.locations.map { loc ->
                        async { fetchLocationWeather(loc) }
                    }.awaitAll()
                }
                Result.success(results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun fetchLocationWeather(location: NilgirisLocation): LiveWeatherTelemetry {
        val response = api.getForecast(
            latitude = location.latitude,
            longitude = location.longitude
        )
        val current = response.current
        val hourly = response.hourly
        val precipList = hourly?.precipitation.orEmpty().map { it ?: 0.0 }
        val timeList = hourly?.time.orEmpty()
        val soilList = hourly?.soilMoisture0To7Cm.orEmpty()

        val currentPrefix = current?.time?.take(13)
        val currentIdx = if (!currentPrefix.isNullOrBlank()) {
            timeList.indexOfFirst { it.startsWith(currentPrefix) }.takeIf { it >= 0 }
                ?: (precipList.size - 1).coerceAtLeast(0)
        } else {
            (precipList.size - 1).coerceAtLeast(0)
        }

        val start72 = (currentIdx - 71).coerceAtLeast(0)
        val start24 = (currentIdx - 23).coerceAtLeast(0)
        val window72 = if (precipList.isNotEmpty()) {
            precipList.subList(start72, (currentIdx + 1).coerceAtMost(precipList.size))
        } else emptyList()
        val rain72h = window72.sum()
        val rainfallDurationHours72h = window72.count { it >= 0.2 }

        val rain24h = if (precipList.isNotEmpty()) {
            precipList.subList(start24, (currentIdx + 1).coerceAtMost(precipList.size)).sum()
        } else 0.0

        // Preserve null if Open-Meteo has no soil moisture reading rather than faking 0.0
        val soilMoisture: Double? = soilList.getOrNull(currentIdx)
            ?: soilList.subList(start24.coerceAtMost(soilList.size), (currentIdx + 1).coerceAtMost(soilList.size))
                .filterNotNull()
                .lastOrNull()

        val nowEpoch = System.currentTimeMillis()
        val stamp = SimpleDateFormat("dd MMM HH:mm", Locale.US).format(Date(nowEpoch))
        return LiveWeatherTelemetry(
            locationId = location.id,
            locationName = location.name,
            temperatureC = current?.temperature2m ?: 0.0,
            rainCurrentMm = current?.rain ?: 0.0,
            rain24hMm = rain24h,
            rain72hMm = rain72h,
            rainfallDurationHours72h = rainfallDurationHours72h,
            soilMoistureM3M3 = soilMoisture,
            windKmh = current?.windSpeed10m ?: 0.0,
            weatherLabel = decodeWeatherCode(current?.weatherCode ?: 0),
            fetchedAtLabel = "$stamp IST",
            telemetryTimestampIso = current?.time.orEmpty(),
            fetchedAtEpochMillis = nowEpoch,
            isLiveFromApi = true,
            sourceLabel = "Open-Meteo Live Forecast & ERA5 Soil Telemetry"
        )
    }

    private fun decodeWeatherCode(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Hill Mist / Fog"
        51, 53, 55 -> "Drizzle"
        61, 63 -> "Moderate Rain"
        65, 81, 82 -> "Heavy Rain"
        80 -> "Rain Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Mountain Cloud Cover"
    }
}
