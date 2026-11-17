package com.example.sudosleep.data.repository

import java.time.LocalDate
import java.time.temporal.WeekFields
import com.example.sudosleep.data.local.AppDatabase
import com.example.sudosleep.data.local.entity.AcademicEventEntity
import com.example.sudosleep.data.local.entity.ActivityRequirementEntity
import com.example.sudosleep.data.local.entity.GradeEntity
import com.example.sudosleep.data.local.entity.SubjectEntity
import com.example.sudosleep.data.local.entity.TimetableEntryEntity
import com.example.sudosleep.data.local.pojo.AcademicEventWithSubject
import com.example.sudosleep.data.local.pojo.SubjectWithDetails
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.local.pojo.UnscheduledRequirementWithSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SudoSleepRepository(private val database: AppDatabase) {

    private val subjectDao = database.subjectDao()
    private val requirementDao = database.requirementDao()
    private val timetableDao = database.timetableDao()
    private val gradeDao = database.gradeDao()
    private val academicEventDao = database.academicEventDao()

    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjectsFlow()
    val allSubjectsWithDetails: Flow<List<SubjectWithDetails>> = subjectDao.getAllSubjectsWithDetailsFlow()
    val allTimetableEntries: Flow<List<TimetableEntryWithSubject>> = timetableDao.getAllEntriesWithSubjectFlow()
    val unscheduledRequirements: Flow<List<UnscheduledRequirementWithSubject>> =
        requirementDao.getUnscheduledRequirementsWithSubjectFlow()
    val allAcademicEvents: Flow<List<AcademicEventWithSubject>> =
        academicEventDao.getAllEventsWithSubjectFlow()

    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?> = subjectDao.getSubjectByIdFlow(id)

    fun getSubjectWithDetailsByIdFlow(id: Long): Flow<SubjectWithDetails?> =
        subjectDao.getSubjectWithDetailsByIdFlow(id)

    fun getTimetableEntriesForDay(day: Int): Flow<List<TimetableEntryWithSubject>> =
        timetableDao.getEntriesForDayWithSubjectFlow(day)

    fun getGradesForSubjectFlow(subjectId: Long): Flow<List<GradeEntity>> =
        gradeDao.getGradesForSubjectFlow(subjectId)

    fun getAcademicEventsForDateFlow(epochDay: Long): Flow<List<AcademicEventWithSubject>> =
        academicEventDao.getEventsForDateFlow(epochDay)

    companion object {
        fun computeWeekEpochId(date: LocalDate = LocalDate.now()): Int {
            val weekFields = WeekFields.ISO
            val year = date.get(weekFields.weekBasedYear())
            val week = date.get(weekFields.weekOfWeekBasedYear())
            return year * 100 + week
        }

        fun isCanceledForWeek(
            entry: TimetableEntryEntity,
            referenceDate: LocalDate = LocalDate.now()
        ): Boolean {
            val canceledId = entry.canceledWeekEpochId ?: return false
            return canceledId == computeWeekEpochId(referenceDate)
        }
    }

    suspend fun addSubjectWithRequirements(
        name: String,
        colorHex: String,
        requirements: List<Pair<String, String?>>
    ): Long {
        val subjectId = subjectDao.insertSubject(
            SubjectEntity(name = name, colorHex = colorHex)
        )
        for ((type, customName) in requirements) {
            requirementDao.insertRequirement(
                ActivityRequirementEntity(
                    subjectId = subjectId,
                    activityType = type,
                    customTypeName = customName,
                    isScheduled = false
                )
            )
        }
        return subjectId
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        subjectDao.updateSubject(subject)
    }

    suspend fun deleteSubject(subjectId: Long) {
        subjectDao.deleteSubjectById(subjectId)
    }

    suspend fun addRequirement(
        subjectId: Long,
        activityType: String,
        customTypeName: String? = null
    ): Long {
        return requirementDao.insertRequirement(
            ActivityRequirementEntity(
                subjectId = subjectId,
                activityType = activityType,
                customTypeName = customTypeName,
                isScheduled = false
            )
        )
    }

    suspend fun deleteRequirement(requirementId: Long) {
        val req = requirementDao.getRequirementById(requirementId) ?: return
        requirementDao.deleteRequirement(req)
    }

    suspend fun checkOverlapConflict(
        day: Int,
        startHour: Int,
        duration: Int,
        ignoreEntryId: Long = 0L
    ): TimetableEntryWithSubject? {
        val entries = timetableDao.getEntriesForDayWithSubjectFlow(day).first()
        val newStart = startHour
        val newEnd = startHour + duration

        return entries.firstOrNull { item ->
            if (item.entry.id == ignoreEntryId) return@firstOrNull false
            if (isCanceledForWeek(item.entry)) return@firstOrNull false
            val existingStart = item.entry.startHour
            val existingEnd = item.entry.startHour + item.entry.duration
            newStart < existingEnd && newEnd > existingStart
        }
    }

    suspend fun scheduleRequirement(
        requirementId: Long,
        subjectId: Long,
        day: Int,
        startHour: Int,
        duration: Int,
        room: String?,
        teacher: String?,
        notes: String? = null,
        notificationsEnabled: Boolean = true,
        reminderOffsetsCsv: String = "15"
    ): Long {
        val entry = TimetableEntryEntity(
            subjectId = subjectId,
            requirementId = requirementId,
            day = day,
            startHour = startHour,
            duration = duration,
            room = room,
            teacher = teacher,
            notes = notes,
            notificationsEnabled = notificationsEnabled,
            reminderOffsetsCsv = reminderOffsetsCsv
        )
        val entryId = timetableDao.insertEntry(entry)
        requirementDao.updateScheduledStatus(requirementId, true)
        return entryId
    }

    /**
     * Direct Tap-to-Add quick scheduling: links to an existing subject or creates a new subject on the spot,
     * creates the activity requirement and schedules it into the timetable immediately.
     */
    suspend fun scheduleQuickActivity(
        existingSubjectId: Long?,
        newSubjectName: String?,
        newSubjectColorHex: String,
        activityType: String,
        customTypeName: String?,
        day: Int,
        startHour: Int,
        duration: Int,
        room: String?,
        teacher: String?,
        notes: String?,
        notificationsEnabled: Boolean,
        reminderOffsetsCsv: String
    ): Long {
        val targetSubjectId = existingSubjectId ?: subjectDao.insertSubject(
            SubjectEntity(
                name = newSubjectName?.takeIf { it.isNotBlank() } ?: "Quick Activity",
                colorHex = newSubjectColorHex
            )
        )
        val reqId = requirementDao.insertRequirement(
            ActivityRequirementEntity(
                subjectId = targetSubjectId,
                activityType = activityType,
                customTypeName = customTypeName,
                isScheduled = true
            )
        )
        return timetableDao.insertEntry(
            TimetableEntryEntity(
                subjectId = targetSubjectId,
                requirementId = reqId,
                day = day,
                startHour = startHour,
                duration = duration,
                room = room,
                teacher = teacher,
                notes = notes,
                notificationsEnabled = notificationsEnabled,
                reminderOffsetsCsv = reminderOffsetsCsv
            )
        )
    }

    suspend fun updateTimetableEntry(entry: TimetableEntryEntity) {
        timetableDao.updateEntry(entry)
    }

    suspend fun setEntryCanceledThisWeek(
        entryId: Long,
        canceled: Boolean,
        referenceDate: LocalDate = LocalDate.now()
    ) {
        val entry = timetableDao.getEntryById(entryId) ?: return
        val updated = entry.copy(
            canceledWeekEpochId = if (canceled) computeWeekEpochId(referenceDate) else null
        )
        timetableDao.updateEntry(updated)
    }

    suspend fun deleteTimetableEntry(entryId: Long) {
        val entry = timetableDao.getEntryById(entryId) ?: return
        val requirementId = entry.requirementId
        timetableDao.deleteEntry(entry)

        val remaining = timetableDao.getEntriesForRequirement(requirementId)
        if (remaining.isEmpty()) {
            requirementDao.updateScheduledStatus(requirementId, false)
        }
    }

    suspend fun addAcademicEvent(event: AcademicEventEntity): Long {
        return academicEventDao.insertEvent(event)
    }

    suspend fun updateAcademicEvent(event: AcademicEventEntity) {
        academicEventDao.updateEvent(event)
    }

    suspend fun deleteAcademicEvent(eventId: Long) {
        academicEventDao.deleteEventById(eventId)
    }

    suspend fun addGrade(subjectId: Long, value: Double, weight: Double): Long {
        return gradeDao.insertGrade(
            GradeEntity(
                subjectId = subjectId,
                value = value,
                weight = weight
            )
        )
    }

    suspend fun deleteGrade(gradeId: Long) {
        gradeDao.deleteGradeById(gradeId)
    }

    fun calculateAverage(grades: List<GradeEntity>): Double {
        if (grades.isEmpty()) return 0.0
        val totalWeight = grades.sumOf { it.weight }
        return if (totalWeight > 0.0) {
            grades.sumOf { it.value * it.weight } / totalWeight
        } else {
            grades.sumOf { it.value } / grades.size
        }
    }

    fun calculateOverallGpa(subjects: List<SubjectWithDetails>): Double {
        val subjectsWithGrades = subjects.filter { it.grades.isNotEmpty() }
        if (subjectsWithGrades.isEmpty()) return 0.0
        return subjectsWithGrades.map { calculateAverage(it.grades) }.average()
    }

    suspend fun seedInitialDataIfEmpty() {
        val currentSubjects = subjectDao.getAllSubjectsFlow().first()
        if (currentSubjects.isNotEmpty()) return

        // Seed 4 initial subjects
        val sub1Id = addSubjectWithRequirements(
            name = "Mobile Systems & Apps",
            colorHex = "#0288D1", // Cyan Blue
            requirements = listOf(
                "Lecture" to null,
                "Laboratory" to null,
                "Seminar" to null
            )
        )
        val sub2Id = addSubjectWithRequirements(
            name = "Software Architecture",
            colorHex = "#7B1FA2", // Deep Purple
            requirements = listOf(
                "Lecture" to null,
                "Laboratory" to null
            )
        )
        val sub3Id = addSubjectWithRequirements(
            name = "Database Management",
            colorHex = "#E65100", // Vibrant Orange
            requirements = listOf(
                "Lecture" to null,
                "Laboratory" to null,
                "Practical Work" to null
            )
        )
        val sub4Id = addSubjectWithRequirements(
            name = "UI/UX & Human Factors",
            colorHex = "#00897B", // Teal Green
            requirements = listOf(
                "Lecture" to null,
                "Project Studio" to "Studio"
            )
        )

        // Seed sample grades
        addGrade(sub1Id, 9.5, 1.0)
        addGrade(sub1Id, 10.0, 1.5)
        addGrade(sub2Id, 8.5, 1.0)
        addGrade(sub2Id, 9.2, 1.0)
        addGrade(sub3Id, 10.0, 2.0)
        addGrade(sub4Id, 9.8, 1.5)

        val reqs1 = requirementDao.getRequirementsForSubject(sub1Id)
        val reqs2 = requirementDao.getRequirementsForSubject(sub2Id)
        val reqs3 = requirementDao.getRequirementsForSubject(sub3Id)
        val reqs4 = requirementDao.getRequirementsForSubject(sub4Id)

        reqs1.firstOrNull { it.activityType == "Lecture" }?.let {
            scheduleRequirement(
                requirementId = it.id,
                subjectId = sub1Id,
                day = 1, // Monday
                startHour = 8,
                duration = 2,
                room = "Hall A101",
                teacher = "Prof. Alan Turing",
                notes = "Jetpack Compose & Material 3 Architecture",
                reminderOffsetsCsv = "15,60"
            )
        }
        reqs2.firstOrNull { it.activityType == "Lecture" }?.let {
            scheduleRequirement(
                requirementId = it.id,
                subjectId = sub2Id,
                day = 1, // Monday
                startHour = 11,
                duration = 2,
                room = "Auditorium 2",
                teacher = "Prof. Donald Knuth",
                notes = "Clean Architecture & SOLID Principles",
                reminderOffsetsCsv = "15"
            )
        }
        reqs3.firstOrNull { it.activityType == "Lecture" }?.let {
            scheduleRequirement(
                requirementId = it.id,
                subjectId = sub3Id,
                day = 2, // Tuesday
                startHour = 10,
                duration = 2,
                room = "Amphitheater B",
                teacher = "Dr. Edgar Codd",
                notes = "B-Trees, Indexing & Query Optimization",
                reminderOffsetsCsv = "15,30"
            )
        }
        reqs1.firstOrNull { it.activityType == "Laboratory" }?.let {
            scheduleRequirement(
                requirementId = it.id,
                subjectId = sub1Id,
                day = 3, // Wednesday
                startHour = 10,
                duration = 2,
                room = "Lab 302",
                teacher = "Dr. Grace Hopper",
                notes = "Hands-on Android Studio Debugging",
                reminderOffsetsCsv = "15,60"
            )
        }
        reqs4.firstOrNull { it.activityType == "Lecture" }?.let {
            scheduleRequirement(
                requirementId = it.id,
                subjectId = sub4Id,
                day = 4, // Thursday
                startHour = 12,
                duration = 2,
                room = "Design Lab 104",
                teacher = "Prof. Don Norman",
                notes = "Micro-interactions & Haptic Feedback",
                reminderOffsetsCsv = "15"
            )
        }
        reqs2.firstOrNull { it.activityType == "Laboratory" }?.let {
            scheduleRequirement(
                requirementId = it.id,
                subjectId = sub2Id,
                day = 5, // Friday
                startHour = 9,
                duration = 2,
                room = "Lab 205",
                teacher = "Dr. Barbara Liskov",
                notes = "Design Patterns Workshop",
                reminderOffsetsCsv = "15"
            )
        }

        // Seed sample Exams, Midterms & Project Deadlines
        val today = LocalDate.now()
        addAcademicEvent(
            AcademicEventEntity(
                subjectId = sub1Id,
                title = "Mobile Systems Midterm Exam",
                eventType = "Midterm",
                dateEpochDay = today.plusDays(4).toEpochDay(),
                startHour = 10,
                duration = 2,
                room = "Main Exam Hall C1",
                notes = "Covers Kotlin Coroutines, Room DB, and Compose state management.",
                colorHex = "#D81B60",
                notificationsEnabled = true,
                reminderOffsetsCsv = "15,60,1440"
            )
        )
        addAcademicEvent(
            AcademicEventEntity(
                subjectId = sub4Id,
                title = "Sudo Sleep UI/UX Prototype Submission",
                eventType = "Project Deadline",
                dateEpochDay = today.plusDays(9).toEpochDay(),
                startHour = 14,
                duration = 1,
                room = "Online Portal / Studio 104",
                notes = "Submit final interactive prototype and wallpaper export demo.",
                colorHex = "#00897B",
                notificationsEnabled = true,
                reminderOffsetsCsv = "60,1440"
            )
        )
        addAcademicEvent(
            AcademicEventEntity(
                subjectId = sub3Id,
                title = "Database Management Final Exam",
                eventType = "Exam",
                dateEpochDay = today.plusDays(16).toEpochDay(),
                startHour = 9,
                duration = 3,
                room = "Aula Magna",
                notes = "Bring student ID card. SQL + Normalization + Transactions.",
                colorHex = "#E65100",
                notificationsEnabled = true,
                reminderOffsetsCsv = "15,60,1440"
            )
        )
    }
}
