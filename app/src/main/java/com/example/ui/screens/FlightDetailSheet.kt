package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Flight
import com.example.ui.components.AirlineLogoBadge
import com.example.ui.components.formatTimeIST
import com.example.ui.theme.AviationAmber
import com.example.ui.theme.AviationCrimson
import com.example.ui.theme.AviationEmerald
import com.example.ui.theme.AviationTeal
import com.example.ui.theme.AviationYellow
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardCard
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.BoardTextDim
import com.example.ui.theme.BoardTextMuted
import com.example.ui.theme.BoardTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailSheet(
    flight: Flight,
    onToggleTrack: (Flight) -> Unit,
    onDispatchDiscord10m: (Flight) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BoardSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Flight Number, Airline Logo, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AirlineLogoBadge(
                        airlineCode = flight.airlineCode,
                        logoUrl = flight.logoUrl,
                        airlineName = flight.airlineName,
                        width = 52.dp,
                        height = 26.dp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Column {
                        Text(
                            text = flight.flightNumber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AviationAmber
                        )
                        Text(
                            text = flight.airlineName,
                            fontSize = 11.sp,
                            color = BoardTextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BoardTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sector Banner (From -> To)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BoardCard)
                    .border(1.dp, BoardBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = flight.originIata,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = BoardTextPrimary
                    )
                    Text(
                        text = flight.originCity ?: "Origin",
                        fontSize = 11.sp,
                        color = BoardTextMuted
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (flight.isArrival) "ARRIVING" else "DEPARTING",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AviationCrimson
                    )
                    Text(
                        text = "✈ ➔",
                        fontSize = 16.sp,
                        color = AviationAmber
                    )
                    Text(
                        text = flight.remainingFormatted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AviationEmerald
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = flight.destIata,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = BoardTextPrimary
                    )
                    Text(
                        text = flight.destCity ?: "Destination",
                        fontSize = 11.sp,
                        color = BoardTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Approach Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BoardCard)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "APPROACH PROGRESS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = BoardTextDim,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(flight.approachProgressPercent * 100).toInt()}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AviationAmber,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { flight.approachProgressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AviationCrimson,
                    trackColor = BoardBorder
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Detailed Telemetry Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Time Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BoardCard)
                        .border(1.dp, BoardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text("TIMINGS (IST)", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = BoardTextDim, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("STA: ${formatTimeIST(flight.scheduledTime)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = BoardTextMuted)
                    Text("ETA: ${formatTimeIST(flight.bestEta)}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BoardTextPrimary)
                    if (flight.delayMinutes > 0) {
                        Text("Delay: +${flight.delayMinutes} min", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = AviationYellow)
                    }
                }

                // Aircraft Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BoardCard)
                        .border(1.dp, BoardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text("AIRCRAFT DOSSIER", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = BoardTextDim, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Reg: ${flight.registration}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AviationTeal)
                    Text("Model: ${flight.aircraftModel}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = BoardTextMuted)
                    Text("Status: ${flight.statusText}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = BoardTextDim)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 10-Minute Heads-Up Alarm (Offline Capable) Card
            val etaIST = formatTimeIST(flight.bestEta)
            val headsUpIST = formatTimeIST(flight.bestEta - 600)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (flight.isTracked) AviationAmber.copy(alpha = 0.12f) else BoardCard
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (flight.isTracked) AviationAmber else BoardBorder
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (flight.isTracked) Icons.Default.AlarmOn else Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (flight.isTracked) AviationAmber else BoardTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (flight.isTracked) "10-MIN HEADS-UP SCHEDULED (OFFLINE READY)" else "10-MIN HEADS-UP ALERT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (flight.isTracked) AviationAmber else BoardTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (flight.isTracked)
                                "Exact system alarm will wake your device and sound alert at $headsUpIST IST (10 min before $etaIST arrival) even when offline or app closed."
                            else
                                "Opt in to receive an automatic system wake-up alarm at $headsUpIST IST (10 min before arrival) even if offline.",
                            fontSize = 10.sp,
                            color = BoardTextMuted,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Primary Action Buttons
            Button(
                onClick = { onToggleTrack(flight) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("detail_track_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (flight.isTracked) BoardCard else AviationCrimson,
                    contentColor = if (flight.isTracked) AviationAmber else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                border = if (flight.isTracked) androidx.compose.foundation.BorderStroke(1.dp, AviationAmber) else null
            ) {
                Icon(
                    imageVector = if (flight.isTracked) Icons.Default.AlarmOn else Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (flight.isTracked) "CANCEL 10-MIN HEADS-UP ALARM" else "OPT-IN 10-MIN HEADS-UP (OFFLINE ALARM)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Discord Immediate Dispatch Action
            OutlinedButton(
                onClick = { onDispatchDiscord10m(flight) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("detail_discord_btn"),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AviationAmber.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AviationAmber)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DISPATCH 10-MIN HEADS-UP TO DISCORD",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
