package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.data.remote.FlightRadarApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.Flight
import com.example.data.model.FlightStatusCategory
import com.example.ui.theme.AviationAmber
import com.example.ui.theme.AviationCrimson
import com.example.ui.theme.AviationEmerald
import com.example.ui.theme.AviationRed
import com.example.ui.theme.AviationTeal
import com.example.ui.theme.AviationYellow
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardCard
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.BoardTextDim
import com.example.ui.theme.BoardTextMuted
import com.example.ui.theme.BoardTextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun TacticalTopBar(
    airportCode: String,
    lastUpdatedIST: String,
    isLiveServiceActive: Boolean,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInfoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BoardSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand mark & Station Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(AviationCrimson, AviationAmber)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✈",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AIX SEC OPS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BoardTextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AviationCrimson.copy(alpha = 0.2f))
                                    .border(0.5.dp, AviationCrimson.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = airportCode.uppercase(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AviationCrimson
                                )
                            }
                        }
                        Text(
                            text = "COCHIN INTL · GROUND & RADAR",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = BoardTextMuted,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Actions: Status Pill, Refresh, Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Live Status Pill
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BoardCard)
                            .border(1.dp, BoardBorder, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AviationEmerald.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (lastUpdatedIST.isNotBlank()) lastUpdatedIST.take(5) else "LIVE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = AviationEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh board",
                            tint = BoardTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings and Discord Webhook",
                            tint = if (isLiveServiceActive) AviationAmber else BoardTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Operations Spec and Instructions",
                            tint = AviationTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getAirlineBrandColor(code: String): Color {
    return when (code.uppercase()) {
        "IX" -> Color(0xFFD32F2F) // Air India Express Red
        "AK", "FD" -> Color(0xFFED1C24) // AirAsia Red
        "FZ" -> Color(0xFF005596) // flydubai Blue
        "WY" -> Color(0xFFB8860B) // Oman Air Gold
        "J9" -> Color(0xFF0099DA) // Jazeera Blue
        "EY" -> Color(0xFFC5A059) // Etihad Gold
        "G9", "3L" -> Color(0xFFD31245) // Air Arabia Crimson
        "AI" -> Color(0xFFED1B24) // Air India
        "6E" -> Color(0xFF001B94) // IndiGo
        "EK" -> Color(0xFFD71921) // Emirates
        else -> AviationAmber
    }
}

@Composable
fun AirlineLogoBadge(
    airlineCode: String,
    logoUrl: String?,
    airlineName: String,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 46.dp,
    height: androidx.compose.ui.unit.Dp = 22.dp
) {
    val code = airlineCode.ifBlank { "FL" }.take(3).uppercase()
    val brandColor = remember(code) { getAirlineBrandColor(code) }
    val effectiveUrl = logoUrl ?: FlightRadarApi.getAirlineLogoUrl(code)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(0.75.dp, Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = effectiveUrl,
            contentDescription = airlineName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = brandColor
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = brandColor
                    )
                }
            }
        )
    }
}

@Composable
fun FlightCardItem(
    flight: Flight,
    onTrackClick: (Flight) -> Unit,
    onCardClick: (Flight) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (flight.statusCategory) {
        FlightStatusCategory.ON_TIME -> AviationEmerald
        FlightStatusCategory.DELAYED -> AviationYellow
        FlightStatusCategory.HEAVY_DELAY -> AviationRed
        FlightStatusCategory.APPROACHING -> AviationCrimson
        FlightStatusCategory.LANDED -> AviationTeal
        FlightStatusCategory.CANCELLED -> BoardTextDim
    }

    val etaFormatted = formatTimeIST(flight.bestEta)
    val schFormatted = formatTimeIST(flight.scheduledTime)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BoardCard)
            .border(1.dp, if (flight.isTracked) AviationCrimson.copy(alpha = 0.6f) else BoardBorder, RoundedCornerShape(8.dp))
            .clickable { onCardClick(flight) }
            .testTag("flight_item_${flight.flightNumber}")
    ) {
        // Left tactical indicator bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(4.dp)
                .height(68.dp)
                .background(statusColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Flight ID & Registration & Aircraft & Logo
            Row(
                modifier = Modifier.weight(1.35f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AirlineLogoBadge(
                    airlineCode = flight.airlineCode,
                    logoUrl = flight.logoUrl,
                    airlineName = flight.airlineName,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Column {
                    Text(
                        text = flight.flightNumber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AviationTeal
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = flight.registration,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = BoardTextMuted
                        )
                        Text(
                            text = " · ${flight.aircraftModel}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = BoardTextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Route: From -> To
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = flight.originIata,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BoardTextPrimary
                    )
                    Text(
                        text = " ➔ ",
                        fontSize = 11.sp,
                        color = BoardTextDim
                    )
                    Text(
                        text = flight.destIata,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BoardTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val cityText = (if (flight.isArrival) flight.originCity else flight.destCity) ?: ""
                Text(
                    text = cityText.ifBlank { if (flight.isArrival) "Inbound" else "Outbound" },
                    fontSize = 10.sp,
                    color = BoardTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ETA / ETD & Status
            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = etaFormatted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BoardTextPrimary
                    )
                    if (flight.delayMinutes > 5) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "+${flight.delayMinutes}m",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = AviationYellow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = flight.statusText.take(14),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Track toggle star button
            IconButton(
                onClick = { onTrackClick(flight) },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("track_btn_${flight.flightNumber}")
            ) {
                Icon(
                    imageVector = if (flight.isTracked) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (flight.isTracked) "Unfollow flight" else "Follow flight for Live Activity",
                    tint = if (flight.isTracked) AviationAmber else BoardTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AirlineFilterChips(
    selectedAirline: String?,
    onAirlineSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    handledAirlines: Set<String> = emptySet()
) {
    val airlines = remember(handledAirlines) {
        val list = mutableListOf<Pair<String, String?>>(Pair("ALL", null))
        if (handledAirlines.isNotEmpty()) {
            handledAirlines.sorted().forEach { code ->
                val name = FlightRadarApi.AIRLINE_MAP[code] ?: "Airline $code"
                list.add(Pair(code, name))
            }
        } else {
            list.addAll(
                listOf(
                    Pair("IX", "Air India Express"),
                    Pair("AK", "AirAsia"),
                    Pair("FD", "Thai AirAsia"),
                    Pair("FZ", "flydubai"),
                    Pair("WY", "Oman Air"),
                    Pair("J9", "Jazeera"),
                    Pair("EY", "Etihad"),
                    Pair("G9", "Air Arabia"),
                    Pair("3L", "Air Arabia Abu Dhabi")
                )
            )
        }
        list
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        airlines.forEach { (code, label) ->
            val isSelected = (code == "ALL" && selectedAirline == null) || (code == selectedAirline)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (isSelected) AviationCrimson else BoardCard)
                    .border(
                        1.dp,
                        if (isSelected) AviationCrimson else BoardBorder,
                        RoundedCornerShape(99.dp)
                    )
                    .clickable { onAirlineSelect(if (code == "ALL") null else code) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else BoardTextMuted
                )
            }
        }
    }
}

fun formatTimeIST(timestampSec: Long): String {
    if (timestampSec <= 0) return "–"
    val df = SimpleDateFormat("HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }
    return df.format(Date(timestampSec * 1000))
}
