package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.model.CommunityReport
import com.example.data.model.CorridorOperationalStatus
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.LocationPermissionState
import com.example.data.model.OfficialAdvisory
import com.example.data.model.RoadCorridorStatus
import com.example.data.model.UserLocationSnapshot
import com.example.ui.components.EarthWatchCard
import com.example.ui.components.NilgirisGoogleMapSection
import com.example.ui.components.RiskLevelBadge
import com.example.ui.components.SourceBadgeKind
import com.example.ui.components.SourceTagBadge
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RiskHighRed
import com.example.ui.theme.RiskLowGreen
import com.example.ui.theme.RiskMediumAmber
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.UnverifiedViolet

private enum class MapFilterLayer(val label: String) {
    ALL("All Layers"),
    PREDICTED("Predicted Risk"),
    CLOSURES("Official Closures"),
    VERIFIED("Verified Incidents"),
    UNVERIFIED("Unverified Reports"),
    RAINFALL("Rainfall Stations")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NilgirisRiskMapScreen(
    riskEstimates: List<ExperimentalRiskEstimate>,
    weatherState: UiState<List<LiveWeatherTelemetry>>,
    reportsState: UiState<List<CommunityReport>>,
    advisoriesState: UiState<List<OfficialAdvisory>>,
    corridorStatusesState: UiState<List<RoadCorridorStatus>>,
    userLocation: UserLocationSnapshot? = null,
    locationPermissionState: LocationPermissionState = LocationPermissionState.UNKNOWN,
    isLiveTrackingEnabled: Boolean = true,
    onRequestLocationPermission: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    onToggleLiveTracking: () -> Unit = {}
) {
    var selectedCorridorId by remember { mutableStateOf("coimbatore_ooty") }
    var activeLayer by remember { mutableStateOf(MapFilterLayer.ALL) }

    val estimatesMap = remember(riskEstimates) { riskEstimates.associateBy { it.corridorId } }
    val weatherList = (weatherState as? UiState.Success)?.data.orEmpty()
    val allReports = (reportsState as? UiState.Success)?.data.orEmpty()
    val allAdvisories = (advisoriesState as? UiState.Success)?.data.orEmpty()
    val statusesMap = remember(corridorStatusesState) {
        ((corridorStatusesState as? UiState.Success)?.data.orEmpty()).associateBy { it.id }
    }

    val showPredicted = activeLayer == MapFilterLayer.ALL || activeLayer == MapFilterLayer.PREDICTED
    val showClosures = activeLayer == MapFilterLayer.ALL || activeLayer == MapFilterLayer.CLOSURES
    val showVerified = activeLayer == MapFilterLayer.ALL || activeLayer == MapFilterLayer.VERIFIED
    val showUnverified = activeLayer == MapFilterLayer.ALL || activeLayer == MapFilterLayer.UNVERIFIED
    val showRainfall = activeLayer == MapFilterLayer.ALL || activeLayer == MapFilterLayer.RAINFALL

    val selectedCorridor = NilgirisReferenceCatalog.corridorById(selectedCorridorId)
    val selectedEstimate = estimatesMap[selectedCorridorId]
    val selectedStatus = statusesMap[selectedCorridorId]
    val corridorReports = allReports.filter { it.corridorId == selectedCorridorId }
    val corridorAdvisories = allAdvisories.filter { it.corridorId == selectedCorridorId }

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
                        text = "Nilgiris Interactive Risk Map",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite
                    )
                    Text(
                        text = "Distinguishes experimental predictions from confirmed incidents and official road closures.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MapFilterLayer.entries.forEach { layer ->
                        FilterChip(
                            selected = activeLayer == layer,
                            onClick = { activeLayer = layer },
                            label = { Text(layer.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = NavyDeepest,
                                containerColor = NavyCard,
                                labelColor = SlateLight
                            ),
                            modifier = Modifier.testTag("map_filter_${layer.name.lowercase()}")
                        )
                    }
                }
            }

            item {
                NilgirisGoogleMapSection(
                    corridors = NilgirisReferenceCatalog.corridors,
                    estimatesByCorridor = estimatesMap,
                    statusesByCorridor = statusesMap,
                    weatherList = weatherList,
                    reports = allReports,
                    advisories = allAdvisories,
                    highlightedCorridorId = selectedCorridorId,
                    showPredictedRisk = showPredicted,
                    showVerifiedIncidents = showVerified,
                    showOfficialClosures = showClosures,
                    showRainfallStations = showRainfall,
                    showUnverifiedReports = showUnverified,
                    userLocation = userLocation,
                    locationPermissionState = locationPermissionState,
                    isLiveTrackingEnabled = isLiveTrackingEnabled,
                    onRequestLocationPermission = onRequestLocationPermission,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onToggleLiveTracking = onToggleLiveTracking,
                    onSelectCorridor = { selectedCorridorId = it }
                )
            }

            // Selected Corridor Inspection Panel
            item {
                EarthWatchCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = TealPrimary.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedCorridor.name, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                Text(
                                    text = "${selectedCorridor.highwayCode} • ${selectedCorridor.distanceKm} km • ${selectedCorridor.hairpinCount} hairpins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TealBright
                                )
                            }
                            if (selectedEstimate != null) {
                                RiskLevelBadge(level = selectedEstimate.riskLevel)
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SourceTagBadge("EXPERIMENTAL MODEL", SourceBadgeKind.EXPERIMENTAL)
                            if (selectedStatus != null) {
                                val st = CorridorOperationalStatus.safeFrom(selectedStatus.status)
                                SourceTagBadge(
                                    label = st.displayLabel.uppercase(),
                                    kind = if (st == CorridorOperationalStatus.CLOSED_OFFICIAL) SourceBadgeKind.OFFICIAL else SourceBadgeKind.LIVE_API
                                )
                            } else {
                                SourceTagBadge("SAFETY UNVERIFIED (NO ROAD STATUS)", SourceBadgeKind.UNVERIFIED)
                            }
                        }

                        if (selectedStatus != null && selectedStatus.verifiedSafety && selectedStatus.status == CorridorOperationalStatus.OPEN.code) {
                            Text(
                                text = "Official/Admin Note: ${selectedStatus.statusNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = OffWhite
                            )
                        } else if (selectedStatus != null) {
                            Text(
                                text = "Road Status: ${selectedStatus.statusNote} — Safety not verified as guaranteed open.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RiskMediumAmber
                            )
                        } else {
                            Text(
                                text = "No verified official road-status record on file for this corridor. Never treat unverified roads as safe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RiskMediumAmber
                            )
                        }

                        if (corridorAdvisories.isNotEmpty()) {
                            corridorAdvisories.forEach { adv ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = RiskHighRed.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, RiskHighRed.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (adv.roadClosed) "OFFICIAL CLOSURE: ${adv.title}" else "OFFICIAL ADVISORY: ${adv.title}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = RiskHighRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(adv.summary, style = MaterialTheme.typography.bodySmall, color = OffWhite)
                                    }
                                }
                            }
                        }

                        if (corridorReports.isNotEmpty()) {
                            corridorReports.forEach { rep ->
                                val isConfirmed = rep.verificationStatus == "CONFIRMED"
                                val cardAccent = if (isConfirmed) RiskHighRed else UnverifiedViolet
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = cardAccent.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, cardAccent.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (isConfirmed) {
                                                "[VERIFIED INCIDENT] ${rep.locationLabel} • ${rep.observedTimeText}"
                                            } else {
                                                "[UNVERIFIED REPORT] ${rep.locationLabel} • ${rep.observedTimeText}"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = cardAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(rep.description, style = MaterialTheme.typography.bodySmall, color = OffWhite)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Corridor Selector List
            items(NilgirisReferenceCatalog.corridors, key = { it.id }) { c ->
                val est = estimatesMap[c.id]
                EarthWatchCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (c.id == selectedCorridorId) TealPrimary else NavyBorder,
                    onClick = { selectedCorridorId = c.id }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                            Text("${c.highwayCode} • Max slope ${c.maxSlopeDegrees}°", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
                        }
                        if (est != null) {
                            RiskLevelBadge(level = est.riskLevel)
                        }
                    }
                }
            }
        }
    }
}
