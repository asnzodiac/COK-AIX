package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.AIXApplication
import com.example.data.model.Flight
import com.example.ui.components.formatTimeIST
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlightAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "FlightAlarmReceiver"
        const val ACTION_10M_HEADS_UP = "com.example.ACTION_10M_HEADS_UP"

        const val EXTRA_FLIGHT_ID = "flight_id"
        const val EXTRA_FLIGHT_NUM = "flight_number"
        const val EXTRA_REG = "reg"
        const val EXTRA_MODEL = "model"
        const val EXTRA_ORIGIN = "origin"
        const val EXTRA_DEST = "dest"
        const val EXTRA_ETA = "eta"
        const val EXTRA_AIRLINE_NAME = "airline_name"
        const val EXTRA_AIRLINE_CODE = "airline_code"
        const val EXTRA_LOGO_URL = "logo_url"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AIX:Flight10mAlarmWakeLock"
        )
        wakeLock?.acquire(10_000L) // 10 seconds max

        try {
            when (action) {
                ACTION_10M_HEADS_UP -> handle10MinuteHeadsUp(context, intent)
                Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
            }
        } finally {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
        }
    }

    private fun handle10MinuteHeadsUp(context: Context, intent: Intent) {
        val flightId = intent.getStringExtra(EXTRA_FLIGHT_ID) ?: return
        val flightNumber = intent.getStringExtra(EXTRA_FLIGHT_NUM) ?: "Flight"
        val reg = intent.getStringExtra(EXTRA_REG) ?: "–"
        val model = intent.getStringExtra(EXTRA_MODEL) ?: "–"
        val origin = intent.getStringExtra(EXTRA_ORIGIN) ?: "???"
        val dest = intent.getStringExtra(EXTRA_DEST) ?: "COK"
        val eta = intent.getLongExtra(EXTRA_ETA, System.currentTimeMillis() / 1000 + 600)
        val airlineName = intent.getStringExtra(EXTRA_AIRLINE_NAME) ?: "Aviation"
        val airlineCode = intent.getStringExtra(EXTRA_AIRLINE_CODE) ?: flightNumber.take(2).uppercase()
        val logoUrl = intent.getStringExtra(EXTRA_LOGO_URL)

        val flight = Flight(
            id = flightId,
            flightNumber = flightNumber,
            registration = reg,
            aircraftModel = model,
            originIata = origin,
            destIata = dest,
            estimatedTime = eta,
            scheduledTime = eta,
            airlineName = airlineName,
            airlineCode = airlineCode,
            logoUrl = logoUrl,
            statusText = "Approaching (10m Heads-Up)",
            isArrival = true
        )

        val etaIST = formatTimeIST(eta)
        val app = context.applicationContext as? AIXApplication
        val notificationHelper = app?.notificationHelper ?: NotificationHelper(context)
        val repository = app?.repository

        // Local alert is fired FIRST - works 100% offline!
        notificationHelper.send10MinuteHeadsUpNotification(
            flight = flight,
            etaIST = etaIST,
            sentToDiscord = false
        )

        // If repository is available and online, attempt Discord alert & update Room DB
        repository?.let { repo ->
            CoroutineScope(Dispatchers.IO).launch {
                var discordSent = false
                if (repo.isWebhookEnabled() && repo.getDiscordWebhookUrl().isNotBlank()) {
                    val res = repo.sendDiscord10mAlert(flight, etaIST)
                    discordSent = res.isSuccess
                }

                // If sent to discord, update notification with badge
                if (discordSent) {
                    notificationHelper.send10MinuteHeadsUpNotification(
                        flight = flight,
                        etaIST = etaIST,
                        sentToDiscord = true
                    )
                }

                // Update Room DB tracked entity
                val entity = repo.getAllTrackedSync().firstOrNull { it.flightId == flightId }
                if (entity != null) {
                    val updated = entity.copy(
                        discordWebhookSent10m = true,
                        lastKnownStatus = "10m Heads-Up Dispatched"
                    ).withMilestone(10)
                    repo.updateTrackedEntity(updated)
                }
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        val app = context.applicationContext as? AIXApplication ?: return
        val repo = app.repository
        CoroutineScope(Dispatchers.IO).launch {
            val tracked = repo.getAllTrackedSync()
            val now = System.currentTimeMillis() / 1000
            for (item in tracked) {
                val eta = if (item.estimatedTime > 0) item.estimatedTime else item.scheduledTime
                if (eta > now + 60 && !item.discordWebhookSent10m) {
                    val flight = Flight(
                        id = item.flightId,
                        flightNumber = item.flightNumber,
                        registration = item.registration,
                        aircraftModel = item.aircraftModel,
                        originIata = item.originIata,
                        destIata = item.destIata,
                        estimatedTime = item.estimatedTime,
                        scheduledTime = item.scheduledTime,
                        airlineCode = item.airlineCode,
                        airlineName = item.airlineName,
                        isArrival = item.isArrival
                    )
                    FlightAlarmScheduler.schedule10MinuteHeadsUp(context, flight)
                }
            }
        }
    }
}
