package com.example.sudosleep.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.sudosleep.data.local.entity.TimetableEntryEntity
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Transaction
    @Query("SELECT * FROM timetable_entries ORDER BY day ASC, startHour ASC")
    fun getAllEntriesWithSubjectFlow(): Flow<List<TimetableEntryWithSubject>>

    @Transaction
    @Query("SELECT * FROM timetable_entries WHERE day = :day ORDER BY startHour ASC")
    fun getEntriesForDayWithSubjectFlow(day: Int): Flow<List<TimetableEntryWithSubject>>

    @Transaction
    @Query("SELECT * FROM timetable_entries WHERE subjectId = :subjectId ORDER BY day ASC, startHour ASC")
    fun getEntriesForSubjectWithSubjectFlow(subjectId: Long): Flow<List<TimetableEntryWithSubject>>

    @Query("SELECT * FROM timetable_entries")
    suspend fun getAllEntries(): List<TimetableEntryEntity>

    @Query("SELECT * FROM timetable_entries WHERE day = :day")
    suspend fun getEntriesForDay(day: Int): List<TimetableEntryEntity>

    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): TimetableEntryEntity?

    @Query("SELECT * FROM timetable_entries WHERE requirementId = :requirementId")
    suspend fun getEntriesForRequirement(requirementId: Long): List<TimetableEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: TimetableEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}
