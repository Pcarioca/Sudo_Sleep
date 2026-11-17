package com.example.sudosleep.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable_entries",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ActivityRequirementEntity::class,
            parentColumns = ["id"],
            childColumns = ["requirementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["requirementId"])
    ]
)
data class TimetableEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectId: Long,
    val requirementId: Long,
    val day: Int, // 1 = Monday to 7 = Sunday
    val startHour: Int, // 8 to 21
    val duration: Int, // 1 to 4 hours
    val room: String? = null,
    val teacher: String? = null,
    val notes: String? = null,
    val canceledWeekEpochId: Int? = null, // e.g. 202646 when canceled for that specific ISO year-week
    val notificationsEnabled: Boolean = true,
    val reminderOffsetsCsv: String = "15" // comma-separated minutes before start, e.g. "15,60"
) {
    fun getReminderOffsetsMinutes(): List<Int> {
        if (reminderOffsetsCsv.isBlank()) return emptyList()
        return reminderOffsetsCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .sorted()
    }
}
