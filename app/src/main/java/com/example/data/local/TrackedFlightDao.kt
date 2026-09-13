package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedFlightDao {

    @Query("SELECT * FROM tracked_flights ORDER BY estimatedTime ASC, scheduledTime ASC")
    fun getAllTrackedFlights(): Flow<List<TrackedFlightEntity>>

    @Query("SELECT * FROM tracked_flights WHERE flightId = :id LIMIT 1")
    suspend fun getFlightById(id: String): TrackedFlightEntity?

    @Query("SELECT * FROM tracked_flights")
    suspend fun getAllTrackedFlightsSync(): List<TrackedFlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedFlight(flight: TrackedFlightEntity)

    @Update
    suspend fun updateTrackedFlight(flight: TrackedFlightEntity)

    @Query("DELETE FROM tracked_flights WHERE flightId = :flightId")
    suspend fun deleteTrackedFlight(flightId: String)

    @Query("DELETE FROM tracked_flights")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM tracked_flights")
    fun getTrackedCount(): Flow<Int>
}
