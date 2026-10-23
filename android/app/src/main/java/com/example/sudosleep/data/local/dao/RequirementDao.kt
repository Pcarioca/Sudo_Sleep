package com.example.sudosleep.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.sudosleep.data.local.entity.ActivityRequirementEntity
import com.example.sudosleep.data.local.pojo.UnscheduledRequirementWithSubject
import kotlinx.coroutines.flow.Flow

@Dao
interface RequirementDao {

    @Query("SELECT * FROM activity_requirements")
    fun getAllRequirementsFlow(): Flow<List<ActivityRequirementEntity>>

    @Query("SELECT * FROM activity_requirements WHERE subjectId = :subjectId")
    fun getRequirementsForSubjectFlow(subjectId: Long): Flow<List<ActivityRequirementEntity>>

    @Query("SELECT * FROM activity_requirements WHERE subjectId = :subjectId")
    suspend fun getRequirementsForSubject(subjectId: Long): List<ActivityRequirementEntity>

    @Transaction
    @Query("SELECT * FROM activity_requirements WHERE isScheduled = 0")
    fun getUnscheduledRequirementsWithSubjectFlow(): Flow<List<UnscheduledRequirementWithSubject>>

    @Query("SELECT * FROM activity_requirements WHERE id = :id")
    suspend fun getRequirementById(id: Long): ActivityRequirementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequirement(requirement: ActivityRequirementEntity): Long

    @Update
    suspend fun updateRequirement(requirement: ActivityRequirementEntity)

    @Delete
    suspend fun deleteRequirement(requirement: ActivityRequirementEntity)

    @Query("UPDATE activity_requirements SET isScheduled = :isScheduled WHERE id = :id")
    suspend fun updateScheduledStatus(id: Long, isScheduled: Boolean)
}
