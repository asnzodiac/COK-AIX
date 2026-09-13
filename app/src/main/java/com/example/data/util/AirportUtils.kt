package com.example.data.util

object AirportUtils {

    // Comprehensive list of Indian domestic airport IATA codes
    val INDIAN_DOMESTIC_AIRPORTS = setOf(
        "DEL", "BOM", "BLR", "MAA", "HYD", "CCU", "GOI", "GOX", "TRV", "COK",
        "CCJ", "CNN", "CJB", "IXE", "IXM", "TRZ", "TIR", "PNQ", "AMD", "NAG",
        "IDR", "JAI", "LKO", "PAT", "VNS", "RPR", "BBI", "VTZ", "STV", "RAJ",
        "UDR", "ATQ", "GAU", "IXR", "SXR", "IXB", "DED", "BDQ", "BHO", "IXZ",
        "GAY", "IMF", "IXA", "IXS", "DIB", "IXJ", "IXL", "RJA", "VGA", "IXC",
        "KNU", "MYQ", "HBX", "IXG", "KLH", "SHL", "AJL", "DMU", "TEZ", "JRH",
        "RDP", "GWL", "JLR", "JDH", "JSA", "BKB", "DHM", "KUU", "PGX", "BHU",
        "JGA", "IXU", "IXW", "TCR", "IXY", "HSS", "BEK", "BUP", "IXD", "AGR",
        "KQH", "PYG", "HGI", "RUP", "TEI", "ZER", "IXP", "BEP", "IER", "DDU"
    )

    private val AIRPORT_CITIES = mapOf(
        "COK" to "Cochin",
        "TRV" to "Trivandrum",
        "CNN" to "Kannur",
        "CCJ" to "Calicut",
        "BLR" to "Bengaluru",
        "MAA" to "Chennai",
        "BOM" to "Mumbai",
        "DEL" to "Delhi",
        "HYD" to "Hyderabad",
        "CCU" to "Kolkata",
        "GOI" to "Goa (Dabolim)",
        "GOX" to "Goa (Mopa)",
        "CJB" to "Coimbatore",
        "IXE" to "Mangalore",
        "IXM" to "Madurai",
        "TRZ" to "Tiruchirappalli",
        "TIR" to "Tirupati",
        "PNQ" to "Pune",
        "AMD" to "Ahmedabad",
        "DXB" to "Dubai",
        "SHJ" to "Sharjah",
        "DOH" to "Doha",
        "MCT" to "Muscat",
        "AUH" to "Abu Dhabi",
        "BAH" to "Bahrain",
        "KWI" to "Kuwait",
        "RUH" to "Riyadh",
        "JED" to "Jeddah",
        "CMB" to "Colombo",
        "MLE" to "Male",
        "SIN" to "Singapore",
        "KUL" to "Kuala Lumpur",
        "BKK" to "Bangkok (BKK)",
        "DMK" to "Bangkok (Don Mueang)",
        "SLL" to "Salalah"
    )

    fun isIndianDomestic(iata: String?, city: String? = null): Boolean {
        if (iata.isNullOrBlank()) return false
        val code = iata.trim().uppercase()
        if (INDIAN_DOMESTIC_AIRPORTS.contains(code)) return true
        val c = city?.lowercase() ?: ""
        return c.contains("india") || c.contains("kerala") || c.contains("karnataka") ||
                c.contains("tamil nadu") || c.contains("maharashtra") || c.contains("delhi") ||
                c.contains("gujarat") || c.contains("punjab") || c.contains("rajasthan")
    }

    fun getCityForAirport(iata: String?): String? {
        if (iata.isNullOrBlank()) return null
        return AIRPORT_CITIES[iata.trim().uppercase()]
    }

    fun normalizeFlightNumber(flt: String?): String {
        if (flt == null) return ""
        return flt.replace("\\s+".toRegex(), "").uppercase()
    }

    /**
     * Eliminates duplicate flight entries caused by:
     * 1. Multi-window API overlaps (e.g. -6h, 0, +6h windows)
     * 2. Live FR24 hex ID vs fallback ID transition
     * 3. Codeshare duplications (e.g. IX 434 and AI 9434 for the same physical plane)
     * 4. Minor estimated time drifts
     */
    fun deduplicateFlights(flights: List<com.example.data.model.Flight>, handledAirlines: Set<String> = emptySet()): List<com.example.data.model.Flight> {
        if (flights.isEmpty()) return emptyList()

        val result = mutableListOf<com.example.data.model.Flight>()

        for (flight in flights) {
            val flightNorm = normalizeFlightNumber(flight.flightNumber)
            val flightTime = if (flight.scheduledTime > 0) flight.scheduledTime else flight.bestEta
            val flightReg = flight.registration.trim().uppercase()

            val existingIndex = result.indexOfFirst { existing ->
                // Direction must match (arrival vs departure)
                if (existing.isArrival != flight.isArrival) return@indexOfFirst false

                // 1. Direct ID match
                if (existing.id.isNotBlank() && existing.id == flight.id) return@indexOfFirst true

                val existingNorm = normalizeFlightNumber(existing.flightNumber)
                val existingTime = if (existing.scheduledTime > 0) existing.scheduledTime else existing.bestEta
                val timeDiff = Math.abs(existingTime - flightTime)

                // 2. Same normalized flight number within 4 hours (same flight instance)
                if (existingNorm == flightNorm && timeDiff <= 4 * 3600) return@indexOfFirst true

                // 3. Same physical aircraft registration on same route within 45 minutes (codeshare duplicates)
                val existingReg = existing.registration.trim().uppercase()
                if (existingReg.isNotBlank() && existingReg != "–" && existingReg == flightReg && timeDiff <= 45 * 60) {
                    val existingOrig = existing.displayOriginIata.uppercase()
                    val flightOrig = flight.displayOriginIata.uppercase()
                    val existingDest = existing.displayDestIata.uppercase()
                    val flightDest = flight.displayDestIata.uppercase()
                    if (existingOrig == flightOrig || existingDest == flightDest) {
                        return@indexOfFirst true
                    }
                }

                false
            }

            if (existingIndex >= 0) {
                val existing = result[existingIndex]
                result[existingIndex] = mergeFlightRecords(existing, flight, handledAirlines)
            } else {
                result.add(flight)
            }
        }

        return result.sortedBy { if (it.scheduledTime > 0) it.scheduledTime else it.bestEta }
    }

    private fun mergeFlightRecords(
        existing: com.example.data.model.Flight,
        incoming: com.example.data.model.Flight,
        handledAirlines: Set<String>
    ): com.example.data.model.Flight {
        // Prefer operating / handled airline if available (e.g. IX over AI codeshare)
        val existingIsHandled = handledAirlines.any { existing.flightNumber.startsWith(it, ignoreCase = true) }
        val incomingIsHandled = handledAirlines.any { incoming.flightNumber.startsWith(it, ignoreCase = true) }
        val preferredBase = if (!existingIsHandled && incomingIsHandled) incoming else existing
        val secondary = if (preferredBase === incoming) existing else incoming

        // Prefer real FR24 hex ID (does not contain '_') over generated fallback
        val preferredId = when {
            !preferredBase.id.contains("_") && preferredBase.id.isNotBlank() -> preferredBase.id
            !secondary.id.contains("_") && secondary.id.isNotBlank() -> secondary.id
            else -> preferredBase.id
        }

        // Prefer non-placeholder registration
        val reg = when {
            preferredBase.registration.isNotBlank() && preferredBase.registration != "–" -> preferredBase.registration
            secondary.registration.isNotBlank() && secondary.registration != "–" -> secondary.registration
            else -> preferredBase.registration
        }

        val callsign = preferredBase.callsign ?: secondary.callsign

        // Prefer tactical/live status text over generic "–" or "Scheduled"
        val status = when {
            preferredBase.statusText.contains("Landed", true) || preferredBase.statusText.contains("Divert", true) -> preferredBase.statusText
            secondary.statusText.contains("Landed", true) || secondary.statusText.contains("Divert", true) -> secondary.statusText
            preferredBase.statusText.isNotBlank() && preferredBase.statusText != "–" && preferredBase.statusText != "Scheduled" -> preferredBase.statusText
            secondary.statusText.isNotBlank() && secondary.statusText != "–" && secondary.statusText != "Scheduled" -> secondary.statusText
            else -> preferredBase.statusText
        }

        val incomingHasLive = incoming.liveIsAirborne || (incoming.liveAltitudeFt ?: 0) > 0 || incoming.liveLatitude != null
        val telemetrySource = if (incomingHasLive) incoming else preferredBase

        val sch = if (preferredBase.scheduledTime > 0) preferredBase.scheduledTime else secondary.scheduledTime
        val est = if (preferredBase.estimatedTime > 0) preferredBase.estimatedTime else secondary.estimatedTime
        val act = if (preferredBase.actualTime > 0) preferredBase.actualTime else secondary.actualTime

        val model = when {
            preferredBase.aircraftModel.isNotBlank() && preferredBase.aircraftModel != "–" -> preferredBase.aircraftModel
            secondary.aircraftModel.isNotBlank() && secondary.aircraftModel != "–" -> secondary.aircraftModel
            else -> preferredBase.aircraftModel
        }

        val origCity = preferredBase.originCity ?: secondary.originCity
        val destCity = preferredBase.destCity ?: secondary.destCity

        return preferredBase.copy(
            id = preferredId,
            registration = reg,
            callsign = callsign,
            statusText = status,
            scheduledTime = sch,
            estimatedTime = est,
            actualTime = act,
            aircraftModel = model,
            originCity = origCity,
            destCity = destCity,
            delayMinutes = if (preferredBase.delayMinutes > 0) preferredBase.delayMinutes else secondary.delayMinutes,
            liveLatitude = telemetrySource.liveLatitude ?: preferredBase.liveLatitude ?: secondary.liveLatitude,
            liveLongitude = telemetrySource.liveLongitude ?: preferredBase.liveLongitude ?: secondary.liveLongitude,
            liveAltitudeFt = telemetrySource.liveAltitudeFt ?: preferredBase.liveAltitudeFt ?: secondary.liveAltitudeFt,
            liveSpeedKts = telemetrySource.liveSpeedKts ?: preferredBase.liveSpeedKts ?: secondary.liveSpeedKts,
            liveHeading = telemetrySource.liveHeading ?: preferredBase.liveHeading ?: secondary.liveHeading,
            liveIsAirborne = existing.liveIsAirborne || incoming.liveIsAirborne,
            isTracked = existing.isTracked || incoming.isTracked
        )
    }
}
