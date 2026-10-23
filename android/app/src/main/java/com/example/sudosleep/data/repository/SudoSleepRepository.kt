package com.example.sudosleep.data.repository

import com.example.sudosleep.data.local.AppDatabase
import com.example.sudosleep.data.local.entity.ActivityRequirementEntity
import com.example.sudosleep.data.local.entity.GradeEntity
import com.example.sudosleep.data.local.entity.SubjectEntity
import com.example.sudosleep.data.local.entity.TimetableEntryEntity
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

    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjectsFlow()
    val allSubjectsWithDetails: Flow<List<SubjectWithDetails>> = subjectDao.getAllSubjectsWithDetailsFlow()
    val allTimetableEntries: Flow<List<TimetableEntryWithSubject>> = timetableDao.getAllEntriesWithSubjectFlow()
    val unscheduledRequirements: Flow<List<UnscheduledRequirementWithSubject>> =
        requirementDao.getUnscheduledRequirementsWithSubjectFlow()

    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?> = subjectDao.getSubjectByIdFlow(id)

    fun getSubjectWithDetailsByIdFlow(id: Long): Flow<SubjectWithDetails?> =
        subjectDao.getSubjectWithDetailsByIdFlow(id)

    fun getTimetableEntriesForDay(day: Int): Flow<List<TimetableEntryWithSubject>> =
        timetableDao.getEntriesForDayWithSubjectFlow(day)

    fun getGradesForSubjectFlow(subjectId: Long): Flow<List<GradeEntity>> =
        gradeDao.getGradesForSubjectFlow(subjectId)

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
        teacher: String?
    ): Long {
        val entry = TimetableEntryEntity(
            subjectId = subjectId,
            requirementId = requirementId,
            day = day,
            startHour = startHour,
            duration = duration,
            room = room,
            teacher = teacher
        )
        val entryId = timetableDao.insertEntry(entry)
        requirementDao.updateScheduledStatus(requirementId, true)
        return entryId
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

        // Seed 3 initial subjects
        val sub1Id = addSubjectWithRequirements(
            name = "Mobile Systems and Applications",
            colorHex = "#0288D1", // Vibrant Blue
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
            colorHex = "#E65100", // Warm Amber/Orange
            requirements = listOf(
                "Lecture" to null,
                "Laboratory" to null,
                "Practical Work" to null
            )
        )

        // Seed sample grades
        addGrade(sub1Id, 9.5, 1.0)
        addGrade(sub1Id, 10.0, 1.5)
        addGrade(sub2Id, 8.5, 1.0)
        addGrade(sub2Id, 9.0, 1.0)
        addGrade(sub3Id, 10.0, 2.0)

        // Schedule some requirements into timetable
        val reqs1 = requirementDao.getRequirementsForSubject(sub1Id)
        val reqs2 = requirementDao.getRequirementsForSubject(sub2Id)

        val sub1Lecture = reqs1.firstOrNull { it.activityType == "Lecture" }
        val sub1Lab = reqs1.firstOrNull { it.activityType == "Laboratory" }
        val sub2Lecture = reqs2.firstOrNull { it.activityType == "Lecture" }

        if (sub1Lecture != null) {
            scheduleRequirement(
                requirementId = sub1Lecture.id,
                subjectId = sub1Id,
                day = 1, // Monday
                startHour = 8,
                duration = 2,
                room = "Hall A101",
                teacher = "Prof. Alan Turing"
            )
        }

        if (sub1Lab != null) {
            scheduleRequirement(
                requirementId = sub1Lab.id,
                subjectId = sub1Id,
                day = 3, // Wednesday
                startHour = 10,
                duration = 2,
                room = "Lab 302",
                teacher = "Dr. Grace Hopper"
            )
        }

        if (sub2Lecture != null) {
            scheduleRequirement(
                requirementId = sub2Lecture.id,
                subjectId = sub2Id,
                day = 1, // Monday
                startHour = 11,
                duration = 2,
                room = "Auditorium 2",
                teacher = "Prof. Donald Knuth"
            )
        }
    }
}
