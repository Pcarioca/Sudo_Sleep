package com.example.sudosleep

import com.example.sudosleep.data.local.entity.AcademicEventEntity
import com.example.sudosleep.data.remote.AcademicMappers
import com.example.sudosleep.data.remote.AcademicMappers.toEntity
import com.example.sudosleep.data.remote.dto.AcademicEventResponseDto
import com.example.sudosleep.data.remote.dto.TimetableEntryResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AcademicMappersTest {

    @Test
    fun activityRequirementMappingHandlesStandardAndCustomTypes() {
        val lecture = AcademicMappers.toBackendActivityRequirement("Lecture", null)
        assertEquals("LECTURE", lecture.activityType)
        assertNull(lecture.customTypeName)
        assertEquals("Lecture", AcademicMappers.toLocalActivityType(lecture.activityType))

        val practical = AcademicMappers.toBackendActivityRequirement("Practical Work", null)
        assertEquals("PRACTICAL_WORK", practical.activityType)
        assertNull(practical.customTypeName)
        assertEquals("Practical Work", AcademicMappers.toLocalActivityType(practical.activityType))

        val customExplicit = AcademicMappers.toBackendActivityRequirement("Custom", "Colloquium")
        assertEquals("CUSTOM", customExplicit.activityType)
        assertEquals("Colloquium", customExplicit.customTypeName)
        assertEquals("Custom", AcademicMappers.toLocalActivityType(customExplicit.activityType))

        val customInline = AcademicMappers.toBackendActivityRequirement("Study Session", null)
        assertEquals("CUSTOM", customInline.activityType)
        assertEquals("Study Session", customInline.customTypeName)

        val customBlankFallback = AcademicMappers.toBackendActivityRequirement("Custom", "   ")
        assertEquals("CUSTOM", customBlankFallback.activityType)
        assertEquals("Custom Activity", customBlankFallback.customTypeName)
    }

    @Test
    fun eventTypeMappingRoundTripsAllTypes() {
        val pairs = listOf(
            "Exam" to "EXAM",
            "Midterm" to "MIDTERM",
            "Project Deadline" to "PROJECT_DEADLINE",
            "Quiz" to "QUIZ",
            "Custom Event" to "CUSTOM_EVENT"
        )
        pairs.forEach { (local, backend) ->
            assertEquals(backend, AcademicMappers.toBackendEventType(local))
            assertEquals(local, AcademicMappers.toLocalEventType(backend))
        }
        assertEquals("CUSTOM_EVENT", AcademicMappers.toBackendEventType("Something Else"))
    }

    @Test
    fun timetableResponseToEntityMapsDurationAndCurrentWeekCancellation() {
        val dto = TimetableEntryResponseDto(
            id = 42L,
            requirementId = 10L,
            subjectId = 5L,
            subjectName = "Algorithms",
            colorHex = "#0288D1",
            activityType = "LECTURE",
            dayOfWeek = 3,
            startTime = "10:00:00",
            endTime = "12:00:00",
            room = "C301",
            teacher = "Prof. Ionescu",
            notes = "Bring laptop",
            notificationsEnabled = true,
            reminderOffsetsCsv = "15,30",
            canceledIsoWeeks = listOf(202612, 202615)
        )

        val canceledEntity = dto.toEntity(currentIsoYearWeek = 202615)
        assertEquals(42L, canceledEntity.id)
        assertEquals(3, canceledEntity.day)
        assertEquals(10, canceledEntity.startHour)
        assertEquals(2, canceledEntity.duration)
        assertEquals(202615, canceledEntity.canceledWeekEpochId)
        assertEquals("15,30", canceledEntity.reminderOffsetsCsv)

        val activeEntity = dto.toEntity(currentIsoYearWeek = 202616)
        assertNull(activeEntity.canceledWeekEpochId)
    }

    @Test
    fun academicEventMappingHandlesDateTimeAndLateEndTime() {
        val targetDate = LocalDate.of(2026, 6, 18)
        val localEvent = AcademicEventEntity(
            id = 7L,
            subjectId = 3L,
            title = "Final Project Submission",
            eventType = "Project Deadline",
            dateEpochDay = targetDate.toEpochDay(),
            startHour = 22,
            duration = 2,
            room = "Online",
            notes = "Submit PDF",
            colorHex = "invalid",
            notificationsEnabled = true,
            reminderOffsetsCsv = "60,1440"
        )

        val request = AcademicMappers.toBackendEventRequest(localEvent)
        assertEquals("2026-06-18", request.eventDate)
        assertEquals("22:00", request.startTime)
        assertEquals("23:59", request.endTime)
        assertEquals("PROJECT_DEADLINE", request.eventType)
        assertEquals("#D81B60", request.colorHex)

        val response = AcademicEventResponseDto(
            id = 7L,
            semesterId = 1L,
            subjectId = 3L,
            subjectName = "Software Engineering",
            title = request.title,
            eventType = request.eventType,
            eventDate = request.eventDate,
            startTime = "22:00:00",
            endTime = "23:59:00",
            room = request.room,
            notes = request.notes,
            colorHex = "#D81B60",
            notificationsEnabled = true,
            reminderOffsetsCsv = "60,1440"
        )
        val roundTripped = response.toEntity()
        assertEquals(targetDate.toEpochDay(), roundTripped.dateEpochDay)
        assertEquals(22, roundTripped.startHour)
        assertEquals(2, roundTripped.duration)
        assertEquals("Project Deadline", roundTripped.eventType)
    }

    @Test
    fun normalizeColorHexValidatesAndUppercases() {
        assertEquals("#0288D1", AcademicMappers.normalizeColorHex("  #0288d1 "))
        assertEquals("#0288D1", AcademicMappers.normalizeColorHex("not-a-color"))
        assertEquals("#D81B60", AcademicMappers.normalizeColorHex("#123", fallback = "#D81B60"))
    }
}
