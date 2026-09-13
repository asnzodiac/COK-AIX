package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Flight
import com.example.data.model.FlightStatusCategory
import com.example.data.model.TurnaroundStatus
import com.example.data.repository.FlightRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AIX SEC OPS", appName)
    }

    @Test
    fun `turnaround computation matches arrival and departure by registration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FlightRepository(context)

        val now = System.currentTimeMillis() / 1000

        val arrFlight = Flight(
            id = "f_arr_1",
            flightNumber = "IX 434",
            registration = "VT-AXP",
            originIata = "DXB",
            destIata = "COK",
            scheduledTime = now + 1800,
            estimatedTime = now + 1800,
            aircraftModel = "B738",
            airlineCode = "IX",
            airlineName = "Air India Express",
            isArrival = true
        )

        val depFlight = Flight(
            id = "f_dep_1",
            flightNumber = "IX 473",
            registration = "VT-AXP",
            originIata = "COK",
            destIata = "BAH",
            scheduledTime = now + 5400, // 60 min after arrival
            estimatedTime = now + 5400,
            aircraftModel = "B738",
            airlineCode = "IX",
            airlineName = "Air India Express",
            isArrival = false
        )

        val turnarounds = repo.computeTurnarounds(listOf(arrFlight), listOf(depFlight))
        assertEquals(1, turnarounds.size)
        assertEquals("VT-AXP", turnarounds[0].registration)
        assertEquals(60, turnarounds[0].gapMinutes)
        assertEquals(TurnaroundStatus.NORMAL, turnarounds[0].status)
    }

    @Test
    fun `flight delay calculation and remaining time formatted`() {
        val now = System.currentTimeMillis() / 1000
        val flight = Flight(
            id = "test_f1",
            flightNumber = "6E 1402",
            registration = "VT-IZG",
            originIata = "SHJ",
            destIata = "COK",
            scheduledTime = now + 600,
            estimatedTime = now + 1200, // 10 min delayed
            aircraftModel = "A321",
            airlineCode = "6E",
            airlineName = "IndiGo",
            isArrival = true
        )

        assertEquals(10, flight.delayMinutes)
        assertEquals(FlightStatusCategory.DELAYED, flight.statusCategory)
        assertNotNull(flight.remainingFormatted)
    }
}
