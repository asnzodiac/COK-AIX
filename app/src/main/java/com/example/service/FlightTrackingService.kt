package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.data.local.TrackedFlightEntity
import com.example.data.model.Flight
import com.example.data.repository.FlightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FlightTrackingService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: FlightRepository
    private lateinit var notificationHelper: NotificationHelper
    private val notifiedDivertedFlightTimestamps = mutableMapOf<String, Long>()
    private var isFirstDiversionScan = true

    companion object {
        const val TAG = "FlightTrackingService"
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_REFRESH_NOW = "com.example.service.ACTION_REFRESH_NOW"

        fun startService(context: Context) {
            val intent = Intent(context, FlightTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FlightTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = FlightRepository(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_REFRESH_NOW, null -> {
                startForegroundNotification()
                startTrackingLoop()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        try {
            val initialNotif = notificationHelper.buildForegroundServiceNotification(
                followedCount = 0,
                nextFlightSummary = "Connecting to Cochin (COK) live radar feed..."
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID,
                    initialNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID, initialNotif)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Foreground service notification could not be started: ${e.message}")
        }
    }

    private var trackingJob: Job? = null

    private fun startTrackingLoop() {
        if (trackingJob?.isActive == true) return

        trackingJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkAndNotifyTrackedFlights()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in tracking loop", e)
                }
                delay(20000) // Poll every 20 seconds for real-time live activity
            }
        }
    }

    private suspend fun checkAndNotifyTrackedFlights() {
        val selectedStation = repository.getSelectedAirport().uppercase()
        // Fetch fresh schedule data for arrivals to monitor status & diversions
        val arrivals = repository.getArrivals()

        // 1. Check for diverted flights of handled airlines to current station (e.g. COK)
        val nowSec = System.currentTimeMillis() / 1000
        // Prune entries older than 24 hours (86,400s) to prevent memory leak
        notifiedDivertedFlightTimestamps.entries.removeIf { (nowSec - it.value) > 86400 }

        // On cold launch, seed existing diverted flights so launching the app never triggers false notifications
        if (isFirstDiversionScan) {
            isFirstDiversionScan = false
            arrivals.filter { it.isDivertedToCok }.forEach { notifiedDivertedFlightTimestamps[it.id] = nowSec }
        } else {
            for (flight in arrivals) {
                // Never notify on mock/fallback flights; notify only when a real flight diverts during active monitoring
                if (!flight.id.startsWith("fl_") && flight.isDivertedToCok && !notifiedDivertedFlightTimestamps.containsKey(flight.id)) {
                    notifiedDivertedFlightTimestamps[flight.id] = nowSec
                    notificationHelper.sendDiversionAlertNotification(flight, selectedStation)
                }
            }
        }

        val trackedList = repository.getAllTrackedSync()

        if (trackedList.isEmpty()) {
            val notif = notificationHelper.buildForegroundServiceNotification(
                followedCount = 0,
                nextFlightSummary = "Radar feed active ($selectedStation). Star a flight on the board to follow live."
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(NotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID, notif)
            return
        }

        val arrivalsMap = arrivals.associateBy { it.id }

        var closestFlight: Flight? = null
        var minRemaining = Long.MAX_VALUE

        for (entity in trackedList) {
            val freshFlight = arrivalsMap[entity.flightId] ?: Flight(
                id = entity.flightId,
                flightNumber = entity.flightNumber,
                registration = entity.registration,
                originIata = entity.originIata,
                originCity = entity.originCity,
                destIata = entity.destIata,
                scheduledTime = entity.scheduledTime,
                estimatedTime = entity.estimatedTime,
                aircraftModel = entity.aircraftModel,
                airlineCode = entity.airlineCode,
                airlineName = entity.airlineName,
                isArrival = entity.isArrival,
                isTracked = true
            )

            val eta = freshFlight.bestEta
            val remainingSec = (eta - nowSec).coerceAtLeast(0)
            val remainingMin = (remainingSec / 60).toInt()
            val etaIST = formatIST(eta)

            if (remainingSec in 1..<minRemaining) {
                minRemaining = remainingSec
                closestFlight = freshFlight
            }

            // Update Ongoing Live Activity Notification
            notificationHelper.updateFlightLiveActivityNotification(
                flight = freshFlight,
                remainingSec = remainingSec,
                etaIST = etaIST
            )

            // Check 10-Minute Heads-Up threshold (explicit user prompt requirement!)
            // Trigger when remaining is <= 10 minutes (600s) and hasn't been sent yet
            if (remainingMin <= 10 && remainingMin > 0 && !entity.discordWebhookSent10m) {
                var discordSent = false
                if (repository.isWebhookEnabled() && repository.getDiscordWebhookUrl().isNotBlank()) {
                    val result = repository.sendDiscord10mAlert(freshFlight, etaIST)
                    discordSent = result.isSuccess
                }

                // Push notification with high priority
                notificationHelper.send10MinuteHeadsUpNotification(
                    flight = freshFlight,
                    etaIST = etaIST,
                    sentToDiscord = discordSent
                )

                // Update entity in Room
                val updated = entity.copy(
                    discordWebhookSent10m = true,
                    estimatedTime = freshFlight.estimatedTime,
                    lastKnownStatus = "10m Heads-up Dispatched"
                ).withMilestone(10)
                repository.updateTrackedEntity(updated)
            }

            // Check Landed threshold
            if (remainingSec <= 0 && !entity.discordWebhookSentLanded) {
                notificationHelper.sendLandedNotification(freshFlight)
                val updated = entity.copy(
                    discordWebhookSentLanded = true,
                    lastKnownStatus = "Landed / Due Now"
                ).withMilestone(0)
                repository.updateTrackedEntity(updated)
            }
        }

        // Update foreground service summary notification
        val summaryText = closestFlight?.let { next ->
            "${next.flightNumber} (${next.originIata}) next in ~${(minRemaining / 60)}m (ETA ${formatIST(next.bestEta)} IST)"
        } ?: "Monitoring ${trackedList.size} flights"

        val summaryNotif = notificationHelper.buildForegroundServiceNotification(
            followedCount = trackedList.size,
            nextFlightSummary = summaryText
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID, summaryNotif)
    }

    private fun formatIST(timestampSec: Long): String {
        if (timestampSec <= 0) return "–"
        val df = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        return df.format(Date(timestampSec * 1000))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
