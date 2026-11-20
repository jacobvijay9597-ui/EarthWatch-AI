package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NilgirisReferenceCatalog
import com.example.data.location.UserLocationTracker
import com.example.data.model.CommunityReport
import com.example.data.model.CorridorOperationalStatus
import com.example.data.model.ExperimentalRiskEstimate
import com.example.data.model.LiveWeatherTelemetry
import com.example.data.model.MonitoredCorridor
import com.example.data.model.OfficialAdvisory
import com.example.data.model.RiskLevel
import com.example.data.model.RoadCorridorStatus
import com.example.data.model.UserLocationSnapshot
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoContainer
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.InfoCyanContainer
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
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
import com.example.ui.theme.UnverifiedViolet
import com.example.ui.theme.UnverifiedVioletContainer
import kotlin.math.hypot

enum class SourceBadgeKind {
    OFFICIAL,
    EXPERIMENTAL,
    UNVERIFIED,
    SIMULATED_DEMO,
    LIVE_API
}

@Composable
fun RiskLevelBadge(
    level: RiskLevel,
    modifier: Modifier = Modifier,
    prefix: String = ""
) {
    val (bg, fg) = when (level) {
        RiskLevel.LOW -> RiskLowContainer to RiskLowGreen
        RiskLevel.MEDIUM -> RiskMediumContainer to RiskMediumAmber
        RiskLevel.HIGH -> RiskHighContainer to RiskHighRed
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.55f))
    ) {
        Text(
            text = if (prefix.isNotBlank()) "$prefix ${level.label}" else "${level.label} RISK",
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SourceTagBadge(
    label: String,
    kind: SourceBadgeKind,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (kind) {
        SourceBadgeKind.OFFICIAL -> InfoCyanContainer to InfoCyan
        SourceBadgeKind.EXPERIMENTAL -> TealContainer to TealBright
        SourceBadgeKind.UNVERIFIED -> UnverifiedVioletContainer to UnverifiedViolet
        SourceBadgeKind.SIMULATED_DEMO -> DemoContainer to DemoAmber
        SourceBadgeKind.LIVE_API -> RiskLowContainer to RiskLowGreen
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EarthWatchCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NavyBorder,
    containerColor: Color = NavyCard,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun LoadingTelemetryCard(
    title: String,
    modifier: Modifier = Modifier
) {
    EarthWatchCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = TealPrimary,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SlateLight
            )
        }
    }
}

@Composable
fun ErrorBannerCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EarthWatchCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = RiskHighRed.copy(alpha = 0.6f),
        containerColor = RiskHighContainer.copy(alpha = 0.45f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error notice",
                    tint = RiskHighRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OffWhite,
                    modifier = Modifier.weight(1f)
                )
            }
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDeepest),
                    modifier = Modifier.testTag("retry_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Live Sync")
                }
            }
        }
    }
}

@Composable
fun InteractiveNilgirisMapCanvas(
    corridors: List<MonitoredCorridor>,
    estimatesByCorridor: Map<String, ExperimentalRiskEstimate>,
    statusesByCorridor: Map<String, RoadCorridorStatus>,
    reports: List<CommunityReport>,
    advisories: List<OfficialAdvisory>,
    highlightedCorridorId: String?,
    onSelectCorridor: (String) -> Unit,
    modifier: Modifier = Modifier,
    weatherList: List<LiveWeatherTelemetry> = emptyList(),
    showPredictedRisk: Boolean = true,
    showOfficialClosures: Boolean = true,
    userLocation: UserLocationSnapshot? = null
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val weatherMap = remember(weatherList) { weatherList.associateBy { it.locationId } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(NavyElevated, NavySurface, NavyDeepest)
                )
            )
            .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(corridors, zoomScale) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val cx = w / 2f
                        val cy = h / 2f
                        val normX = ((tapOffset.x - cx) / zoomScale + cx) / w
                        val normY = ((tapOffset.y - cy) / zoomScale + cy) / h

                        var bestCorridorId: String? = null
                        var bestDist = Float.MAX_VALUE
                        for (c in corridors) {
                            for (pt in c.mapPolyline) {
                                val d = hypot(pt.first - normX, pt.second - normY)
                                if (d < bestDist) {
                                    bestDist = d
                                    bestCorridorId = c.id
                                }
                            }
                        }
                        if (bestCorridorId != null && bestDist < 0.14f) {
                            onSelectCorridor(bestCorridorId)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            fun mapPt(nx: Float, ny: Float): Offset = Offset(
                x = cx + (nx * w - cx) * zoomScale,
                y = cy + (ny * h - cy) * zoomScale
            )

            // 1. Subtle topographic grid & elevation rings around Ooty / Doddabetta plateau
            val gridColor = NavyBorder.copy(alpha = 0.25f)
            for (i in 1..5) {
                val xPos = w * i / 6f
                val yPos = h * i / 6f
                drawLine(gridColor, Offset(xPos, 0f), Offset(xPos, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, yPos), Offset(w, yPos), strokeWidth = 1f)
            }
            val plateauCenter = mapPt(0.50f, 0.48f)
            listOf(0.14f, 0.26f, 0.38f).forEach { r ->
                drawCircle(
                    color = TealPrimary.copy(alpha = 0.10f),
                    radius = minOf(w, h) * r * zoomScale,
                    center = plateauCenter,
                    style = Stroke(width = 1.5f)
                )
            }

            // 2. Monitored road corridor polylines
            corridors.forEach { corridor ->
                val estimate = estimatesByCorridor[corridor.id]
                val status = statusesByCorridor[corridor.id]
                val isSelected = corridor.id == highlightedCorridorId
                val isClosed = status?.status == CorridorOperationalStatus.CLOSED_OFFICIAL.code ||
                    advisories.any { it.corridorId == corridor.id && it.roadClosed }

                val lineColor = when {
                    isClosed && showOfficialClosures -> RiskHighRed
                    !showPredictedRisk -> SlateMuted.copy(alpha = 0.6f)
                    estimate?.riskLevel == RiskLevel.HIGH -> RiskHighRed
                    estimate?.riskLevel == RiskLevel.MEDIUM -> RiskMediumAmber
                    estimate?.riskLevel == RiskLevel.LOW -> RiskLowGreen
                    else -> TealPrimary
                }

                val path = Path()
                corridor.mapPolyline.forEachIndexed { index, pair ->
                    val pt = mapPt(pair.first, pair.second)
                    if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }

                if (isSelected) {
                    drawPath(
                        path = path,
                        color = PureWhite.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 16f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = if (isSelected) 9f else 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (isClosed && showOfficialClosures) PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f) else null
                    )
                )

                // Monitored spot experimental risk markers (triangle)
                if (showPredictedRisk) {
                    corridor.monitoredSpots.forEach { spot ->
                        val sp = mapPt(spot.mapX, spot.mapY)
                        val triSize = if (isSelected) 11f else 8f
                        val tri = Path().apply {
                            moveTo(sp.x, sp.y - triSize)
                            lineTo(sp.x - triSize, sp.y + triSize * 0.8f)
                            lineTo(sp.x + triSize, sp.y + triSize * 0.8f)
                            close()
                        }
                        drawPath(tri, color = lineColor)
                    }
                }
            }

            // 3. Community reports (unverified = violet square; confirmed = coral diamond)
            reports.filter { it.verificationStatus != "DISMISSED" && it.verificationStatus != "RESOLVED" }
                .forEachIndexed { idx, rep ->
                    val corridor = NilgirisReferenceCatalog.corridorById(rep.corridorId)
                    val midIdx = (corridor.mapPolyline.size / 2).coerceAtMost(corridor.mapPolyline.lastIndex)
                    val base = corridor.mapPolyline[midIdx]
                    val jitter = ((idx % 3) - 1) * 0.022f
                    val pt = mapPt(base.first + jitter, base.second - jitter)
                    if (rep.verificationStatus == "CONFIRMED") {
                        val d = 13f
                        val diamond = Path().apply {
                            moveTo(pt.x, pt.y - d)
                            lineTo(pt.x + d, pt.y)
                            lineTo(pt.x, pt.y + d)
                            lineTo(pt.x - d, pt.y)
                            close()
                        }
                        drawPath(diamond, color = RiskHighRed)
                        drawPath(diamond, color = PureWhite, style = Stroke(width = 2f))
                    } else {
                        drawRect(
                            color = UnverifiedViolet,
                            topLeft = Offset(pt.x - 9f, pt.y - 9f),
                            size = Size(18f, 18f)
                        )
                        drawRect(
                            color = PureWhite,
                            topLeft = Offset(pt.x - 9f, pt.y - 9f),
                            size = Size(18f, 18f),
                            style = Stroke(width = 2f)
                        )
                    }
                }

            // 4. Nilgiris town / elevation & rainfall nodes
            NilgirisReferenceCatalog.locations.forEach { loc ->
                val center = mapPt(loc.mapX, loc.mapY)
                val telemetry = weatherMap[loc.id]
                if (telemetry != null && telemetry.rain24hMm > 0.5) {
                    drawCircle(
                        color = InfoCyan.copy(alpha = 0.18f),
                        radius = (18f + telemetry.rain24hMm.toFloat().coerceAtMost(40f)) * zoomScale,
                        center = center
                    )
                }
                drawCircle(
                    color = NavyDeepest,
                    radius = 12f,
                    center = center
                )
                drawCircle(
                    color = if (telemetry != null) InfoCyan else TealBright,
                    radius = 9f,
                    center = center,
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = PureWhite,
                    radius = 4f,
                    center = center
                )

                val shortLabel = loc.name.substringBefore(" (")
                val rainSuffix = telemetry?.let {
                    String.format(java.util.Locale.US, " %.1fmm", it.rainCurrentMm)
                } ?: ""
                val measured = textMeasurer.measure(
                    text = "$shortLabel$rainSuffix",
                    style = TextStyle(
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                val labelX = (center.x - measured.size.width / 2f)
                    .coerceIn(8f, (w - measured.size.width - 8f).coerceAtLeast(8f))
                val labelY = (center.y + 14f)
                    .coerceIn(8f, (h - measured.size.height - 6f).coerceAtLeast(8f))

                drawRect(
                    color = NavyDeepest.copy(alpha = 0.82f),
                    topLeft = Offset(labelX - 4f, labelY - 2f),
                    size = Size(measured.size.width + 8f, measured.size.height + 4f)
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(labelX, labelY)
                )
            }

            // 5. Real-Time User GPS Position Marker (when within Nilgiris sector)
            userLocation?.let { loc ->
                val projected = UserLocationTracker.projectLatLngToNilgirisSector(loc.latitude, loc.longitude)
                if (projected != null) {
                    val userCenter = mapPt(projected.first, projected.second)
                    val accuracyRadiusPx = (14f + (loc.accuracyMeters / 30f)).coerceIn(16f, 44f) * zoomScale
                    drawCircle(
                        color = TealBright.copy(alpha = 0.24f),
                        radius = accuracyRadiusPx,
                        center = userCenter
                    )
                    drawCircle(
                        color = TealBright.copy(alpha = 0.7f),
                        radius = accuracyRadiusPx,
                        center = userCenter,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = PureWhite,
                        radius = 10f,
                        center = userCenter
                    )
                    drawCircle(
                        color = InfoCyan,
                        radius = 7f,
                        center = userCenter
                    )
                    val youLabel = textMeasurer.measure(
                        text = if (loc.isFinePrecision) "YOU (GPS)" else "YOU (~APPROX)",
                        style = TextStyle(
                            color = NavyDeepest,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    val pillW = youLabel.size.width + 10f
                    val pillH = youLabel.size.height + 4f
                    val pillX = (userCenter.x - pillW / 2f).coerceIn(6f, (w - pillW - 6f).coerceAtLeast(6f))
                    val pillY = (userCenter.y - pillH - 12f).coerceIn(6f, (h - pillH - 6f).coerceAtLeast(6f))
                    drawRect(
                        color = TealBright,
                        topLeft = Offset(pillX, pillY),
                        size = Size(pillW, pillH)
                    )
                    drawText(
                        textLayoutResult = youLabel,
                        topLeft = Offset(pillX + 5f, pillY + 2f)
                    )
                }
            }
        }

        // Zoom controls overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = NavyDeepest.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, NavyBorder)
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.0f) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom in map", tint = TealBright, modifier = Modifier.size(18.dp))
                }
            }
            Surface(
                shape = CircleShape,
                color = NavyDeepest.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, NavyBorder)
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(1.0f) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom out map", tint = TealBright, modifier = Modifier.size(18.dp))
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            color = NavyDeepest.copy(alpha = 0.82f),
            border = BorderStroke(1.dp, NavyBorder)
        ) {
            Text(
                text = "NILGIRIS TOPOGRAPHIC SECTOR • TAP CORRIDOR",
                style = MaterialTheme.typography.labelSmall,
                color = TealBright,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
