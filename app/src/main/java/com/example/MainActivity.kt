package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.TacticalTopBar
import com.example.ui.screens.BoardScreen
import com.example.ui.screens.FlightDetailSheet
import com.example.ui.screens.OperationsSpecDialog
import com.example.ui.screens.RadarScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.TrackedScreen
import com.example.ui.screens.TurnaroundScreen
import com.example.ui.theme.AviationAmber
import com.example.ui.theme.AviationCrimson
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.BoardBorder
import com.example.ui.theme.BoardCard
import com.example.ui.theme.BoardSurface
import com.example.ui.theme.BoardTextDim
import com.example.ui.theme.BoardTextMuted
import com.example.ui.theme.BoardTextPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FlightViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FlightViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: FlightViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trackedList by viewModel.trackedFlights.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showOperationsSpecDialog by remember { mutableStateOf(false) }

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BoardBackground),
        topBar = {
            TacticalTopBar(
                airportCode = uiState.selectedAirport,
                lastUpdatedIST = uiState.lastUpdatedIST,
                isLiveServiceActive = uiState.isLiveActivityServiceEnabled,
                onRefreshClick = { viewModel.loadData(force = true) },
                onSettingsClick = { showSettingsDialog = true },
                onInfoClick = { showOperationsSpecDialog = true }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BoardSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(0.5.dp, BoardBorder)
                    .testTag("main_navigation_bar")
            ) {
                val navItems = listOf(
                    Triple(0, "Board", Icons.Default.FlightLand),
                    Triple(1, "Live Activity", Icons.Default.NotificationsActive),
                    Triple(2, "Radar", Icons.Default.Radar),
                    Triple(3, "Turnaround", Icons.Default.SwapHoriz)
                )

                navItems.forEach { (index, label, icon) ->
                    val isSelected = uiState.selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedTab(index) },
                        icon = {
                            if (index == 1 && trackedList.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = AviationCrimson,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = trackedList.size.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AviationCrimson,
                            selectedTextColor = AviationCrimson,
                            unselectedIconColor = BoardTextMuted,
                            unselectedTextColor = BoardTextDim,
                            indicatorColor = BoardCard
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BoardBackground)
        ) {
            when (uiState.selectedTab) {
                0 -> BoardScreen(
                    arrivals = uiState.arrivals,
                    departures = uiState.departures,
                    isLoading = uiState.isLoading,
                    searchQuery = uiState.searchQuery,
                    selectedAirline = uiState.selectedAirlineFilter,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onAirlineSelect = { viewModel.setAirlineFilter(it) },
                    onTrackClick = { viewModel.toggleTrackFlight(it) },
                    onFlightClick = { viewModel.selectFlightForDetail(it) },
                    handledAirlines = uiState.handledAirlines
                )
                1 -> TrackedScreen(
                    trackedEntities = trackedList,
                    allFlights = uiState.arrivals + uiState.departures,
                    isLiveServiceActive = uiState.isLiveActivityServiceEnabled,
                    discordWebhookUrl = uiState.discordWebhookUrl,
                    isWebhookEnabled = uiState.isDiscordWebhookEnabled,
                    testWebhookMessage = uiState.testWebhookMessage,
                    testWebhookSuccess = uiState.testWebhookSuccess,
                    onToggleLiveService = { viewModel.toggleLiveActivityService(it) },
                    onUntrackClick = { viewModel.untrackFlight(it) },
                    onClearAllClick = { viewModel.clearAllTracked() },
                    onTestDiscordClick = { viewModel.testDiscordWebhook() },
                    onManual10mAlertClick = { viewModel.sendManual10mAlert(it) },
                    onFlightClick = { viewModel.selectFlightForDetail(it) },
                    onOpenSettingsClick = { showSettingsDialog = true }
                )
                2 -> RadarScreen(
                    arrivals = uiState.arrivals,
                    onToggleTrack = { viewModel.toggleTrackFlight(it) },
                    onFlightDetail = { viewModel.selectFlightForDetail(it) }
                )
                3 -> TurnaroundScreen(
                    turnarounds = uiState.turnarounds,
                    onFlightClick = { viewModel.selectFlightForDetail(it) }
                )
            }
        }
    }

    // Modal Flight Detail Sheet
    uiState.selectedFlightForDetail?.let { flight ->
        FlightDetailSheet(
            flight = flight,
            onToggleTrack = {
                viewModel.toggleTrackFlight(it)
                viewModel.selectFlightForDetail(it.copy(isTracked = !it.isTracked))
            },
            onDispatchDiscord10m = { viewModel.sendManual10mAlert(it) },
            onDismiss = { viewModel.selectFlightForDetail(null) }
        )
    }

    // Settings Modal Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            discordWebhookUrl = uiState.discordWebhookUrl,
            isWebhookEnabled = uiState.isDiscordWebhookEnabled,
            isLiveActivityEnabled = uiState.isLiveActivityServiceEnabled,
            isSoundEnabled = uiState.isSoundEnabled,
            selectedAirport = uiState.selectedAirport,
            handledAirlines = uiState.handledAirlines,
            testWebhookMessage = uiState.testWebhookMessage,
            testWebhookSuccess = uiState.testWebhookSuccess,
            onSaveWebhookUrl = { viewModel.updateDiscordWebhookUrl(it) },
            onToggleWebhook = { viewModel.toggleDiscordWebhook(it) },
            onToggleLiveActivity = { viewModel.toggleLiveActivityService(it) },
            onToggleSound = { viewModel.toggleSound(it) },
            onSelectAirport = { viewModel.changeAirport(it) },
            onAddAirline = { viewModel.addHandledAirline(it) },
            onRemoveAirline = { viewModel.removeHandledAirline(it) },
            onResetAirlines = { viewModel.resetHandledAirlines() },
            onTestWebhookClick = { viewModel.testDiscordWebhook() },
            onDismiss = {
                showSettingsDialog = false
                viewModel.clearTestWebhookMessage()
            }
        )
    }

    // Operations Specification & Logic Dialog (index.html in-app viewer)
    if (showOperationsSpecDialog) {
        OperationsSpecDialog(
            onDismiss = { showOperationsSpecDialog = false }
        )
    }
}
