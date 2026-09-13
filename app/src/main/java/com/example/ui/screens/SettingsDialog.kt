package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Airlines
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.FlightRadarApi
import com.example.ui.theme.AviationAmber
import com.example.ui.theme.AviationCrimson
import com.example.ui.theme.AviationEmerald
import com.example.ui.theme.AviationRed
import com.example.ui.theme.AviationTeal
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardCard
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.BoardTextDim
import com.example.ui.theme.BoardTextMuted
import com.example.ui.theme.BoardTextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    discordWebhookUrl: String,
    isWebhookEnabled: Boolean,
    isLiveActivityEnabled: Boolean,
    isSoundEnabled: Boolean,
    selectedAirport: String,
    handledAirlines: Set<String>,
    testWebhookMessage: String?,
    testWebhookSuccess: Boolean?,
    onSaveWebhookUrl: (String) -> Unit,
    onToggleWebhook: (Boolean) -> Unit,
    onToggleLiveActivity: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onSelectAirport: (String) -> Unit,
    onAddAirline: (String) -> Unit,
    onRemoveAirline: (String) -> Unit,
    onResetAirlines: () -> Unit,
    onTestWebhookClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputUrl by remember { mutableStateOf(discordWebhookUrl) }
    var newAirlineInput by remember { mutableStateOf("") }

    val airports = listOf(
        Pair("COK", "Cochin"),
        Pair("TRV", "Trivandrum"),
        Pair("CNN", "Kannur"),
        Pair("CCJ", "Calicut"),
        Pair("BLR", "Bangalore"),
        Pair("MAA", "Chennai"),
        Pair("DXB", "Dubai")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BoardBorder, RoundedCornerShape(12.dp)),
            color = BoardSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM SETTINGS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BoardTextPrimary
                        )
                        Text(
                            text = "Discord webhook, Live Activity & Alerts",
                            fontSize = 11.sp,
                            color = BoardTextMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BoardTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Discord Webhook Integration (Explicit user prompt feature!)
                Text(
                    text = "DISCORD WEBHOOK INTEGRATION",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = AviationAmber
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dispatches instant 10-minute ETA heads-up alert cards directly to your Discord operations channel.",
                    fontSize = 11.sp,
                    color = BoardTextMuted,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = {
                        inputUrl = it
                        onSaveWebhookUrl(it)
                    },
                    placeholder = {
                        Text(
                            text = "https://discord.com/api/webhooks/...",
                            fontSize = 11.sp,
                            color = BoardTextDim,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discord_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BoardCard,
                        unfocusedContainerColor = BoardCard,
                        focusedBorderColor = AviationCrimson,
                        unfocusedBorderColor = BoardBorder,
                        focusedTextColor = BoardTextPrimary,
                        unfocusedTextColor = BoardTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isWebhookEnabled,
                            onCheckedChange = onToggleWebhook,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AviationCrimson,
                                uncheckedThumbColor = BoardTextDim,
                                uncheckedTrackColor = BoardBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWebhookEnabled) "Webhook Armed" else "Disabled",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWebhookEnabled) AviationEmerald else BoardTextDim
                        )
                    }

                    Button(
                        onClick = onTestWebhookClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AviationAmber.copy(alpha = 0.2f), contentColor = AviationAmber),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("test_webhook_dialog_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Webhook", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Android Native Features (Foreground Service / Live Activity)
                Text(
                    text = "ANDROID NATIVE LIVE ACTIVITY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = AviationCrimson
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BoardCard)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AviationCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Live Activity Status Bar Service",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BoardTextPrimary
                            )
                            Text(
                                text = "Keeps persistent progress bar notification active even in background",
                                fontSize = 10.sp,
                                color = BoardTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isLiveActivityEnabled,
                        onCheckedChange = onToggleLiveActivity,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AviationCrimson,
                            uncheckedThumbColor = BoardTextDim,
                            uncheckedTrackColor = BoardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sound & Audio Chimes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BoardCard)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = AviationEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Milestone Audio Chimes",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BoardTextPrimary
                            )
                            Text(
                                text = "Play tactical chimes at 60m, 30m, 10m and touchdown",
                                fontSize = 10.sp,
                                color = BoardTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isSoundEnabled,
                        onCheckedChange = onToggleSound,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AviationEmerald,
                            uncheckedThumbColor = BoardTextDim,
                            uncheckedTrackColor = BoardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 3: Handled Airlines (Operations Filtering)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Airlines,
                            contentDescription = null,
                            tint = AviationAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HANDLED AIRLINES (FILTER)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = AviationAmber
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AviationAmber.copy(alpha = 0.15f))
                            .border(1.dp, AviationAmber.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${handledAirlines.size} ACTIVE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = AviationAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Only flights operated by your handled airlines are displayed, radar-tracked, and dispatched via webhook alerts.",
                    fontSize = 10.sp,
                    color = BoardTextMuted,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Add Airline input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newAirlineInput,
                        onValueChange = { if (it.length <= 4) newAirlineInput = it.uppercase() },
                        placeholder = {
                            Text("Add 2-letter IATA (e.g. UL, AI)", fontSize = 11.sp, color = BoardTextDim, fontFamily = FontFamily.Monospace)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("add_airline_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BoardCard,
                            unfocusedContainerColor = BoardCard,
                            focusedBorderColor = AviationAmber,
                            unfocusedBorderColor = BoardBorder,
                            focusedTextColor = BoardTextPrimary,
                            unfocusedTextColor = BoardTextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val code = newAirlineInput.trim().uppercase()
                            if (code.isNotBlank()) {
                                onAddAirline(code)
                                newAirlineInput = ""
                            }
                        },
                        enabled = newAirlineInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AviationAmber,
                            contentColor = Color.Black,
                            disabledContainerColor = BoardBorder,
                            disabledContentColor = BoardTextDim
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("add_airline_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Airline chips display
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    handledAirlines.sorted().forEach { code ->
                        val airlineName = FlightRadarApi.AIRLINE_MAP[code] ?: "Airline $code"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BoardCard)
                                .border(1.dp, BoardBorder, RoundedCornerShape(6.dp))
                                .padding(start = 8.dp, end = 2.dp, top = 3.dp, bottom = 3.dp)
                                .testTag("airline_chip_$code")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = code,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = AviationAmber
                                )
                                Text(
                                    text = airlineName.take(12),
                                    fontSize = 9.sp,
                                    color = BoardTextMuted
                                )
                                IconButton(
                                    onClick = { onRemoveAirline(code) },
                                    modifier = Modifier
                                        .size(20.dp)
                                        .testTag("remove_airline_$code")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove $code",
                                        tint = AviationRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reset to defaults button
                OutlinedButton(
                    onClick = onResetAirlines,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("reset_airlines_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BoardTextMuted)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset Default (IX, AK, FD, FZ, WY, J9, EY, G9, 3L)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 4: Operational Station (Airport Selector)
                Text(
                    text = "OPERATIONAL STATION / AIRPORT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = BoardTextDim
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    airports.take(4).forEach { (code, name) ->
                        val isSelected = selectedAirport.equals(code, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AviationCrimson else BoardCard)
                                .border(1.dp, if (isSelected) AviationCrimson else BoardBorder, RoundedCornerShape(6.dp))
                                .clickable { onSelectAirport(code) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = code,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else BoardTextPrimary
                                )
                                Text(
                                    text = name,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else BoardTextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AviationCrimson),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "SAVE & RETURN TO OPS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
