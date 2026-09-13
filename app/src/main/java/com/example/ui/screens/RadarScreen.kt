package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Flight
import com.example.data.remote.FlightRadarApi
import com.example.ui.components.formatTimeIST
import com.example.ui.theme.AviationAmber
import com.example.ui.theme.AviationCrimson
import com.example.ui.theme.AviationEmerald
import com.example.ui.theme.AviationTeal
import com.example.ui.theme.AviationYellow
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardCard
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.BoardTextDim
import com.example.ui.theme.BoardTextMuted
import com.example.ui.theme.BoardTextPrimary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PlottedRadarTarget(
    val flight: Flight,
    val distanceNm: Float,
    val bearingDeg: Float,
    val screenX: Float,
    val screenY: Float,
    val isLiveCoordinate: Boolean,
    val heading: Float,
    val altitudeText: String,
    val speedText: String
)

data class AviationWaypoint(
    val code: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val isVor: Boolean = false
)

@Composable
fun RadarScreen(
    arrivals: List<Flight>,
    onToggleTrack: (Flight) -> Unit,
    onFlightDetail: (Flight) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mode: 0 = Radar Scope (concentric rings + beam), 1 = Tactical Air Map (Kerala coastline, runways, waypoints)
    var mapModeIndex by remember { mutableStateOf(0) }
    var selectedRangeNm by remember { mutableFloatStateOf(100f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedFlightId by remember { mutableStateOf<String?>(null) }

    // Animated radar sweep beam
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val cokLat = 10.152
    val cokLng = 76.392

    // Cochin RNAV Arrival Waypoints
    val rnavWaypoints = remember {
        listOf(
            AviationWaypoint("CIA", "Cochin VOR 113.5", 10.152, 76.392, isVor = true),
            AviationWaypoint("BIKEN", "NW Inbound Fix (DXB/SHJ)", 10.650, 75.750),
            AviationWaypoint("NOBAT", "West Ocean Fix", 10.180, 75.350),
            AviationWaypoint("OPAMO", "South Approach (MLE/TRV)", 9.550, 76.450),
            AviationWaypoint("VELAG", "NE Corridor (BLR/MAA)", 10.720, 76.950)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BoardBackground)
    ) {
        // Top Toolbar: Mode Switcher (Scope / Air Map) and Range Selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode switch pills
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BoardSurface)
                    .border(1.dp, BoardBorder, RoundedCornerShape(6.dp))
                    .padding(2.dp)
            ) {
                listOf("RADAR SCOPE", "AIR MAP").forEachIndexed { idx, title ->
                    val isSelected = mapModeIndex == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) AviationCrimson else Color.Transparent)
                            .clickable { mapModeIndex = idx }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (idx == 0) Icons.Default.Radar else Icons.Default.Map,
                                contentDescription = title,
                                tint = if (isSelected) Color.White else BoardTextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else BoardTextMuted
                            )
                        }
                    }
                }
            }

            // Range buttons (50NM, 100NM, 150NM, 300NM)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(50f, 100f, 150f, 300f).forEach { range ->
                    val isSelected = selectedRangeNm == range
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) AviationAmber.copy(alpha = 0.25f) else BoardSurface)
                            .border(1.dp, if (isSelected) AviationAmber else BoardBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                selectedRangeNm = range
                                zoomScale = 1.0f
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${range.toInt()}NM",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AviationAmber else BoardTextMuted
                        )
                    }
                }
            }
        }

        // Radar / Map Main Viewport
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()
            val centerBase = Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
            val effectiveCenter = centerBase + panOffset
            val radarRadius = (minOf(canvasWidthPx, canvasHeightPx) / 2f) * 0.90f * zoomScale

            // PRECOMPUTE Plotted Flights OUTSIDE DrawScope (Eliminates State Mutation Bug!)
            val plottedTargets: List<PlottedRadarTarget> = remember(
                arrivals,
                selectedRangeNm,
                zoomScale,
                panOffset,
                canvasWidthPx,
                canvasHeightPx
            ) {
                val targets = mutableListOf<PlottedRadarTarget>()

                arrivals.forEach { flight ->
                    val targetCoords: Pair<Double, Double>? = if (flight.liveLatitude != null && flight.liveLongitude != null) {
                        Pair(flight.liveLatitude, flight.liveLongitude)
                    } else {
                        FlightRadarApi.AIRPORT_COORDS[flight.originIata]
                    }

                    if (targetCoords != null) {
                        val (fLat, fLng) = targetCoords
                        val isLive = flight.liveLatitude != null

                        // Haversine distance in NM
                        val dLat = Math.toRadians(fLat - cokLat)
                        val dLng = Math.toRadians(fLng - cokLng)
                        val a = sin(dLat / 2) * sin(dLat / 2) +
                                cos(Math.toRadians(cokLat)) * cos(Math.toRadians(fLat)) *
                                sin(dLng / 2) * sin(dLng / 2)
                        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                        val distNm = (6371.0 * c / 1.852).toFloat()

                        // True bearing from COK to target
                        val y = sin(dLng) * cos(Math.toRadians(fLat))
                        val x = cos(Math.toRadians(cokLat)) * sin(Math.toRadians(fLat)) -
                                sin(Math.toRadians(cokLat)) * cos(Math.toRadians(fLat)) * cos(dLng)
                        val bearingRad = atan2(y, x)
                        val bearingDeg = ((Math.toDegrees(bearingRad) + 360) % 360).toFloat()

                        // Plot position relative to center
                        val rRatio = (distNm / selectedRangeNm)
                        val plotDist = radarRadius * rRatio
                        val rad = Math.toRadians(bearingDeg.toDouble() - 90)
                        val plotX = (effectiveCenter.x + plotDist * cos(rad)).toFloat()
                        val plotY = (effectiveCenter.y + plotDist * sin(rad)).toFloat()

                        val heading = flight.liveHeading ?: ((bearingDeg + 180) % 360)
                        val altText = when {
                            flight.liveAltitudeFt != null && flight.liveAltitudeFt > 10000 -> "FL${flight.liveAltitudeFt / 100}"
                            flight.liveAltitudeFt != null -> "${flight.liveAltitudeFt}ft"
                            else -> "–"
                        }
                        val spdText = if (flight.liveSpeedKts != null) "${flight.liveSpeedKts}kt" else "–"

                        targets.add(
                            PlottedRadarTarget(
                                flight = flight,
                                distanceNm = distNm,
                                bearingDeg = bearingDeg,
                                screenX = plotX,
                                screenY = plotY,
                                isLiveCoordinate = isLive,
                                heading = heading,
                                altitudeText = altText,
                                speedText = spdText
                            )
                        )
                    }
                }
                targets
            }

            val density = LocalDensity.current

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF080A0E))
                    .border(1.dp, BoardBorder, RoundedCornerShape(12.dp))
                    .pointerInput(plottedTargets) {
                        detectTapGestures { tapOffset ->
                            val touchRadius = with(density) { 36.dp.toPx() }
                            val hit = plottedTargets.minByOrNull {
                                val dx = it.screenX - tapOffset.x
                                val dy = it.screenY - tapOffset.y
                                sqrt(dx * dx + dy * dy)
                            }
                            if (hit != null) {
                                val dx = hit.screenX - tapOffset.x
                                val dy = hit.screenY - tapOffset.y
                                if (sqrt(dx * dx + dy * dy) <= touchRadius) {
                                    selectedFlightId = if (selectedFlightId == hit.flight.id) null else hit.flight.id
                                } else {
                                    selectedFlightId = null
                                }
                            } else {
                                selectedFlightId = null
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panOffset += dragAmount
                        }
                    }
                    .testTag("radar_canvas")
            ) {
                if (mapModeIndex == 0) {
                    // === MODE 0: TACTICAL RADAR SCOPE ===
                    drawRadarScope(
                        center = effectiveCenter,
                        radarRadius = radarRadius,
                        sweepAngle = sweepAngle,
                        selectedRangeNm = selectedRangeNm
                    )
                } else {
                    // === MODE 1: GEOGRAPHIC AIR MAP ===
                    drawAviationMapBackdrop(
                        center = effectiveCenter,
                        radarRadius = radarRadius,
                        selectedRangeNm = selectedRangeNm,
                        cokLat = cokLat,
                        cokLng = cokLng
                    )
                }

                // Draw RNAV Waypoints
                rnavWaypoints.forEach { wp ->
                    val dLat = Math.toRadians(wp.lat - cokLat)
                    val dLng = Math.toRadians(wp.lng - cokLng)
                    val a = sin(dLat / 2) * sin(dLat / 2) +
                            cos(Math.toRadians(cokLat)) * cos(Math.toRadians(wp.lat)) *
                            sin(dLng / 2) * sin(dLng / 2)
                    val distNm = (6371.0 * 2 * atan2(sqrt(a), sqrt(1 - a)) / 1.852).toFloat()
                    val y = sin(dLng) * cos(Math.toRadians(wp.lat))
                    val x = cos(Math.toRadians(cokLat)) * sin(Math.toRadians(wp.lat)) -
                            sin(Math.toRadians(cokLat)) * cos(Math.toRadians(wp.lat)) * cos(dLng)
                    val bearingDeg = ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()

                    val rRatio = (distNm / selectedRangeNm)
                    val plotDist = radarRadius * rRatio
                    val rad = Math.toRadians(bearingDeg.toDouble() - 90)
                    val wpX = (effectiveCenter.x + plotDist * cos(rad)).toFloat()
                    val wpY = (effectiveCenter.y + plotDist * sin(rad)).toFloat()

                    if (wp.isVor) {
                        drawCircle(
                            color = AviationCrimson,
                            radius = 4.dp.toPx(),
                            center = Offset(wpX, wpY)
                        )
                    } else {
                        // Diamond waypoint marker
                        drawWaypointDiamond(Offset(wpX, wpY), AviationTeal.copy(alpha = 0.7f))
                    }
                }

                // Center Runway 09/27 (Nedumbassery - oriented 090°/270°)
                val rwLengthPx = 28.dp.toPx()
                drawLine(
                    color = AviationCrimson,
                    start = Offset(effectiveCenter.x - rwLengthPx / 2, effectiveCenter.y),
                    end = Offset(effectiveCenter.x + rwLengthPx / 2, effectiveCenter.y),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Square
                )
                // Extended approach centerlines (5NM out east and west)
                drawLine(
                    color = AviationCrimson.copy(alpha = 0.4f),
                    start = Offset(effectiveCenter.x - rwLengthPx * 2.5f, effectiveCenter.y),
                    end = Offset(effectiveCenter.x + rwLengthPx * 2.5f, effectiveCenter.y),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw Plotted Flights
                plottedTargets.forEach { target ->
                    val isSelected = target.flight.id == selectedFlightId
                    val planeColor = when {
                        target.flight.isTracked -> AviationAmber
                        target.flight.remainingSeconds <= 660 -> AviationCrimson
                        target.isLiveCoordinate -> AviationEmerald
                        else -> AviationTeal
                    }

                    // Range vector to runway if selected
                    if (isSelected) {
                        drawLine(
                            color = planeColor.copy(alpha = 0.5f),
                            start = effectiveCenter,
                            end = Offset(target.screenX, target.screenY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(
                            color = planeColor.copy(alpha = 0.25f),
                            radius = 20.dp.toPx(),
                            center = Offset(target.screenX, target.screenY)
                        )
                        drawCircle(
                            color = planeColor,
                            radius = 20.dp.toPx(),
                            center = Offset(target.screenX, target.screenY),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Heading velocity vector
                    val vecLen = 22.dp.toPx()
                    val hRad = Math.toRadians(target.heading.toDouble() - 90)
                    val vEndX = target.screenX + vecLen * cos(hRad).toFloat()
                    val vEndY = target.screenY + vecLen * sin(hRad).toFloat()
                    drawLine(
                        color = planeColor.copy(alpha = 0.6f),
                        start = Offset(target.screenX, target.screenY),
                        end = Offset(vEndX, vEndY),
                        strokeWidth = 1.2.dp.toPx()
                    )

                    // Aircraft Glyph
                    rotate(degrees = target.heading, pivot = Offset(target.screenX, target.screenY)) {
                        drawAirplaneGlyph(Offset(target.screenX, target.screenY), planeColor)
                    }

                    // Tactical Tag (flight number & altitude)
                    drawCircle(
                        color = planeColor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(target.screenX, target.screenY)
                    )
                }
            }

            // Floating Navigation & Zoom Controls (Overlay on Top Right)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Zoom In
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BoardSurface)
                        .border(1.dp, BoardBorder, CircleShape)
                        .clickable { zoomScale = (zoomScale * 1.25f).coerceAtMost(4.0f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = BoardTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Zoom Out
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BoardSurface)
                        .border(1.dp, BoardBorder, CircleShape)
                        .clickable { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.5f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = BoardTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Recenter on COK
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BoardSurface)
                        .border(1.dp, BoardBorder, CircleShape)
                        .clickable {
                            panOffset = Offset.Zero
                            zoomScale = 1.0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter on COK",
                        tint = AviationCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom Inspection Sheet: Shows details when a flight is tapped
        val selectedFlight = arrivals.firstOrNull { it.id == selectedFlightId }
        if (selectedFlight != null) {
            val fl = selectedFlight
            val targetCoords = if (fl.liveLatitude != null && fl.liveLongitude != null) {
                Pair(fl.liveLatitude, fl.liveLongitude)
            } else {
                FlightRadarApi.AIRPORT_COORDS[fl.originIata]
            }

            var distNm = 0f
            var bearingDeg = 0f
            if (targetCoords != null) {
                val (fLat, fLng) = targetCoords
                val dLat = Math.toRadians(fLat - cokLat)
                val dLng = Math.toRadians(fLng - cokLng)
                val a = sin(dLat / 2) * sin(dLat / 2) +
                        cos(Math.toRadians(cokLat)) * cos(Math.toRadians(fLat)) *
                        sin(dLng / 2) * sin(dLng / 2)
                val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                distNm = (6371.0 * c / 1.852).toFloat()

                val y = sin(dLng) * cos(Math.toRadians(fLat))
                val x = cos(Math.toRadians(cokLat)) * sin(Math.toRadians(fLat)) -
                        sin(Math.toRadians(cokLat)) * cos(Math.toRadians(fLat)) * cos(dLng)
                bearingDeg = ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
            }

            val altitudeText = when {
                fl.liveAltitudeFt != null && fl.liveAltitudeFt > 10000 -> "FL${fl.liveAltitudeFt / 100}"
                fl.liveAltitudeFt != null -> "${fl.liveAltitudeFt}ft"
                else -> "–"
            }
            val speedText = if (fl.liveSpeedKts != null) "${fl.liveSpeedKts}kt" else "–"

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, AviationCrimson.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
                color = BoardSurface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = fl.flightNumber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = AviationAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${fl.originIata} ➔ ${fl.destIata}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BoardTextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${fl.registration})",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = BoardTextMuted
                            )
                        }

                        IconButton(
                            onClick = { onToggleTrack(fl) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (fl.isTracked) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Track Flight",
                                tint = if (fl.isTracked) AviationAmber else BoardTextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Radar Telemetry Bar (Distance, Bearing, Altitude, Speed)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(BoardCard)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DIST TO COK", fontSize = 8.sp, color = BoardTextDim, fontFamily = FontFamily.Monospace)
                            Text("${distNm.toInt()} NM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AviationTeal, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("BEARING", fontSize = 8.sp, color = BoardTextDim, fontFamily = FontFamily.Monospace)
                            Text("${bearingDeg.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BoardTextPrimary, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("ALTITUDE", fontSize = 8.sp, color = BoardTextDim, fontFamily = FontFamily.Monospace)
                            Text(altitudeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AviationEmerald, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("SPEED", fontSize = 8.sp, color = BoardTextDim, fontFamily = FontFamily.Monospace)
                            Text(speedText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BoardTextPrimary, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("ETA (IST)", fontSize = 8.sp, color = BoardTextDim, fontFamily = FontFamily.Monospace)
                            Text(formatTimeIST(fl.bestEta), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AviationCrimson, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { onFlightDetail(fl) },
                            colors = ButtonDefaults.buttonColors(containerColor = AviationCrimson),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "Full Flight Dossier ➔",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // Helper Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AviationAmber))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Followed", fontSize = 9.sp, color = BoardTextMuted, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AviationCrimson))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("<10m Alert", fontSize = 9.sp, color = BoardTextMuted, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AviationEmerald))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Live ADS-B", fontSize = 9.sp, color = BoardTextMuted, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AviationTeal))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RNAV Waypoint", fontSize = 9.sp, color = BoardTextMuted, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

private fun DrawScope.drawRadarScope(
    center: Offset,
    radarRadius: Float,
    sweepAngle: Float,
    selectedRangeNm: Float
) {
    // Concentric range rings
    listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { fraction ->
        drawCircle(
            color = Color(0xFF19202E),
            radius = radarRadius * fraction,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Crosshair axes (0, 90, 180, 270 deg)
    drawLine(
        color = Color(0xFF19202E),
        start = Offset(center.x - radarRadius, center.y),
        end = Offset(center.x + radarRadius, center.y),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = Color(0xFF19202E),
        start = Offset(center.x, center.y - radarRadius),
        end = Offset(center.x, center.y + radarRadius),
        strokeWidth = 1.dp.toPx()
    )

    // Animated rotating radar sweep
    rotate(degrees = sweepAngle, pivot = center) {
        val sweepPath = Path().apply {
            moveTo(center.x, center.y)
            val endX = center.x + radarRadius * cos(0.0)
            val endY = center.y + radarRadius * sin(0.0)
            lineTo(endX.toFloat(), endY.toFloat())
        }
        drawPath(
            path = sweepPath,
            brush = Brush.sweepGradient(
                listOf(
                    AviationTeal.copy(alpha = 0f),
                    AviationTeal.copy(alpha = 0.22f)
                ),
                center = center
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawAviationMapBackdrop(
    center: Offset,
    radarRadius: Float,
    selectedRangeNm: Float,
    cokLat: Double,
    cokLng: Double
) {
    // Deep Arabian Sea on the West
    drawRect(
        color = Color(0xFF070B14),
        size = size
    )

    // Landmass contour representing Kerala Coastline (runs Northwest to Southeast)
    // Cochin is near 10.15N, 76.39E with the coastline just west of the airport (approx 18-25km west)
    val coastPath = Path().apply {
        // Approximate Kerala Coastline projected around COK center
        val westShift = (radarRadius * 0.15f) // Coast is ~15-20 NM west of Nedumbassery
        moveTo(center.x - westShift - radarRadius * 0.5f, center.y - radarRadius * 0.9f) // Kannur
        quadraticTo(
            center.x - westShift - radarRadius * 0.35f, center.y - radarRadius * 0.45f, // Kozhikode
            center.x - westShift - radarRadius * 0.18f, center.y - radarRadius * 0.1f // Thrissur coast
        )
        quadraticTo(
            center.x - westShift, center.y + radarRadius * 0.05f, // Fort Kochi / Vypin inlet
            center.x - westShift + radarRadius * 0.08f, center.y + radarRadius * 0.35f // Alappuzha
        )
        quadraticTo(
            center.x - westShift + radarRadius * 0.25f, center.y + radarRadius * 0.65f, // Kollam
            center.x - westShift + radarRadius * 0.45f, center.y + radarRadius * 0.95f // Trivandrum
        )
        lineTo(size.width, size.height)
        lineTo(size.width, 0f)
        close()
    }

    // Draw Landmass
    drawPath(
        path = coastPath,
        color = Color(0xFF101622)
    )

    // Draw Coastline contour stroke
    drawPath(
        path = coastPath,
        color = Color(0xFF1E283C),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Vembanad Lake / Cochin Backwaters contour
    val backwatersPath = Path().apply {
        val inletX = center.x - radarRadius * 0.12f
        val inletY = center.y + radarRadius * 0.05f
        moveTo(inletX, inletY)
        quadraticTo(inletX + 15.dp.toPx(), inletY + 25.dp.toPx(), inletX + 8.dp.toPx(), inletY + 50.dp.toPx())
        quadraticTo(inletX + 12.dp.toPx(), inletY + 70.dp.toPx(), inletX + 4.dp.toPx(), inletY + 90.dp.toPx())
    }
    drawPath(
        path = backwatersPath,
        color = Color(0xFF09101C),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Concentric nautical mile guide rings (dotted/thin)
    listOf(0.5f, 1.0f).forEach { fraction ->
        drawCircle(
            color = Color(0xFF161F2E),
            radius = radarRadius * fraction,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawWaypointDiamond(center: Offset, color: Color) {
    val r = 4.dp.toPx()
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r, center.y)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r, center.y)
        close()
    }
    drawPath(path, color, style = Stroke(width = 1.2.dp.toPx()))
}

fun DrawScope.drawAirplaneGlyph(center: Offset, color: Color) {
    val sizePx = 10.dp.toPx()
    val path = Path().apply {
        moveTo(center.x, center.y - sizePx)
        lineTo(center.x + sizePx * 0.7f, center.y + sizePx * 0.7f)
        lineTo(center.x, center.y + sizePx * 0.3f)
        lineTo(center.x - sizePx * 0.7f, center.y + sizePx * 0.7f)
        close()
    }
    drawPath(path, color)
}
