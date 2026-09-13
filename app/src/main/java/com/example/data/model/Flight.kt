package com.example.data.model

data class Flight(
    val id: String,
    val flightNumber: String,
    val callsign: String? = null,
    val registration: String = "–",
    val originIata: String = "???",
    val originCity: String? = null,
    val destIata: String = "COK",
    val destCity: String? = null,
    val scheduledTime: Long = 0L,
    val estimatedTime: Long = 0L,
    val actualTime: Long = 0L,
    val aircraftModel: String = "–",
    val statusText: String = "–",
    val delayMinutes: Int = if (scheduledTime > 0 && estimatedTime > scheduledTime) ((estimatedTime - scheduledTime) / 60).toInt() else 0,
    val airlineCode: String = "",
    val airlineName: String = "",
    val logoUrl: String? = null,
    val isArrival: Boolean = true,
    // Live Telemetry
    val liveLatitude: Double? = null,
    val liveLongitude: Double? = null,
    val liveHeading: Float? = null,
    val liveAltitudeFt: Int? = null,
    val liveSpeedKts: Int? = null,
    val liveIsAirborne: Boolean = false,
    val isTracked: Boolean = false
) {
    val bestEta: Long
        get() = if (estimatedTime > 0) estimatedTime else scheduledTime

    val remainingSeconds: Long
        get() {
            val nowSec = System.currentTimeMillis() / 1000
            return (bestEta - nowSec).coerceAtLeast(0)
        }

    val remainingFormatted: String
        get() {
            val sec = remainingSeconds
            if (sec <= 0) return "Due now"
            val h = sec / 3600
            val m = (sec % 3600) / 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }

    val approachProgressPercent: Float
        get() {
            // Window of 3 hours (10800s)
            val windowSec = 3 * 3600f
            val sec = remainingSeconds.toFloat()
            return ((windowSec - sec) / windowSec).coerceIn(0f, 1f)
        }

    val displayOriginIata: String
        get() = if (originIata.isBlank() || originIata == "???") {
            if (!isArrival) "COK" else "???"
        } else originIata

    val displayDestIata: String
        get() = if (destIata.isBlank() || destIata == "???") {
            if (isArrival) "COK" else "???"
        } else destIata

    val isDivertedToCok: Boolean
        get() {
            val s = statusText.lowercase()
            return s.contains("divert") || s.contains("dvt")
        }

    val isYetToArriveDelayed: Boolean
        get() {
            val nowSec = System.currentTimeMillis() / 1000
            // Scheduled time is in the past (up to 5 hours earlier), but hasn't arrived/landed yet
            return isArrival && scheduledTime in (nowSec - 18000)..nowSec &&
                    (actualTime == 0L || actualTime > nowSec) &&
                    !statusText.lowercase().contains("land") &&
                    !statusText.lowercase().contains("cancel")
        }

    val statusCategory: FlightStatusCategory
        get() {
            val s = statusText.lowercase()
            return when {
                isDivertedToCok -> FlightStatusCategory.DIVERTED
                s.contains("cancel") -> FlightStatusCategory.CANCELLED
                s.contains("landed") -> FlightStatusCategory.LANDED
                delayMinutes > 30 -> FlightStatusCategory.HEAVY_DELAY
                delayMinutes > 5 -> FlightStatusCategory.DELAYED
                remainingSeconds in 1..900 -> FlightStatusCategory.APPROACHING
                else -> FlightStatusCategory.ON_TIME
            }
        }
}

enum class FlightStatusCategory {
    ON_TIME,
    DELAYED,
    HEAVY_DELAY,
    APPROACHING,
    LANDED,
    CANCELLED,
    DIVERTED
}
