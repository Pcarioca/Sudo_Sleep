package com.example.sudosleep.notifications

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.sudosleep.data.local.pojo.AcademicEventWithSubject
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.repository.SudoSleepRepository

object NotificationHelper {

    const val CHANNEL_CLASSES = "sudo_sleep_class_reminders"
    const val CHANNEL_EXAMS = "sudo_sleep_exam_reminders"

    val AVAILABLE_REMINDER_OFFSETS = listOf(
        5 to "5 min before",
        10 to "10 min before",
        15 to "15 min before",
        30 to "30 min before",
        60 to "1 hour before",
        120 to "2 hours before",
        1440 to "1 day before"
    )

    fun formatOffsetLabel(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}m before"
            minutes == 60 -> "1h before"
            minutes < 1440 && minutes % 60 == 0 -> "${minutes / 60}h before"
            minutes == 1440 -> "1 day before"
            minutes % 1440 == 0 -> "${minutes / 1440} days before"
            else -> "${minutes}m before"
        }
    }

    fun ensureChannelsCreated(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val classChannel = NotificationChannel(
            CHANNEL_CLASSES,
            "Class & Activity Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Friendly reminders before your lectures, labs, and seminars begin."
        }

        val examChannel = NotificationChannel(
            CHANNEL_EXAMS,
            "Exam & Deadline Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important countdown alerts for upcoming exams, midterms, and deadlines."
        }

        manager.createNotificationChannel(classChannel)
        manager.createNotificationChannel(examChannel)
    }

    /**
     * Triggers an immediate test notification so the user can preview how reminders look and feel.
     */
    fun sendInstantTestNotification(context: Context) {
        ensureChannelsCreated(context)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, "✨ Sudo Sleep Reminder Test!")
            putExtra(
                ReminderReceiver.EXTRA_SUBTITLE,
                "Your class & exam notifications are active! Next up: Mobile Systems in Hall A101 🎓"
            )
            putExtra(ReminderReceiver.EXTRA_IS_EXAM, false)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, 999991)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Synchronizes all scheduled AlarmManager reminders for weekly classes and date-specific exams.
     * Automatically skips any class marked as "Canceled this week only" for the current week!
     */
    fun syncAllAlarms(
        context: Context,
        globalEnabled: Boolean,
        entries: List<TimetableEntryWithSubject>,
        events: List<AcademicEventWithSubject>
    ) {
        ensureChannelsCreated(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Clear existing range of deterministic request codes for current entries/events
        entries.forEach { item ->
            AVAILABLE_REMINDER_OFFSETS.forEach { (offsetMin, _) ->
                cancelAlarm(context, alarmManager, buildEntryRequestCode(item.entry.id, offsetMin))
            }
        }
        events.forEach { item ->
            AVAILABLE_REMINDER_OFFSETS.forEach { (offsetMin, _) ->
                cancelAlarm(context, alarmManager, buildEventRequestCode(item.event.id, offsetMin))
            }
        }

        if (!globalEnabled) return

        val now = LocalDateTime.now()
        val zoneId = ZoneId.systemDefault()

        // 1. Schedule next occurrence for each weekly TimetableEntry
        entries.forEach { item ->
            val entry = item.entry
            if (!entry.notificationsEnabled) return@forEach

            val offsets = entry.getReminderOffsetsMinutes()
            if (offsets.isEmpty()) return@forEach

            val targetDayOfWeek = DayOfWeek.of(entry.day.coerceIn(1, 7))
            var nextClassDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(targetDayOfWeek))
            var nextClassDateTime = nextClassDate.atTime(entry.startHour, 0)

            // If today's class already started, or if it's canceled this week, push to next week
            val isCanceledForCandidateWeek = SudoSleepRepository.isCanceledForWeek(entry, nextClassDate)
            if (nextClassDateTime.isBefore(now) || isCanceledForCandidateWeek) {
                nextClassDate = nextClassDate.plusWeeks(1)
                nextClassDateTime = nextClassDate.atTime(entry.startHour, 0)
            }

            val activityName = item.requirement.customTypeName ?: item.requirement.activityType
            val roomSuffix = entry.room?.takeIf { it.isNotBlank() }?.let { " • Room $it" } ?: ""

            offsets.forEach { offsetMinutes ->
                val triggerTime = nextClassDateTime.minusMinutes(offsetMinutes.toLong())
                if (triggerTime.isAfter(now)) {
                    val triggerMillis = triggerTime.atZone(zoneId).toInstant().toEpochMilli()
                    val reqCode = buildEntryRequestCode(entry.id, offsetMinutes)
                    val title = "📚 ${item.subject.name} ($activityName)"
                    val subtitle = "Starts in ${formatOffsetLabel(offsetMinutes).removeSuffix(" before")} at ${
                        String.format("%02d:00", entry.startHour)
                    }$roomSuffix"

                    scheduleAlarm(
                        context = context,
                        alarmManager = alarmManager,
                        requestCode = reqCode,
                        triggerAtMillis = triggerMillis,
                        title = title,
                        subtitle = subtitle,
                        isExam = false
                    )
                }
            }
        }

        // 2. Schedule date-specific Exams & Deadlines
        events.forEach { item ->
            val event = item.event
            if (!event.notificationsEnabled) return@forEach

            val offsets = event.getReminderOffsetsMinutes()
            if (offsets.isEmpty()) return@forEach

            val eventDate = LocalDate.ofEpochDay(event.dateEpochDay)
            val eventDateTime = eventDate.atTime(event.startHour, 0)
            val roomSuffix = event.room?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""

            offsets.forEach { offsetMinutes ->
                val triggerTime = eventDateTime.minusMinutes(offsetMinutes.toLong())
                if (triggerTime.isAfter(now)) {
                    val triggerMillis = triggerTime.atZone(zoneId).toInstant().toEpochMilli()
                    val reqCode = buildEventRequestCode(event.id, offsetMinutes)
                    val title = "🎯 ${event.eventType}: ${event.title}"
                    val subtitle = "Scheduled for ${eventDate} at ${
                        String.format("%02d:00", event.startHour)
                    }$roomSuffix (${formatOffsetLabel(offsetMinutes)})"

                    scheduleAlarm(
                        context = context,
                        alarmManager = alarmManager,
                        requestCode = reqCode,
                        triggerAtMillis = triggerMillis,
                        title = title,
                        subtitle = subtitle,
                        isExam = true
                    )
                }
            }
        }
    }

    private fun buildEntryRequestCode(entryId: Long, offsetMinutes: Int): Int {
        return (100_000 + (entryId.toInt() * 31) + offsetMinutes)
    }

    private fun buildEventRequestCode(eventId: Long, offsetMinutes: Int): Int {
        return (500_000 + (eventId.toInt() * 31) + offsetMinutes)
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        triggerAtMillis: Long,
        title: String,
        subtitle: String,
        isExam: Boolean
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_SUBTITLE, subtitle)
            putExtra(ReminderReceiver.EXTRA_IS_EXAM, isExam)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
