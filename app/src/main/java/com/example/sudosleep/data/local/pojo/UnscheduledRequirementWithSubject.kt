package com.example.sudosleep.data.local.pojo

import androidx.room.Embedded
import androidx.room.Relation
import com.example.sudosleep.data.local.entity.ActivityRequirementEntity
import com.example.sudosleep.data.local.entity.SubjectEntity

data class UnscheduledRequirementWithSubject(
    @Embedded val requirement: ActivityRequirementEntity,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "id"
    )
    val subject: SubjectEntity
)
