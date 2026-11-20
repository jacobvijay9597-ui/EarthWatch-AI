package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.model.CommunityReport
import com.example.data.model.CorridorOperationalStatus
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.IncidentCategory
import com.example.data.model.OfficialAdvisory
import com.example.data.model.RiskLevel
import com.example.data.model.RoadCorridorStatus
import com.example.data.model.UserProfile
import com.example.ui.components.EarthWatchCard
import com.example.ui.components.ErrorBannerCard
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
import com.example.ui.theme.RiskHighRed
import com.example.ui.theme.RiskLowGreen
import com.example.ui.theme.RiskMediumAmber
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.UnverifiedViolet
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private enum class AlertsHubTab(val label: String) {
    ALERTS("Smart Alerts"),
    REPORT("Report Hazard"),
    ADMIN("Admin Desk")
}

@Composable
fun AlertsReportsAdminScreen(
    isAdminUser: Boolean,
    lastWeatherSyncLabel: String,
    userProfileState: UiState<UserProfile?>,
    riskEstimates: List<ExperimentalRiskEstimate>,
    advisoriesState: UiState<List<OfficialAdvisory>>,
    publicReportsState: UiState<List<CommunityReport>>,
    adminReportsState: UiState<List<CommunityReport>>,
    corridorStatusesState: UiState<List<RoadCorridorStatus>>,
    isSubmittingAction: Boolean,
    onSaveSubscriptions: (List<String>, Boolean, String) -> Unit,
    onSubmitReport: (String, String, String, String, String, String) -> Unit,
    onAdminUpdateReport: (String, String, String, String) -> Unit,
    onAdminUpdateCorridor: (String, String, String, Boolean, Boolean) -> Unit,
    onAdminCreateAdvisory: (String, String, String, String, String, String, Boolean, Boolean) -> Unit,
    onAdminSeedDemo: () -> Unit
) {
    var activeTab by remember { mutableStateOf(AlertsHubTab.ALERTS) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                AlertsHubTab.entries.forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(idx, AlertsHubTab.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = TealPrimary,
                            activeContentColor = NavyDeepest,
                            inactiveContainerColor = NavyCard,
                            inactiveContentColor = SlateLight
                        ),
                        modifier = Modifier.testTag("hub_tab_${tab.name.lowercase()}")
                    ) {
                        Text(tab.label, maxLines = 1)
                    }
                }
            }

            when (activeTab) {
                AlertsHubTab.ALERTS -> SmartAlertsTab(
                    userProfileState = userProfileState,
                    riskEstimates = riskEstimates,
                    advisoriesState = advisoriesState,
                    lastWeatherSyncLabel = lastWeatherSyncLabel,
                    isSubmitting = isSubmittingAction,
                    onSaveSubscriptions = onSaveSubscriptions
                )
                AlertsHubTab.REPORT -> CommunityReportingTab(
                    reportsState = publicReportsState,
                    isSubmitting = isSubmittingAction,
                    onSubmitReport = onSubmitReport
                )
                AlertsHubTab.ADMIN -> AdminDashboardTab(
                    isAdminUser = isAdminUser,
                    lastWeatherSyncLabel = lastWeatherSyncLabel,
                    adminReportsState = adminReportsState,
                    corridorStatusesState = corridorStatusesState,
                    isSubmitting = isSubmittingAction,
                    onAdminUpdateReport = onAdminUpdateReport,
                    onAdminUpdateCorridor = onAdminUpdateCorridor,
                    onAdminCreateAdvisory = onAdminCreateAdvisory,
                    onAdminSeedDemo = onAdminSeedDemo
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartAlertsTab(
    userProfileState: UiState<UserProfile?>,
    riskEstimates: List<ExperimentalRiskEstimate>,
    advisoriesState: UiState<List<OfficialAdvisory>>,
    lastWeatherSyncLabel: String,
    isSubmitting: Boolean,
    onSaveSubscriptions: (List<String>, Boolean, String) -> Unit
) {
    val profile = (userProfileState as? UiState.Success)?.data
    var alertsEnabled by remember { mutableStateOf(true) }
    var threshold by remember { mutableStateOf("MEDIUM") }
    var selectedCorridors by remember { mutableStateOf(listOf("mettupalayam_coonoor")) }

    LaunchedEffect(profile) {
        if (profile != null) {
            alertsEnabled = profile.alertsEnabled
            threshold = profile.riskThreshold
            selectedCorridors = profile.subscribedCorridors
        }
    }

    val advisories = (advisoriesState as? UiState.Success)?.data.orEmpty()
    val elevatedEstimates = riskEstimates.filter { it.riskLevel != RiskLevel.LOW }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Location-Based Alert Subscriptions", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                            Text("Stored securely in your Firestore profile.", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
                        }
                        Switch(
                            checked = alertsEnabled,
                            onCheckedChange = { alertsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NavyDeepest, checkedTrackColor = TealPrimary),
                            modifier = Modifier.testTag("alerts_enabled_switch")
                        )
                    }

                    Text("Minimum Risk Threshold", style = MaterialTheme.typography.labelMedium, color = TealBright)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { tier ->
                            FilterChip(
                                selected = threshold == tier,
                                onClick = { threshold = tier },
                                label = { Text(tier) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = NavyDeepest
                                )
                            )
                        }
                    }

                    Text("Subscribed Nilgiris Corridors", style = MaterialTheme.typography.labelMedium, color = TealBright)
                    NilgirisReferenceCatalog.corridors.forEach { corridor ->
                        val checked = selectedCorridors.contains(corridor.id)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedCorridors = if (isChecked) {
                                        (selectedCorridors + corridor.id).distinct()
                                    } else {
                                        selectedCorridors - corridor.id
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = TealPrimary, checkmarkColor = NavyDeepest)
                            )
                            Text(corridor.name, style = MaterialTheme.typography.bodySmall, color = OffWhite)
                        }
                    }

                    Button(
                        onClick = { onSaveSubscriptions(selectedCorridors, alertsEnabled, threshold) },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDeepest),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_alert_subscriptions_button")
                    ) {
                        Text(if (isSubmitting) "Saving..." else "Save Alert Preferences", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Official Advisories vs App Estimates", style = MaterialTheme.typography.titleMedium, color = OffWhite)
        }

        if (advisoriesState is UiState.Error) {
            item { ErrorBannerCard(message = advisoriesState.message) }
        }

        items(advisories, key = { it.id }) { adv ->
            val timeLabel = adv.updatedAt?.toDate()?.let {
                SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(it)
            } ?: "Recent"
            EarthWatchCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (adv.roadClosed) RiskHighRed else InfoCyan.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SourceTagBadge(if (adv.roadClosed) "OFFICIAL CLOSURE" else "OFFICIAL ADVISORY", SourceBadgeKind.OFFICIAL)
                        if (adv.simulatedDemo) {
                            SourceTagBadge("SIMULATED DEMO", SourceBadgeKind.SIMULATED_DEMO)
                        }
                    }
                    Text(adv.title, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                    Text("${adv.corridorName} • Source: ${adv.sourceLabel} • $timeLabel", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                    Text(adv.summary, style = MaterialTheme.typography.bodySmall, color = SlateLight)
                }
            }
        }

        items(elevatedEstimates, key = { "est_${it.corridorId}" }) { est ->
            EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SourceTagBadge("APP EXPERIMENTAL ESTIMATE", SourceBadgeKind.EXPERIMENTAL)
                        RiskLevelBadge(level = est.riskLevel)
                    }
                    Text(est.corridorName, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                    Text("Generated from Open-Meteo Telemetry • $lastWeatherSyncLabel", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                    Text(est.contributingFactors.firstOrNull().orEmpty(), style = MaterialTheme.typography.bodySmall, color = SlateLight)
                }
            }
        }

        if (advisories.isEmpty() && elevatedEstimates.isEmpty()) {
            item {
                EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TealBright)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("No elevated risk warnings or official closures at this time.", style = MaterialTheme.typography.bodyMedium, color = SlateLight)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityReportingTab(
    reportsState: UiState<List<CommunityReport>>,
    isSubmitting: Boolean,
    onSubmitReport: (String, String, String, String, String, String) -> Unit
) {
    var selectedCorridorId by remember { mutableStateOf(NilgirisReferenceCatalog.corridors.first().id) }
    var locationLabel by remember { mutableStateOf("") }
    var incidentType by remember { mutableStateOf(IncidentCategory.LANDSLIDE.code) }
    var severity by remember { mutableStateOf(RiskLevel.MEDIUM.label) }
    var observedTime by remember { mutableStateOf("Just now") }
    var description by remember { mutableStateOf("") }
    var corridorMenuOpen by remember { mutableStateOf(false) }

    val reports = (reportsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, tint = UnverifiedViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report Road Blockage or Landslide", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    }
                    Text(
                        text = "All community submissions are stored in Firebase Firestore and labeled UNVERIFIED until confirmed by an admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateLight
                    )

                    ExposedDropdownMenuBox(
                        expanded = corridorMenuOpen,
                        onExpandedChange = { corridorMenuOpen = !corridorMenuOpen }
                    ) {
                        OutlinedTextField(
                            value = NilgirisReferenceCatalog.corridorById(selectedCorridorId).name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Hill Corridor") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = corridorMenuOpen) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = NavyBorder, focusedTextColor = PureWhite, unfocusedTextColor = OffWhite),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = corridorMenuOpen,
                            onDismissRequest = { corridorMenuOpen = false },
                            containerColor = NavyElevated
                        ) {
                            NilgirisReferenceCatalog.corridors.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name, color = OffWhite) },
                                    onClick = {
                                        selectedCorridorId = c.id
                                        corridorMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = locationLabel,
                        onValueChange = { locationLabel = it },
                        label = { Text("Exact Location / Hairpin Bend") },
                        placeholder = { Text("e.g., Kallar Hairpin Bend 6") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = NavyBorder, focusedTextColor = PureWhite, unfocusedTextColor = OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_location_input")
                    )

                    OutlinedTextField(
                        value = observedTime,
                        onValueChange = { observedTime = it },
                        label = { Text("Observed Time") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = NavyBorder, focusedTextColor = PureWhite, unfocusedTextColor = OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_time_input")
                    )

                    Text("Incident Type", style = MaterialTheme.typography.labelMedium, color = TealBright)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            IncidentCategory.LANDSLIDE,
                            IncidentCategory.ROAD_BLOCKAGE,
                            IncidentCategory.ROCKFALL
                        ).forEach { cat ->
                            FilterChip(
                                selected = incidentType == cat.code,
                                onClick = { incidentType = cat.code },
                                label = { Text(cat.displayLabel.substringBefore(" /")) }
                            )
                        }
                    }

                    Text("Observed Severity", style = MaterialTheme.typography.labelMedium, color = TealBright)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { sev ->
                            FilterChip(
                                selected = severity == sev,
                                onClick = { severity = sev },
                                label = { Text(sev) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (min 5 chars)") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = NavyBorder, focusedTextColor = PureWhite, unfocusedTextColor = OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_description_input")
                    )

                    Button(
                        onClick = {
                            onSubmitReport(
                                selectedCorridorId,
                                locationLabel,
                                incidentType,
                                severity,
                                description,
                                observedTime
                            )
                            locationLabel = ""
                            description = ""
                        },
                        enabled = !isSubmitting && locationLabel.trim().length in 2..120 && description.trim().length in 5..500 && observedTime.trim().length in 2..60,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDeepest),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_community_report_button")
                    ) {
                        Text(if (isSubmitting) "Submitting..." else "Submit Unverified Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Community Hazard Feed", style = MaterialTheme.typography.titleMedium, color = OffWhite)
        }

        if (reportsState is UiState.Error) {
            item { ErrorBannerCard(message = reportsState.message) }
        }

        items(reports, key = { it.id }) { rep ->
            EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SourceTagBadge(
                            label = if (rep.verificationStatus == "CONFIRMED") "CONFIRMED INCIDENT" else "UNVERIFIED REPORT",
                            kind = if (rep.verificationStatus == "CONFIRMED") SourceBadgeKind.OFFICIAL else SourceBadgeKind.UNVERIFIED
                        )
                        RiskLevelBadge(level = RiskLevel.safeFrom(rep.severity))
                    }
                    Text("${rep.corridorName} • ${rep.locationLabel}", style = MaterialTheme.typography.titleSmall, color = PureWhite)
                    Text(rep.description, style = MaterialTheme.typography.bodySmall, color = SlateLight)
                    Text(
                        text = "Reported by ${rep.reporterName} • Observed: ${rep.observedTimeText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminDashboardTab(
    isAdminUser: Boolean,
    lastWeatherSyncLabel: String,
    adminReportsState: UiState<List<CommunityReport>>,
    corridorStatusesState: UiState<List<RoadCorridorStatus>>,
    isSubmitting: Boolean,
    onAdminUpdateReport: (String, String, String, String) -> Unit,
    onAdminUpdateCorridor: (String, String, String, Boolean, Boolean) -> Unit,
    onAdminCreateAdvisory: (String, String, String, String, String, String, Boolean, Boolean) -> Unit,
    onAdminSeedDemo: () -> Unit
) {
    val reports = (adminReportsState as? UiState.Success)?.data.orEmpty()
    val statuses = ((corridorStatusesState as? UiState.Success)?.data.orEmpty()).associateBy { it.id }

    var advTitle by remember { mutableStateOf("") }
    var advSummary by remember { mutableStateOf("") }
    var advCorridorId by remember { mutableStateOf(NilgirisReferenceCatalog.corridors.first().id) }
    var advRoadClosed by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Data Freshness Monitor & Role Header
        item {
            EarthWatchCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = NavyElevated,
                borderColor = TealPrimary.copy(alpha = 0.55f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = TealBright)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Admin Control & Data Freshness", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    }
                    Text("Open-Meteo Live Sync: $lastWeatherSyncLabel", style = MaterialTheme.typography.bodySmall, color = TealBright)
                    Text(
                        text = if (isAdminUser) {
                            "Signed in with authorized Admin role — Firestore verification, closures, and advisories enabled."
                        } else {
                            "Read-only preview: Sign in with authorized admin email to modify verification states or official closures."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAdminUser) RiskLowGreen else RiskMediumAmber
                    )

                    if (isAdminUser) {
                        Button(
                            onClick = onAdminSeedDemo,
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDeepest),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("seed_demo_data_button")
                        ) {
                            Text("Seed [SIMULATED DEMO] Advisories & Road States", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Manage Incident Reports
        item {
            Text("Community Report Review Queue (${reports.size})", style = MaterialTheme.typography.titleMedium, color = OffWhite)
        }
        items(reports, key = { it.id }) { rep ->
            EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SourceTagBadge(
                            label = rep.verificationStatus,
                            kind = if (rep.verificationStatus == "CONFIRMED") SourceBadgeKind.OFFICIAL else SourceBadgeKind.UNVERIFIED
                        )
                        Text(rep.observedTimeText, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                    }
                    Text("${rep.corridorName} • ${rep.locationLabel}", style = MaterialTheme.typography.titleSmall, color = PureWhite)
                    Text(rep.description, style = MaterialTheme.typography.bodySmall, color = SlateLight)
                    if (isAdminUser) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onAdminUpdateReport(rep.id, "CONFIRMED", "PUBLIC", "Verified by EarthWatch admin.")
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = RiskLowGreen, contentColor = NavyDeepest),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm")
                            }
                            OutlinedButton(
                                onClick = {
                                    onAdminUpdateReport(rep.id, "RESOLVED", "PUBLIC", "Cleared and resolved.")
                                },
                                enabled = !isSubmitting,
                                border = BorderStroke(1.dp, TealPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Resolve", color = TealBright)
                            }
                            OutlinedButton(
                                onClick = {
                                    onAdminUpdateReport(rep.id, "DISMISSED", "HIDDEN", "Dismissed as duplicate/invalid.")
                                },
                                enabled = !isSubmitting,
                                border = BorderStroke(1.dp, RiskHighRed.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = RiskHighRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dismiss", color = RiskHighRed)
                            }
                        }
                    }
                }
            }
        }

        // Corridor Operational Status Management
        item {
            Text("Corridor Status & Closure Controls", style = MaterialTheme.typography.titleMedium, color = OffWhite)
        }
        items(NilgirisReferenceCatalog.corridors, key = { "admin_corridor_${it.id}" }) { corridor ->
            val currentStatus = statuses[corridor.id]?.status ?: "UNCONFIRMED"
            EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(corridor.name, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                    Text("Current Firestore State: $currentStatus", style = MaterialTheme.typography.labelSmall, color = TealBright)
                    if (isAdminUser) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    onAdminUpdateCorridor(
                                        corridor.id,
                                        CorridorOperationalStatus.OPEN.code,
                                        "Admin verified corridor open for traffic.",
                                        true,
                                        false
                                    )
                                },
                                enabled = !isSubmitting,
                                border = BorderStroke(1.dp, RiskLowGreen)
                            ) { Text("Mark Open", color = RiskLowGreen) }

                            OutlinedButton(
                                onClick = {
                                    onAdminUpdateCorridor(
                                        corridor.id,
                                        CorridorOperationalStatus.CAUTION.code,
                                        "Caution: wet slopes & rockfall watch.",
                                        false,
                                        false
                                    )
                                },
                                enabled = !isSubmitting,
                                border = BorderStroke(1.dp, RiskMediumAmber)
                            ) { Text("Caution", color = RiskMediumAmber) }

                            OutlinedButton(
                                onClick = {
                                    onAdminUpdateCorridor(
                                        corridor.id,
                                        CorridorOperationalStatus.CLOSED_OFFICIAL.code,
                                        "Official closure active.",
                                        false,
                                        false
                                    )
                                },
                                enabled = !isSubmitting,
                                border = BorderStroke(1.dp, RiskHighRed)
                            ) { Text("Close Road", color = RiskHighRed) }
                        }
                    }
                }
            }
        }

        // Quick Official Advisory Publisher
        if (isAdminUser) {
            item {
                EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Publish Official Advisory", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        OutlinedTextField(
                            value = advTitle,
                            onValueChange = { advTitle = it },
                            label = { Text("Advisory Title") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = NavyBorder, focusedTextColor = PureWhite, unfocusedTextColor = OffWhite),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = advSummary,
                            onValueChange = { advSummary = it },
                            label = { Text("Advisory Summary") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = NavyBorder, focusedTextColor = PureWhite, unfocusedTextColor = OffWhite),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mark Road Closed", style = MaterialTheme.typography.bodyMedium, color = OffWhite)
                            Switch(checked = advRoadClosed, onCheckedChange = { advRoadClosed = it })
                        }
                        Button(
                            onClick = {
                                onAdminCreateAdvisory(
                                    advTitle,
                                    advCorridorId,
                                    if (advRoadClosed) "CRITICAL" else "HIGH",
                                    if (advRoadClosed) "OFFICIAL_CLOSURE" else "OFFICIAL_ADVISORY",
                                    "EarthWatch Admin Desk",
                                    advSummary,
                                    advRoadClosed,
                                    false
                                )
                                advTitle = ""
                                advSummary = ""
                            },
                            enabled = !isSubmitting && advTitle.trim().length >= 3 && advSummary.trim().length >= 5,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDeepest),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Publish to Travellers", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
