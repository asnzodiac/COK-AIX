package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.Flight
import com.example.ui.components.formatTimeIST

object FlightAlarmScheduler {
    private const val TAG = "FlightAlarmScheduler"

    fun schedule10MinuteHeadsUp(context: Context, flight: Flight): Boolean {
        val eta = if (flight.estimatedTime > 0) flight.estimatedTime else flight.scheduledTime
        if (eta <= 0) {
            Log.w(TAG, "Cannot schedule heads-up: Invalid ETA for flight ${flight.flightNumber}")
            return false
        }

        // Heads-up target is exactly 10 minutes (600s) before ETA
        val headsUpEpochSec = eta - 600L
        val triggerAtMillis = headsUpEpochSec * 1000L
        val nowMillis = System.currentTimeMillis()

        // If the flight is already within the 10-minute window or closer
        if (triggerAtMillis <= nowMillis + 5000L) {
            Log.d(TAG, "Flight ${flight.flightNumber} already in 10-min window, triggering immediately.")
            val notificationHelper = NotificationHelper(context)
            notificationHelper.send10MinuteHeadsUpNotification(
                flight = flight,
                etaIST = formatTimeIST(eta),
                sentToDiscord = false
            )
            return true
        }

        val intent = Intent(context, FlightAlarmReceiver::class.java).apply {
            action = FlightAlarmReceiver.ACTION_10M_HEADS_UP
            putExtra(FlightAlarmReceiver.EXTRA_FLIGHT_ID, flight.id)
            putExtra(FlightAlarmReceiver.EXTRA_FLIGHT_NUM, flight.flightNumber)
            putExtra(FlightAlarmReceiver.EXTRA_REG, flight.registration)
            putExtra(FlightAlarmReceiver.EXTRA_MODEL, flight.aircraftModel)
            putExtra(FlightAlarmReceiver.EXTRA_ORIGIN, flight.originIata)
            putExtra(FlightAlarmReceiver.EXTRA_DEST, flight.destIata)
            putExtra(FlightAlarmReceiver.EXTRA_ETA, eta)
            putExtra(FlightAlarmReceiver.EXTRA_AIRLINE_NAME, flight.airlineName)
            putExtra(FlightAlarmReceiver.EXTRA_AIRLINE_CODE, flight.airlineCode)
            putExtra(FlightAlarmReceiver.EXTRA_LOGO_URL, flight.logoUrl)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            flight.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.i(TAG, "Scheduled 10-min offline heads-up alarm for ${flight.flightNumber} at $triggerAtMillis (ETA: $eta)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm for ${flight.flightNumber}", e)
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return true
            } catch (_: Exception) {
                return false
            }
        }
    }

    fun cancel10MinuteHeadsUp(context: Context, flightId: String) {
        val intent = Intent(context, FlightAlarmReceiver::class.java).apply {
            action = FlightAlarmReceiver.ACTION_10M_HEADS_UP
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            flightId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled 10-min heads-up alarm for flightId: $flightId")
        }
    }
}
