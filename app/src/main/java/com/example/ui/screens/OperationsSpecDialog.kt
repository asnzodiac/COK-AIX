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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AviationAmber
import com.example.ui.theme.AviationCrimson
import com.example.ui.theme.AviationEmerald
import com.example.ui.theme.AviationTeal
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardCard
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.BoardTextDim
import com.example.ui.theme.BoardTextMuted
import com.example.ui.theme.BoardTextPrimary

@Composable
fun OperationsSpecDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BoardBorder, RoundedCornerShape(12.dp))
                .testTag("operations_spec_dialog"),
            color = BoardSurface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AviationCrimson.copy(alpha = 0.2f))
                                .border(1.dp, AviationCrimson, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AviationCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OPERATIONS SPEC & LOGIC",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BoardTextPrimary
                            )
                            Text(
                                text = "System Architecture & Formulas (index.html)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = BoardTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BoardTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: index.html Location Notice
                SpecCard(
                    title = "WHERE IS INDEX.HTML?",
                    icon = Icons.Default.Code,
                    accentColor = AviationTeal
                ) {
                    Text(
                        text = "Your complete logic and instructions are preserved in:\n• /index.html (Project Root)\n• /public/index.html (Web Directory)\n\nThis application is a native Android (Kotlin + Jetpack Compose) system, with all mathematical models and operational rules compiled directly into the application.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BoardTextPrimary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 1: 10-Minute Heads-Up Logic
                SpecCard(
                    title = "10-MIN ETA HEADS-UP ENGINE",
                    icon = Icons.Default.NotificationsActive,
                    accentColor = AviationAmber
                ) {
                    Text(
                        text = "1. Condition: (flight.estimatedTime - now) <= 600s\n2. Actions:\n   • Dispatches rich Discord Webhook with flight number, route, reg, aircraft, and ETA in IST.\n   • Triggers Android Heads-Up Push Notification with audio alert.\n3. Deduplication: Tracks state in Room DB (hasDispatched10mAlert = true) to guarantee exactly one alert per approach cycle.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BoardTextPrimary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: Turnaround Matrix Logic
                SpecCard(
                    title = "TURNAROUND GROUND MATRIX",
                    icon = Icons.Default.SwapHoriz,
                    accentColor = AviationEmerald
                ) {
                    Text(
                        text = "Pairs inbound arrivals with outbound departures matching tail registration (e.g. VT-AXP):\n• gapMinutes = (dep.bestEta - arr.bestEta) / 60\n• Sector-specific thresholds:\n   Domestic Sectors (Std: 30 min):\n     < 20 min: CRITICAL TIGHT\n     20 - 30 min: TIGHT TURNAROUND\n     > 30 min: NORMAL GROUND BUFFER\n   International Sectors (Std: 60 min):\n     < 40 min: CRITICAL TIGHT\n     40 - 60 min: TIGHT TURNAROUND\n     > 60 min: NORMAL GROUND BUFFER\n• DELAY RISK: Inbound delay exceeds buffer or max delay > 25 min",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BoardTextPrimary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 3: Radar & Air Map Coordinates
                SpecCard(
                    title = "TACTICAL RADAR & AIR MAP",
                    icon = Icons.Default.Radar,
                    accentColor = AviationCrimson
                ) {
                    Text(
                        text = "• COK / VOCI Reference: 10.152°N, 76.392°E\n• Runway: Nedumbassery 09/27 (3,400m, oriented 090°/270°)\n• RNAV Waypoints:\n   CIA (VOR 113.5) · BIKEN (NW Inbound)\n   NOBAT (West Ocean) · OPAMO (South)\n   VELAG (Northeast Corridor)\n• Dual Views: Radar Scope (concentric rings + beam) & Air Map (Kerala coastline, Kochi harbor, backwaters).",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BoardTextPrimary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AviationCrimson),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "ACKNOWLEDGE & CLOSE",
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

@Composable
private fun SpecCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BoardCard)
            .border(1.dp, BoardBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = accentColor
            )
        }
        content()
    }
}
