package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Flight
import com.example.data.model.Turnaround
import com.example.data.model.TurnaroundStatus
import com.example.ui.components.formatTimeIST
import com.example.ui.theme.AviationAmber
import com.example.ui.theme.AviationCrimson
import com.example.ui.theme.AviationEmerald
import com.example.ui.theme.AviationRed
import com.example.ui.theme.AviationYellow
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardCard
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.BoardTextDim
import com.example.ui.theme.BoardTextMuted
import com.example.ui.theme.BoardTextPrimary

@Composable
fun TurnaroundScreen(
    turnarounds: List<Turnaround>,
    onFlightClick: (Flight) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BoardBackground)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Info Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BoardSurface)
                .border(1.dp, BoardBorder, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = AviationAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AIRCRAFT TURNAROUND MATRIX",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = BoardTextPrimary
                    )
                    Text(
                        text = "Matching inbound tail numbers with outbound legs to identify ground time buffers and delay spillovers.",
                        fontSize = 11.sp,
                        color = BoardTextMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DETECTED TURNS (${turnarounds.size})",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = BoardTextDim
            )

            val tightCount = turnarounds.count { it.status == TurnaroundStatus.CRITICAL_TIGHT || it.status == TurnaroundStatus.TIGHT }
            if (tightCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AviationYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$tightCount Tight Turns",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = AviationYellow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (turnarounds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔄", fontSize = 34.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Turnarounds Detected",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BoardTextPrimary
                    )
                    Text(
                        text = "Turnaround pairs will appear when incoming registrations match scheduled departures.",
                        fontSize = 11.sp,
                        color = BoardTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("turnaround_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = turnarounds,
                    key = { index, turn ->
                        "${turn.registration}_${turn.arrivalFlight.id}_${turn.departureFlight.id}_$index"
                    }
                ) { _, turn ->
                    val statusColor = when (turn.status) {
                        TurnaroundStatus.NORMAL -> AviationEmerald
                        TurnaroundStatus.TIGHT -> AviationYellow
                        TurnaroundStatus.CRITICAL_TIGHT -> AviationRed
                        TurnaroundStatus.DELAY_RISK -> AviationCrimson
                    }

                    val statusLabel = when (turn.status) {
                        TurnaroundStatus.NORMAL -> "Normal (${turn.gapMinutes}m)"
                        TurnaroundStatus.TIGHT -> "Tight Turn (${turn.gapMinutes}m)"
                        TurnaroundStatus.CRITICAL_TIGHT -> "Critical (${turn.gapMinutes}m)"
                        TurnaroundStatus.DELAY_RISK -> "Delay Spillover"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BoardCard)
                            .border(1.dp, BoardBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            // Top Row: Registration, Aircraft Type, Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = turn.registration,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AviationAmber
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${turn.aircraftModel})",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = BoardTextDim
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Inbound Leg Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BoardSurface)
                                    .clickable { onFlightClick(turn.arrivalFlight) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ARR",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = AviationEmerald
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${turn.arrivalFlight.flightNumber} (${turn.arrivalFlight.originIata} ➔ COK)",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BoardTextPrimary
                                    )
                                }

                                Text(
                                    text = "ETA ${formatTimeIST(turn.arrivalFlight.bestEta)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = AviationEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Outbound Leg Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BoardSurface)
                                    .clickable { onFlightClick(turn.departureFlight) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DEP",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = AviationAmber
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${turn.departureFlight.flightNumber} (COK ➔ ${turn.departureFlight.destIata})",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BoardTextPrimary
                                    )
                                }

                                Text(
                                    text = "ETD ${formatTimeIST(turn.departureFlight.bestEta)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = AviationAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
