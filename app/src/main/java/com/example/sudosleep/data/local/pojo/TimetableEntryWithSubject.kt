package com.example.sudosleep.data.local.pojo

import androidx.room.Embedded
import androidx.room.Relation
import com.example.sudosleep.data.local.entity.ActivityRequirementEntity
import com.example.sudosleep.data.local.entity.SubjectEntity
import com.example.sudosleep.data.local.entity.TimetableEntryEntity

data class TimetableEntryWithSubject(
    @Embedded val entry: TimetableEntryEntity,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "id"
    )
    val subject: SubjectEntity,
    @Relation(
        parentColumn = "requirementId",
        entityColumn = "id"
    )
    val requirement: ActivityRequirementEntity
)
