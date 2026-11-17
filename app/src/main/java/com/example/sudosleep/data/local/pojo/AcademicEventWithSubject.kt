package com.example.sudosleep.data.local.pojo

import androidx.room.Embedded
import androidx.room.Relation
import com.example.sudosleep.data.local.entity.AcademicEventEntity
import com.example.sudosleep.data.local.entity.SubjectEntity

data class AcademicEventWithSubject(
    @Embedded val event: AcademicEventEntity,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "id"
    )
    val subject: SubjectEntity?
)
