package com.example.sudosleep

import java.time.LocalDate
import com.example.sudosleep.data.local.entity.AcademicEventEntity
import com.example.sudosleep.data.local.entity.TimetableEntryEntity
import com.example.sudosleep.data.repository.SudoSleepRepository
import com.example.sudosleep.notifications.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleUpgradesTest {

    @Test
    fun testCanceledThisWeekOnlyAffectsTargetWeek() {
        val currentWeekMonday = LocalDate.of(2026, 11, 16)
        val nextWeekMonday = currentWeekMonday.plusWeeks(1)
        val targetWeekEpochId = SudoSleepRepository.computeWeekEpochId(currentWeekMonday)

        val entry = TimetableEntryEntity(
            id = 1L,
            subjectId = 1L,
            requirementId = 1L,
            day = 1,
            startHour = 8,
            duration = 2,
            canceledWeekEpochId = targetWeekEpochId
        )

        assertTrue(
            "Entry must be marked canceled during the target week",
            SudoSleepRepository.isCanceledForWeek(entry, currentWeekMonday.plusDays(2))
        )
        assertFalse(
            "Entry must automatically resume in the following week",
            SudoSleepRepository.isCanceledForWeek(entry, nextWeekMonday)
        )
    }

    @Test
    fun testMultipleReminderOffsetsParsing() {
        val entry = TimetableEntryEntity(
            id = 2L,
            subjectId = 1L,
            requirementId = 1L,
            day = 3,
            startHour = 10,
            duration = 2,
            reminderOffsetsCsv = "60, 15, 1440, 15"
        )
        assertEquals(listOf(15, 60, 1440), entry.getReminderOffsetsMinutes())

        val exam = AcademicEventEntity(
            id = 5L,
            title = "Final Exam",
            eventType = "Exam",
            dateEpochDay = LocalDate.of(2026, 12, 1).toEpochDay(),
            reminderOffsetsCsv = "30,1440"
        )
        assertEquals(listOf(30, 1440), exam.getReminderOffsetsMinutes())
        assertEquals("1 day before", NotificationHelper.formatOffsetLabel(1440))
        assertEquals("1h before", NotificationHelper.formatOffsetLabel(60))
        assertEquals("15m before", NotificationHelper.formatOffsetLabel(15))
    }
}
