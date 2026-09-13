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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.AirlineFilterChips
import com.example.ui.components.FlightCardItem
import com.example.ui.components.formatTimeIST
import com.example.ui.theme.AviationAmber
import com.example.ui.theme.AviationCrimson
import com.example.ui.theme.AviationEmerald
import com.example.ui.theme.AviationYellow
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardCard
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.BoardTextDim
import com.example.ui.theme.BoardTextMuted
import com.example.ui.theme.BoardTextPrimary

@Composable
fun BoardScreen(
    arrivals: List<Flight>,
    departures: List<Flight>,
    isLoading: Boolean,
    searchQuery: String,
    selectedAirline: String?,
    onSearchChange: (String) -> Unit,
    onAirlineSelect: (String?) -> Unit,
    onTrackClick: (Flight) -> Unit,
    onFlightClick: (Flight) -> Unit,
    modifier: Modifier = Modifier,
    handledAirlines: Set<String> = emptySet(),
    isLiveData: Boolean = true
) {
    var showArrivals by remember { mutableStateOf(true) }

    val rawList = remember(showArrivals, arrivals, departures, handledAirlines) {
        val list = if (showArrivals) arrivals else departures
        com.example.data.util.AirportUtils.deduplicateFlights(list, handledAirlines)
    }

    val divertedFlights = remember(arrivals, handledAirlines) {
        com.example.data.util.AirportUtils.deduplicateFlights(arrivals, handledAirlines).filter { it.isDivertedToCok }
    }

    val filteredList = remember(rawList, searchQuery, selectedAirline) {
        rawList.filter { flight ->
            val matchesAirline = selectedAirline == null || flight.flightNumber.startsWith(selectedAirline, ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                    flight.flightNumber.lowercase().contains(q) ||
                    flight.registration.lowercase().contains(q) ||
                    flight.displayOriginIata.lowercase().contains(q) ||
                    flight.displayDestIata.lowercase().contains(q) ||
                    (flight.originCity?.lowercase()?.contains(q) == true) ||
                    (flight.destCity?.lowercase()?.contains(q) == true) ||
                    flight.statusText.lowercase().contains(q)
            matchesAirline && matchesSearch
        }
    }

    val delayedCount = remember(rawList) {
        rawList.count { it.delayMinutes > 5 }
    }

    val trackedCount = remember(rawList) {
        rawList.count { it.isTracked }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BoardBackground)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Demo / offline data indicator banner
        if (!isLiveData) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AviationAmber.copy(alpha = 0.15f))
                    .border(1.dp, AviationAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠ Showing demo data — live feed unavailable",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AviationAmber
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Arrivals / Departures Switcher Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BoardSurface)
                .border(1.dp, BoardBorder, RoundedCornerShape(8.dp))
                .padding(3.dp)
        ) {
            // Arrivals tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showArrivals) AviationCrimson else Color.Transparent)
                    .clickable { showArrivals = true }
                    .padding(vertical = 8.dp)
                    .testTag("tab_arrivals"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlightLand,
                        contentDescription = null,
                        tint = if (showArrivals) Color.White else BoardTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ARRIVALS (${arrivals.size})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showArrivals) Color.White else BoardTextMuted
                    )
                }
            }

            // Departures tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (!showArrivals) AviationAmber else Color.Transparent)
                    .clickable { showArrivals = false }
                    .padding(vertical = 8.dp)
                    .testTag("tab_departures"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        tint = if (!showArrivals) Color.White else BoardTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEPARTURES (${departures.size})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!showArrivals) Color.White else BoardTextMuted
                    )
                }
            }
        }

        // Diverted to COK Heads-up Alert Banner
        if (divertedFlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AviationCrimson.copy(alpha = 0.12f))
                    .border(1.5.dp, AviationCrimson.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🚨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DIVERTED TO COK HEADS-UP (${divertedFlights.size})",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AviationCrimson
                            )
                        }
                        Text(
                            text = "TACTICAL ALERT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AviationCrimson
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    divertedFlights.forEach { divFlight ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(BoardSurface)
                                .clickable { onFlightClick(divFlight) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = divFlight.flightNumber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AviationAmber
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${divFlight.displayOriginIata} ➔ Diverted to COK",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = BoardTextPrimary
                                )
                            }
                            Text(
                                text = "ETA ${formatTimeIST(divFlight.bestEta)}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = AviationCrimson
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = "Search flight, reg (e.g. VT-AXP), city...",
                    fontSize = 12.sp,
                    color = BoardTextDim,
                    fontFamily = FontFamily.Monospace
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = BoardTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = BoardTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BoardSurface,
                unfocusedContainerColor = BoardSurface,
                focusedBorderColor = AviationCrimson,
                unfocusedBorderColor = BoardBorder,
                focusedTextColor = BoardTextPrimary,
                unfocusedTextColor = BoardTextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Airline Chips
        AirlineFilterChips(
            selectedAirline = selectedAirline,
            onAirlineSelect = onAirlineSelect,
            handledAirlines = handledAirlines
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tactical Status Metrics row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BoardSurface)
                .border(1.dp, BoardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "STATUS: ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = BoardTextDim
                )
                Text(
                    text = "${filteredList.size} In Window",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BoardTextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (delayedCount > 0) AviationYellow else AviationEmerald)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$delayedCount Delayed",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (delayedCount > 0) AviationYellow else AviationEmerald
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "★ $trackedCount Active",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AviationAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Flight Table List
        if (isLoading && filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = AviationCrimson,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SYNCING LIVE AIRPORT RADAR...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = BoardTextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "✈",
                        fontSize = 32.sp,
                        color = BoardTextDim
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No flights matching criteria",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = BoardTextMuted
                    )
                    Text(
                        text = "Try clearing search filters or changing airport in Settings",
                        fontSize = 11.sp,
                        color = BoardTextDim
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("flight_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredList,
                    key = { flight ->
                        "${flight.id}_${flight.isArrival}"
                    }
                ) { flight ->
                    FlightCardItem(
                        flight = flight,
                        onTrackClick = onTrackClick,
                        onCardClick = onFlightClick
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
