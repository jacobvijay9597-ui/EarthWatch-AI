package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Thermostat
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.UiState
import com.example.data.model.CommunityReport
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.OfficialAdvisory
import com.example.data.model.RiskLevel
import com.example.ui.components.EarthWatchCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingTelemetryCard
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
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealPrimary
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    weatherState: UiState<List<LiveWeatherTelemetry>>,
    lastSyncLabel: String,
    riskEstimates: List<ExperimentalRiskEstimate>,
    advisoriesState: UiState<List<OfficialAdvisory>>,
    reportsState: UiState<List<CommunityReport>>,
    onRefreshWeather: () -> Unit,
    onNavigateRouteChecker: () -> Unit,
    onNavigateAiPrediction: () -> Unit,
    onNavigateAlerts: () -> Unit,
    onSignOut: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Brand & Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_nilgiris_hero),
                        contentDescription = "Nilgiris hills telemetry banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        NavyDeepest.copy(alpha = 0.92f),
                                        NavyDeepest.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "EarthWatch AI Logo",
                                    tint = TealBright,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EARTHWATCH AI",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureWhite
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onRefreshWeather,
                                    modifier = Modifier.testTag("home_refresh_weather_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh live weather",
                                        tint = TealBright
                                    )
                                }
                                OutlinedButton(
                                    onClick = onSignOut,
                                    border = BorderStroke(1.dp, NavyBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Text("Sign Out", style = MaterialTheme.typography.labelSmall, color = SlateLight)
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "Predict Before It Happens. Protect Every Journey.",
                                style = MaterialTheme.typography.titleMedium,
                                color = TealBright
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sector: Coimbatore • Ooty • Coonoor • Mettupalayam • Kalhatti • Gudalur",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateLight
                            )
                        }
                    }
                }
            }

            // Quick Route-Checking Action Card (Module 1 -> Module 2)
            item {
                EarthWatchCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NavyElevated,
                    borderColor = TealPrimary.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quick Travel Route Check",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Inspect monitored ghat hotspots, rainfall & closures before ascending.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateLight
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onNavigateRouteChecker,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDeepest),
                            modifier = Modifier.testTag("quick_route_check_button")
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Live Weather & Rainfall Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Nilgiris Weather & Rainfall",
                            style = MaterialTheme.typography.titleLarge,
                            color = OffWhite
                        )
                        Text(
                            text = "Source: Open-Meteo Live API • Synced: $lastSyncLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                    SourceTagBadge(label = "LIVE TELEMETRY", kind = SourceBadgeKind.LIVE_API)
                }
            }

            item {
                when (weatherState) {
                    is UiState.Loading -> LoadingTelemetryCard("Fetching live Open-Meteo rainfall & soil moisture...")
                    is UiState.Error -> ErrorBannerCard(message = weatherState.message, onRetry = onRefreshWeather)
                    is UiState.Success -> {
                        val locations = weatherState.data
                        val peakRain24h = locations.maxOfOrNull { it.rain24hMm } ?: 0.0
                        val peakRain72h = locations.maxOfOrNull { it.rain72hMm } ?: 0.0
                        val peakSoil = locations.mapNotNull { it.soilMoistureM3M3 }.maxOrNull()

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SummaryStatBox(
                                    label = "Peak 24h Rain",
                                    value = String.format(Locale.US, "%.1f mm", peakRain24h),
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryStatBox(
                                    label = "Peak 72h Rain",
                                    value = String.format(Locale.US, "%.1f mm", peakRain72h),
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryStatBox(
                                    label = "Max Soil Sat.",
                                    value = peakSoil?.let { String.format(Locale.US, "%.2f m³", it) } ?: "N/A",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(locations, key = { it.locationId }) { w ->
                                    EarthWatchCard(
                                        modifier = Modifier.width(205.dp),
                                        containerColor = NavyCard
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = w.locationName.substringBefore(" ("),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = PureWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = w.weatherLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TealBright
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Thermostat, contentDescription = null, tint = InfoCyan, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = String.format(Locale.US, "%.1f°C", w.temperatureC),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = OffWhite
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = TealBright, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = String.format(Locale.US, "%.1f mm/h", w.rainCurrentMm),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = OffWhite
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val soilText = w.soilMoistureM3M3?.let { String.format(Locale.US, "%.3f", it) } ?: "N/A"
                                            Text(
                                                text = String.format(
                                                    Locale.US,
                                                    "24h: %.1fmm • Soil: %s",
                                                    w.rain24hMm,
                                                    soilText
                                                ),
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
            }

            // Landslide Risk Overview (Experimental)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Corridor Risk Overview",
                            style = MaterialTheme.typography.titleLarge,
                            color = OffWhite
                        )
                        Text(
                            text = "Experimental estimates from live rainfall + terrain slope",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateAiPrediction,
                        border = BorderStroke(1.dp, TealPrimary),
                        modifier = Modifier.testTag("open_ai_prediction_button")
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = TealBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Lab", color = TealBright)
                    }
                }
            }

            if (riskEstimates.isNotEmpty()) {
                items(riskEstimates, key = { it.corridorId }) { est ->
                    EarthWatchCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateAiPrediction
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SourceTagBadge(
                                        label = "EXPERIMENTAL ESTIMATE",
                                        kind = SourceBadgeKind.EXPERIMENTAL
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Score ${est.riskScore}/100",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SlateLight
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = est.corridorName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PureWhite
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            RiskLevelBadge(level = est.riskLevel)
                        }
                    }
                }
            }

            // Latest Alerts & Community Incidents Preview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Latest Alerts & Reports",
                        style = MaterialTheme.typography.titleLarge,
                        color = OffWhite
                    )
                    OutlinedButton(
                        onClick = onNavigateAlerts,
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier.testTag("view_all_alerts_button")
                    ) {
                        Text("Manage", color = TealBright)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TealBright, modifier = Modifier.size(15.dp))
                    }
                }
            }

            item {
                val advisories = (advisoriesState as? UiState.Success)?.data.orEmpty()
                val reports = (reportsState as? UiState.Success)?.data.orEmpty()
                val highRiskEstimates = riskEstimates.filter { it.riskLevel != RiskLevel.LOW }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (advisories.isEmpty() && reports.isEmpty() && highRiskEstimates.isEmpty()) {
                        EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TealBright)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "No active official closures or community hazard reports in Firestore right now.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateLight
                                )
                            }
                        }
                    }

                    advisories.take(2).forEach { adv ->
                        EarthWatchCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (adv.roadClosed) RiskHighRed.copy(alpha = 0.65f) else InfoCyan.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SourceTagBadge(
                                        label = if (adv.roadClosed) "OFFICIAL CLOSURE" else "OFFICIAL ADVISORY",
                                        kind = SourceBadgeKind.OFFICIAL
                                    )
                                    if (adv.simulatedDemo) {
                                        SourceTagBadge(label = "SIMULATED DEMO", kind = SourceBadgeKind.SIMULATED_DEMO)
                                    }
                                }
                                Text(adv.title, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                                Text(adv.summary, style = MaterialTheme.typography.bodySmall, color = SlateLight)
                            }
                        }
                    }

                    reports.take(2).forEach { rep ->
                        EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SourceTagBadge(
                                        label = if (rep.verificationStatus == "CONFIRMED") "CONFIRMED INCIDENT" else "UNVERIFIED REPORT",
                                        kind = if (rep.verificationStatus == "CONFIRMED") SourceBadgeKind.OFFICIAL else SourceBadgeKind.UNVERIFIED
                                    )
                                    Text(rep.observedTimeText, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                }
                                Text("${rep.corridorName} • ${rep.locationLabel}", style = MaterialTheme.typography.titleSmall, color = PureWhite)
                                Text(rep.description, style = MaterialTheme.typography.bodySmall, color = SlateLight)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = NavyCard,
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = TealBright, fontWeight = FontWeight.Bold)
        }
    }
}
