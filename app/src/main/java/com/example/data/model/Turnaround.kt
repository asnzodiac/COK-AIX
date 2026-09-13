package com.example.data.model

data class Turnaround(
    val registration: String,
    val arrivalFlight: Flight,
    val departureFlight: Flight,
    val gapMinutes: Int,
    val aircraftModel: String = "–",
    val maxDelayMinutes: Int = 0,
    val isDomestic: Boolean = false,
    val standardTurnaroundMinutes: Int = if (isDomestic) 30 else 60
) {
    val sectorType: String
        get() = if (isDomestic) "DOMESTIC" else "INTERNATIONAL"

    val bufferMinutes: Int
        get() = gapMinutes - standardTurnaroundMinutes

    val status: TurnaroundStatus
        get() = when {
            gapMinutes < (if (isDomestic) 20 else 40) -> TurnaroundStatus.CRITICAL_TIGHT
            gapMinutes < standardTurnaroundMinutes -> TurnaroundStatus.TIGHT
            maxDelayMinutes > 25 || arrivalFlight.delayMinutes > maxOf(bufferMinutes, 0) -> TurnaroundStatus.DELAY_RISK
            else -> TurnaroundStatus.NORMAL
        }
}

enum class TurnaroundStatus {
    NORMAL,
    TIGHT,
    CRITICAL_TIGHT,
    DELAY_RISK
}
