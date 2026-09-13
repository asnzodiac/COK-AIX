package com.example.data.remote

import android.util.Log
import com.example.data.model.Flight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DiscordWebhookClient {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        const val TAG = "DiscordWebhook"
        const val COLOR_CRIMSON = 14825802 // 0xE2394A
        const val COLOR_AMBER = 16747038   // 0xFF8A1E
        const val COLOR_EMERALD = 3523722  // 0x35C48A
        const val COLOR_YELLOW = 16757304  // 0xFFB238
    }

    suspend fun send10MinuteHeadsUp(
        webhookUrl: String,
        flight: Flight,
        etaIST: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Webhook URL is empty"))
        }

        try {
            val root = JSONObject()
            root.put("username", "AIX SEC OPS (COK)")
            root.put("avatar_url", "https://cdn.flightradar24.com/assets/airlines/logotypes/25.png")

            val embed = JSONObject()
            embed.put("title", "🚨 HEADS UP: 10-Min ETA Alert · ${flight.flightNumber}")
            embed.put(
                "description",
                "Flight **${flight.flightNumber}** is on final approach to **${flight.destIata} (Cochin International Airport)**.\nETA is in approximately **10 minutes** (${etaIST} IST). Ground ops & security team prepare for arrival."
            )
            embed.put("color", COLOR_CRIMSON)

            val fields = JSONArray()

            val fFlight = JSONObject()
            fFlight.put("name", "✈ Flight")
            fFlight.put("value", "**${flight.flightNumber}** (${flight.airlineName})")
            fFlight.put("inline", true)
            fields.put(fFlight)

            val fReg = JSONObject()
            fReg.put("name", "🔖 Registration")
            fReg.put("value", "`${flight.registration}` (${flight.aircraftModel})")
            fReg.put("inline", true)
            fields.put(fReg)

            val fRoute = JSONObject()
            fRoute.put("name", "📍 Sector")
            fRoute.put("value", "**${flight.originIata}** ➔ **${flight.destIata}**")
            fRoute.put("inline", true)
            fields.put(fRoute)

            val fEta = JSONObject()
            fEta.put("name", "⏱ Estimated Arrival")
            fEta.put("value", "**$etaIST IST**")
            fEta.put("inline", true)
            fields.put(fEta)

            val fCountdown = JSONObject()
            fCountdown.put("name", "⏳ Heads-up Window")
            fCountdown.put("value", "< 10 minutes out")
            fCountdown.put("inline", true)
            fields.put(fCountdown)

            val fStatus = JSONObject()
            fStatus.put("name", "📡 Tactical Status")
            val statusNote = if (flight.delayMinutes > 5) "Delayed (+${flight.delayMinutes}m)" else "On Schedule"
            fStatus.put("value", "$statusNote · ${flight.statusText}")
            fStatus.put("inline", true)
            fields.put(fStatus)

            embed.put("fields", fields)

            val footer = JSONObject()
            footer.put("text", "AIX SEC OPS • Cochin International Ground Operations")
            embed.put("footer", footer)

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            embed.put("timestamp", isoFormat.format(Date()))

            if (!flight.logoUrl.isNullOrBlank()) {
                val thumb = JSONObject()
                thumb.put("url", flight.logoUrl)
                embed.put("thumbnail", thumb)
            }

            val embedsArray = JSONArray()
            embedsArray.put(embed)
            root.put("embeds", embedsArray)

            val reqBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(webhookUrl.trim())
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    Result.success("10-Minute alert successfully sent to Discord")
                } else {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    Result.failure(Exception("Discord rejected webhook: $err"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending 10m heads up to Discord", e)
            Result.failure(e)
        }
    }

    suspend fun sendTestWebhook(webhookUrl: String): Result<String> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Webhook URL is empty"))
        }

        try {
            val root = JSONObject()
            root.put("username", "AIX SEC OPS (COK)")

            val embed = JSONObject()
            embed.put("title", "✅ AIX SEC OPS Webhook Connected!")
            embed.put(
                "description",
                "Your Android app is successfully configured to broadcast live flight alerts and **10-minute ETA heads-ups** to this Discord channel."
            )
            embed.put("color", COLOR_EMERALD)

            val fields = JSONArray()

            val fStation = JSONObject()
            fStation.put("name", "Station")
            fStation.put("value", "Cochin International Airport (COK / VOCI)")
            fStation.put("inline", true)
            fields.put(fStation)

            val fFeatures = JSONObject()
            fFeatures.put("name", "Active Alerts")
            fFeatures.put("value", "• 10-Minute ETA Heads-up\n• Live Activity Push\n• Delay & Turnaround Risk Monitor")
            fFeatures.put("inline", false)
            fields.put(fFeatures)

            embed.put("fields", fields)

            val footer = JSONObject()
            footer.put("text", "AIX SEC OPS Native Android Client")
            embed.put("footer", footer)

            val embedsArray = JSONArray()
            embedsArray.put(embed)
            root.put("embeds", embedsArray)

            val reqBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(webhookUrl.trim())
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    Result.success("Test alert delivered successfully to Discord channel!")
                } else {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    Result.failure(Exception("Discord rejected test: $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
