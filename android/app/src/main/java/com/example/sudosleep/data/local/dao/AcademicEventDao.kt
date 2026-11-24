package com.example.sudosleep.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.sudosleep.data.local.entity.AcademicEventEntity
import com.example.sudosleep.data.local.pojo.AcademicEventWithSubject
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicEventDao {

    @Transaction
    @Query("SELECT * FROM academic_events ORDER BY dateEpochDay ASC, startHour ASC")
    fun getAllEventsWithSubjectFlow(): Flow<List<AcademicEventWithSubject>>

    @Transaction
    @Query("SELECT * FROM academic_events WHERE dateEpochDay = :epochDay ORDER BY startHour ASC")
    fun getEventsForDateFlow(epochDay: Long): Flow<List<AcademicEventWithSubject>>

    @Transaction
    @Query("SELECT * FROM academic_events WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC, startHour ASC")
    fun getEventsInRangeFlow(startEpochDay: Long, endEpochDay: Long): Flow<List<AcademicEventWithSubject>>

    @Query("SELECT * FROM academic_events WHERE id = :id")
    suspend fun getEventById(id: Long): AcademicEventEntity?

    @Query("SELECT * FROM academic_events")
    suspend fun getAllEvents(): List<AcademicEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AcademicEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AcademicEventEntity>)

    @Update
    suspend fun updateEvent(event: AcademicEventEntity)

    @Delete
    suspend fun deleteEvent(event: AcademicEventEntity)

    @Query("DELETE FROM academic_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM academic_events")
    suspend fun deleteAll()
}
