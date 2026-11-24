package com.example.sudosleep.data.remote

import com.example.sudosleep.data.local.entity.AcademicEventEntity
import com.example.sudosleep.data.local.entity.ActivityRequirementEntity
import com.example.sudosleep.data.local.entity.GradeEntity
import com.example.sudosleep.data.local.entity.SubjectEntity
import com.example.sudosleep.data.local.entity.TimetableEntryEntity
import com.example.sudosleep.data.remote.dto.AcademicEventRequestDto
import com.example.sudosleep.data.remote.dto.AcademicEventResponseDto
import com.example.sudosleep.data.remote.dto.ActivityRequirementRequestDto
import com.example.sudosleep.data.remote.dto.ActivityRequirementResponseDto
import com.example.sudosleep.data.remote.dto.GradeResponseDto
import com.example.sudosleep.data.remote.dto.SubjectResponseDto
import com.example.sudosleep.data.remote.dto.TimetableEntryRequestDto
import com.example.sudosleep.data.remote.dto.TimetableEntryResponseDto
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * Bidirectional conversions between Android Room entities and Spring Boot API DTOs.
 */
object AcademicMappers {

    private val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")

    fun normalizeColorHex(colorHex: String, fallback: String = "#0288D1"): String {
        val trimmed = colorHex.trim().uppercase(Locale.ROOT)
        return if (HEX_COLOR_REGEX.matches(trimmed)) trimmed else fallback
    }

    fun toBackendActivityRequirement(
        activityType: String,
        customTypeName: String?
    ): ActivityRequirementRequestDto {
        val normalized = activityType.trim()
        return when (normalized.lowercase(Locale.ROOT)) {
            "lecture" -> ActivityRequirementRequestDto("LECTURE", null)
            "laboratory", "lab" -> ActivityRequirementRequestDto("LABORATORY", null)
            "seminar" -> ActivityRequirementRequestDto("SEMINAR", null)
            "practical work", "practical_work", "practical" ->
                ActivityRequirementRequestDto("PRACTICAL_WORK", null)
            else -> {
                val resolvedCustom = customTypeName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: normalized.takeIf { it.isNotEmpty() && !it.equals("Custom", ignoreCase = true) }
                    ?: "Custom Activity"
                ActivityRequirementRequestDto("CUSTOM", resolvedCustom.take(100))
            }
        }
    }

    fun toLocalActivityType(backendType: String): String {
        return when (backendType.uppercase(Locale.ROOT)) {
            "LECTURE" -> "Lecture"
            "LABORATORY" -> "Laboratory"
            "SEMINAR" -> "Seminar"
            "PRACTICAL_WORK" -> "Practical Work"
            else -> "Custom"
        }
    }

    fun toBackendEventType(localType: String): String {
        return when (localType.trim().lowercase(Locale.ROOT)) {
            "exam" -> "EXAM"
            "midterm" -> "MIDTERM"
            "project deadline", "project_deadline", "deadline" -> "PROJECT_DEADLINE"
            "quiz" -> "QUIZ"
            else -> "CUSTOM_EVENT"
        }
    }

    fun toLocalEventType(backendType: String): String {
        return when (backendType.uppercase(Locale.ROOT)) {
            "EXAM" -> "Exam"
            "MIDTERM" -> "Midterm"
            "PROJECT_DEADLINE" -> "Project Deadline"
            "QUIZ" -> "Quiz"
            else -> "Custom Event"
        }
    }

    fun formatHourAsTime(hour: Int): String {
        val clamped = hour.coerceIn(0, 23)
        return String.format(Locale.ROOT, "%02d:00", clamped)
    }

    fun toBackendTimetableRequest(
        day: Int,
        startHour: Int,
        duration: Int,
        room: String?,
        teacher: String?,
        notes: String?,
        notificationsEnabled: Boolean,
        reminderOffsetsCsv: String
    ): TimetableEntryRequestDto {
        val safeStart = startHour.coerceIn(8, 21)
        val safeDuration = duration.coerceIn(1, minOf(4, 22 - safeStart))
        val endHour = (safeStart + safeDuration).coerceAtMost(22)
        return TimetableEntryRequestDto(
            dayOfWeek = day.coerceIn(1, 7),
            startTime = formatHourAsTime(safeStart),
            endTime = formatHourAsTime(endHour),
            weekFrom = null,
            weekTo = null,
            weekParity = "ALL",
            room = room?.trim()?.takeIf { it.isNotEmpty() },
            teacher = teacher?.trim()?.takeIf { it.isNotEmpty() },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            notificationsEnabled = notificationsEnabled,
            reminderOffsetsCsv = reminderOffsetsCsv.trim()
        )
    }

    fun toBackendEventRequest(event: AcademicEventEntity): AcademicEventRequestDto {
        val safeStart = event.startHour.coerceIn(0, 22)
        val safeDuration = event.duration.coerceAtLeast(1)
        val endHour = safeStart + safeDuration
        val endTime = if (endHour >= 24) "23:59" else formatHourAsTime(endHour)
        return AcademicEventRequestDto(
            subjectId = event.subjectId,
            title = event.title.trim(),
            eventType = toBackendEventType(event.eventType),
            eventDate = LocalDate.ofEpochDay(event.dateEpochDay).toString(),
            startTime = formatHourAsTime(safeStart),
            endTime = endTime,
            room = event.room?.trim()?.takeIf { it.isNotEmpty() },
            notes = event.notes?.trim()?.takeIf { it.isNotEmpty() },
            colorHex = normalizeColorHex(event.colorHex, fallback = "#D81B60"),
            notificationsEnabled = event.notificationsEnabled,
            reminderOffsetsCsv = event.reminderOffsetsCsv.trim()
        )
    }

    fun SubjectResponseDto.toEntity(): SubjectEntity = SubjectEntity(
        id = id,
        name = name,
        colorHex = colorHex
    )

    fun ActivityRequirementResponseDto.toEntity(): ActivityRequirementEntity = ActivityRequirementEntity(
        id = id,
        subjectId = subjectId,
        activityType = toLocalActivityType(activityType),
        customTypeName = customTypeName,
        isScheduled = scheduled
    )

    fun GradeResponseDto.toEntity(): GradeEntity = GradeEntity(
        id = id,
        subjectId = subjectId,
        value = score,
        weight = weight
    )

    fun TimetableEntryResponseDto.toEntity(currentIsoYearWeek: Int): TimetableEntryEntity {
        val start = runCatching { LocalTime.parse(startTime).hour }.getOrDefault(8).coerceIn(8, 21)
        val end = runCatching { LocalTime.parse(endTime).hour }.getOrDefault(start + 1)
        val duration = (end - start).coerceIn(1, 4)
        val canceledWeek = if (canceledIsoWeeks.contains(currentIsoYearWeek)) currentIsoYearWeek else null
        return TimetableEntryEntity(
            id = id,
            subjectId = subjectId,
            requirementId = requirementId,
            day = dayOfWeek.coerceIn(1, 7),
            startHour = start,
            duration = duration,
            room = room,
            teacher = teacher,
            notes = notes,
            canceledWeekEpochId = canceledWeek,
            notificationsEnabled = notificationsEnabled,
            reminderOffsetsCsv = reminderOffsetsCsv
        )
    }

    fun AcademicEventResponseDto.toEntity(): AcademicEventEntity {
        val date = runCatching { LocalDate.parse(eventDate).toEpochDay() }
            .getOrDefault(LocalDate.now().toEpochDay())
        val start = runCatching { LocalTime.parse(startTime).hour }.getOrDefault(9).coerceIn(0, 22)
        val parsedEnd = runCatching { LocalTime.parse(endTime) }.getOrNull()
        val endHour = when {
            parsedEnd == null -> start + 1
            parsedEnd.hour == 23 && parsedEnd.minute >= 59 -> 24
            parsedEnd.minute > 0 -> parsedEnd.hour + 1
            else -> parsedEnd.hour
        }
        val duration = (endHour - start).coerceIn(1, 4)
        return AcademicEventEntity(
            id = id,
            subjectId = subjectId,
            title = title,
            eventType = toLocalEventType(eventType),
            dateEpochDay = date,
            startHour = start,
            duration = duration,
            room = room,
            notes = notes,
            colorHex = colorHex,
            notificationsEnabled = notificationsEnabled,
            reminderOffsetsCsv = reminderOffsetsCsv
        )
    }
}
