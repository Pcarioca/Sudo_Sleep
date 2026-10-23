package com.example.sudosleep.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.sudosleep.data.local.entity.SubjectEntity
import com.example.sudosleep.data.local.pojo.SubjectWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjectsFlow(): Flow<List<SubjectEntity>>

    @Transaction
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjectsWithDetailsFlow(): Flow<List<SubjectWithDetails>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Transaction
    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectWithDetailsByIdFlow(id: Long): Flow<SubjectWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)
}
