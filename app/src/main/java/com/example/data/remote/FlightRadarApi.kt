package com.example.data.remote

import android.util.Log
import com.example.data.model.Flight
import com.example.data.util.AirportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FlightRadarApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    companion object {
        const val TAG = "FlightRadarApi"
        const val DEFAULT_AIRPORT = "cok"

        val AIRPORT_COORDS = mapOf(
            "COK" to Pair(10.152, 76.392),
            "DXB" to Pair(25.253, 55.365),
            "SHJ" to Pair(25.328, 55.517),
            "DOH" to Pair(25.273, 51.608),
            "MCT" to Pair(23.593, 58.284),
            "AUH" to Pair(24.433, 54.651),
            "BAH" to Pair(26.271, 50.634),
            "KWI" to Pair(29.227, 47.969),
            "RUH" to Pair(24.958, 46.699),
            "JED" to Pair(21.680, 39.157),
            "CMB" to Pair(7.181, 79.884),
            "MLE" to Pair(4.192, 73.529),
            "SIN" to Pair(1.350, 103.994),
            "KUL" to Pair(2.745, 101.710),
            "BKK" to Pair(13.690, 100.750),
            "DEL" to Pair(28.556, 77.100),
            "BOM" to Pair(19.089, 72.868),
            "BLR" to Pair(13.199, 77.710),
            "MAA" to Pair(12.994, 80.171),
            "HYD" to Pair(17.231, 78.430),
            "CCU" to Pair(22.655, 88.446),
            "GOI" to Pair(15.381, 73.831),
            "TRV" to Pair(8.482, 76.920),
            "CNN" to Pair(11.918, 75.547),
            "CJB" to Pair(11.030, 77.043),
            "IXE" to Pair(12.961, 74.890),
            "IXM" to Pair(9.835, 78.093),
            "SLL" to Pair(17.039, 54.091),
            "PNQ" to Pair(18.582, 73.920),
            "AMD" to Pair(23.077, 72.635),
            "LKO" to Pair(26.761, 80.889)
        )

        val AIRLINE_MAP = mapOf(
            "IX" to "Air India Express",
            "AI" to "Air India",
            "6E" to "IndiGo",
            "UK" to "Vistara",
            "SG" to "SpiceJet",
            "QP" to "Akasa Air",
            "EK" to "Emirates",
            "EY" to "Etihad Airways",
            "QR" to "Qatar Airways",
            "SV" to "Saudia",
            "KU" to "Kuwait Airways",
            "WY" to "Oman Air",
            "GF" to "Gulf Air",
            "J9" to "Jazeera Airways",
            "G9" to "Air Arabia",
            "3L" to "Air Arabia Abu Dhabi",
            "UL" to "SriLankan Airlines",
            "SQ" to "Singapore Airlines",
            "TR" to "Scoot",
            "MH" to "Malaysia Airlines",
            "TG" to "Thai Airways",
            "FZ" to "flydubai",
            "AK" to "AirAsia",
            "FD" to "Thai AirAsia"
        )

        val BRANDFETCH_DOMAINS = mapOf(
            "IX" to "airindiaexpress.com",
            "AI" to "airindia.com",
            "6E" to "goindigo.in",
            "UK" to "airvistara.com",
            "SG" to "spicejet.com",
            "QP" to "akasaair.com",
            "EK" to "emirates.com",
            "EY" to "etihad.com",
            "QR" to "qatarairways.com",
            "WY" to "omanair.com",
            "J9" to "jazeeraairways.com",
            "G9" to "airarabia.com",
            "3L" to "airarabia.com",
            "UL" to "srilankan.com",
            "FZ" to "flydubai.com"
        )

        fun getAirlineName(flightNumber: String): String {
            if (flightNumber.length < 2) return "Aviation"
            val code = flightNumber.take(2).uppercase()
            return AIRLINE_MAP[code] ?: "Airline $code"
        }

        fun getAirlineLogoUrl(flightNumber: String): String {
            val code = flightNumber.trim().take(2).uppercase()
            return when (code) {
                "J9" -> "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6d/Jazeera_Airways_logo.svg/250px-Jazeera_Airways_logo.svg.png"
                "AK", "FD" -> "https://cdn.flightradar24.com/assets/airlines/logotypes/25.png"
                else -> "https://pics.avs.io/200/80/$code.png"
            }
        }
    }

    suspend fun fetchAirportScheduleMultiWindow(
        airport: String = DEFAULT_AIRPORT,
        mode: String = "arrivals",
        baseTimestampSec: Long? = null,
        includeEarlierWindow: Boolean = true
    ): List<Flight> = withContext(Dispatchers.IO) {
        val baseTs = baseTimestampSec ?: (System.currentTimeMillis() / 1000)
        // Multi-window logic matching asnzodiac.github.io/AIX_COK:
        // Window -1: past 6 hours (fetches delayed flights scheduled up to 5h ago yet to arrive)
        // Windows 0, 1, 2, 3: covers current time up to +24 hours ahead
        val windowOffsets = if (includeEarlierWindow) listOf(-1, 0, 1, 2, 3) else listOf(0, 1, 2, 3)
        val deferred = windowOffsets.map { offset ->
            async {
                val ts = baseTs + offset * 6 * 3600
                fetchAirportScheduleForTimestamp(airport, mode, ts)
            }
        }
        val allLists = deferred.awaitAll()
        val combined = allLists.flatten()
        com.example.data.util.AirportUtils.deduplicateFlights(combined)
    }

    suspend fun fetchAirportSchedule(
        airport: String = DEFAULT_AIRPORT,
        mode: String = "arrivals",
        timestampSec: Long? = null
    ): List<Flight> {
        return fetchAirportScheduleMultiWindow(airport, mode, timestampSec, includeEarlierWindow = true)
    }

    private suspend fun fetchAirportScheduleForTimestamp(
        airport: String,
        mode: String,
        ts: Long
    ): List<Flight> = withContext(Dispatchers.IO) {
        val rawUrl = "https://api.flightradar24.com/common/v1/airport.json?code=${airport.lowercase()}&plugin-setting[schedule][mode]=$mode&plugin-setting[schedule][timestamp]=$ts&limit=100"

        val candidates = listOf(
            rawUrl,
            "https://corsproxy.io/?" + URLEncoder.encode(rawUrl, "UTF-8"),
            "https://api.allorigins.win/raw?url=" + URLEncoder.encode(rawUrl, "UTF-8")
        )

        for (target in candidates) {
            try {
                val req = Request.Builder()
                    .url(target)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; AIXSecOps)")
                    .header("Accept", "application/json")
                    .build()

                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: return@use
                        val parsed = parseScheduleJson(body, mode == "arrivals", airport)
                        if (parsed.isNotEmpty()) {
                            return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetch from $target: ${e.message}")
            }
        }
        emptyList()
    }

    fun isInOperationalWindow(isArrival: Boolean, sch: Long, est: Long, act: Long, statusText: String): Boolean {
        val now = System.currentTimeMillis() / 1000
        val targetTime = if (est > 0) est else sch
        val fiveHoursAgo = now - 18000 // 5 hours earlier (18000s)
        val twentyHoursAhead = now + 72000 // 20 hours ahead (72000s)

        // 1. Standard operational window (-5h to +20h)
        val inRange = targetTime in fiveHoursAgo..twentyHoursAhead

        // 2. Delayed flights yet to arrive: scheduled up to 5 hours earlier (sch in fiveHoursAgo..now),
        // but haven't landed/arrived yet (act == 0L || act > now), not cancelled
        val isLaggingDelayed = (sch in fiveHoursAgo..now) && (act == 0L || act > now) &&
                !statusText.lowercase().contains("cancel")

        // 3. Recently landed / completed (within last 3 hours for arrivals, 30 min for departures)
        val recent = if (isArrival) (act in (now - 10800)..now) else (act in (now - 1800)..now)

        return inRange || isLaggingDelayed || recent
    }

    fun parseScheduleJson(raw: String, isArrival: Boolean, currentStation: String = DEFAULT_AIRPORT): List<Flight> {
        val results = mutableListOf<Flight>()
        try {
            val i = raw.indexOf('{')
            val j = raw.lastIndexOf('}')
            val cleanJson = if (i >= 0 && j > i) raw.substring(i, j + 1) else raw
            val root = JSONObject(cleanJson)
            val scheduleObj = root.optJSONObject("result")
                ?.optJSONObject("response")
                ?.optJSONObject("airport")
                ?.optJSONObject("pluginData")
                ?.optJSONObject("schedule") ?: return emptyList()

            val listKey = if (isArrival) "arrivals" else "departures"
            val array = scheduleObj.optJSONObject(listKey)?.optJSONArray("data") ?: return emptyList()

            val stationUpper = currentStation.uppercase().trim()
            val defaultStationCity = AirportUtils.getCityForAirport(stationUpper) ?: "Cochin"

            for (idx in 0 until array.length()) {
                val item = array.optJSONObject(idx) ?: continue
                val flightObj = item.optJSONObject("flight") ?: continue
                val identObj = flightObj.optJSONObject("identification")
                val flightNumber = identObj?.optJSONObject("number")?.optString("default") ?: ""
                if (flightNumber.isBlank()) continue

                val timeObj = flightObj.optJSONObject("time")
                val schKey = if (isArrival) "arrival" else "departure"
                val sch = timeObj?.optJSONObject("scheduled")?.optLong(schKey, 0L) ?: 0L
                val est = timeObj?.optJSONObject("estimated")?.optLong(schKey, 0L) ?: 0L
                val act = timeObj?.optJSONObject("actual")?.optLong(schKey, 0L) ?: 0L

                val sanitizedFlt = flightNumber.replace("\\s+".toRegex(), "").uppercase()
                val rawId = identObj?.optString("id")?.trim()
                val fid = if (rawId.isNullOrBlank() || rawId.equals("null", ignoreCase = true)) {
                    val effectiveTime = if (sch > 0) sch else if (est > 0) est else 0L
                    "${sanitizedFlt}_${effectiveTime}_${if (isArrival) "ARR" else "DEP"}"
                } else {
                    rawId
                }
                val rawCallsign = identObj?.optJSONObject("callsign")?.optString("default")?.trim()
                val callsign = if (rawCallsign.isNullOrBlank() || rawCallsign.equals("null", ignoreCase = true)) null else rawCallsign

                val statusObj = flightObj.optJSONObject("status")
                val rawStatus = statusObj?.optString("text")?.trim()
                val genericStatus = statusObj?.optJSONObject("generic")?.optJSONObject("status")?.optString("text")?.trim()
                var statusText = when {
                    !rawStatus.isNullOrBlank() && !rawStatus.equals("null", ignoreCase = true) -> rawStatus
                    !genericStatus.isNullOrBlank() && !genericStatus.equals("null", ignoreCase = true) -> genericStatus
                    else -> "–"
                }

                val airportObj = flightObj.optJSONObject("airport")
                val rawOrig = airportObj?.optJSONObject("origin")?.optJSONObject("code")?.optString("iata")?.trim()
                val rawOrigCity = airportObj?.optJSONObject("origin")?.optJSONObject("position")?.optJSONObject("region")?.optString("city")?.takeIf { it != "null" && it.isNotBlank() }

                val rawDest = airportObj?.optJSONObject("destination")?.optJSONObject("code")?.optString("iata")?.trim()
                val rawDestCity = airportObj?.optJSONObject("destination")?.optJSONObject("position")?.optJSONObject("region")?.optString("city")?.takeIf { it != "null" && it.isNotBlank() }

                // Origin & Destination resolution (Departures originate from current station, NEVER "???")
                val originIata: String
                val originCity: String?
                val destIata: String
                val destCity: String?

                if (isArrival) {
                    originIata = if (rawOrig.isNullOrBlank() || rawOrig.equals("null", ignoreCase = true) || rawOrig == "???") "???" else rawOrig
                    originCity = rawOrigCity ?: AirportUtils.getCityForAirport(originIata)
                    destIata = if (rawDest.isNullOrBlank() || rawDest.equals("null", ignoreCase = true) || rawDest == "???") stationUpper else rawDest
                    destCity = rawDestCity ?: AirportUtils.getCityForAirport(destIata) ?: defaultStationCity
                } else {
                    // For Departures: From station is the collecting/selected station (e.g. COK), NEVER "???"
                    originIata = if (rawOrig.isNullOrBlank() || rawOrig.equals("null", ignoreCase = true) || rawOrig == "???") stationUpper else rawOrig
                    originCity = rawOrigCity ?: AirportUtils.getCityForAirport(originIata) ?: defaultStationCity
                    destIata = if (rawDest.isNullOrBlank() || rawDest.equals("null", ignoreCase = true) || rawDest == "???") "???" else rawDest
                    destCity = rawDestCity ?: AirportUtils.getCityForAirport(destIata)
                }

                // Check for flight diversion to current station:
                val isDivertedStatus = statusText.contains("divert", ignoreCase = true) ||
                        statusText.contains("dvt", ignoreCase = true) ||
                        (genericStatus?.contains("divert", ignoreCase = true) == true)
                if (isDivertedStatus && !statusText.contains("divert", ignoreCase = true)) {
                    statusText = "Diverted to $stationUpper"
                }

                // Filter flights within operational window (-5h delayed/yet-to-arrive to +20h ahead)
                if (!isInOperationalWindow(isArrival, sch, est, act, statusText)) {
                    continue
                }

                val aircraftObj = flightObj.optJSONObject("aircraft")
                val rawReg = aircraftObj?.optString("registration")?.trim()
                val reg = if (rawReg.isNullOrBlank() || rawReg.equals("null", ignoreCase = true)) "–" else rawReg
                val modelObj = aircraftObj?.optJSONObject("model")
                val rawModel = (modelObj?.optString("code") ?: modelObj?.optString("text"))?.trim()
                val model = if (rawModel.isNullOrBlank() || rawModel.equals("null", ignoreCase = true)) "–" else rawModel

                val delayMin = if (sch > 0 && est > 0) ((est - sch) / 60).toInt() else 0

                val airlineCode = flightNumber.take(2).uppercase()
                val airlineName = getAirlineName(flightNumber)
                val logoUrl = getAirlineLogoUrl(flightNumber)

                results.add(
                    Flight(
                        id = fid,
                        flightNumber = flightNumber,
                        callsign = callsign,
                        registration = reg,
                        originIata = originIata,
                        originCity = originCity,
                        destIata = destIata,
                        destCity = destCity,
                        scheduledTime = sch,
                        estimatedTime = est,
                        actualTime = act,
                        aircraftModel = model,
                        statusText = statusText,
                        delayMinutes = delayMin,
                        airlineCode = airlineCode,
                        airlineName = airlineName,
                        logoUrl = logoUrl,
                        isArrival = isArrival
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing schedule JSON", e)
        }
        return results.distinctBy { it.id }
    }

    data class LiveTelemetry(
        val lat: Double,
        val lon: Double,
        val heading: Float?,
        val altitudeFt: Int?,
        val speedKts: Int?,
        val isAirborne: Boolean
    )

    suspend fun fetchLiveTelemetry(flightId: String): LiveTelemetry? = withContext(Dispatchers.IO) {
        if (flightId.isBlank()) return@withContext null
        val url = "https://data-live.flightradar24.com/clickhandler/?flight=${URLEncoder.encode(flightId, "UTF-8")}&version=1.5"

        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; AIXSecOps)")
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use null
                val body = res.body?.string() ?: return@use null
                val json = JSONObject(body)

                val statusObj = json.optJSONObject("status")
                val isLive = statusObj?.optBoolean("live", true) ?: true

                val trail = json.optJSONArray("trail") ?: return@use null
                if (trail.length() == 0) return@use null

                // Pick latest point
                var bestPoint: JSONObject? = null
                var bestTs = 0L
                for (i in 0 until trail.length()) {
                    val p = trail.optJSONObject(i) ?: continue
                    val ts = p.optLong("ts", 0L)
                    if (ts >= bestTs) {
                        bestTs = ts
                        bestPoint = p
                    }
                }

                val targetPoint = bestPoint ?: trail.optJSONObject(0) ?: return@use null
                val lat = targetPoint.optDouble("lat", Double.NaN)
                val lon = targetPoint.optDouble("lng", Double.NaN)
                val hd = if (targetPoint.has("hd")) targetPoint.optDouble("hd").toFloat() else null
                val alt = if (targetPoint.has("alt")) targetPoint.optInt("alt") else null
                val spd = if (targetPoint.has("spd")) targetPoint.optInt("spd") else null

                if (lat.isNaN() || lon.isNaN() || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                    return@use null
                }

                // Verify within 7000km of Cochin
                val distKm = haversineKm(lat, lon, 10.152, 76.392)
                if (distKm > 7000) return@use null

                val isAirborne = isLive && (alt ?: 0) > 400

                LiveTelemetry(
                    lat = lat,
                    lon = lon,
                    heading = hd,
                    altitudeFt = alt,
                    speedKts = spd,
                    isAirborne = isAirborne
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return r * c
    }
}
