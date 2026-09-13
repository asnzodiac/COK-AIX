package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Flight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_LIVE_ACTIVITY_ID = "channel_live_activity"
        const val CHANNEL_ALERTS_ID = "channel_tactical_alerts"
        const val NOTIFICATION_GROUP_KEY = "com.example.aix.LIVE_FLIGHTS"
        const val SUMMARY_NOTIFICATION_ID = 1001
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1000
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Live Activity ongoing channel
            val liveChannel = NotificationChannel(
                CHANNEL_LIVE_ACTIVITY_ID,
                "Live Flight Activity",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing live progress and approach status for tracked flights"
                setShowBadge(false)
            }

            // High Priority Tactical Alerts (10m heads up, delays, landing)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Tactical Flight Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent 10-minute ETA heads-ups, arrival alerts, and delay warnings"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(
                    defaultSoundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(liveChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun buildForegroundServiceNotification(followedCount: Int, nextFlightSummary: String?): android.app.Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (followedCount > 0) {
            "✈ Live Activity: Monitoring $followedCount Flight${if (followedCount > 1) "s" else ""}"
        } else {
            "✈ AIX SEC OPS: Live Radar Active"
        }

        val content = nextFlightSummary ?: "Tracking arrivals into Cochin (COK)"

        return NotificationCompat.Builder(context, CHANNEL_LIVE_ACTIVITY_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateFlightLiveActivityNotification(
        flight: Flight,
        remainingSec: Long,
        etaIST: String
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            flight.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = (flight.approachProgressPercent * 100).toInt()
        val remainingText = if (remainingSec <= 0) "Due now / Landed" else flight.remainingFormatted

        val title = "✈ ${flight.flightNumber} · ${flight.originIata} ➔ ${flight.destIata}"
        val content = "ETA $etaIST IST · $remainingText remaining (${flight.registration})"

        val notification = NotificationCompat.Builder(context, CHANNEL_LIVE_ACTIVITY_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notifId = 2000 + (flight.id.hashCode() and 0xFFFF)
        notificationManager.notify(notifId, notification)
    }

    fun send10MinuteHeadsUpNotification(
        flight: Flight,
        etaIST: String,
        sentToDiscord: Boolean
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            flight.id.hashCode() + 1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val discordTag = if (sentToDiscord) " • Discord Webhook Dispatched ✓" else ""
        val title = "🚨 10-MIN HEADS-UP: ${flight.flightNumber} Approaching COK"
        val message = "Flight ${flight.flightNumber} (${flight.airlineName}) from ${flight.originIata} is ~10 min out!\nETA: $etaIST IST · Reg: ${flight.registration}$discordTag"

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("ETA: $etaIST IST (~10 min out) · ${flight.originIata} ➔ ${flight.destIata}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val alertId = 5000 + (flight.id.hashCode() and 0xFFFF)
        notificationManager.notify(alertId, notification)
    }

    fun sendLandedNotification(flight: Flight) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            flight.id.hashCode() + 2,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle("🛬 Touchdown: ${flight.flightNumber}")
            .setContentText("${flight.flightNumber} from ${flight.originIata} has landed at COK (${flight.registration})")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val alertId = 6000 + (flight.id.hashCode() and 0xFFFF)
        notificationManager.notify(alertId, notification)
    }

    fun sendDiversionAlertNotification(flight: Flight, targetAirport: String = "COK") {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            flight.id.hashCode() + 3,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🚨 FLIGHT DIVERTED TO ${targetAirport.uppercase()}: ${flight.flightNumber}"
        val message = "Tactical Alert: Flight ${flight.flightNumber} (${flight.airlineName}) en route ${flight.displayOriginIata} ➔ ${flight.destIata} is DIVERTING to ${targetAirport.uppercase()}!\nReg: ${flight.registration} · Status: ${flight.statusText}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("${flight.flightNumber} diverted to ${targetAirport.uppercase()} (Original dest: ${flight.destIata})")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val alertId = 7000 + (flight.id.hashCode() and 0xFFFF)
        notificationManager.notify(alertId, notification)
    }

    fun removeFlightLiveActivity(flightId: String) {
        val notifId = 2000 + (flightId.hashCode() and 0xFFFF)
        notificationManager.cancel(notifId)
    }

    fun playAlertChime() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            tone.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 350)
        } catch (_: Exception) {}
    }
}
