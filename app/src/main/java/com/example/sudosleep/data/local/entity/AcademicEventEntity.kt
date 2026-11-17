package com.example.sudosleep.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "academic_events",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["dateEpochDay"])
    ]
)
data class AcademicEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectId: Long? = null,
    val title: String,
    val eventType: String, // "Exam", "Midterm", "Project Deadline", "Quiz", "Custom Event"
    val dateEpochDay: Long, // LocalDate.toEpochDay()
    val startHour: Int = 9, // 8 to 21
    val duration: Int = 2, // 1 to 4 hours
    val room: String? = null,
    val notes: String? = null,
    val colorHex: String = "#D81B60",
    val notificationsEnabled: Boolean = true,
    val reminderOffsetsCsv: String = "15,60,1440" // 15m, 1h, 1 day before
) {
    fun getReminderOffsetsMinutes(): List<Int> {
        if (reminderOffsetsCsv.isBlank()) return emptyList()
        return reminderOffsetsCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .sorted()
    }
}
