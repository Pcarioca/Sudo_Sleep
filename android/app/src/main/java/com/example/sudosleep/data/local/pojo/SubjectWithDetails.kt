package com.example.sudosleep.data.local.pojo

import androidx.room.Embedded
import androidx.room.Relation
import com.example.sudosleep.data.local.entity.ActivityRequirementEntity
import com.example.sudosleep.data.local.entity.GradeEntity
import com.example.sudosleep.data.local.entity.SubjectEntity

data class SubjectWithDetails(
    @Embedded val subject: SubjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId"
    )
    val requirements: List<ActivityRequirementEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId"
    )
    val grades: List<GradeEntity> = emptyList()
)
