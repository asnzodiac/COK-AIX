package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.TrackedFlightDao
import com.example.data.local.TrackedFlightEntity
import com.example.data.model.Flight
import com.example.data.model.Turnaround
import com.example.data.remote.DiscordWebhookClient
import com.example.data.remote.FlightRadarApi
import com.example.data.util.AirportUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FlightRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao: TrackedFlightDao = db.trackedFlightDao()
    private val api = FlightRadarApi()
    private val discord = DiscordWebhookClient()
    private val prefs: SharedPreferences = context.getSharedPreferences("aix_ops_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_DISCORD_WEBHOOK = "pref_discord_webhook"
        const val PREF_ENABLE_WEBHOOK = "pref_enable_webhook"
        const val PREF_SOUND_ENABLED = "pref_sound_enabled"
        const val PREF_HAPTIC_ENABLED = "pref_haptic_enabled"
        const val PREF_SELECTED_AIRPORT = "pref_selected_airport"
        const val PREF_LIVE_ACTIVITY_ENABLED = "pref_live_activity_enabled"
        const val PREF_HANDLED_AIRLINES = "pref_handled_airlines"

        // Handled airlines: IX, AK, FD, FZ, WY, J9, EY, G9, 3L
        val DEFAULT_HANDLED_AIRLINES = linkedSetOf("IX", "AK", "FD", "FZ", "WY", "J9", "EY", "G9", "3L")
    }

    val trackedFlightsFlow: Flow<List<TrackedFlightEntity>> = dao.getAllTrackedFlights()

    fun getDiscordWebhookUrl(): String = prefs.getString(PREF_DISCORD_WEBHOOK, "") ?: ""
    fun setDiscordWebhookUrl(url: String) = prefs.edit().putString(PREF_DISCORD_WEBHOOK, url.trim()).apply()

    fun isWebhookEnabled(): Boolean = prefs.getBoolean(PREF_ENABLE_WEBHOOK, true)
    fun setWebhookEnabled(enabled: Boolean) = prefs.edit().putBoolean(PREF_ENABLE_WEBHOOK, enabled).apply()

    fun isSoundEnabled(): Boolean = prefs.getBoolean(PREF_SOUND_ENABLED, true)
    fun setSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean(PREF_SOUND_ENABLED, enabled).apply()

    fun isLiveActivityEnabled(): Boolean = prefs.getBoolean(PREF_LIVE_ACTIVITY_ENABLED, true)
    fun setLiveActivityEnabled(enabled: Boolean) = prefs.edit().putBoolean(PREF_LIVE_ACTIVITY_ENABLED, enabled).apply()

    fun getSelectedAirport(): String = prefs.getString(PREF_SELECTED_AIRPORT, "cok") ?: "cok"
    fun setSelectedAirport(code: String) = prefs.edit().putString(PREF_SELECTED_AIRPORT, code.lowercase()).apply()

    fun getHandledAirlines(): Set<String> {
        val stored = prefs.getStringSet(PREF_HANDLED_AIRLINES, null)
        return if (stored == null || stored.isEmpty()) {
            DEFAULT_HANDLED_AIRLINES.toSet()
        } else {
            stored.map { it.uppercase().trim() }.filter { it.isNotBlank() }.toSet()
        }
    }

    fun setHandledAirlines(airlines: Set<String>) {
        val sanitized = airlines.map { it.uppercase().trim() }.filter { it.isNotBlank() }.toSet()
        prefs.edit().putStringSet(PREF_HANDLED_AIRLINES, sanitized).apply()
    }

    fun addHandledAirline(code: String): Set<String> {
        val clean = code.uppercase().trim()
        if (clean.isBlank()) return getHandledAirlines()
        val current = getHandledAirlines().toMutableSet()
        current.add(clean)
        setHandledAirlines(current)
        return current
    }

    fun removeHandledAirline(code: String): Set<String> {
        val clean = code.uppercase().trim()
        val current = getHandledAirlines().toMutableSet()
        current.remove(clean)
        setHandledAirlines(current)
        return current
    }

    fun resetHandledAirlines(): Set<String> {
        val defaults = DEFAULT_HANDLED_AIRLINES.toSet()
        setHandledAirlines(defaults)
        return defaults
    }

    var isLastFetchLiveData: Boolean = true
        private set

    fun isAirlineHandled(flightNumber: String, airlineCode: String?, handled: Set<String>): Boolean {
        if (handled.isEmpty()) return false
        val codeFromNum = flightNumber.trim().replace("\\s+".toRegex(), "").take(2).uppercase()
        val codeDirect = airlineCode?.trim()?.uppercase() ?: ""
        return handled.contains(codeFromNum) || (codeDirect.isNotEmpty() && handled.contains(codeDirect))
    }

    suspend fun getArrivals(airport: String = getSelectedAirport()): List<Flight> {
        val live = api.fetchAirportSchedule(airport, "arrivals")
        val isLive = live.isNotEmpty()
        isLastFetchLiveData = isLive
        val raw = if (isLive) live else getFallbackArrivals(airport)
        val handled = getHandledAirlines()
        val filtered = raw.filter { isAirlineHandled(it.flightNumber, it.airlineCode, handled) }
        return AirportUtils.deduplicateFlights(filtered, handled)
    }

    suspend fun getDepartures(airport: String = getSelectedAirport()): List<Flight> {
        val live = api.fetchAirportSchedule(airport, "departures")
        val isLive = live.isNotEmpty()
        if (isLive) {
            isLastFetchLiveData = true
        }
        val raw = if (isLive) live else getFallbackDepartures(airport)
        val handled = getHandledAirlines()
        val filtered = raw.filter { isAirlineHandled(it.flightNumber, it.airlineCode, handled) }
        return AirportUtils.deduplicateFlights(filtered, handled)
    }

    private fun getFallbackArrivals(airport: String): List<Flight> {
        val now = System.currentTimeMillis() / 1000
        val isCok = airport.equals("cok", ignoreCase = true)
        val targetDest = if (isCok) "COK" else airport.uppercase()

        return listOf(
            Flight(
                id = "fl_arr_div_1",
                flightNumber = "IX 384",
                callsign = "AXB384",
                registration = "VT-AYB",
                originIata = "DXB",
                originCity = "Dubai",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 1200,
                estimatedTime = now + 780,
                aircraftModel = "B738",
                statusText = "Approaching (FL040)",
                delayMinutes = 0,
                airlineCode = "IX",
                airlineName = "Air India Express",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("IX 384"),
                isArrival = true,
                liveLatitude = 10.20,
                liveLongitude = 76.15,
                liveHeading = 120f,
                liveAltitudeFt = 4000,
                liveSpeedKts = 230,
                liveIsAirborne = true
            ),
            Flight(
                id = "fl_arr_lag_1",
                flightNumber = "IX 542",
                callsign = "AXB542",
                registration = "VT-AYD",
                originIata = "MCT",
                originCity = "Muscat",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now - 7200, // Scheduled 2 hours ago (lagging delayed flight)
                estimatedTime = now + 1200, // Estimated in 20 min (yet to arrive)
                actualTime = 0L,
                aircraftModel = "B738",
                statusText = "Delayed / En Route (FL280)",
                delayMinutes = 140,
                airlineCode = "IX",
                airlineName = "Air India Express",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("IX 542"),
                isArrival = true,
                liveLatitude = 10.85,
                liveLongitude = 75.40,
                liveHeading = 135f,
                liveAltitudeFt = 28000,
                liveSpeedKts = 430,
                liveIsAirborne = true
            ),
            Flight(
                id = "fl_arr_1",
                flightNumber = "IX 434",
                callsign = "AXB434",
                registration = "VT-AXP",
                originIata = "DXB",
                originCity = "Dubai",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 900,
                estimatedTime = now + 650, // ~10.8 mins - prime candidate for 10-min heads up
                aircraftModel = "B738",
                statusText = "Approaching (Desc. 8500ft)",
                delayMinutes = 0,
                airlineCode = "IX",
                airlineName = "Air India Express",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("IX 434"),
                isArrival = true,
                liveLatitude = 10.32,
                liveLongitude = 75.92,
                liveHeading = 112f,
                liveAltitudeFt = 8500,
                liveSpeedKts = 280,
                liveIsAirborne = true
            ),
            Flight(
                id = "fl_arr_2",
                flightNumber = "AK 38",
                callsign = "AXM38",
                registration = "9M-AHD",
                originIata = "KUL",
                originCity = "Kuala Lumpur",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 1800,
                estimatedTime = now + 1950,
                aircraftModel = "A320",
                statusText = "En Route (FL320)",
                delayMinutes = 2,
                airlineCode = "AK",
                airlineName = "AirAsia",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("AK 38"),
                isArrival = true,
                liveLatitude = 10.60,
                liveLongitude = 77.10,
                liveHeading = 265f,
                liveAltitudeFt = 32000,
                liveSpeedKts = 460,
                liveIsAirborne = true
            ),
            Flight(
                id = "fl_arr_3",
                flightNumber = "FD 170",
                callsign = "AIQ170",
                registration = "HS-ABB",
                originIata = "DMK",
                originCity = "Bangkok",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 2700,
                estimatedTime = now + 2700,
                aircraftModel = "A320",
                statusText = "En Route (FL350)",
                delayMinutes = 0,
                airlineCode = "FD",
                airlineName = "Thai AirAsia",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("FD 170"),
                isArrival = true,
                liveLatitude = 10.95,
                liveLongitude = 77.80,
                liveHeading = 260f,
                liveAltitudeFt = 35000,
                liveSpeedKts = 470,
                liveIsAirborne = true
            ),
            Flight(
                id = "fl_arr_4",
                flightNumber = "FZ 441",
                callsign = "FDB441",
                registration = "A6-FDO",
                originIata = "DXB",
                originCity = "Dubai",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 3600,
                estimatedTime = now + 3900,
                aircraftModel = "B38M",
                statusText = "En Route (FL370)",
                delayMinutes = 5,
                airlineCode = "FZ",
                airlineName = "flydubai",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("FZ 441"),
                isArrival = true,
                liveLatitude = 11.40,
                liveLongitude = 74.40,
                liveHeading = 125f,
                liveAltitudeFt = 37000,
                liveSpeedKts = 480,
                liveIsAirborne = true
            ),
            Flight(
                id = "fl_arr_5",
                flightNumber = "WY 225",
                callsign = "OMA225",
                registration = "A4O-BA",
                originIata = "MCT",
                originCity = "Muscat",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 4800,
                estimatedTime = now + 4800,
                aircraftModel = "B738",
                statusText = "En Route (FL360)",
                delayMinutes = 0,
                airlineCode = "WY",
                airlineName = "Oman Air",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("WY 225"),
                isArrival = true,
                liveLatitude = 11.90,
                liveLongitude = 73.80,
                liveHeading = 130f,
                liveAltitudeFt = 36000,
                liveSpeedKts = 465,
                liveIsAirborne = true
            ),
            Flight(
                id = "fl_arr_6",
                flightNumber = "J9 405",
                callsign = "JZR405",
                registration = "9K-CAQ",
                originIata = "KWI",
                originCity = "Kuwait",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 6000,
                estimatedTime = now + 6000,
                aircraftModel = "A320",
                statusText = "On Time",
                delayMinutes = 0,
                airlineCode = "J9",
                airlineName = "Jazeera Airways",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("J9 405"),
                isArrival = true,
                liveLatitude = 12.30,
                liveLongitude = 73.20,
                liveHeading = 135f,
                liveAltitudeFt = 38000,
                liveSpeedKts = 475,
                liveIsAirborne = true
            ),
            Flight(
                id = "fl_arr_7",
                flightNumber = "EY 280",
                callsign = "ETD280",
                registration = "A6-EIA",
                originIata = "AUH",
                originCity = "Abu Dhabi",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 7200,
                estimatedTime = now + 7200,
                aircraftModel = "A321",
                statusText = "On Time",
                delayMinutes = 0,
                airlineCode = "EY",
                airlineName = "Etihad Airways",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("EY 280"),
                isArrival = true
            ),
            Flight(
                id = "fl_arr_8",
                flightNumber = "G9 426",
                callsign = "ABY426",
                registration = "A6-AOA",
                originIata = "SHJ",
                originCity = "Sharjah",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 8400,
                estimatedTime = now + 8400,
                aircraftModel = "A320",
                statusText = "Scheduled",
                delayMinutes = 0,
                airlineCode = "G9",
                airlineName = "Air Arabia",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("G9 426"),
                isArrival = true
            ),
            Flight(
                id = "fl_arr_9",
                flightNumber = "3L 055",
                callsign = "NAV055",
                registration = "A6-AUA",
                originIata = "AUH",
                originCity = "Abu Dhabi",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 9600,
                estimatedTime = now + 9600,
                aircraftModel = "A320",
                statusText = "Scheduled",
                delayMinutes = 0,
                airlineCode = "3L",
                airlineName = "Air Arabia Abu Dhabi",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("3L 055"),
                isArrival = true
            ),
            Flight(
                id = "fl_arr_10",
                flightNumber = "IX 474",
                callsign = "AXB474",
                registration = "VT-AYC",
                originIata = "BAH",
                originCity = "Bahrain",
                destIata = targetDest,
                destCity = "Cochin",
                scheduledTime = now + 10800,
                estimatedTime = now + 10800,
                aircraftModel = "B738",
                statusText = "Scheduled",
                delayMinutes = 0,
                airlineCode = "IX",
                airlineName = "Air India Express",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("IX 474"),
                isArrival = true
            )
        )
    }

    private fun getFallbackDepartures(airport: String): List<Flight> {
        val now = System.currentTimeMillis() / 1000
        val isCok = airport.equals("cok", ignoreCase = true)
        val targetOrigin = if (isCok) "COK" else airport.uppercase()

        return listOf(
            Flight(
                id = "fl_dep_dom_1",
                flightNumber = "IX 1120",
                callsign = "AXB1120",
                registration = "VT-AYD", // Pairs with IX 542 (VT-AYD) - 35 min turnaround (Domestic std: 30m)!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "BLR",
                destCity = "Bengaluru",
                scheduledTime = now + 3300,
                estimatedTime = now + 3300,
                aircraftModel = "B738",
                statusText = "Domestic Turn / Boarding",
                delayMinutes = 0,
                airlineCode = "IX",
                airlineName = "Air India Express",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("IX 1120"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_1",
                flightNumber = "IX 473",
                callsign = "AXB473",
                registration = "VT-AXP", // Pairs with IX 434 (VT-AXP) - 60 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "BAH",
                destCity = "Bahrain",
                scheduledTime = now + 4250,
                estimatedTime = now + 4250,
                aircraftModel = "B738",
                statusText = "Scheduled / Turnaround",
                delayMinutes = 0,
                airlineCode = "IX",
                airlineName = "Air India Express",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("IX 473"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_2",
                flightNumber = "AK 39",
                callsign = "AXM39",
                registration = "9M-AHD", // Pairs with AK 38 (9M-AHD) - 55 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "KUL",
                destCity = "Kuala Lumpur",
                scheduledTime = now + 5250,
                estimatedTime = now + 5250,
                aircraftModel = "A320",
                statusText = "Scheduled / Turnaround",
                delayMinutes = 0,
                airlineCode = "AK",
                airlineName = "AirAsia",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("AK 39"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_3",
                flightNumber = "FD 171",
                callsign = "AIQ171",
                registration = "HS-ABB", // Pairs with FD 170 (HS-ABB) - 50 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "DMK",
                destCity = "Bangkok",
                scheduledTime = now + 5700,
                estimatedTime = now + 5700,
                aircraftModel = "A320",
                statusText = "Scheduled / Turnaround",
                delayMinutes = 0,
                airlineCode = "FD",
                airlineName = "Thai AirAsia",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("FD 171"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_4",
                flightNumber = "FZ 442",
                callsign = "FDB442",
                registration = "A6-FDO", // Pairs with FZ 441 (A6-FDO) - 60 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "DXB",
                destCity = "Dubai",
                scheduledTime = now + 7500,
                estimatedTime = now + 7500,
                aircraftModel = "B38M",
                statusText = "Scheduled / Turnaround",
                delayMinutes = 0,
                airlineCode = "FZ",
                airlineName = "flydubai",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("FZ 442"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_5",
                flightNumber = "WY 226",
                callsign = "OMA226",
                registration = "A4O-BA", // Pairs with WY 225 (A4O-BA) - 55 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "MCT",
                destCity = "Muscat",
                scheduledTime = now + 8100,
                estimatedTime = now + 8100,
                aircraftModel = "B738",
                statusText = "Scheduled / Turnaround",
                delayMinutes = 0,
                airlineCode = "WY",
                airlineName = "Oman Air",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("WY 226"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_6",
                flightNumber = "J9 406",
                callsign = "JZR406",
                registration = "9K-CAQ", // Pairs with J9 405 (9K-CAQ) - 60 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "KWI",
                destCity = "Kuwait",
                scheduledTime = now + 9600,
                estimatedTime = now + 9600,
                aircraftModel = "A320",
                statusText = "Scheduled / Turnaround",
                delayMinutes = 0,
                airlineCode = "J9",
                airlineName = "Jazeera Airways",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("J9 406"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_7",
                flightNumber = "EY 281",
                callsign = "ETD281",
                registration = "A6-EIA", // Pairs with EY 280 (A6-EIA) - 60 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "AUH",
                destCity = "Abu Dhabi",
                scheduledTime = now + 10800,
                estimatedTime = now + 10800,
                aircraftModel = "A321",
                statusText = "Scheduled / Turnaround",
                delayMinutes = 0,
                airlineCode = "EY",
                airlineName = "Etihad Airways",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("EY 281"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_8",
                flightNumber = "G9 427",
                callsign = "ABY427",
                registration = "A6-AOA", // Pairs with G9 426 (A6-AOA) - 50 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "SHJ",
                destCity = "Sharjah",
                scheduledTime = now + 11400,
                estimatedTime = now + 11400,
                aircraftModel = "A320",
                statusText = "Scheduled / Turnaround",
                delayMinutes = 0,
                airlineCode = "G9",
                airlineName = "Air Arabia",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("G9 427"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_9",
                flightNumber = "3L 056",
                callsign = "NAV056",
                registration = "A6-AUA", // Pairs with 3L 055 (A6-AUA) - 50 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "AUH",
                destCity = "Abu Dhabi",
                scheduledTime = now + 12600,
                estimatedTime = now + 12600,
                aircraftModel = "A320",
                statusText = "Scheduled / Turnaround",
                delayMinutes = 0,
                airlineCode = "3L",
                airlineName = "Air Arabia Abu Dhabi",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("3L 056"),
                isArrival = false
            ),
            Flight(
                id = "fl_dep_10",
                flightNumber = "IX 475",
                callsign = "AXB475",
                registration = "VT-AYC", // Pairs with IX 474 (VT-AYC) - 65 min gap!
                originIata = targetOrigin,
                originCity = "Cochin",
                destIata = "DOH",
                destCity = "Doha",
                scheduledTime = now + 14700,
                estimatedTime = now + 14700,
                aircraftModel = "B738",
                statusText = "Scheduled",
                delayMinutes = 0,
                airlineCode = "IX",
                airlineName = "Air India Express",
                logoUrl = FlightRadarApi.getAirlineLogoUrl("IX 475"),
                isArrival = false
            )
        )
    }

    suspend fun getLiveTelemetry(flightId: String): FlightRadarApi.LiveTelemetry? {
        return api.fetchLiveTelemetry(flightId)
    }

    suspend fun isFlightTracked(flightId: String): Boolean {
        return dao.getFlightById(flightId) != null
    }

    suspend fun trackFlight(flight: Flight) {
        val entity = TrackedFlightEntity(
            flightId = flight.id,
            flightNumber = flight.flightNumber,
            registration = flight.registration,
            originIata = flight.originIata,
            originCity = flight.originCity,
            destIata = flight.destIata,
            scheduledTime = flight.scheduledTime,
            estimatedTime = flight.estimatedTime,
            aircraftModel = flight.aircraftModel,
            airlineCode = flight.airlineCode,
            airlineName = flight.airlineName,
            isArrival = flight.isArrival,
            lastKnownStatus = flight.statusText
        )
        dao.insertTrackedFlight(entity)
    }

    suspend fun untrackFlight(flightId: String) {
        dao.deleteTrackedFlight(flightId)
    }

    suspend fun clearAllTracked() {
        dao.clearAll()
    }

    suspend fun updateTrackedEntity(entity: TrackedFlightEntity) {
        dao.updateTrackedFlight(entity)
    }

    suspend fun getAllTrackedSync(): List<TrackedFlightEntity> {
        return dao.getAllTrackedFlightsSync()
    }

    suspend fun sendDiscord10mAlert(flight: Flight, etaIST: String): Result<String> {
        val url = getDiscordWebhookUrl()
        if (url.isBlank() || !isWebhookEnabled()) {
            return Result.failure(IllegalStateException("Discord Webhook is not configured or disabled"))
        }
        return discord.send10MinuteHeadsUp(url, flight, etaIST)
    }

    suspend fun testDiscordWebhook(url: String = getDiscordWebhookUrl()): Result<String> {
        return discord.sendTestWebhook(url)
    }

    fun computeTurnarounds(arrivals: List<Flight>, departures: List<Flight>): List<Turnaround> {
        val cleanArr = AirportUtils.deduplicateFlights(arrivals)
        val cleanDep = AirportUtils.deduplicateFlights(departures)
        val arrByReg = cleanArr.filter { it.registration != "–" && it.registration.isNotBlank() }
            .groupBy { it.registration.trim().uppercase() }
        val depByReg = cleanDep.filter { it.registration != "–" && it.registration.isNotBlank() }
            .groupBy { it.registration.trim().uppercase() }

        val turnarounds = mutableListOf<Turnaround>()
        val pairedDepIds = mutableSetOf<String>()

        for ((reg, arrList) in arrByReg) {
            val depList = depByReg[reg] ?: continue
            val sortedArr = arrList.sortedBy { it.bestEta }
            val sortedDep = depList.sortedBy { it.bestEta }

            for (arr in sortedArr) {
                // Find the first departure after this arrival within 180 min that has not been paired
                val dep = sortedDep.firstOrNull { candidate ->
                    !pairedDepIds.contains(candidate.id) &&
                    candidate.bestEta >= arr.bestEta &&
                    (candidate.bestEta - arr.bestEta) in 60..10800 // 1 to 180 min
                } ?: continue

                pairedDepIds.add(dep.id)
                val gapSeconds = dep.bestEta - arr.bestEta
                val gapMinutes = (gapSeconds / 60).toInt()
                val maxDelay = maxOf(arr.delayMinutes, dep.delayMinutes)
                val isDomestic = AirportUtils.isIndianDomestic(dep.destIata, dep.destCity)
                turnarounds.add(
                    Turnaround(
                        registration = reg,
                        arrivalFlight = arr,
                        departureFlight = dep,
                        gapMinutes = gapMinutes,
                        aircraftModel = arr.aircraftModel.ifBlank { dep.aircraftModel },
                        maxDelayMinutes = maxDelay,
                        isDomestic = isDomestic
                    )
                )
            }
        }

        return turnarounds.distinctBy { "${it.registration}_${it.arrivalFlight.flightNumber}_${it.departureFlight.flightNumber}" }
            .sortedBy { it.arrivalFlight.bestEta }
    }
}
