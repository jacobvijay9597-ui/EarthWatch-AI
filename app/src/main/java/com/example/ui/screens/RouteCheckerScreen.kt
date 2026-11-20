package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.core.UiState
import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.model.CommunityReport
import com.example.data.model.CorridorOperationalStatus
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.LocationPermissionState
import com.example.data.model.OfficialAdvisory
import com.example.data.model.RoadCorridorStatus
import com.example.data.model.RouteEvaluation
import com.example.data.model.UserLocationSnapshot
import com.example.ui.components.EarthWatchCard
import com.example.ui.components.NilgirisGoogleMapSection
import com.example.ui.components.RiskLevelBadge
import com.example.ui.components.SourceBadgeKind
import com.example.ui.components.SourceTagBadge
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RiskHighContainer
import com.example.ui.theme.RiskHighRed
import com.example.ui.theme.RiskLowContainer
import com.example.ui.theme.RiskLowGreen
import com.example.ui.theme.RiskMediumAmber
import com.example.ui.theme.RiskMediumContainer
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RouteCheckerScreen(
    originId: String,
    destinationId: String,
    travelDateMillis: Long,
    routeEvaluation: RouteEvaluation,
    allEstimates: List<ExperimentalRiskEstimate>,
    weatherState: UiState<List<LiveWeatherTelemetry>>,
    advisoriesState: UiState<List<OfficialAdvisory>>,
    reportsState: UiState<List<CommunityReport>>,
    corridorStatusesState: UiState<List<RoadCorridorStatus>>,
    onOriginSelected: (String) -> Unit,
    onDestinationSelected: (String) -> Unit,
    onSwapLocations: () -> Unit,
    onDateSelected: (Long) -> Unit,
    userLocation: UserLocationSnapshot? = null,
    locationPermissionState: LocationPermissionState = LocationPermissionState.UNKNOWN,
    isLiveTrackingEnabled: Boolean = true,
    onRequestLocationPermission: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    onToggleLiveTracking: () -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.US) }
    val formattedDate = dateFormatter.format(Date(travelDateMillis))

    val estimatesMap = remember(allEstimates) { allEstimates.associateBy { it.corridorId } }
    val statusesMap = remember(corridorStatusesState) {
        ((corridorStatusesState as? UiState.Success)?.data.orEmpty()).associateBy { it.id }
    }
    val weatherList = (weatherState as? UiState.Success)?.data.orEmpty()
    val reports = (reportsState as? UiState.Success)?.data.orEmpty()
    val advisories = (advisoriesState as? UiState.Success)?.data.orEmpty()

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = travelDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDateSelected)
                        showDatePicker = false
                    }
                ) { Text("Select", color = TealBright) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = SlateLight)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Travel Route Checker & Safe Assistance",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite
                    )
                    Text(
                        text = "Evaluate Nilgiris ghat corridors, weather at stops, closures, and alternate routes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                }
            }

            // Route Input Card
            item {
                EarthWatchCard(modifier = Modifier.fillMaxWidth(), containerColor = NavyCard) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LocationSelectorField(
                                label = "Starting Location",
                                selectedId = originId,
                                onSelect = onOriginSelected,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("origin_selector")
                            )
                            IconButton(
                                onClick = onSwapLocations,
                                modifier = Modifier.testTag("swap_route_button")
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap origin and destination", tint = TealBright)
                            }
                            LocationSelectorField(
                                label = "Destination",
                                selectedId = destinationId,
                                onSelect = onDestinationSelected,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("destination_selector")
                            )
                        }

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            border = BorderStroke(1.dp, NavyBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("travel_date_button")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TealBright, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Travel Date: $formattedDate", color = OffWhite)
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val presets = listOf(
                                Triple("Coimbatore → Ooty", "coimbatore", "ooty"),
                                Triple("Mettupalayam → Kotagiri → Ooty", "mettupalayam", "kotagiri"),
                                Triple("Ooty → Masinagudi", "ooty", "masinagudi")
                            )
                            presets.forEach { (title, fromLoc, toLoc) ->
                                val isPresetActive = originId == fromLoc && destinationId == toLoc
                                FilterChip(
                                    selected = isPresetActive,
                                    onClick = {
                                        onOriginSelected(fromLoc)
                                        onDestinationSelected(toLoc)
                                    },
                                    label = { Text(title, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealPrimary,
                                        selectedLabelColor = NavyDeepest,
                                        containerColor = NavyElevated,
                                        labelColor = SlateLight
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Safe Route Verification Banner (Module 6 strict rule: never claim unverified road is safe)
            item {
                val isClosed = routeEvaluation.corridorStatus?.status == CorridorOperationalStatus.CLOSED_OFFICIAL.code ||
                    routeEvaluation.relatedAdvisories.any { it.roadClosed }
                val bannerColor = when {
                    isClosed -> RiskHighContainer.copy(alpha = 0.7f)
                    routeEvaluation.safetyConfirmed -> RiskLowContainer.copy(alpha = 0.55f)
                    else -> RiskMediumContainer.copy(alpha = 0.65f)
                }
                val borderColor = when {
                    isClosed -> RiskHighRed
                    routeEvaluation.safetyConfirmed -> RiskLowGreen
                    else -> RiskMediumAmber
                }
                EarthWatchCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = bannerColor,
                    borderColor = borderColor
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (routeEvaluation.safetyConfirmed) Icons.Default.VerifiedUser else Icons.Default.Warning,
                            contentDescription = "Route safety status",
                            tint = borderColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (routeEvaluation.safetyConfirmed) "ADMIN-VERIFIED CORRIDOR STATUS" else "SAFE ROUTE ASSISTANCE WARNING",
                                style = MaterialTheme.typography.labelMedium,
                                color = borderColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = routeEvaluation.safetyNotice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite
                            )
                        }
                    }
                }
            }

            // Interactive Google Maps Platform Selected Route Map
            item {
                NilgirisGoogleMapSection(
                    corridors = NilgirisReferenceCatalog.corridors,
                    estimatesByCorridor = estimatesMap,
                    statusesByCorridor = statusesMap,
                    weatherList = weatherList,
                    reports = reports,
                    advisories = advisories,
                    highlightedCorridorId = routeEvaluation.primaryCorridor.id,
                    userLocation = userLocation,
                    locationPermissionState = locationPermissionState,
                    isLiveTrackingEnabled = isLiveTrackingEnabled,
                    onRequestLocationPermission = onRequestLocationPermission,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onToggleLiveTracking = onToggleLiveTracking,
                    onSelectCorridor = { tappedId ->
                        val c = NilgirisReferenceCatalog.corridorById(tappedId)
                        onOriginSelected(c.fromLocationId)
                        onDestinationSelected(c.toLocationId)
                    }
                )
            }

            // Primary Corridor Summary & Experimental Risk
            item {
                val corridor = routeEvaluation.primaryCorridor
                val est = routeEvaluation.riskEstimate
                EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(corridor.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                Text(
                                    text = "${corridor.highwayCode} • ${corridor.distanceKm} km • ~${corridor.averageDriveMinutes} mins • ${corridor.hairpinCount} hairpins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TealBright
                                )
                            }
                            if (est != null) {
                                RiskLevelBadge(level = est.riskLevel)
                            }
                        }

                        if (est != null) {
                            SourceTagBadge(
                                label = "EXPERIMENTAL ESTIMATE • SCORE ${est.riskScore}/100",
                                kind = SourceBadgeKind.EXPERIMENTAL
                            )
                        } else {
                            Text(
                                text = "Live weather telemetry pending — risk estimate withheld rather than simulated.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RiskMediumAmber
                            )
                        }

                        Text(
                            text = "Geology: ${corridor.soilAndGeology} (Max slope ${corridor.maxSlopeDegrees}°)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateLight
                        )
                    }
                }
            }

            // Monitored Landslide-Risk Locations Along Route
            item {
                Text(
                    text = "Monitored Hazard Spots Along Route",
                    style = MaterialTheme.typography.titleMedium,
                    color = OffWhite
                )
            }
            items(routeEvaluation.primaryCorridor.monitoredSpots, key = { it.id }) { spot ->
                EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TealBright, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(spot.name, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                            Text(
                                text = "Elevation ${spot.elevationMeters}m • Slope ${spot.slopeDegrees}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = InfoCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(spot.hazardNote, style = MaterialTheme.typography.bodySmall, color = SlateLight)
                        }
                    }
                }
            }

            // Live Weather at Route Stops
            item {
                Text(
                    text = "Live Weather Conditions at Route Locations",
                    style = MaterialTheme.typography.titleMedium,
                    color = OffWhite
                )
            }
            if (routeEvaluation.weatherAtStops.isEmpty()) {
                item {
                    EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Live Open-Meteo weather is currently unavailable or loading.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(routeEvaluation.weatherAtStops, key = { it.locationId }) { w ->
                    EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.locationName, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                                Text("${w.weatherLabel} • Wind ${w.windKmh} km/h", style = MaterialTheme.typography.bodySmall, color = TealBright)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.1f°C • %.1f mm/h", w.temperatureC, w.rainCurrentMm),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OffWhite
                                )
                                val soilStr = w.soilMoistureM3M3?.let { String.format(Locale.US, "%.3f", it) } ?: "N/A"
                                Text(
                                    text = String.format(Locale.US, "24h: %.1f mm • Soil: %s", w.rain24hMm, soilStr),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }
                        }
                    }
                }
            }

            // Module 6: Alternative Routes
            item {
                Text(
                    text = "Alternative Hill Routes (Safe Route Assistance)",
                    style = MaterialTheme.typography.titleMedium,
                    color = OffWhite
                )
            }
            items(routeEvaluation.alternateCorridors, key = { it.id }) { alt ->
                val altEst = estimatesMap[alt.id]
                val altStatus = statusesMap[alt.id]
                val altClosed = altStatus?.status == CorridorOperationalStatus.CLOSED_OFFICIAL.code ||
                    advisories.any { it.corridorId == alt.id && it.roadClosed }

                EarthWatchCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NavyElevated,
                    onClick = {
                        onOriginSelected(alt.fromLocationId)
                        onDestinationSelected(alt.toLocationId)
                    }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.AltRoute, contentDescription = null, tint = TealBright, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(alt.name, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                            }
                            if (altEst != null) {
                                RiskLevelBadge(level = altEst.riskLevel)
                            }
                        }
                        Text(
                            text = "${alt.highwayCode} • ${alt.distanceKm} km • Max slope ${alt.maxSlopeDegrees}° • ${alt.hairpinCount} hairpins",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateLight
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (altClosed) {
                                SourceTagBadge("ROAD CLOSED / RESTRICTED", SourceBadgeKind.OFFICIAL)
                            } else if (altStatus?.verifiedSafety == true && altStatus.status == CorridorOperationalStatus.OPEN.code) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RiskLowGreen, modifier = Modifier.size(14.dp))
                                Text("Admin-monitored open", style = MaterialTheme.typography.labelSmall, color = RiskLowGreen)
                            } else {
                                Text(
                                    text = "Safety unverified — confirm official closure status before taking this alternate.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RiskMediumAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSelectorField(
    label: String,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = NilgirisReferenceCatalog.locationById(selectedId)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.name.substringBefore(" ("),
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                unfocusedBorderColor = NavyBorder,
                focusedTextColor = PureWhite,
                unfocusedTextColor = OffWhite
            ),
            singleLine = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = NavyElevated
        ) {
            NilgirisReferenceCatalog.locations.forEach { loc ->
                DropdownMenuItem(
                    text = {
                        Text("${loc.name} (${loc.elevationMeters}m)", color = OffWhite)
                    },
                    onClick = {
                        onSelect(loc.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
