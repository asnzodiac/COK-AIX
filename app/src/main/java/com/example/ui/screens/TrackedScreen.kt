package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TrackedFlightEntity
import com.example.data.model.Flight
import com.example.ui.components.formatTimeIST
import com.example.ui.theme.AviationAmber
import com.example.ui.theme.AviationCrimson
import com.example.ui.theme.AviationEmerald
import com.example.ui.theme.AviationRed
import com.example.ui.theme.AviationTeal
import com.example.ui.theme.AviationYellow
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardCard
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.BoardTextDim
import com.example.ui.theme.BoardTextMuted
import com.example.ui.theme.BoardTextPrimary

@Composable
fun TrackedScreen(
    trackedEntities: List<TrackedFlightEntity>,
    allFlights: List<Flight>,
    isLiveServiceActive: Boolean,
    discordWebhookUrl: String,
    isWebhookEnabled: Boolean,
    testWebhookMessage: String?,
    testWebhookSuccess: Boolean?,
    onToggleLiveService: (Boolean) -> Unit,
    onUntrackClick: (String) -> Unit,
    onClearAllClick: () -> Unit,
    onTestDiscordClick: () -> Unit,
    onManual10mAlertClick: (Flight) -> Unit,
    onFlightClick: (Flight) -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flightsMap = allFlights.associateBy { it.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BoardBackground)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Live Activity Service Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            BoardSurface,
                            BoardCard
                        )
                    )
                )
                .border(
                    1.dp,
                    if (isLiveServiceActive) AviationCrimson.copy(alpha = 0.5f) else BoardBorder,
                    RoundedCornerShape(10.dp)
                )
                .padding(14.dp)
                .testTag("live_activity_banner")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLiveServiceActive) AviationCrimson.copy(alpha = 0.2f) else BoardBorder.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (isLiveServiceActive) AviationCrimson else BoardTextDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Live Activity Notifications",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BoardTextPrimary
                            )
                        }
                        Text(
                            text = if (isLiveServiceActive)
                                "Foreground Service active • Push & Live Progress running"
                            else
                                "Foreground Service inactive • Tap to enable Live Activity",
                            fontSize = 11.sp,
                            color = if (isLiveServiceActive) AviationEmerald else BoardTextMuted
                        )
                    }
                }

                Switch(
                    checked = isLiveServiceActive,
                    onCheckedChange = onToggleLiveService,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AviationCrimson,
                        uncheckedThumbColor = BoardTextDim,
                        uncheckedTrackColor = BoardBorder
                    ),
                    modifier = Modifier.testTag("live_service_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Discord 10-Min Heads-up Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BoardSurface)
                .border(1.dp, BoardBorder, RoundedCornerShape(10.dp))
                .padding(12.dp)
                .testTag("discord_webhook_card")
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (discordWebhookUrl.isNotBlank() && isWebhookEnabled) AviationEmerald else AviationYellow)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DISCORD 10-MIN HEADS-UP",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BoardTextPrimary
                        )
                    }

                    if (discordWebhookUrl.isBlank()) {
                        Text(
                            text = "Configure ⚙",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AviationAmber,
                            modifier = Modifier
                                .clickable { onOpenSettingsClick() }
                                .padding(4.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AviationEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ARMED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AviationEmerald
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (discordWebhookUrl.isNotBlank())
                        "Webhook target configured. Followed flights will automatically dispatch a rich alert card 10 minutes prior to ETA."
                    else
                        "No Discord webhook set. Tap 'Configure' to add your channel webhook URL for instant group alerts.",
                    fontSize = 11.sp,
                    color = BoardTextMuted,
                    lineHeight = 15.sp
                )

                if (discordWebhookUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onTestDiscordClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AviationAmber.copy(alpha = 0.2f),
                                contentColor = AviationAmber
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("test_discord_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Send Test Alert",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = testWebhookMessage != null) {
                    val isSuccess = testWebhookSuccess == true
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSuccess) AviationEmerald.copy(alpha = 0.15f) else AviationRed.copy(alpha = 0.15f))
                            .border(1.dp, if (isSuccess) AviationEmerald else AviationRed, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isSuccess) AviationEmerald else AviationRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = testWebhookMessage ?: "",
                                fontSize = 11.sp,
                                color = if (isSuccess) AviationEmerald else AviationRed
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Followed Flights Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FOLLOWED FLIGHTS (${trackedEntities.size})",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BoardTextPrimary
                )
            }

            if (trackedEntities.isNotEmpty()) {
                IconButton(
                    onClick = onClearAllClick,
                    modifier = Modifier.size(32.dp).testTag("clear_all_tracked_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear all followed",
                        tint = AviationRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (trackedEntities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📡",
                        fontSize = 36.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Followed Flights",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BoardTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Star any flight on the Arrivals board to track its live approach progress and receive 10-minute ETA heads-up notifications.",
                        fontSize = 11.sp,
                        color = BoardTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            val nowSec = System.currentTimeMillis() / 1000

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("tracked_flights_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = trackedEntities,
                    key = { index, entity ->
                        val cleanId = entity.flightId.takeIf { it.isNotBlank() && it != "null" }
                            ?: "${entity.flightNumber.replace(" ", "")}_${entity.scheduledTime}"
                        "${cleanId}_$index"
                    }
                ) { _, entity ->
                    val flight = flightsMap[entity.flightId] ?: Flight(
                        id = entity.flightId,
                        flightNumber = entity.flightNumber,
                        registration = entity.registration,
                        originIata = entity.originIata,
                        originCity = entity.originCity,
                        destIata = entity.destIata,
                        scheduledTime = entity.scheduledTime,
                        estimatedTime = entity.estimatedTime,
                        aircraftModel = entity.aircraftModel,
                        airlineCode = entity.airlineCode,
                        airlineName = entity.airlineName,
                        isArrival = entity.isArrival,
                        isTracked = true
                    )

                    val remainingSec = (flight.bestEta - nowSec).coerceAtLeast(0)
                    val remainingText = flight.remainingFormatted
                    val etaIST = formatTimeIST(flight.bestEta)
                    val progress = flight.approachProgressPercent

                    val barColor = when {
                        remainingSec <= 0 -> AviationRed
                        remainingSec <= 600 -> AviationCrimson
                        remainingSec <= 1800 -> AviationAmber
                        else -> AviationEmerald
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BoardCard)
                            .border(1.dp, AviationCrimson.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { onFlightClick(flight) }
                            .padding(14.dp)
                    ) {
                        Column {
                            // Top Row: Flight Number, Route, Remove button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = flight.flightNumber,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AviationAmber
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${flight.originIata} ➔ ${flight.destIata}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BoardTextPrimary
                                    )
                                }

                                IconButton(
                                    onClick = { onUntrackClick(entity.flightId) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text(
                                        text = "✕",
                                        color = BoardTextMuted,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Metadata: Reg, Model, ETA, Remaining countdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${flight.registration} · ${flight.aircraftModel}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = BoardTextMuted
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ETA $etaIST IST",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BoardTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(barColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = remainingText,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = barColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Live Approach Progress Bar
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = barColor,
                                trackColor = BoardBorder
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Actions: Quick trigger 10-min heads up manually
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (entity.discordWebhookSent10m) "✓ 10m Alert Sent" else "Standby for 10m window",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = if (entity.discordWebhookSent10m) AviationEmerald else BoardTextDim
                                )

                                Button(
                                    onClick = { onManual10mAlertClick(flight) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AviationCrimson.copy(alpha = 0.15f),
                                        contentColor = AviationCrimson
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "⚡ Trigger 10m Heads-Up",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
