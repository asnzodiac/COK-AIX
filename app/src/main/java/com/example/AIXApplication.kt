package com.example

import android.app.Application
import com.example.data.repository.FlightRepository
import com.example.service.FlightTrackingService
import com.example.service.NotificationHelper

class AIXApplication : Application() {

    lateinit var repository: FlightRepository
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FlightRepository(this)
        notificationHelper = NotificationHelper(this)
    }
}
