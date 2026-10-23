package com.example.sudosleep.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sudosleep.data.local.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    @Query("SELECT * FROM grades WHERE subjectId = :subjectId")
    fun getGradesForSubjectFlow(subjectId: Long): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades")
    fun getAllGradesFlow(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE subjectId = :subjectId")
    suspend fun getGradesForSubject(subjectId: Long): List<GradeEntity>

    @Query("SELECT * FROM grades WHERE id = :id")
    suspend fun getGradeById(id: Long): GradeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity): Long

    @Update
    suspend fun updateGrade(grade: GradeEntity)

    @Delete
    suspend fun deleteGrade(grade: GradeEntity)

    @Query("DELETE FROM grades WHERE id = :id")
    suspend fun deleteGradeById(id: Long)
}
