package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MonitoredCorridor
import com.example.data.model.RiskLevel
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class ProjectedPoint(val sx: Float, val sy: Float, val depth: Float)

private data class RenderQuad(
    val p0: ProjectedPoint,
    val p1: ProjectedPoint,
    val p2: ProjectedPoint,
    val p3: ProjectedPoint,
    val fill: Color,
    val stroke: Color? = null,
    val avgDepth: Float
)

private enum class CameraPreset(val label: String, val yawDeg: Float, val pitchDeg: Float) {
    ISOMETRIC("3D Iso", -38f, 28f),
    PROFILE("Slope Side", -84f, 14f),
    OVERHEAD("Plan View", -15f, 68f)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Nilgiris3DTerrainSceneCard(
    corridor: MonitoredCorridor,
    rainfallIntensityMmHr: Double?,
    soilMoistureM3M3: Double?,
    riskLevel: RiskLevel?,
    estimatedProbability: Double?,
    modifier: Modifier = Modifier
) {
    var targetYaw by remember { mutableFloatStateOf(CameraPreset.ISOMETRIC.yawDeg) }
    var targetPitch by remember { mutableFloatStateOf(CameraPreset.ISOMETRIC.pitchDeg) }
    var targetZoom by remember { mutableFloatStateOf(1.05f) }
    var activePreset by remember { mutableStateOf(CameraPreset.ISOMETRIC) }
    var animTimeSec by remember { mutableFloatStateOf(0f) }

    val yawDeg by animateFloatAsState(
        targetValue = targetYaw,
        animationSpec = tween(durationMillis = 120),
        label = "camera_yaw"
    )
    val pitchDeg by animateFloatAsState(
        targetValue = targetPitch,
        animationSpec = tween(durationMillis = 120),
        label = "camera_pitch"
    )
    val zoom by animateFloatAsState(
        targetValue = targetZoom,
        animationSpec = tween(durationMillis = 160),
        label = "camera_zoom"
    )

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    animTimeSec += dt
                }
                lastNanos = frameNanos
            }
        }
    }

    val rainIntensity = (rainfallIntensityMmHr ?: 0.0).toFloat()
    val moisture = (soilMoistureM3M3 ?: 0.24).toFloat().coerceIn(0f, 0.65f)
    val slopeDeg = corridor.maxSlopeDegrees.toFloat()
    val hazardColor = when (riskLevel) {
        RiskLevel.HIGH -> RiskHighRed
        RiskLevel.MEDIUM -> RiskMediumAmber
        RiskLevel.LOW -> RiskLowGreen
        null -> TealBright
    }

    EarthWatchCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = NavyCard,
        borderColor = TealPrimary.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = TealBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Interactive 3D Slope & Strata Scene",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite
                        )
                        Text(
                            text = "Drag to orbit • ${corridor.maxSlopeDegrees}° escarpment • ${corridor.hairpinCount} hairpins",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { targetZoom = (targetZoom - 0.15f).coerceIn(0.75f, 1.65f) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out 3D scene", tint = SlateLight)
                    }
                    IconButton(
                        onClick = { targetZoom = (targetZoom + 0.15f).coerceIn(0.75f, 1.65f) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in 3D scene", tint = SlateLight)
                    }
                    IconButton(
                        onClick = {
                            activePreset = CameraPreset.ISOMETRIC
                            targetYaw = CameraPreset.ISOMETRIC.yawDeg
                            targetPitch = CameraPreset.ISOMETRIC.pitchDeg
                            targetZoom = 1.05f
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "Reset 3D camera", tint = TealBright)
                    }
                }
            }

            // Camera Presets
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CameraPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = activePreset == preset,
                        onClick = {
                            activePreset = preset
                            targetYaw = preset.yawDeg
                            targetPitch = preset.pitchDeg
                        },
                        label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = NavyDeepest,
                            containerColor = NavyElevated,
                            labelColor = SlateLight
                        )
                    )
                }
            }

            // Hardware-accelerated interactive 3D Terrain Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF06101E),
                                Color(0xFF0C1F36),
                                Color(0xFF091627)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            targetYaw = (targetYaw + dragAmount.x * 0.42f) % 360f
                            targetPitch = (targetPitch + dragAmount.y * 0.32f).coerceIn(10f, 78f)
                        }
                    }
                    .testTag("terrain_3d_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawNilgiris3DScene(
                        yawDeg = yawDeg,
                        pitchDeg = pitchDeg,
                        zoom = zoom,
                        slopeDeg = slopeDeg,
                        moisture = moisture,
                        rainIntensity = rainIntensity,
                        hazardColor = hazardColor,
                        riskLevel = riskLevel,
                        animTimeSec = animTimeSec
                    )
                }

                // Compact corner telemetry HUD (<15% footprint)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavyDeepest.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "${corridor.representativeElevationMeters}m Elev • ${corridor.maxSlopeDegrees}° Slope",
                            style = MaterialTheme.typography.labelSmall,
                            color = OffWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        val probPct = estimatedProbability?.let { "${(it * 100).toInt()}%" } ?: "N/A"
                        Text(
                            text = "Rain ${String.format(java.util.Locale.US, "%.1f", rainIntensity)} mm/h • Slip Prob $probPct",
                            style = MaterialTheme.typography.labelSmall,
                            color = hazardColor
                        )
                    }
                }
            }

            // Subsurface strata legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendSwatch(color = Color(0xFF285D54), label = "Surface Forest/Slope")
                LegendSwatch(color = saturateSoilColor(moisture), label = "Laterite Moisture Layer")
                LegendSwatch(color = Color(0xFF2C3B4E), label = "Charnockite Bedrock")
            }
        }
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SlateLight)
    }
}

private fun saturateSoilColor(moisture: Float): Color {
    val t = ((moisture - 0.10f) / 0.45f).coerceIn(0f, 1f)
    val r = (0.58f * (1f - t) + 0.14f * t).coerceIn(0f, 1f)
    val g = (0.40f * (1f - t) + 0.48f * t).coerceIn(0f, 1f)
    val b = (0.24f * (1f - t) + 0.62f * t).coerceIn(0f, 1f)
    return Color(r, g, b, 1f)
}

private fun terrainHeight(x: Float, z: Float, slopeDeg: Float): Float {
    // Normalized grid x,z in [-1.0, 1.0]: higher elevation toward -x/-z escarpment ridge
    val slopeScale = (slopeDeg / 35f).coerceIn(0.45f, 1.35f)
    val escarpment = ((1f - (x + 1f) * 0.5f) * 0.68f + (1f - (z + 1f) * 0.5f) * 0.35f) * slopeScale
    val ridgeUndulation = 0.08f * sin(x * 3.1f) * cos(z * 2.6f)
    return (escarpment + ridgeUndulation).coerceAtLeast(0.05f)
}

private fun DrawScope.drawNilgiris3DScene(
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    slopeDeg: Float,
    moisture: Float,
    rainIntensity: Float,
    hazardColor: Color,
    riskLevel: RiskLevel?,
    animTimeSec: Float
) {
    val yaw = (yawDeg * PI / 180.0).toFloat()
    val pitch = (pitchDeg * PI / 180.0).toFloat()
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val cosP = cos(pitch)
    val sinP = sin(pitch)

    val centerX = size.width * 0.5f
    val centerY = size.height * 0.57f
    val scale = (minOf(size.width, size.height) * 0.34f) * zoom

    fun project(v: Vec3): ProjectedPoint {
        val x1 = v.x * cosY - v.z * sinY
        val z1 = v.x * sinY + v.z * cosY
        val y2 = v.y * cosP - z1 * sinP
        val z2 = v.y * sinP + z1 * cosP
        val perspective = 4.6f / (4.6f + z2).coerceAtLeast(1.2f)
        return ProjectedPoint(
            sx = centerX + x1 * scale * perspective,
            sy = centerY - y2 * scale * perspective,
            depth = z2
        )
    }

    val quads = mutableListOf<RenderQuad>()
    val steps = 8
    val baseDepth = -0.34f
    val clayThickness = 0.16f

    // 1. Subsurface Cutaway Sides (Charnockite Bedrock + Laterite Soil Layer)
    val soilColor = saturateSoilColor(moisture)
    val bedrockColor = Color(0xFF233245)

    for (i in 0 until steps) {
        val t0 = -1f + (2f * i) / steps
        val t1 = -1f + (2f * (i + 1)) / steps

        // Front (+z = 1) & Right (+x = 1) & Left (-x = -1) & Back (-z = -1) geological walls
        val edges = listOf(
            Pair(Vec3(t0, 0f, 1f), Vec3(t1, 0f, 1f)),
            Pair(Vec3(1f, 0f, t0), Vec3(1f, 0f, t1)),
            Pair(Vec3(-1f, 0f, t0), Vec3(-1f, 0f, t1)),
            Pair(Vec3(t0, 0f, -1f), Vec3(t1, 0f, -1f))
        )
        edges.forEachIndexed { edgeIdx, (a, b) ->
            val ha = terrainHeight(a.x, a.z, slopeDeg)
            val hb = terrainHeight(b.x, b.z, slopeDeg)
            val clayBottomA = (ha - clayThickness).coerceAtLeast(baseDepth + 0.08f)
            val clayBottomB = (hb - clayThickness).coerceAtLeast(baseDepth + 0.08f)
            val shade = if (edgeIdx % 2 == 0) 0.90f else 0.72f

            // Soil strata quad
            val s0 = project(Vec3(a.x, ha, a.z))
            val s1 = project(Vec3(b.x, hb, b.z))
            val s2 = project(Vec3(b.x, clayBottomB, b.z))
            val s3 = project(Vec3(a.x, clayBottomA, a.z))
            quads.add(
                RenderQuad(
                    s0, s1, s2, s3,
                    fill = Color(
                        soilColor.red * shade,
                        soilColor.green * shade,
                        soilColor.blue * shade,
                        1f
                    ),
                    stroke = Color.Black.copy(alpha = 0.18f),
                    avgDepth = (s0.depth + s1.depth + s2.depth + s3.depth) * 0.25f
                )
            )

            // Bedrock strata quad
            val b0 = s3
            val b1 = s2
            val b2 = project(Vec3(b.x, baseDepth, b.z))
            val b3 = project(Vec3(a.x, baseDepth, a.z))
            quads.add(
                RenderQuad(
                    b0, b1, b2, b3,
                    fill = Color(
                        bedrockColor.red * shade,
                        bedrockColor.green * shade,
                        bedrockColor.blue * shade,
                        1f
                    ),
                    stroke = Color.Black.copy(alpha = 0.22f),
                    avgDepth = (b0.depth + b1.depth + b2.depth + b3.depth) * 0.25f
                )
            )
        }
    }

    // 2. Topographic Surface Escarpment Mesh (modulated by moisture & slope steepness)
    for (ix in 0 until steps) {
        val x0 = -1f + (2f * ix) / steps
        val x1 = -1f + (2f * (ix + 1)) / steps
        for (iz in 0 until steps) {
            val z0 = -1f + (2f * iz) / steps
            val z1 = -1f + (2f * (iz + 1)) / steps

            val h00 = terrainHeight(x0, z0, slopeDeg)
            val h10 = terrainHeight(x1, z0, slopeDeg)
            val h11 = terrainHeight(x1, z1, slopeDeg)
            val h01 = terrainHeight(x0, z1, slopeDeg)

            val avgH = (h00 + h10 + h11 + h01) * 0.25f
            val localSlope = kotlin.math.abs(h00 - h11)
            val wetTint = (moisture / 0.55f).coerceIn(0f, 1f)
            val isCriticalZone = localSlope > 0.20f && (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.MEDIUM)

            val baseGreen = Color(
                red = (0.10f + 0.14f * (1f - wetTint) + if (isCriticalZone) 0.12f else 0f).coerceIn(0f, 1f),
                green = (0.28f + 0.16f * avgH).coerceIn(0f, 1f),
                blue = (0.24f + 0.18f * wetTint).coerceIn(0f, 1f),
                alpha = 1f
            )

            val p0 = project(Vec3(x0, h00, z0))
            val p1 = project(Vec3(x1, h10, z0))
            val p2 = project(Vec3(x1, h11, z1))
            val p3 = project(Vec3(x0, h01, z1))
            quads.add(
                RenderQuad(
                    p0, p1, p2, p3,
                    fill = baseGreen,
                    stroke = TealBright.copy(alpha = 0.22f),
                    avgDepth = (p0.depth + p1.depth + p2.depth + p3.depth) * 0.25f
                )
            )
        }
    }

    // Painter's algorithm sort (farthest first)
    quads.sortByDescending { it.avgDepth }
    quads.forEach { q ->
        val path = Path().apply {
            moveTo(q.p0.sx, q.p0.sy)
            lineTo(q.p1.sx, q.p1.sy)
            lineTo(q.p2.sx, q.p2.sy)
            lineTo(q.p3.sx, q.p3.sy)
            close()
        }
        drawPath(path = path, color = q.fill)
        q.stroke?.let { drawPath(path = path, color = it, style = Stroke(width = 1.1f)) }
    }

    // 3. Winding 3D Ghat Highway + Hairpins + Retaining Barrier Posts
    val roadSamples = 24
    val roadPoints = (0..roadSamples).map { i ->
        val t = i.toFloat() / roadSamples
        val x = 0.85f - 1.65f * t
        val z = 0.72f * sin(t * PI.toFloat() * 2.4f) * (1f - 0.25f * t)
        val y = terrainHeight(x, z, slopeDeg) + 0.025f
        Vec3(x, y, z)
    }
    val projectedRoad = roadPoints.map { project(it) }

    for (i in 0 until projectedRoad.size - 1) {
        val a = projectedRoad[i]
        val b = projectedRoad[i + 1]
        drawLine(
            color = Color(0xFF141B24),
            start = Offset(a.sx, a.sy),
            end = Offset(b.sx, b.sy),
            strokeWidth = 11f * zoom,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFE2E8F0).copy(alpha = 0.65f),
            start = Offset(a.sx, a.sy),
            end = Offset(b.sx, b.sy),
            strokeWidth = 2f * zoom,
            cap = StrokeCap.Round
        )
    }

    // Roadside guardrail posts (every 3rd segment)
    for (i in 0 until roadPoints.size step 3) {
        val rp = roadPoints[i]
        val base = project(rp)
        val top = project(Vec3(rp.x, rp.y + 0.08f, rp.z))
        drawLine(
            color = TealBright.copy(alpha = 0.8f),
            start = Offset(base.sx, base.sy),
            end = Offset(top.sx, top.sy),
            strokeWidth = 2f
        )
    }

    // 4. Composite Multi-Part Props (Ground-pivoted with contact shadow)
    // Prop A: Automated Weather & Telemetry Mast on Upper Ridge
    val mastBasePos = Vec3(-0.55f, terrainHeight(-0.55f, -0.45f, slopeDeg), -0.45f)
    val mastShadow = project(mastBasePos)
    drawCircle(
        color = Color.Black.copy(alpha = 0.35f),
        radius = 10f * zoom,
        center = Offset(mastShadow.sx, mastShadow.sy)
    )
    val mastTopPos = Vec3(mastBasePos.x, mastBasePos.y + 0.34f, mastBasePos.z)
    val mastTop = project(mastTopPos)
    // Concrete footing
    drawCircle(color = SlateMuted, radius = 4.5f * zoom, center = Offset(mastShadow.sx, mastShadow.sy))
    // Steel pole
    drawLine(
        color = OffWhite,
        start = Offset(mastShadow.sx, mastShadow.sy),
        end = Offset(mastTop.sx, mastTop.sy),
        strokeWidth = 3f * zoom
    )
    // Solar panel crossbar
    val solarP = project(Vec3(mastBasePos.x + 0.09f, mastBasePos.y + 0.22f, mastBasePos.z))
    drawLine(
        color = InfoCyan,
        start = Offset(mastTop.sx, (mastShadow.sy + mastTop.sy) * 0.5f),
        end = Offset(solarP.sx, solarP.sy),
        strokeWidth = 4.5f * zoom
    )
    // Spinning anemometer cups
    val spinAngle = animTimeSec * 3.5f
    val cupA = project(Vec3(mastTopPos.x + 0.08f * cos(spinAngle), mastTopPos.y, mastTopPos.z + 0.08f * sin(spinAngle)))
    val cupB = project(Vec3(mastTopPos.x - 0.08f * cos(spinAngle), mastTopPos.y, mastTopPos.z - 0.08f * sin(spinAngle)))
    drawLine(color = TealBright, start = Offset(cupA.sx, cupA.sy), end = Offset(cupB.sx, cupB.sy), strokeWidth = 2f)
    drawCircle(color = TealBright, radius = 3f * zoom, center = Offset(cupA.sx, cupA.sy))
    drawCircle(color = TealBright, radius = 3f * zoom, center = Offset(cupB.sx, cupB.sy))

    // Prop B: Composite 3D Traveller Vehicle moving slowly along corridor
    val progress = ((animTimeSec * 0.08f) % 1f)
    val idxFloat = progress * (roadPoints.size - 1)
    val idx0 = idxFloat.toInt().coerceIn(0, roadPoints.size - 2)
    val frac = idxFloat - idx0
    val carPos = Vec3(
        x = roadPoints[idx0].x + (roadPoints[idx0 + 1].x - roadPoints[idx0].x) * frac,
        y = roadPoints[idx0].y + (roadPoints[idx0 + 1].y - roadPoints[idx0].y) * frac,
        z = roadPoints[idx0].z + (roadPoints[idx0 + 1].z - roadPoints[idx0].z) * frac
    )
    val carBase = project(carPos)
    val carRoof = project(Vec3(carPos.x, carPos.y + 0.07f, carPos.z))
    drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = 8f * zoom, center = Offset(carBase.sx, carBase.sy + 2f))
    drawCircle(color = PureWhite, radius = 5.5f * zoom, center = Offset(carBase.sx, carBase.sy))
    drawCircle(color = TealBright, radius = 3.5f * zoom, center = Offset(carRoof.sx, carRoof.sy))

    // Prop C: Monitored Hazard Pocket Marker & Slip-Plane Vector
    val hazardPos = Vec3(-0.15f, terrainHeight(-0.15f, 0.25f, slopeDeg), 0.25f)
    val hzBase = project(hazardPos)
    val hzTip = project(Vec3(hazardPos.x, hazardPos.y + 0.24f, hazardPos.z))
    val pulse = 0.7f + 0.3f * sin(animTimeSec * 4.2f)
    drawCircle(
        color = hazardColor.copy(alpha = 0.26f * pulse),
        radius = 16f * zoom,
        center = Offset(hzBase.sx, hzBase.sy)
    )
    drawLine(
        color = hazardColor,
        start = Offset(hzBase.sx, hzBase.sy),
        end = Offset(hzTip.sx, hzTip.sy),
        strokeWidth = 3f * zoom
    )
    drawCircle(color = hazardColor, radius = 5f * zoom, center = Offset(hzTip.sx, hzTip.sy))

    // Slip-plane shear vector arrow on elevated risk
    if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.MEDIUM) {
        val shearStart = project(Vec3(-0.35f, terrainHeight(-0.35f, 0.15f, slopeDeg) + 0.06f, 0.15f))
        val shearEnd = project(Vec3(0.15f, terrainHeight(0.15f, 0.35f, slopeDeg) + 0.06f, 0.35f))
        drawLine(
            color = hazardColor.copy(alpha = 0.85f),
            start = Offset(shearStart.sx, shearStart.sy),
            end = Offset(shearEnd.sx, shearEnd.sy),
            strokeWidth = 3f * zoom,
            cap = StrokeCap.Round
        )
        drawCircle(color = hazardColor, radius = 4.5f * zoom, center = Offset(shearEnd.sx, shearEnd.sy))
    }

    // 5. Dynamic 3D Rainfall Particles scaled by intensity
    val dropCount = when {
        rainIntensity >= 15f -> 46
        rainIntensity >= 4f -> 26
        rainIntensity >= 0.3f -> 12
        else -> 0
    }
    for (d in 0 until dropCount) {
        val seed = d * 137.5f
        val rx = (((seed * 13f) % 200f) / 100f) - 1f
        val rz = (((seed * 29f) % 200f) / 100f) - 1f
        val fallPhase = ((animTimeSec * 1.4f + (d % 9) * 0.11f) % 1f)
        val ry = 1.45f * (1f - fallPhase) + 0.1f
        val pTop = project(Vec3(rx, ry + 0.12f, rz))
        val pBot = project(Vec3(rx + 0.02f, ry, rz))
        drawLine(
            color = InfoCyan.copy(alpha = 0.55f),
            start = Offset(pTop.sx, pTop.sy),
            end = Offset(pBot.sx, pBot.sy),
            strokeWidth = 1.5f
        )
    }
}
