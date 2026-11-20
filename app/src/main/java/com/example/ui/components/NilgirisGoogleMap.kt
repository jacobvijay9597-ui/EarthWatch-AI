package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.location.UserLocationTracker
import com.example.data.model.CommunityReport
import com.example.data.model.CorridorOperationalStatus
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.LocationPermissionState
import com.example.data.model.MonitoredCorridor
import com.example.data.model.OfficialAdvisory
import com.example.data.model.RiskLevel
import com.example.data.model.RoadCorridorStatus
import com.example.data.model.UserLocationSnapshot
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RiskHighRed
import com.example.ui.theme.RiskLowContainer
import com.example.ui.theme.RiskLowGreen
import com.example.ui.theme.RiskMediumAmber
import com.example.ui.theme.RiskMediumContainer
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.UnverifiedViolet
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch
import java.util.Locale

private const val DARK_NAVY_MAP_STYLE_JSON = """
[
  {"elementType": "geometry", "stylers": [{"color": "#0b192c"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#8aa4c2"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#071321"}]},
  {"featureType": "administrative", "elementType": "geometry.stroke", "stylers": [{"color": "#233a59"}]},
  {"featureType": "landscape.natural", "elementType": "geometry", "stylers": [{"color": "#0e2138"}]},
  {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#10283d"}]},
  {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#1f4263"}]},
  {"featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{"color": "#132942"}]},
  {"featureType": "road.arterial", "elementType": "geometry", "stylers": [{"color": "#16314c"}]},
  {"featureType": "road.local", "elementType": "geometry", "stylers": [{"color": "#12263d"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#06101c"}]},
  {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#4f7296"}]}
]
"""

private sealed class MapSelectionInfo {
    data class SpotInfo(
        val spotName: String,
        val corridorName: String,
        val elevationMeters: Int,
        val slopeDegrees: Int,
        val riskLevel: RiskLevel?,
        val hazardNote: String
    ) : MapSelectionInfo()

    data class RainfallInfo(
        val locationName: String,
        val telemetry: LiveWeatherTelemetry?
    ) : MapSelectionInfo()

    data class IncidentInfo(
        val report: CommunityReport
    ) : MapSelectionInfo()

    data class ClosureInfo(
        val corridorName: String,
        val title: String,
        val details: String
    ) : MapSelectionInfo()
}

fun isPlausibleGoogleMapsKey(rawKey: String): Boolean {
    val key = rawKey.trim()
    return key.startsWith("AIza") && key.length >= 30
}

fun hasConfiguredMapsApiKey(): Boolean = isPlausibleGoogleMapsKey(BuildConfig.MAPS_API_KEY)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NilgirisGoogleMapSection(
    corridors: List<MonitoredCorridor>,
    estimatesByCorridor: Map<String, ExperimentalRiskEstimate>,
    statusesByCorridor: Map<String, RoadCorridorStatus>,
    weatherList: List<LiveWeatherTelemetry>,
    reports: List<CommunityReport>,
    advisories: List<OfficialAdvisory>,
    highlightedCorridorId: String?,
    showPredictedRisk: Boolean = true,
    showVerifiedIncidents: Boolean = true,
    showOfficialClosures: Boolean = true,
    showRainfallStations: Boolean = true,
    showUnverifiedReports: Boolean = true,
    userLocation: UserLocationSnapshot? = null,
    locationPermissionState: LocationPermissionState = LocationPermissionState.UNKNOWN,
    isLiveTrackingEnabled: Boolean = true,
    onRequestLocationPermission: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    onToggleLiveTracking: () -> Unit = {},
    onSelectCorridor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apiKeyConfigured = remember { hasConfiguredMapsApiKey() }
    var mapsSdkReady by remember { mutableStateOf(false) }
    var mapsSdkNotice by remember {
        mutableStateOf(
            if (apiKeyConfigured) null
            else "Add a valid Google Maps key (MAPS_API_KEY) in AI Studio Secrets for live map tiles. Showing interactive Nilgiris topo map."
        )
    }

    LaunchedEffect(apiKeyConfigured) {
        if (!apiKeyConfigured) {
            mapsSdkReady = false
            return@LaunchedEffect
        }
        try {
            val status = MapsInitializer.initialize(context.applicationContext)
            if (status == ConnectionResult.SUCCESS) {
                mapsSdkReady = true
                mapsSdkNotice = null
            } else {
                mapsSdkReady = false
                mapsSdkNotice = "Google Play Services Maps unavailable (status $status). Showing interactive topo fallback."
            }
        } catch (t: Throwable) {
            mapsSdkReady = false
            mapsSdkNotice = "Google Maps SDK could not initialize (${t.localizedMessage ?: "missing/invalid API key"}). Showing interactive topo fallback."
        }
    }

    var selectedInfo by remember { mutableStateOf<MapSelectionInfo?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        mapsSdkNotice?.let { notice ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NavyElevated,
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Google Maps API key status",
                        tint = TealBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.labelSmall,
                        color = OffWhite
                    )
                }
            }
        }

        LivePositionSituationalBar(
            permissionState = locationPermissionState,
            userLocation = userLocation,
            isLiveTrackingEnabled = isLiveTrackingEnabled,
            isFallbackCanvasActive = !mapsSdkReady,
            onRequestLocationPermission = onRequestLocationPermission,
            onOpenLocationSettings = onOpenLocationSettings,
            onToggleLiveTracking = onToggleLiveTracking
        )

        if (mapsSdkReady) {
            NilgirisGoogleMapCanvas(
                corridors = corridors,
                estimatesByCorridor = estimatesByCorridor,
                statusesByCorridor = statusesByCorridor,
                weatherList = weatherList,
                reports = reports,
                advisories = advisories,
                highlightedCorridorId = highlightedCorridorId,
                showPredictedRisk = showPredictedRisk,
                showVerifiedIncidents = showVerifiedIncidents,
                showOfficialClosures = showOfficialClosures,
                showRainfallStations = showRainfallStations,
                showUnverifiedReports = showUnverifiedReports,
                userLocation = userLocation,
                locationPermissionState = locationPermissionState,
                isLiveTrackingEnabled = isLiveTrackingEnabled,
                onRequestLocationPermission = onRequestLocationPermission,
                onSelectCorridor = onSelectCorridor,
                onSelectInfo = { selectedInfo = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .testTag("google_map_container")
            )
        } else {
            InteractiveNilgirisMapCanvas(
                corridors = corridors,
                estimatesByCorridor = estimatesByCorridor,
                statusesByCorridor = statusesByCorridor,
                reports = reports.filter {
                    val isConfirmed = it.verificationStatus == "CONFIRMED"
                    (isConfirmed && showVerifiedIncidents) || (!isConfirmed && showUnverifiedReports)
                },
                advisories = if (showOfficialClosures) advisories else emptyList(),
                weatherList = if (showRainfallStations) weatherList else emptyList(),
                showPredictedRisk = showPredictedRisk,
                showOfficialClosures = showOfficialClosures,
                userLocation = if (isLiveTrackingEnabled) userLocation else null,
                highlightedCorridorId = highlightedCorridorId,
                onSelectCorridor = onSelectCorridor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .testTag("fallback_topo_map_container")
            )
        }

        // Selected Marker / Telemetry Detail Banner
        selectedInfo?.let { info ->
            SelectedMarkerTelemetryCard(
                info = info,
                onDismiss = { selectedInfo = null }
            )
        }

        // Distinct Indicator Legend (Experimental vs Verified vs Official Closure vs Rainfall vs Unverified)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NavyCard,
            border = BorderStroke(1.dp, NavyBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "VISUAL INDICATOR KEY (SEPARATED BY SOURCE & VERIFICATION)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealBright,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MapKeyBadge(
                        color = RiskLowGreen,
                        shapeLabel = "Solid Line / Spot",
                        description = "Predicted Risk (Experimental: Low / Med / High)"
                    )
                    MapKeyBadge(
                        color = RiskHighRed,
                        shapeLabel = "Shield Badge",
                        description = "Verified Incident (Confirmed)"
                    )
                    MapKeyBadge(
                        color = RiskHighRed,
                        shapeLabel = "Dashed Line + Stop",
                        description = "Official Road Closure"
                    )
                    MapKeyBadge(
                        color = UnverifiedViolet,
                        shapeLabel = "Violet Square",
                        description = "Unverified Community Report"
                    )
                    MapKeyBadge(
                        color = InfoCyan,
                        shapeLabel = "Drop + Ring",
                        description = "Live Open-Meteo Rainfall"
                    )
                    MapKeyBadge(
                        color = TealBright,
                        shapeLabel = "Cyan Halo Pin",
                        description = "Your Live GPS Position"
                    )
                }
            }
        }
    }
}

@Composable
private fun NilgirisGoogleMapCanvas(
    corridors: List<MonitoredCorridor>,
    estimatesByCorridor: Map<String, ExperimentalRiskEstimate>,
    statusesByCorridor: Map<String, RoadCorridorStatus>,
    weatherList: List<LiveWeatherTelemetry>,
    reports: List<CommunityReport>,
    advisories: List<OfficialAdvisory>,
    highlightedCorridorId: String?,
    showPredictedRisk: Boolean,
    showVerifiedIncidents: Boolean,
    showOfficialClosures: Boolean,
    showRainfallStations: Boolean,
    showUnverifiedReports: Boolean,
    userLocation: UserLocationSnapshot?,
    locationPermissionState: LocationPermissionState,
    isLiveTrackingEnabled: Boolean,
    onRequestLocationPermission: () -> Unit,
    onSelectCorridor: (String) -> Unit,
    onSelectInfo: (MapSelectionInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val weatherMap = remember(weatherList) { weatherList.associateBy { it.locationId } }

    // Default camera focuses on the Coimbatore -> Mettupalayam -> Coonoor -> Ooty Nilgiris corridor
    val defaultTarget = remember { LatLng(11.24, 76.82) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultTarget, 9.8f)
    }

    val hasAnyLocationPermission = locationPermissionState == LocationPermissionState.FINE_GRANTED ||
        locationPermissionState == LocationPermissionState.APPROXIMATE_ONLY
    val myLocationLayerActive = hasAnyLocationPermission && isLiveTrackingEnabled

    val mapProperties = remember(myLocationLayerActive) {
        MapProperties(
            mapType = MapType.NORMAL,
            mapStyleOptions = MapStyleOptions(DARK_NAVY_MAP_STYLE_JSON),
            isMyLocationEnabled = myLocationLayerActive
        )
    }
    val uiSettings = remember {
        MapUiSettings(
            compassEnabled = true,
            zoomControlsEnabled = true,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false
        )
    }

    LaunchedEffect(highlightedCorridorId, corridors) {
        val targetCorridor = corridors.firstOrNull { it.id == highlightedCorridorId } ?: return@LaunchedEffect
        if (targetCorridor.geoPolyline.size >= 2) {
            val boundsBuilder = LatLngBounds.builder()
            targetCorridor.geoPolyline.forEach { pt ->
                boundsBuilder.include(LatLng(pt.latitude, pt.longitude))
            }
            runCatching {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 90),
                    durationMs = 550
                )
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(NavyDeepest)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            // 1. Corridor Polylines: distinguish Official Closure (dashed red) vs Predicted Risk (experimental color)
            corridors.forEach { corridor ->
                val points = remember(corridor.id) {
                    corridor.geoPolyline.map { LatLng(it.latitude, it.longitude) }
                }
                if (points.size < 2) return@forEach

                val isSelected = corridor.id == highlightedCorridorId
                val estimate = estimatesByCorridor[corridor.id]
                val status = statusesByCorridor[corridor.id]
                val closedAdvisory = advisories.firstOrNull { it.corridorId == corridor.id && it.roadClosed }
                val isClosedOfficial = status?.status == CorridorOperationalStatus.CLOSED_OFFICIAL.code ||
                    closedAdvisory != null

                if (isClosedOfficial && showOfficialClosures) {
                    if (isSelected) {
                        Polyline(
                            points = points,
                            color = PureWhite.copy(alpha = 0.45f),
                            width = 18f,
                            zIndex = 2f
                        )
                    }
                    Polyline(
                        points = points,
                        color = RiskHighRed,
                        width = if (isSelected) 12f else 8f,
                        pattern = listOf(Dash(28f), Gap(16f)),
                        clickable = true,
                        zIndex = 3f,
                        onClick = {
                            onSelectCorridor(corridor.id)
                            onSelectInfo(
                                MapSelectionInfo.ClosureInfo(
                                    corridorName = corridor.name,
                                    title = closedAdvisory?.title ?: "Official Corridor Closure",
                                    details = closedAdvisory?.summary
                                        ?: status?.statusNote
                                        ?: "Official road closure reported for ${corridor.name}."
                                )
                            )
                        }
                    )

                    val midPt = points[points.size / 2]
                    val closureMarkerState = rememberUpdatedMarkerState(position = midPt)
                    MarkerComposable(
                        state = closureMarkerState,
                        title = "Official Road Closure",
                        snippet = corridor.name,
                        zIndex = 5f,
                        onClick = {
                            onSelectCorridor(corridor.id)
                            onSelectInfo(
                                MapSelectionInfo.ClosureInfo(
                                    corridorName = corridor.name,
                                    title = closedAdvisory?.title ?: "Official Corridor Closure",
                                    details = closedAdvisory?.summary
                                        ?: status?.statusNote
                                        ?: "Officially confirmed road closure."
                                )
                            )
                            true
                        }
                    ) {
                        ClosureMarkerPin()
                    }
                } else {
                    val lineColor = when {
                        !showPredictedRisk -> SlateMuted.copy(alpha = 0.7f)
                        estimate?.riskLevel == RiskLevel.HIGH -> RiskHighRed
                        estimate?.riskLevel == RiskLevel.MEDIUM -> RiskMediumAmber
                        estimate?.riskLevel == RiskLevel.LOW -> RiskLowGreen
                        else -> SlateMuted
                    }
                    if (isSelected) {
                        Polyline(
                            points = points,
                            color = TealBright.copy(alpha = 0.35f),
                            width = 18f,
                            zIndex = 1f
                        )
                    }
                    Polyline(
                        points = points,
                        color = lineColor,
                        width = if (isSelected) 11f else 7f,
                        clickable = true,
                        zIndex = if (isSelected) 2.5f else 1.5f,
                        onClick = {
                            onSelectCorridor(corridor.id)
                        }
                    )
                }

                // 2. Monitored Landslide-Risk Spot Markers (Experimental Prediction Indicator)
                if (showPredictedRisk) {
                    corridor.monitoredSpots.forEach { spot ->
                        val spotLatLng = LatLng(spot.latitude, spot.longitude)
                        val markerState = rememberUpdatedMarkerState(position = spotLatLng)
                        val estLevel = estimate?.riskLevel
                        MarkerComposable(
                            keys = arrayOf<Any>(spot.id, estLevel ?: "PENDING", isSelected),
                            state = markerState,
                            title = spot.name,
                            snippet = "Experimental ${estLevel?.label ?: "PENDING"} • Slope ${spot.slopeDegrees}°",
                            zIndex = 3.5f,
                            onClick = {
                                onSelectCorridor(corridor.id)
                                onSelectInfo(
                                    MapSelectionInfo.SpotInfo(
                                        spotName = spot.name,
                                        corridorName = corridor.name,
                                        elevationMeters = spot.elevationMeters,
                                        slopeDegrees = spot.slopeDegrees,
                                        riskLevel = estLevel,
                                        hazardNote = spot.hazardNote
                                    )
                                )
                                true
                            }
                        ) {
                            MonitoredSpotMarkerPin(
                                level = estLevel,
                                highlighted = isSelected
                            )
                        }
                    }
                }
            }

            // 3. Verified Incidents vs Unverified Community Reports (Separate Marker Shapes & Colors)
            reports
                .filter { it.verificationStatus != "DISMISSED" && it.verificationStatus != "RESOLVED" }
                .forEachIndexed { idx, rep ->
                    val isConfirmed = rep.verificationStatus == "CONFIRMED"
                    if (isConfirmed && !showVerifiedIncidents) return@forEachIndexed
                    if (!isConfirmed && !showUnverifiedReports) return@forEachIndexed

                    val corridor = NilgirisReferenceCatalog.corridorById(rep.corridorId)
                    val pts = corridor.geoPolyline
                    if (pts.isEmpty()) return@forEachIndexed
                    val base = pts[(pts.size / 2).coerceIn(0, pts.lastIndex)]
                    val jitterLat = ((idx % 3) - 1) * 0.008
                    val jitterLng = ((idx % 2) * 2 - 1) * 0.008
                    val pos = LatLng(base.latitude + jitterLat, base.longitude + jitterLng)
                    val markerState = rememberUpdatedMarkerState(position = pos)

                    MarkerComposable(
                        keys = arrayOf(rep.id, rep.verificationStatus),
                        state = markerState,
                        title = if (isConfirmed) "Verified Incident" else "Unverified Report",
                        snippet = "${rep.locationLabel} (${rep.observedTimeText})",
                        zIndex = if (isConfirmed) 4.8f else 4.2f,
                        onClick = {
                            onSelectCorridor(rep.corridorId)
                            onSelectInfo(MapSelectionInfo.IncidentInfo(rep))
                            true
                        }
                    ) {
                        IncidentMarkerPin(isConfirmed = isConfirmed)
                    }
                }

            // 4. Live Rainfall Station Markers & Intensity Circles
            if (showRainfallStations) {
                NilgirisReferenceCatalog.locations.forEach { loc ->
                    val telemetry = weatherMap[loc.id]
                    val locLatLng = LatLng(loc.latitude, loc.longitude)

                    if (telemetry != null && telemetry.rain24hMm > 0.5) {
                        Circle(
                            center = locLatLng,
                            radius = (telemetry.rain24hMm * 120.0).coerceIn(1500.0, 9000.0),
                            fillColor = InfoCyan.copy(alpha = 0.16f),
                            strokeColor = InfoCyan.copy(alpha = 0.55f),
                            strokeWidth = 2f,
                            zIndex = 0.8f
                        )
                    }

                    val markerState = rememberUpdatedMarkerState(position = locLatLng)
                    MarkerComposable(
                        keys = arrayOf<Any>(loc.id, telemetry?.rainCurrentMm ?: -1.0, telemetry?.rain24hMm ?: -1.0),
                        state = markerState,
                        title = loc.name.substringBefore(" ("),
                        snippet = if (telemetry != null) {
                            String.format(Locale.US, "Rain %.1f mm/h • 24h %.1f mm", telemetry.rainCurrentMm, telemetry.rain24hMm)
                        } else {
                            "Awaiting Open-Meteo sync"
                        },
                        zIndex = 4.0f,
                        onClick = {
                            onSelectInfo(MapSelectionInfo.RainfallInfo(loc.name, telemetry))
                            true
                        }
                    ) {
                        RainfallStationPin(
                            shortName = loc.name.substringBefore(" ("),
                            rainCurrentMm = telemetry?.rainCurrentMm
                        )
                    }
                }
            }

            // 5. Real-Time User Position Halo & High-Visibility Pin
            if (isLiveTrackingEnabled && userLocation != null) {
                val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
                Circle(
                    center = userLatLng,
                    radius = userLocation.accuracyMeters.toDouble().coerceIn(25.0, 2500.0),
                    fillColor = TealBright.copy(alpha = 0.20f),
                    strokeColor = TealBright.copy(alpha = 0.75f),
                    strokeWidth = 2.5f,
                    zIndex = 5.2f
                )
                val userMarkerState = rememberUpdatedMarkerState(position = userLatLng)
                MarkerComposable(
                    keys = arrayOf<Any>(
                        userLocation.latitude,
                        userLocation.longitude,
                        userLocation.isFinePrecision
                    ),
                    state = userMarkerState,
                    title = if (userLocation.isFinePrecision) "Your Live GPS Position" else "Your Approximate Position",
                    snippet = String.format(
                        Locale.US,
                        "±%.0fm • Nearest: %s (%.1f km)",
                        userLocation.accuracyMeters,
                        userLocation.nearestCorridorName,
                        userLocation.distanceToNearestCorridorKm
                    ),
                    zIndex = 6.0f
                ) {
                    UserLiveMarkerPin(isFine = userLocation.isFinePrecision)
                }
            }
        }

        // Top HUD Overlay
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            shape = RoundedCornerShape(8.dp),
            color = NavyDeepest.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, NavyBorder)
        ) {
            Text(
                text = "GOOGLE MAPS PLATFORM • COIMBATORE–OOTY & NILGIRIS",
                style = MaterialTheme.typography.labelSmall,
                color = TealBright,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Map Action Controls (Center on My Location + Center on Active Route)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = NavyDeepest.copy(alpha = 0.90f),
                border = BorderStroke(
                    1.dp,
                    if (myLocationLayerActive && userLocation != null) InfoCyan else TealPrimary.copy(alpha = 0.65f)
                )
            ) {
                IconButton(
                    onClick = {
                        if (!hasAnyLocationPermission) {
                            onRequestLocationPermission()
                        } else if (userLocation != null) {
                            scope.launch {
                                runCatching {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(userLocation.latitude, userLocation.longitude),
                                            14.5f
                                        ),
                                        durationMs = 500
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("center_user_location_button")
                ) {
                    Icon(
                        imageVector = if (hasAnyLocationPermission) Icons.Default.MyLocation else Icons.Default.GpsOff,
                        contentDescription = if (hasAnyLocationPermission) "Center map on my location" else "Enable fine location",
                        tint = if (myLocationLayerActive && userLocation != null) InfoCyan else RiskMediumAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = NavyDeepest.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.65f))
            ) {
                IconButton(
                    onClick = {
                        val target = corridors.firstOrNull { it.id == (highlightedCorridorId ?: "coimbatore_ooty") }
                            ?: corridors.first()
                        if (target.geoPolyline.size >= 2) {
                            val b = LatLngBounds.builder()
                            target.geoPolyline.forEach { b.include(LatLng(it.latitude, it.longitude)) }
                            scope.launch {
                                runCatching {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngBounds(b.build(), 90),
                                        durationMs = 500
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("recenter_map_route_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = "Center map on active route",
                        tint = TealBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LivePositionSituationalBar(
    permissionState: LocationPermissionState,
    userLocation: UserLocationSnapshot?,
    isLiveTrackingEnabled: Boolean,
    isFallbackCanvasActive: Boolean,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onToggleLiveTracking: () -> Unit
) {
    val isPermitted = permissionState == LocationPermissionState.FINE_GRANTED ||
        permissionState == LocationPermissionState.APPROXIMATE_ONLY
    val containerColor = when (permissionState) {
        LocationPermissionState.FINE_GRANTED -> if (isLiveTrackingEnabled) TealContainer.copy(alpha = 0.55f) else NavyElevated
        LocationPermissionState.APPROXIMATE_ONLY -> RiskMediumContainer.copy(alpha = 0.45f)
        LocationPermissionState.DENIED, LocationPermissionState.DENIED_PERMANENTLY -> RiskMediumContainer.copy(alpha = 0.55f)
        LocationPermissionState.UNKNOWN -> NavyElevated
    }
    val borderColor = when (permissionState) {
        LocationPermissionState.FINE_GRANTED -> TealPrimary.copy(alpha = 0.65f)
        LocationPermissionState.APPROXIMATE_ONLY, LocationPermissionState.DENIED, LocationPermissionState.DENIED_PERMANENTLY -> RiskMediumAmber.copy(alpha = 0.65f)
        LocationPermissionState.UNKNOWN -> NavyBorder
    }

    EarthWatchCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_gps_situational_card"),
        containerColor = containerColor,
        borderColor = borderColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isPermitted && isLiveTrackingEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        contentDescription = "GPS Tracking Status",
                        tint = when (permissionState) {
                            LocationPermissionState.FINE_GRANTED -> TealBright
                            LocationPermissionState.APPROXIMATE_ONLY -> RiskMediumAmber
                            else -> SlateLight
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPermitted) {
                            if (isLiveTrackingEnabled) permissionState.label else "Live Tracking Paused"
                        } else {
                            "Real-Time Traveller Position"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = PureWhite
                    )
                }

                if (isPermitted) {
                    OutlinedButton(
                        onClick = onToggleLiveTracking,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, if (isLiveTrackingEnabled) TealPrimary else SlateMuted),
                        modifier = Modifier.testTag("toggle_live_tracking_button")
                    ) {
                        Text(
                            text = if (isLiveTrackingEnabled) "Pause GPS" else "Resume GPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLiveTrackingEnabled) TealBright else SlateLight
                        )
                    }
                }
            }

            when (permissionState) {
                LocationPermissionState.UNKNOWN, LocationPermissionState.DENIED -> {
                    Text(
                        text = stringResource(R.string.location_permission_rationale),
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateLight
                    )
                    Button(
                        onClick = onRequestLocationPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDeepest),
                        modifier = Modifier.testTag("enable_fine_location_button")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_enable_fine_location), fontWeight = FontWeight.Bold)
                    }
                }

                LocationPermissionState.DENIED_PERMANENTLY -> {
                    Text(
                        text = stringResource(R.string.location_denied_settings_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = OffWhite
                    )
                    OutlinedButton(
                        onClick = onOpenLocationSettings,
                        border = BorderStroke(1.dp, RiskMediumAmber),
                        modifier = Modifier.testTag("open_location_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = RiskMediumAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_open_location_settings), color = RiskMediumAmber)
                    }
                }

                LocationPermissionState.APPROXIMATE_ONLY, LocationPermissionState.FINE_GRANTED -> {
                    if (permissionState == LocationPermissionState.APPROXIMATE_ONLY) {
                        Text(
                            text = stringResource(R.string.location_approximate_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = RiskMediumAmber
                        )
                        OutlinedButton(
                            onClick = onRequestLocationPermission,
                            border = BorderStroke(1.dp, RiskMediumAmber),
                            modifier = Modifier.testTag("upgrade_fine_location_button")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = RiskMediumAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_upgrade_precise_location), color = RiskMediumAmber)
                        }
                    }

                    if (!isLiveTrackingEnabled) {
                        Text(
                            text = "Position updates paused to preserve battery. Tap Resume GPS anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    } else if (userLocation == null) {
                        Text(
                            text = "Acquiring live fix from Fused Location Provider...",
                            style = MaterialTheme.typography.bodySmall,
                            color = InfoCyan
                        )
                    } else {
                        val speedLabel = userLocation.speedKmh?.let {
                            String.format(Locale.US, " • %.0f km/h", it)
                        } ?: ""
                        Text(
                            text = String.format(
                                Locale.US,
                                "Lat %.5f, Lon %.5f • ±%.0fm accuracy%s",
                                userLocation.latitude,
                                userLocation.longitude,
                                userLocation.accuracyMeters,
                                speedLabel
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = TealBright
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "Nearest Corridor: %s (%.1f km away)",
                                userLocation.nearestCorridorName,
                                userLocation.distanceToNearestCorridorKm
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = OffWhite
                        )
                        if (userLocation.nearestHazardSpotName != null && userLocation.distanceToNearestSpotKm != null) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Nearest Monitored Watchpoint: %s (%.1f km)",
                                    userLocation.nearestHazardSpotName,
                                    userLocation.distanceToNearestSpotKm
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = InfoCyan
                            )
                        }
                        if (isFallbackCanvasActive &&
                            UserLocationTracker.projectLatLngToNilgirisSector(
                                userLocation.latitude,
                                userLocation.longitude
                            ) == null
                        ) {
                            Text(
                                text = "Current device coordinates lie outside the Nilgiris topo canvas sector; nearest corridor distance is still tracked above.",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserLiveMarkerPin(isFine: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isFine) InfoCyan else RiskMediumAmber,
        border = BorderStroke(2.5.dp, PureWhite),
        shadowElevation = 6.dp,
        modifier = Modifier.size(22.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Your live GPS position",
                tint = NavyDeepest,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun MonitoredSpotMarkerPin(
    level: RiskLevel?,
    highlighted: Boolean
) {
    val fill = when (level) {
        RiskLevel.LOW -> RiskLowGreen
        RiskLevel.MEDIUM -> RiskMediumAmber
        RiskLevel.HIGH -> RiskHighRed
        null -> SlateMuted
    }
    Surface(
        shape = CircleShape,
        color = fill,
        border = BorderStroke(if (highlighted) 2.5.dp else 1.5.dp, PureWhite),
        shadowElevation = 4.dp,
        modifier = Modifier.size(if (highlighted) 24.dp else 20.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = NavyDeepest,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun IncidentMarkerPin(isConfirmed: Boolean) {
    Surface(
        shape = if (isConfirmed) CircleShape else RoundedCornerShape(4.dp),
        color = if (isConfirmed) RiskHighRed else UnverifiedViolet,
        border = BorderStroke(2.dp, PureWhite),
        shadowElevation = 5.dp,
        modifier = Modifier.size(26.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isConfirmed) Icons.Default.Verified else Icons.Default.ReportProblem,
                contentDescription = null,
                tint = PureWhite,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ClosureMarkerPin() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = RiskHighRed,
        border = BorderStroke(2.dp, PureWhite),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = PureWhite,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "CLOSED",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun RainfallStationPin(
    shortName: String,
    rainCurrentMm: Double?
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = NavyDeepest.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, InfoCyan),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = InfoCyan,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (rainCurrentMm != null) {
                    String.format(Locale.US, "%s %.1fmm", shortName, rainCurrentMm)
                } else {
                    shortName
                },
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SelectedMarkerTelemetryCard(
    info: MapSelectionInfo,
    onDismiss: () -> Unit
) {
    EarthWatchCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = NavyElevated,
        borderColor = TealPrimary.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (info) {
                    is MapSelectionInfo.SpotInfo -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            SourceTagBadge("EXPERIMENTAL RISK SPOT", SourceBadgeKind.EXPERIMENTAL)
                            info.riskLevel?.let { RiskLevelBadge(it) }
                        }
                        Text(info.spotName, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                        Text(
                            text = "${info.corridorName} • Elev ${info.elevationMeters}m • Slope ${info.slopeDegrees}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = TealBright
                        )
                        Text(info.hazardNote, style = MaterialTheme.typography.bodySmall, color = SlateLight)
                    }

                    is MapSelectionInfo.RainfallInfo -> {
                        SourceTagBadge("OPEN-METEO RAINFALL STATION", SourceBadgeKind.LIVE_API)
                        Text(info.locationName, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                        val t = info.telemetry
                        if (t != null) {
                            val soilLabel = t.soilMoistureM3M3?.let { String.format(Locale.US, "%.3f m³/m³", it) } ?: "Unavailable"
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Current: %.1f mm/h • 24h: %.1f mm • 72h: %.1f mm (%dh wet) • Soil: %s",
                                    t.rainCurrentMm, t.rain24hMm, t.rain72hMm, t.rainfallDurationHours72h, soilLabel
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = InfoCyan
                            )
                        } else {
                            Text(
                                text = "Live rainfall telemetry not loaded yet. Tap Refresh Weather on Home.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RiskMediumAmber
                            )
                        }
                    }

                    is MapSelectionInfo.IncidentInfo -> {
                        val isConfirmed = info.report.verificationStatus == "CONFIRMED"
                        SourceTagBadge(
                            label = if (isConfirmed) "VERIFIED INCIDENT" else "UNVERIFIED COMMUNITY REPORT",
                            kind = if (isConfirmed) SourceBadgeKind.OFFICIAL else SourceBadgeKind.UNVERIFIED
                        )
                        Text(
                            text = "${info.report.corridorName} • ${info.report.locationLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            color = PureWhite
                        )
                        Text(
                            text = "${info.report.observedTimeText} — ${info.report.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateLight
                        )
                    }

                    is MapSelectionInfo.ClosureInfo -> {
                        SourceTagBadge("OFFICIAL ROAD CLOSURE", SourceBadgeKind.OFFICIAL)
                        Text("${info.corridorName}: ${info.title}", style = MaterialTheme.typography.titleSmall, color = RiskHighRed)
                        Text(info.details, style = MaterialTheme.typography.bodySmall, color = OffWhite)
                    }
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close map selection", tint = SlateMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MapKeyBadge(
    color: Color,
    shapeLabel: String,
    description: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$shapeLabel: ",
            style = MaterialTheme.typography.labelSmall,
            color = OffWhite,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = SlateMuted
        )
    }
}
