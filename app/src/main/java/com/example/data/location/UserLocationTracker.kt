package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.model.LocationPermissionState
import com.example.data.model.MonitoredCorridor
import com.example.data.model.UserLocationSnapshot
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object UserLocationTracker {

    val REQUIRED_LOCATION_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun evaluateLocationPermission(
        context: Context,
        hasRequestedBefore: Boolean
    ): LocationPermissionState {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted) return LocationPermissionState.FINE_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (coarseGranted) return LocationPermissionState.APPROXIMATE_ONLY

        if (!hasRequestedBefore) return LocationPermissionState.UNKNOWN

        val activity = findActivity(context)
        val shouldShowRationale = activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        return if (shouldShowRationale) {
            LocationPermissionState.DENIED
        } else {
            LocationPermissionState.DENIED_PERMANENTLY
        }
    }

    fun evaluateFromGrantResults(
        context: Context,
        grantResults: Map<String, Boolean>
    ): LocationPermissionState {
        val fineGranted = grantResults[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted) return LocationPermissionState.FINE_GRANTED

        val coarseGranted = grantResults[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (coarseGranted) return LocationPermissionState.APPROXIMATE_ONLY

        val activity = findActivity(context)
        val shouldShowRationale = activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        return if (shouldShowRationale) {
            LocationPermissionState.DENIED
        } else {
            LocationPermissionState.DENIED_PERMANENTLY
        }
    }

    fun buildAppSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    @SuppressLint("MissingPermission")
    fun observeRealTimeLocation(
        context: Context,
        finePrecision: Boolean
    ): Flow<UserLocationSnapshot> = callbackFlow {
        val appContext = context.applicationContext
        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            close()
            return@callbackFlow
        }

        val effectiveFine = hasFine && finePrecision
        val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)

        fun emitLocation(loc: Location) {
            val snapshot = buildUserLocationSnapshot(
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracyMeters = loc.accuracy,
                speedMetersPerSec = if (loc.hasSpeed()) loc.speed else null,
                bearingDegrees = if (loc.hasBearing()) loc.bearing else null,
                timestampMillis = loc.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                isFinePrecision = effectiveFine
            )
            trySend(snapshot)
        }

        try {
            fusedClient.lastLocation
                .addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        emitLocation(lastLoc)
                    }
                }
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(
            if (effectiveFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            4000L
        )
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val latest = result.lastLocation ?: return
                emitLocation(latest)
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (se: SecurityException) {
            close(se)
            return@callbackFlow
        }

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    fun buildUserLocationSnapshot(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        speedMetersPerSec: Float?,
        bearingDegrees: Float?,
        timestampMillis: Long,
        isFinePrecision: Boolean,
        corridors: List<MonitoredCorridor> = NilgirisReferenceCatalog.corridors
    ): UserLocationSnapshot {
        var nearestCorridor = corridors.first()
        var minCorridorDistKm = Double.MAX_VALUE
        var nearestSpotName: String? = null
        var minSpotDistKm = Double.MAX_VALUE

        for (corridor in corridors) {
            for (pt in corridor.geoPolyline) {
                val d = haversineDistanceKm(latitude, longitude, pt.latitude, pt.longitude)
                if (d < minCorridorDistKm) {
                    minCorridorDistKm = d
                    nearestCorridor = corridor
                }
            }
            for (spot in corridor.monitoredSpots) {
                val d = haversineDistanceKm(latitude, longitude, spot.latitude, spot.longitude)
                if (d < minSpotDistKm) {
                    minSpotDistKm = d
                    nearestSpotName = spot.name
                }
            }
        }

        val safeCorridorDist = if (minCorridorDistKm == Double.MAX_VALUE) 0.0 else minCorridorDistKm
        val safeSpotDist = if (minSpotDistKm == Double.MAX_VALUE) null else minSpotDistKm
        val speedKmh = speedMetersPerSec?.let { (it * 3.6f).coerceAtLeast(0f) }

        return UserLocationSnapshot(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters.coerceAtLeast(0f),
            speedKmh = speedKmh,
            bearingDegrees = bearingDegrees,
            timestampMillis = timestampMillis,
            isFinePrecision = isFinePrecision,
            nearestCorridorId = nearestCorridor.id,
            nearestCorridorName = nearestCorridor.name,
            distanceToNearestCorridorKm = safeCorridorDist,
            nearestHazardSpotName = nearestSpotName,
            distanceToNearestSpotKm = safeSpotDist
        )
    }

    fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return earthRadiusKm * c
    }

    /**
     * Projects GPS coordinates onto the normalized Nilgiris topographic fallback canvas ([0, 1] x [0, 1]).
     * Returns null when the user is outside the Nilgiris sector bounding box so out-of-region coordinates
     * are reported cleanly instead of clamped misleadingly onto a hill corridor.
     */
    fun projectLatLngToNilgirisSector(latitude: Double, longitude: Double): Pair<Float, Float>? {
        if (latitude !in 10.90..11.75 || longitude !in 76.35..77.08) {
            return null
        }
        val normX = ((longitude - 76.405) / 0.610).toFloat().coerceIn(0.03f, 0.97f)
        val normY = ((11.690 - latitude) / 0.720).toFloat().coerceIn(0.03f, 0.97f)
        return normX to normY
    }

    private tailrec fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }
}
