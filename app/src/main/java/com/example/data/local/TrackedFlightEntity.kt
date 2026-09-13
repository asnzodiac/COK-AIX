package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_flights")
data class TrackedFlightEntity(
    @PrimaryKey
    val flightId: String,
    val flightNumber: String,
    val registration: String = "–",
    val originIata: String = "???",
    val originCity: String? = null,
    val destIata: String = "COK",
    val scheduledTime: Long = 0L,
    val estimatedTime: Long = 0L,
    val aircraftModel: String = "–",
    val airlineCode: String = "",
    val airlineName: String = "",
    val isArrival: Boolean = true,
    val followedAt: Long = System.currentTimeMillis() / 1000,
    val notifiedMilestones: String = "", // e.g. "60,30,15,10,0"
    val discordWebhookSent10m: Boolean = false,
    val discordWebhookSentLanded: Boolean = false,
    val lastKnownStatus: String = "Tracking"
) {
    fun hasNotifiedMilestone(threshold: Int): Boolean {
        return notifiedMilestones.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(threshold)
    }

    fun withMilestone(threshold: Int): TrackedFlightEntity {
        val current = notifiedMilestones.split(",").filter { it.isNotBlank() }.toMutableSet()
        current.add(threshold.toString())
        return copy(notifiedMilestones = current.joinToString(","))
    }
}
