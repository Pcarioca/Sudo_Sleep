package com.example.sudosleep.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SemesterRequestDto(
    val name: String,
    val startDate: String,
    val weeksCount: Int = 14
)

@JsonClass(generateAdapter = true)
data class SemesterResponseDto(
    val id: Long,
    val userId: Long,
    val name: String,
    val startDate: String,
    val weeksCount: Int,
    val currentWeek: Int? = null
)

@JsonClass(generateAdapter = true)
data class ActivityRequirementRequestDto(
    val activityType: String,
    val customTypeName: String? = null
)

@JsonClass(generateAdapter = true)
data class ActivityRequirementResponseDto(
    val id: Long,
    val subjectId: Long,
    val activityType: String,
    val customTypeName: String? = null,
    val scheduled: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SubjectRequestDto(
    val name: String,
    val colorHex: String,
    val requirements: List<ActivityRequirementRequestDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SubjectUpdateRequestDto(
    val name: String,
    val colorHex: String
)

@JsonClass(generateAdapter = true)
data class SubjectResponseDto(
    val id: Long,
    val semesterId: Long,
    val name: String,
    val colorHex: String,
    val requirements: List<ActivityRequirementResponseDto> = emptyList(),
    val gradeCount: Int = 0,
    val arithmeticAverage: Double? = null,
    val weightedAverage: Double? = null
)

@JsonClass(generateAdapter = true)
data class GradeRequestDto(
    val score: Double,
    val weight: Double = 1.0
)

@JsonClass(generateAdapter = true)
data class GradeResponseDto(
    val id: Long,
    val subjectId: Long,
    val score: Double,
    val weight: Double
)

@JsonClass(generateAdapter = true)
data class TimetableEntryRequestDto(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val weekFrom: Int? = null,
    val weekTo: Int? = null,
    val weekParity: String? = "ALL",
    val room: String? = null,
    val teacher: String? = null,
    val notes: String? = null,
    val notificationsEnabled: Boolean = true,
    val reminderOffsetsCsv: String = "15"
)

@JsonClass(generateAdapter = true)
data class TimetableEntryResponseDto(
    val id: Long,
    val requirementId: Long,
    val subjectId: Long,
    val subjectName: String,
    val colorHex: String,
    val activityType: String,
    val customTypeName: String? = null,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val weekFrom: Int = 1,
    val weekTo: Int = 14,
    val weekParity: String = "ALL",
    val room: String? = null,
    val teacher: String? = null,
    val notes: String? = null,
    val notificationsEnabled: Boolean = true,
    val reminderOffsetsCsv: String = "15",
    val canceledIsoWeeks: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AcademicEventRequestDto(
    val subjectId: Long? = null,
    val title: String,
    val eventType: String,
    val eventDate: String,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val notes: String? = null,
    val colorHex: String? = null,
    val notificationsEnabled: Boolean = true,
    val reminderOffsetsCsv: String = "15,60,1440"
)

@JsonClass(generateAdapter = true)
data class AcademicEventResponseDto(
    val id: Long,
    val semesterId: Long,
    val subjectId: Long? = null,
    val subjectName: String? = null,
    val title: String,
    val eventType: String,
    val eventDate: String,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val notes: String? = null,
    val colorHex: String,
    val notificationsEnabled: Boolean = true,
    val reminderOffsetsCsv: String = "15,60,1440"
)
