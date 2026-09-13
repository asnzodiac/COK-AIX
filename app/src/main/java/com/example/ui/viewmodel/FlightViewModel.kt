package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AIXApplication
import com.example.data.local.TrackedFlightEntity
import com.example.data.model.Flight
import com.example.data.model.Turnaround
import com.example.data.repository.FlightRepository
import com.example.service.FlightAlarmScheduler
import com.example.service.FlightTrackingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class FlightUiState(
    val arrivals: List<Flight> = emptyList(),
    val departures: List<Flight> = emptyList(),
    val turnarounds: List<Turnaround> = emptyList(),
    val isLoading: Boolean = false,
    val lastUpdatedIST: String = "",
    val errorMessage: String? = null,
    val selectedTab: Int = 0, // 0: Board, 1: Tracked/Live Activity, 2: Radar, 3: Turnaround
    val searchQuery: String = "",
    val selectedAirlineFilter: String? = null,
    val selectedFlightForDetail: Flight? = null,
    val discordWebhookUrl: String = "",
    val isDiscordWebhookEnabled: Boolean = true,
    val isLiveActivityServiceEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val selectedAirport: String = "cok",
    val handledAirlines: Set<String> = emptySet(),
    val testWebhookMessage: String? = null,
    val testWebhookSuccess: Boolean? = null
)

class FlightViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FlightRepository =
        (application as? AIXApplication)?.repository ?: FlightRepository(application)

    private val _uiState = MutableStateFlow(
        FlightUiState(
            discordWebhookUrl = repository.getDiscordWebhookUrl(),
            isDiscordWebhookEnabled = repository.isWebhookEnabled(),
            isLiveActivityServiceEnabled = repository.isLiveActivityEnabled(),
            isSoundEnabled = repository.isSoundEnabled(),
            selectedAirport = repository.getSelectedAirport(),
            handledAirlines = repository.getHandledAirlines()
        )
    )
    val uiState: StateFlow<FlightUiState> = _uiState.asStateFlow()

    val trackedFlights: StateFlow<List<TrackedFlightEntity>> = repository.trackedFlightsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var pollJob: Job? = null

    init {
        loadData(force = true)
        startAutoPoll()
    }

    private fun startAutoPoll() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(25000) // 25 seconds refresh
                loadData(force = false)
            }
        }
    }

    fun loadData(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val airport = _uiState.value.selectedAirport
                val arr = repository.getArrivals(airport)
                val dep = repository.getDepartures(airport)

                val trackedIds = trackedFlights.value.map { it.flightId }.toSet()
                val updatedArr = arr.map { it.copy(isTracked = trackedIds.contains(it.id)) }
                val updatedDep = dep.map { it.copy(isTracked = trackedIds.contains(it.id)) }

                val turns = repository.computeTurnarounds(updatedArr, updatedDep)

                val timeIST = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }.format(Date())

                _uiState.update {
                    it.copy(
                        arrivals = updatedArr,
                        departures = updatedDep,
                        turnarounds = turns,
                        isLoading = false,
                        lastUpdatedIST = timeIST,
                        errorMessage = if (updatedArr.isEmpty() && updatedDep.isEmpty()) "No flights found or network offline" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Fetch error: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun toggleTrackFlight(flight: Flight) {
        viewModelScope.launch {
            val isCurrentlyTracked = trackedFlights.value.any { it.flightId == flight.id }
            if (isCurrentlyTracked) {
                repository.untrackFlight(flight.id)
                FlightAlarmScheduler.cancel10MinuteHeadsUp(getApplication(), flight.id)
                (getApplication() as? AIXApplication)?.notificationHelper?.removeFlightLiveActivity(flight.id)
            } else {
                repository.trackFlight(flight)
                // Schedule exact 10-minute heads-up alarm that wakes device even if app is offline or closed
                FlightAlarmScheduler.schedule10MinuteHeadsUp(getApplication(), flight)
                if (_uiState.value.isLiveActivityServiceEnabled) {
                    FlightTrackingService.startService(getApplication())
                }
            }
            // Trigger quick refresh
            loadData(force = false)
        }
    }

    fun untrackFlight(flightId: String) {
        viewModelScope.launch {
            repository.untrackFlight(flightId)
            FlightAlarmScheduler.cancel10MinuteHeadsUp(getApplication(), flightId)
            (getApplication() as? AIXApplication)?.notificationHelper?.removeFlightLiveActivity(flightId)
            loadData(force = false)
        }
    }

    fun clearAllTracked() {
        viewModelScope.launch {
            trackedFlights.value.forEach {
                FlightAlarmScheduler.cancel10MinuteHeadsUp(getApplication(), it.flightId)
            }
            repository.clearAllTracked()
            loadData(force = false)
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setAirlineFilter(airlineCode: String?) {
        _uiState.update {
            val newFilter = if (it.selectedAirlineFilter == airlineCode) null else airlineCode
            it.copy(selectedAirlineFilter = newFilter)
        }
    }

    fun selectFlightForDetail(flight: Flight?) {
        _uiState.update { it.copy(selectedFlightForDetail = flight) }
    }

    fun updateDiscordWebhookUrl(url: String) {
        repository.setDiscordWebhookUrl(url)
        _uiState.update { it.copy(discordWebhookUrl = url, testWebhookMessage = null, testWebhookSuccess = null) }
    }

    fun toggleDiscordWebhook(enabled: Boolean) {
        repository.setWebhookEnabled(enabled)
        _uiState.update { it.copy(isDiscordWebhookEnabled = enabled) }
    }

    fun toggleLiveActivityService(enabled: Boolean) {
        repository.setLiveActivityEnabled(enabled)
        _uiState.update { it.copy(isLiveActivityServiceEnabled = enabled) }
        if (enabled) {
            FlightTrackingService.startService(getApplication())
        } else {
            FlightTrackingService.stopService(getApplication())
        }
    }

    fun toggleSound(enabled: Boolean) {
        repository.setSoundEnabled(enabled)
        _uiState.update { it.copy(isSoundEnabled = enabled) }
    }

    fun changeAirport(airportCode: String) {
        val code = airportCode.lowercase().trim()
        repository.setSelectedAirport(code)
        _uiState.update { it.copy(selectedAirport = code) }
        loadData(force = true)
    }

    fun addHandledAirline(code: String) {
        val updated = repository.addHandledAirline(code)
        _uiState.update { it.copy(handledAirlines = updated) }
        loadData(force = true)
    }

    fun removeHandledAirline(code: String) {
        val updated = repository.removeHandledAirline(code)
        _uiState.update { it.copy(handledAirlines = updated) }
        loadData(force = true)
    }

    fun resetHandledAirlines() {
        val updated = repository.resetHandledAirlines()
        _uiState.update { it.copy(handledAirlines = updated) }
        loadData(force = true)
    }

    fun testDiscordWebhook() {
        val url = _uiState.value.discordWebhookUrl
        if (url.isBlank()) {
            _uiState.update {
                it.copy(
                    testWebhookMessage = "Please enter a Discord Webhook URL first",
                    testWebhookSuccess = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(testWebhookMessage = "Sending test alert...", testWebhookSuccess = null) }
            val result = repository.testDiscordWebhook(url)
            result.onSuccess { msg ->
                _uiState.update { it.copy(testWebhookMessage = msg, testWebhookSuccess = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(testWebhookMessage = "Error: ${err.message}", testWebhookSuccess = false) }
            }
        }
    }

    fun sendManual10mAlert(flight: Flight) {
        viewModelScope.launch {
            val etaIST = SimpleDateFormat("HH:mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }.format(Date(flight.bestEta * 1000))

            val result = repository.sendDiscord10mAlert(flight, etaIST)
            (getApplication() as? AIXApplication)?.notificationHelper?.send10MinuteHeadsUpNotification(
                flight = flight,
                etaIST = etaIST,
                sentToDiscord = result.isSuccess
            )

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        testWebhookMessage = "10-Minute heads-up alert sent for ${flight.flightNumber}!",
                        testWebhookSuccess = true
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        testWebhookMessage = "Discord dispatch failed: ${err.message}",
                        testWebhookSuccess = false
                    )
                }
            }
        }
    }

    fun clearTestWebhookMessage() {
        _uiState.update { it.copy(testWebhookMessage = null, testWebhookSuccess = null) }
    }
}
