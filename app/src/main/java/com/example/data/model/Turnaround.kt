package com.example.data.model

data class Turnaround(
    val registration: String,
    val arrivalFlight: Flight,
    val departureFlight: Flight,
    val gapMinutes: Int,
    val aircraftModel: String = "–",
    val maxDelayMinutes: Int = 0
) {
    val status: TurnaroundStatus
        get() = when {
            gapMinutes < 40 -> TurnaroundStatus.CRITICAL_TIGHT
            gapMinutes < 60 -> TurnaroundStatus.TIGHT
            maxDelayMinutes > 30 -> TurnaroundStatus.DELAY_RISK
            else -> TurnaroundStatus.NORMAL
        }
}

enum class TurnaroundStatus {
    NORMAL,
    TIGHT,
    CRITICAL_TIGHT,
    DELAY_RISK
}
