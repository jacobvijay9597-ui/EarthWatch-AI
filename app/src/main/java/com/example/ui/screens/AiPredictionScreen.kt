package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.core.UiState
import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.local.db.PredictionHistoryEntity
import com.example.data.model.AiRiskBriefing
import com.example.data.model.ChatMessage
import com.example.data.model.DataFreshnessStatus
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.HistoricalLandslideRecord
import com.example.data.model.MlBackendHealthStatus
import com.example.data.model.MlFactorContribution
import com.example.data.model.MlLandslidePredictionResult
import com.example.data.model.MlPredictionInputSnapshot
import com.example.data.model.PredictionExecutionMode
import com.example.data.model.RiskLevel
import com.example.ui.components.EarthWatchCard
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.LoadingTelemetryCard
import com.example.ui.components.Nilgiris3DTerrainSceneCard
import com.example.ui.components.RiskLevelBadge
import com.example.ui.components.SourceBadgeKind
import com.example.ui.components.SourceTagBadge
import java.text.SimpleDateFormat
import java.util.Date
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoContainer
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.InfoCyanContainer
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
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealPrimary
import java.util.Locale

@Composable
fun AiPredictionScreen(
    selectedCorridorId: String,
    riskEstimates: List<ExperimentalRiskEstimate>,
    mlBackendUrl: String,
    mlBackendHealth: MlBackendHealthStatus?,
    isCheckingMlBackend: Boolean,
    selectedDemoScenarioId: String?,
    mlPredictionState: UiState<MlLandslidePredictionResult>,
    aiBriefingState: UiState<AiRiskBriefing?>?,
    recentPredictionLogs: List<PredictionHistoryEntity> = emptyList(),
    sqlHistoricalRecords: List<HistoricalLandslideRecord> = emptyList(),
    chatMessages: List<ChatMessage>,
    isChatStreaming: Boolean,
    chatError: String?,
    onSelectCorridor: (String) -> Unit,
    onBackendUrlChanged: (String) -> Unit,
    onCheckBackendHealth: () -> Unit,
    onSelectDemoScenario: (String?) -> Unit,
    onRunMlPrediction: () -> Unit,
    onRunAiBriefing: () -> Unit,
    onClearSqlLogs: () -> Unit = {},
    onSendChatQuestion: (String) -> Unit
) {
    val selectedCorridor = NilgirisReferenceCatalog.corridorById(selectedCorridorId)
    val selectedEstimate = riskEstimates.firstOrNull { it.corridorId == selectedCorridorId }
    val historicalRecords = remember(selectedCorridorId, sqlHistoricalRecords) {
        sqlHistoricalRecords.ifEmpty {
            NilgirisReferenceCatalog.simulatedHistoricalRecords.filter { it.corridorId == selectedCorridorId }
        }
    }
    var chatInput by remember { mutableStateOf("") }

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
                        text = "Landslide Risk Prediction Lab",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite
                    )
                    Text(
                        text = "Random Forest ML model analysing rainfall intensity & duration, soil moisture, slope angle, elevation & historical records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                }
            }

            // Corridor Selector Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Select Nilgiris Hill Corridor",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateLight
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NilgirisReferenceCatalog.corridors.forEach { c ->
                            FilterChip(
                                selected = c.id == selectedCorridorId,
                                onClick = { onSelectCorridor(c.id) },
                                label = { Text(c.name.substringBefore(" Ghat")) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = NavyDeepest,
                                    containerColor = NavyCard,
                                    labelColor = SlateLight
                                ),
                                modifier = Modifier.testTag("ai_corridor_chip_${c.id}")
                            )
                        }
                    }
                }
            }

            // Input Source Selector: Live Open-Meteo Telemetry vs Curated Demonstration Scenarios
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Input Telemetry Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateLight
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDemoScenarioId == null,
                            onClick = { onSelectDemoScenario(null) },
                            label = { Text("Live Open-Meteo") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InfoCyan,
                                selectedLabelColor = NavyDeepest,
                                containerColor = NavyCard,
                                labelColor = SlateLight
                            ),
                            modifier = Modifier.testTag("demo_scenario_chip_live")
                        )
                        NilgirisReferenceCatalog.demoScenarios.forEach { preset ->
                            FilterChip(
                                selected = selectedDemoScenarioId == preset.id,
                                onClick = { onSelectDemoScenario(preset.id) },
                                label = { Text(preset.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DemoAmber,
                                    selectedLabelColor = NavyDeepest,
                                    containerColor = NavyCard,
                                    labelColor = SlateLight
                                ),
                                modifier = Modifier.testTag("demo_scenario_chip_${preset.id}")
                            )
                        }
                    }
                }
            }

            // Random Forest Prediction Result Card
            item {
                when (mlPredictionState) {
                    is UiState.Loading -> LoadingTelemetryCard("Running Random Forest landslide risk evaluation...")
                    is UiState.Error -> ErrorBannerCard(
                        message = mlPredictionState.message,
                        onRetry = onRunMlPrediction
                    )
                    is UiState.Success -> {
                        RandomForestPredictionCard(
                            result = mlPredictionState.data,
                            onRerun = onRunMlPrediction
                        )
                    }
                }
            }

            // Interactive 3D Slope & Geological Strata Scene
            item {
                val activePrediction = (mlPredictionState as? UiState.Success)?.data
                Nilgiris3DTerrainSceneCard(
                    corridor = selectedCorridor,
                    rainfallIntensityMmHr = activePrediction?.inputSnapshot?.rainfallIntensityMmHr,
                    soilMoistureM3M3 = activePrediction?.inputSnapshot?.soilMoistureM3M3,
                    riskLevel = activePrediction?.riskCategory ?: selectedEstimate?.riskLevel,
                    estimatedProbability = activePrediction?.estimatedProbability
                )
            }

            // Decoupled Python Random Forest Backend Connection Card
            item {
                PythonMlBackendConnectionCard(
                    backendUrl = mlBackendUrl,
                    health = mlBackendHealth,
                    isChecking = isCheckingMlBackend,
                    onUrlChanged = onBackendUrlChanged,
                    onTestAndSync = onCheckBackendHealth
                )
            }

            // Heuristic Baseline Factor Comparison Card
            item {
                if (selectedEstimate != null) {
                    EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    SourceTagBadge("HEURISTIC BASELINE (CLIENT)", SourceBadgeKind.EXPERIMENTAL)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(selectedEstimate.corridorName, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                }
                                RiskLevelBadge(level = selectedEstimate.riskLevel)
                            }
                            Text(
                                text = "Heuristic Reference Index: ${selectedEstimate.riskScore} / 100",
                                style = MaterialTheme.typography.labelMedium,
                                color = TealBright
                            )
                            FactorBar("Rainfall Intensity & 72h Duration", selectedEstimate.rainfallIntensityScore, 35)
                            FactorBar("Soil Moisture Saturation (0–7cm)", selectedEstimate.soilMoistureScore, 25)
                            FactorBar("Terrain Slope & Geology (${selectedCorridor.maxSlopeDegrees}°)", selectedEstimate.terrainSlopeScore, 25)
                            FactorBar("Historical Landslide Frequency", selectedEstimate.historicalFactorScore, 15)
                        }
                    }
                }
            }

            // Gemini AI Risk Explanation
            item {
                EarthWatchCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NavyElevated,
                    borderColor = TealPrimary.copy(alpha = 0.55f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TealBright, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini AI Risk Explanation",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                        }
                        Text(
                            text = "Generates a human-readable safety briefing from live Open-Meteo telemetry and terrain metadata.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateLight
                        )
                        Button(
                            onClick = onRunAiBriefing,
                            enabled = aiBriefingState !is UiState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDeepest),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_gemini_analysis_button")
                        ) {
                            if (aiBriefingState is UiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NavyDeepest, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Telemetry...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Explain Risk with Gemini AI", fontWeight = FontWeight.Bold)
                            }
                        }

                        when (aiBriefingState) {
                            is UiState.Error -> ErrorBannerCard(message = aiBriefingState.message)
                            is UiState.Success -> {
                                val briefing = aiBriefingState.data
                                if (briefing != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = NavyCard,
                                        border = BorderStroke(1.dp, NavyBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            SourceTagBadge("AI EXPERIMENTAL ESTIMATE", SourceBadgeKind.EXPERIMENTAL)
                                            Text(briefing.summary, style = MaterialTheme.typography.bodyMedium, color = PureWhite)
                                            briefing.keyFactors.forEach { kf ->
                                                Text("• $kf", style = MaterialTheme.typography.bodySmall, color = SlateLight)
                                            }
                                            Text(
                                                text = "Guidance: ${briefing.travellerAdvice}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TealBright
                                            )
                                            Text(
                                                text = briefing.experimentalDisclaimer,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SlateMuted
                                            )
                                        }
                                    }
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }

            // Local SQLite (Room) Prediction Audit Log
            item {
                LocalSqlPredictionAuditCard(
                    logs = recentPredictionLogs,
                    onClearLogs = onClearSqlLogs
                )
            }

            // Historical Landslide Records (SQLite Room + Demo Reference)
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = InfoCyan, modifier = Modifier.size(18.dp))
                    Text("Historical Landslide SQL Catalog", style = MaterialTheme.typography.titleMedium, color = OffWhite)
                    SourceTagBadge("ROOM SQLITE", SourceBadgeKind.LIVE_API)
                }
            }
            items(historicalRecords, key = { it.id }) { rec ->
                EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${rec.year} • ${rec.locationLabel}", style = MaterialTheme.typography.titleSmall, color = PureWhite, modifier = Modifier.weight(1f))
                            RiskLevelBadge(level = rec.severity)
                        }
                        Text(rec.triggerSummary, style = MaterialTheme.typography.bodySmall, color = SlateLight)
                    }
                }
            }

            // Interactive EarthWatch AI Traveller Safety Assistant
            item {
                Text(
                    text = "EarthWatch AI Traveller Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    color = OffWhite
                )
            }
            item {
                EarthWatchCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chatMessages.forEach { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (msg.isUser) TealContainer else NavyElevated,
                                border = BorderStroke(1.dp, if (msg.isUser) TealPrimary.copy(alpha = 0.5f) else NavyBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (msg.isUser) "YOU" else "EARTHWATCH AI ASSISTANT (EXPERIMENTAL)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (msg.isUser) TealBright else InfoCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.text.ifBlank { if (msg.isStreaming) "Thinking..." else "" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OffWhite
                                    )
                                }
                            }
                        }

                        if (chatError != null) {
                            ErrorBannerCard(message = chatError)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Ask about Ooty/Coonoor ghat conditions...", color = SlateMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = NavyBorder,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = OffWhite
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("assistant_chat_input")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val q = chatInput
                                    chatInput = ""
                                    onSendChatQuestion(q)
                                },
                                enabled = !isChatStreaming && chatInput.isNotBlank(),
                                modifier = Modifier.testTag("assistant_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send question to EarthWatch AI Assistant",
                                    tint = TealBright
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RandomForestPredictionCard(
    result: MlLandslidePredictionResult,
    onRerun: () -> Unit
) {
    val isConnectedRf = result.executionMode == PredictionExecutionMode.CONNECTED_PYTHON_RF_API &&
        result.isValidatedModel
    val bannerColor = if (isConnectedRf) RiskLowContainer.copy(alpha = 0.55f) else DemoContainer.copy(alpha = 0.65f)
    val bannerBorder = if (isConnectedRf) RiskLowGreen.copy(alpha = 0.7f) else DemoAmber.copy(alpha = 0.75f)
    val probPctText = result.estimatedProbability?.let {
        String.format(Locale.US, "%.1f%%", it * 100.0)
    } ?: "WITHHELD"

    EarthWatchCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rf_prediction_card"),
        borderColor = if (isConnectedRf) TealPrimary else DemoAmber.copy(alpha = 0.7f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Execution Mode Banner (Connected Python RF vs Demonstration Mode)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = bannerColor,
                border = BorderStroke(1.dp, bannerBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rf_mode_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isConnectedRf) Icons.Default.CloudDone else Icons.Default.Science,
                        contentDescription = result.executionMode.badgeLabel,
                        tint = if (isConnectedRf) RiskLowGreen else DemoAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SourceTagBadge(
                            label = result.executionMode.badgeLabel,
                            kind = if (isConnectedRf) SourceBadgeKind.LIVE_API else SourceBadgeKind.SIMULATED_DEMO
                        )
                        Text(
                            text = result.explanationNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = OffWhite
                        )
                    }
                }
            }

            // Probability & Risk Category Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Estimated Landslide Probability",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted
                    )
                    Text(
                        text = probPctText,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        modifier = Modifier.testTag("rf_probability_text")
                    )
                    Text(
                        text = "${result.modelName} (${result.modelVersion})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TealBright
                    )
                }
                val category = result.riskCategory
                if (category != null) {
                    RiskLevelBadge(level = category, modifier = Modifier.testTag("rf_risk_badge"))
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RiskMediumContainer,
                        border = BorderStroke(1.dp, RiskMediumAmber.copy(alpha = 0.65f)),
                        modifier = Modifier.testTag("rf_risk_badge_withheld")
                    ) {
                        Text(
                            text = "UNCONFIRMED",
                            color = RiskMediumAmber,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Data Freshness Banner
            val (freshBg, freshFg) = when (result.dataFreshnessStatus) {
                DataFreshnessStatus.FRESH -> RiskLowContainer to RiskLowGreen
                DataFreshnessStatus.AGING -> RiskMediumContainer to RiskMediumAmber
                DataFreshnessStatus.STALE -> RiskHighContainer to RiskHighRed
                DataFreshnessStatus.MISSING -> DemoContainer to DemoAmber
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NavyElevated,
                border = BorderStroke(1.dp, freshFg.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = freshBg
                    ) {
                        Text(
                            text = result.dataFreshnessStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = freshFg,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = result.freshnessWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateLight
                    )
                }
            }

            // Missing-Data Warnings
            if (result.missingDataWarnings.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RiskMediumContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, RiskMediumAmber.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rf_missing_warnings_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Data warnings",
                                tint = RiskMediumAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Data Quality & Missing-Sensor Warnings",
                                style = MaterialTheme.typography.labelMedium,
                                color = RiskMediumAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        result.missingDataWarnings.forEach { warning ->
                            Text("• $warning", style = MaterialTheme.typography.bodySmall, color = OffWhite)
                        }
                    }
                }
            }

            // 6-Feature Input Snapshot Grid
            FeatureInputSnapshotGrid(snapshot = result.inputSnapshot)

            // Contributing Factors (Feature Importance Weights)
            if (result.contributingFactors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Contributing Factors (Feature Importance)",
                        style = MaterialTheme.typography.titleSmall,
                        color = PureWhite
                    )
                    result.contributingFactors.forEach { factor ->
                        ContributionFactorRow(factor)
                    }
                }
            }

            OutlinedButton(
                onClick = onRerun,
                border = BorderStroke(1.dp, TealPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TealBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_rf_prediction_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Re-evaluate Random Forest Risk", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FeatureInputSnapshotGrid(snapshot: MlPredictionInputSnapshot) {
    val rainIntLabel = snapshot.rainfallIntensityMmHr?.let {
        String.format(Locale.US, "%.1f mm/h", it)
    } ?: "Missing"
    val rainDurLabel = snapshot.rainfallDurationHours?.let {
        String.format(Locale.US, "%.0f hrs (72h)", it)
    } ?: "Missing"
    val soilLabel = snapshot.soilMoistureM3M3?.let {
        String.format(Locale.US, "%.3f m³/m³", it)
    } ?: "Unavailable"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "6 Random Forest Input Features (${snapshot.telemetryTimestampIso ?: "No sync"})",
            style = MaterialTheme.typography.titleSmall,
            color = PureWhite
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeatureTile("1. Rainfall Intensity", rainIntLabel, Modifier.weight(1f))
            FeatureTile("2. Rainfall Duration", rainDurLabel, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeatureTile("3. Soil Moisture", soilLabel, Modifier.weight(1f))
            FeatureTile("4. Slope Angle", "${snapshot.slopeAngleDeg}° max", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeatureTile("5. Elevation", "${snapshot.terrainElevationMeters} m", Modifier.weight(1f))
            FeatureTile("6. Historical Events", "${snapshot.historicalLandslideCount} records", Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeatureTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = NavyElevated,
        border = BorderStroke(1.dp, NavyBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PureWhite)
        }
    }
}

@Composable
private fun ContributionFactorRow(factor: MlFactorContribution) {
    val pct = (factor.importanceWeight * 100f).coerceIn(0f, 100f)
    val barColor = when {
        factor.impactDirection.contains("Elevating", ignoreCase = true) -> RiskMediumAmber
        factor.impactDirection.contains("Missing", ignoreCase = true) -> DemoAmber
        else -> TealPrimary
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${factor.featureName} (${factor.observedValueLabel})",
                style = MaterialTheme.typography.bodySmall,
                color = OffWhite,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = String.format(Locale.US, "%.0f%% • %s", pct, factor.impactDirection),
                style = MaterialTheme.typography.labelSmall,
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = { factor.importanceWeight.coerceIn(0f, 1f) },
            color = barColor,
            trackColor = NavyDeepest,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
    }
}

@Composable
private fun PythonMlBackendConnectionCard(
    backendUrl: String,
    health: MlBackendHealthStatus?,
    isChecking: Boolean,
    onUrlChanged: (String) -> Unit,
    onTestAndSync: () -> Unit
) {
    val connected = health?.isConnected == true && health.isValidatedModel
    EarthWatchCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = NavyCard,
        borderColor = if (connected) RiskLowGreen.copy(alpha = 0.55f) else NavyBorder
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (connected) Icons.Default.Hub else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (connected) RiskLowGreen else InfoCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Python Random Forest Backend API",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                }
                SourceTagBadge(
                    label = if (connected) "API CONNECTED" else "API OFFLINE",
                    kind = if (connected) SourceBadgeKind.LIVE_API else SourceBadgeKind.OFFICIAL
                )
            }

            Text(
                text = "Keeps scikit-learn Random Forest training & inference separate from the frontend. Default emulator host: http://10.0.2.2:8000/ (or HTTPS backend URL).",
                style = MaterialTheme.typography.bodySmall,
                color = SlateMuted
            )

            if (health != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (connected) RiskLowContainer.copy(alpha = 0.4f) else InfoCyanContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (connected) RiskLowGreen.copy(alpha = 0.5f) else NavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(health.statusDetail, style = MaterialTheme.typography.bodySmall, color = OffWhite)
                        if (connected) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Model: %s (%s) • Dataset: %d records • 5-Fold CV Accuracy: %.1f%%",
                                    health.modelName,
                                    health.modelVersion,
                                    health.datasetRows,
                                    health.cvAccuracyMean * 100.0
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TealBright
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = backendUrl,
                onValueChange = onUrlChanged,
                label = { Text("Python FastAPI Base URL") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = NavyBorder,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = OffWhite,
                    focusedLabelColor = TealBright,
                    unfocusedLabelColor = SlateMuted
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ml_backend_url_input")
            )

            Button(
                onClick = onTestAndSync,
                enabled = !isChecking,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDeepest),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_ml_backend_button")
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NavyDeepest, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting to Python ML API...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect & Sync Python RF API", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FactorBar(
    label: String,
    score: Int,
    maxScore: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = OffWhite)
            Text("$score / $maxScore", style = MaterialTheme.typography.labelSmall, color = TealBright)
        }
        LinearProgressIndicator(
            progress = { (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f) },
            color = TealPrimary,
            trackColor = NavyDeepest,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
        )
    }
}

@Composable
private fun LocalSqlPredictionAuditCard(
    logs: List<PredictionHistoryEntity>,
    onClearLogs: () -> Unit
) {
    EarthWatchCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("local_sql_audit_card"),
        containerColor = NavyCard,
        borderColor = InfoCyan.copy(alpha = 0.45f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = InfoCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Local SQL Prediction Audit Log", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text("Room SQLite cache & history (Firestore holds cloud reports)", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                    }
                }
                if (logs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearLogs,
                        border = BorderStroke(1.dp, NavyBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear SQL logs", tint = SlateLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", style = MaterialTheme.typography.labelSmall, color = SlateLight)
                    }
                }
            }

            if (logs.isEmpty()) {
                Text(
                    text = "No risk evaluations logged in local SQLite yet. Run an assessment above to record an audit entry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateLight
                )
            } else {
                val formatter = remember { SimpleDateFormat("dd MMM, HH:mm:ss", Locale.US) }
                logs.take(5).forEach { entry ->
                    val probText = entry.estimatedProbability?.let { String.format(Locale.US, "%.0f%%", it * 100.0) } ?: "--"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NavyElevated,
                        border = BorderStroke(1.dp, NavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(entry.corridorName, style = MaterialTheme.typography.labelLarge, color = PureWhite)
                                Text(
                                    text = "${entry.executionMode} • ${formatter.format(Date(entry.evaluatedAtEpochMillis))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(probText, style = MaterialTheme.typography.titleSmall, color = TealBright, fontWeight = FontWeight.Bold)
                                RiskLevelBadge(level = RiskLevel.safeFrom(entry.riskCategory))
                            }
                        }
                    }
                }
            }
        }
    }
}
