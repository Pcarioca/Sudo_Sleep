package com.example.sudosleep.data.repository

import androidx.room.withTransaction
import com.example.sudosleep.data.auth.SessionTokenStore
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
import com.example.sudosleep.data.remote.AcademicApi
import com.example.sudosleep.data.remote.AcademicMappers
import com.example.sudosleep.data.remote.AcademicMappers.toEntity
import com.example.sudosleep.data.remote.ApiErrorParser
import com.example.sudosleep.data.remote.dto.GradeRequestDto
import com.example.sudosleep.data.remote.dto.SemesterRequestDto
import com.example.sudosleep.data.remote.dto.SubjectRequestDto
import com.example.sudosleep.data.remote.dto.SubjectUpdateRequestDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

/**
 * Offline-first academic data repository:
 * - UI reads reactively from the local Room cache for instant, lag-free rendering even offline.
 * - Full pulls (on login / launch / manual refresh) synchronize Subjects, Activity Requirements,
 *   Timetable Entries (including per-week cancellations), Grades, and Academic Events atomically.
 * - Mutations persist to the Spring Boot REST API off the main thread with graceful offline fallback.
 */
class SudoSleepRepository(
    private val database: AppDatabase,
    private val academicApi: AcademicApi? = null,
    private val tokenStore: SessionTokenStore? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val subjectDao = database.subjectDao()
    private val requirementDao = database.requirementDao()
    private val timetableDao = database.timetableDao()
    private val gradeDao = database.gradeDao()
    private val academicEventDao = database.academicEventDao()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val syncMessages: SharedFlow<String> = _syncMessages.asSharedFlow()

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

    private fun isRemoteReady(): Boolean = academicApi != null && tokenStore?.hasSession() == true

    /**
     * Pulls the authenticated user's complete active semester snapshot from the backend and
     * replaces the Room cache inside a single transaction (no empty-screen flicker).
     */
    suspend fun syncFromBackend(showSuccessMessage: Boolean = false): Boolean {
        val api = academicApi ?: return false
        val store = tokenStore ?: return false
        if (!store.hasSession()) return false

        return withContext(ioDispatcher) {
            _isSyncing.value = true
            try {
                val semesterId = resolveActiveSemesterId(api, store)
                pullSemesterSnapshot(api, semesterId)
                if (showSuccessMessage) {
                    _syncMessages.tryEmit("Schedule synced with cloud ✨")
                }
                true
            } catch (ex: HttpException) {
                if (ex.code() == 404) {
                    // Stored semesterId may no longer exist; re-resolve from /api/semesters once
                    try {
                        val freshSemesterId = fetchOrCreateDefaultSemesterId(api, store)
                        pullSemesterSnapshot(api, freshSemesterId)
                        if (showSuccessMessage) {
                            _syncMessages.tryEmit("Schedule synced with cloud ✨")
                        }
                        return@withContext true
                    } catch (retryEx: Throwable) {
                        reportSyncFailure("Sync failed", retryEx)
                        return@withContext false
                    }
                }
                reportSyncFailure("Could not sync with server", ex)
                false
            } catch (ex: Throwable) {
                reportSyncFailure("Offline — showing cached schedule", ex)
                false
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun pullSemesterSnapshot(api: AcademicApi, semesterId: Long) {
        val currentIsoWeek = computeWeekEpochId()
        val (remoteSubjects, remoteTimetable, remoteEvents) = coroutineScope {
            val subjectsDeferred = async { api.getSubjects(semesterId) }
            val timetableDeferred = async { api.getTimetable(semesterId) }
            val eventsDeferred = async { api.getEvents(semesterId) }
            Triple(
                subjectsDeferred.await(),
                timetableDeferred.await(),
                eventsDeferred.await()
            )
        }

        val remoteGrades = coroutineScope {
            remoteSubjects
                .filter { it.gradeCount > 0 }
                .map { subject -> async { api.getGrades(subject.id) } }
                .awaitAll()
                .flatten()
        }

        val subjectEntities = remoteSubjects.map { it.toEntity() }
        val requirementEntities = remoteSubjects.flatMap { subject ->
            subject.requirements.map { it.toEntity() }
        }
        val gradeEntities = remoteGrades.map { it.toEntity() }
        val timetableEntities = remoteTimetable.map { it.toEntity(currentIsoWeek) }
        val eventEntities = remoteEvents.map { it.toEntity() }

        database.withTransaction {
            // Delete children before parents to respect FK constraints cleanly
            academicEventDao.deleteAll()
            timetableDao.deleteAll()
            gradeDao.deleteAll()
            requirementDao.deleteAll()
            subjectDao.deleteAll()

            // Insert parents before children
            if (subjectEntities.isNotEmpty()) subjectDao.insertAll(subjectEntities)
            if (requirementEntities.isNotEmpty()) requirementDao.insertAll(requirementEntities)
            if (gradeEntities.isNotEmpty()) gradeDao.insertAll(gradeEntities)
            if (timetableEntities.isNotEmpty()) timetableDao.insertAll(timetableEntities)
            if (eventEntities.isNotEmpty()) academicEventDao.insertAll(eventEntities)
        }
    }

    private suspend fun resolveActiveSemesterId(api: AcademicApi, store: SessionTokenStore): Long {
        val cached = store.getActiveSemesterId()
        if (cached != null && cached > 0L) return cached
        return fetchOrCreateDefaultSemesterId(api, store)
    }

    private suspend fun fetchOrCreateDefaultSemesterId(api: AcademicApi, store: SessionTokenStore): Long {
        val existing = api.getMySemesters()
        val chosen = existing.firstOrNull() ?: run {
            val today = LocalDate.now()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            api.createMySemester(
                SemesterRequestDto(
                    name = "${today.year} Semester",
                    startDate = monday.toString(),
                    weeksCount = 14
                )
            )
        }
        store.updateActiveSemesterId(chosen.id)
        return chosen.id
    }

    // ---- Subjects ----

    suspend fun addSubjectWithRequirements(
        name: String,
        colorHex: String,
        requirements: List<Pair<String, String?>>
    ): Long = withContext(ioDispatcher) {
        val trimmedName = name.trim()
        val normalizedColor = AcademicMappers.normalizeColorHex(colorHex)

        if (isRemoteReady()) {
            val api = academicApi!!
            val store = tokenStore!!
            try {
                val semesterId = resolveActiveSemesterId(api, store)
                val reqDtos = requirements.map { (type, customName) ->
                    AcademicMappers.toBackendActivityRequirement(type, customName)
                }
                val created = api.createSubject(
                    semesterId = semesterId,
                    request = SubjectRequestDto(
                        name = trimmedName,
                        colorHex = normalizedColor,
                        requirements = reqDtos
                    )
                )
                database.withTransaction {
                    subjectDao.insertSubject(created.toEntity())
                    if (created.requirements.isNotEmpty()) {
                        requirementDao.insertAll(created.requirements.map { it.toEntity() })
                    }
                }
                return@withContext created.id
            } catch (ex: HttpException) {
                reportSyncFailure("Could not save subject", ex)
                return@withContext -1L
            } catch (ex: IOException) {
                _syncMessages.tryEmit("Offline: saved '$trimmedName' locally only.")
            } catch (ex: Throwable) {
                reportSyncFailure("Could not sync subject", ex)
                return@withContext -1L
            }
        }

        // Offline / local fallback
        val subjectId = subjectDao.insertSubject(
            SubjectEntity(name = trimmedName, colorHex = normalizedColor)
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
        subjectId
    }

    suspend fun updateSubject(subject: SubjectEntity) = withContext(ioDispatcher) {
        val normalized = subject.copy(
            name = subject.name.trim(),
            colorHex = AcademicMappers.normalizeColorHex(subject.colorHex)
        )
        subjectDao.updateSubject(normalized)

        if (isRemoteReady()) {
            try {
                val updated = academicApi!!.updateSubject(
                    id = normalized.id,
                    request = SubjectUpdateRequestDto(
                        name = normalized.name,
                        colorHex = normalized.colorHex
                    )
                )
                subjectDao.insertSubject(updated.toEntity())
            } catch (ex: HttpException) {
                reportSyncFailure("Could not update subject", ex)
                syncFromBackend()
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: subject changes saved locally.")
            }
        }
    }

    suspend fun deleteSubject(subjectId: Long) = withContext(ioDispatcher) {
        subjectDao.deleteSubjectById(subjectId)

        if (isRemoteReady()) {
            try {
                val response = academicApi!!.deleteSubject(subjectId)
                if (!response.isSuccessful && response.code() != 404) {
                    _syncMessages.tryEmit("Could not delete subject on server (HTTP ${response.code()}).")
                    syncFromBackend()
                }
            } catch (ex: HttpException) {
                if (ex.code() != 404) {
                    reportSyncFailure("Could not delete subject", ex)
                    syncFromBackend()
                }
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: subject deleted locally.")
            }
        }
    }

    // ---- Activity Requirements ----

    suspend fun addRequirement(
        subjectId: Long,
        activityType: String,
        customTypeName: String? = null
    ): Long = withContext(ioDispatcher) {
        if (isRemoteReady()) {
            try {
                val created = academicApi!!.addRequirement(
                    subjectId = subjectId,
                    request = AcademicMappers.toBackendActivityRequirement(activityType, customTypeName)
                )
                requirementDao.insertRequirement(created.toEntity())
                return@withContext created.id
            } catch (ex: HttpException) {
                reportSyncFailure("Could not add activity", ex)
                return@withContext -1L
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: activity saved locally.")
            }
        }

        requirementDao.insertRequirement(
            ActivityRequirementEntity(
                subjectId = subjectId,
                activityType = activityType,
                customTypeName = customTypeName,
                isScheduled = false
            )
        )
    }

    suspend fun deleteRequirement(requirementId: Long) = withContext(ioDispatcher) {
        val req = requirementDao.getRequirementById(requirementId)
        if (req != null) {
            requirementDao.deleteRequirement(req)
        }
        if (isRemoteReady()) {
            try {
                academicApi!!.deleteRequirement(requirementId)
            } catch (ex: HttpException) {
                if (ex.code() != 404) {
                    reportSyncFailure("Could not delete activity", ex)
                    syncFromBackend()
                }
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: activity deleted locally.")
            }
        }
    }

    // ---- Timetable Entries & Per-Week Cancellations ----

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
    ): Long = withContext(ioDispatcher) {
        if (isRemoteReady()) {
            try {
                val created = academicApi!!.createTimetableEntry(
                    requirementId = requirementId,
                    force = true,
                    request = AcademicMappers.toBackendTimetableRequest(
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
                database.withTransaction {
                    timetableDao.insertEntry(created.toEntity(computeWeekEpochId()))
                    requirementDao.updateScheduledStatus(requirementId, true)
                }
                return@withContext created.id
            } catch (ex: HttpException) {
                reportSyncFailure("Could not schedule class", ex)
                return@withContext -1L
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: class scheduled locally.")
            }
        }

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
        entryId
    }

    /**
     * Direct Tap-to-Add quick scheduling: links to an existing subject or creates a new subject on the spot,
     * creates the activity requirement and schedules it into the timetable immediately (synced to backend).
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
    ): Long = withContext(ioDispatcher) {
        val resolvedName = newSubjectName?.trim()?.takeIf { it.isNotEmpty() } ?: "Quick Activity"
        val normalizedColor = AcademicMappers.normalizeColorHex(newSubjectColorHex)

        // Avoid 409 duplicate-name error if user typed a subject that already exists in this semester
        val matchedExistingSubjectId = existingSubjectId
            ?: subjectDao.getAllSubjectsFlow().first()
                .firstOrNull { it.name.equals(resolvedName, ignoreCase = true) }
                ?.id

        if (isRemoteReady()) {
            val api = academicApi!!
            val store = tokenStore!!
            try {
                val reqDto = AcademicMappers.toBackendActivityRequirement(activityType, customTypeName)
                val targetRequirementId: Long

                if (matchedExistingSubjectId == null) {
                    val semesterId = resolveActiveSemesterId(api, store)
                    val createdSubject = api.createSubject(
                        semesterId = semesterId,
                        request = SubjectRequestDto(
                            name = resolvedName,
                            colorHex = normalizedColor,
                            requirements = listOf(reqDto)
                        )
                    )
                    database.withTransaction {
                        subjectDao.insertSubject(createdSubject.toEntity())
                        if (createdSubject.requirements.isNotEmpty()) {
                            requirementDao.insertAll(createdSubject.requirements.map { it.toEntity() })
                        }
                    }
                    targetRequirementId = createdSubject.requirements.first().id
                } else {
                    val createdReq = api.addRequirement(matchedExistingSubjectId, reqDto)
                    requirementDao.insertRequirement(createdReq.toEntity())
                    targetRequirementId = createdReq.id
                }

                val createdEntry = api.createTimetableEntry(
                    requirementId = targetRequirementId,
                    force = true,
                    request = AcademicMappers.toBackendTimetableRequest(
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
                database.withTransaction {
                    timetableDao.insertEntry(createdEntry.toEntity(computeWeekEpochId()))
                    requirementDao.updateScheduledStatus(targetRequirementId, true)
                }
                return@withContext createdEntry.id
            } catch (ex: HttpException) {
                reportSyncFailure("Could not save activity", ex)
                return@withContext -1L
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: activity scheduled locally.")
            }
        }

        // Offline / local fallback
        val targetSubjectId = matchedExistingSubjectId ?: subjectDao.insertSubject(
            SubjectEntity(
                name = resolvedName,
                colorHex = normalizedColor
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
        timetableDao.insertEntry(
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

    suspend fun updateTimetableEntry(entry: TimetableEntryEntity) = withContext(ioDispatcher) {
        timetableDao.updateEntry(entry)

        if (isRemoteReady()) {
            try {
                val updated = academicApi!!.updateTimetableEntry(
                    id = entry.id,
                    force = true,
                    request = AcademicMappers.toBackendTimetableRequest(
                        day = entry.day,
                        startHour = entry.startHour,
                        duration = entry.duration,
                        room = entry.room,
                        teacher = entry.teacher,
                        notes = entry.notes,
                        notificationsEnabled = entry.notificationsEnabled,
                        reminderOffsetsCsv = entry.reminderOffsetsCsv
                    )
                )
                timetableDao.insertEntry(updated.toEntity(computeWeekEpochId()))
            } catch (ex: HttpException) {
                reportSyncFailure("Could not update class", ex)
                syncFromBackend()
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: class changes saved locally.")
            }
        }
    }

    suspend fun setEntryCanceledThisWeek(
        entryId: Long,
        canceled: Boolean,
        referenceDate: LocalDate = LocalDate.now()
    ) = withContext(ioDispatcher) {
        val isoYearWeek = computeWeekEpochId(referenceDate)
        val entry = timetableDao.getEntryById(entryId) ?: return@withContext
        val optimistic = entry.copy(
            canceledWeekEpochId = if (canceled) isoYearWeek else null
        )
        timetableDao.updateEntry(optimistic)

        if (isRemoteReady()) {
            try {
                val updated = if (canceled) {
                    academicApi!!.cancelTimetableEntryForWeek(entryId, isoYearWeek)
                } else {
                    academicApi!!.uncancelTimetableEntryForWeek(entryId, isoYearWeek)
                }
                timetableDao.insertEntry(updated.toEntity(isoYearWeek))
            } catch (ex: HttpException) {
                reportSyncFailure("Could not update weekly cancellation", ex)
                syncFromBackend()
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: cancellation updated locally.")
            }
        }
    }

    suspend fun deleteTimetableEntry(entryId: Long) = withContext(ioDispatcher) {
        val entry = timetableDao.getEntryById(entryId)
        if (entry != null) {
            val requirementId = entry.requirementId
            timetableDao.deleteEntry(entry)
            val remaining = timetableDao.getEntriesForRequirement(requirementId)
            if (remaining.isEmpty()) {
                requirementDao.updateScheduledStatus(requirementId, false)
            }
        }

        if (isRemoteReady()) {
            try {
                val response = academicApi!!.deleteTimetableEntry(entryId)
                if (!response.isSuccessful && response.code() != 404) {
                    _syncMessages.tryEmit("Could not delete class on server (HTTP ${response.code()}).")
                    syncFromBackend()
                }
            } catch (ex: HttpException) {
                if (ex.code() != 404) {
                    reportSyncFailure("Could not delete class", ex)
                    syncFromBackend()
                }
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: class removed locally.")
            }
        }
    }

    // ---- Academic Events (Exams, Midterms, Deadlines) ----

    suspend fun addAcademicEvent(event: AcademicEventEntity): Long = withContext(ioDispatcher) {
        if (isRemoteReady()) {
            val api = academicApi!!
            val store = tokenStore!!
            try {
                val semesterId = resolveActiveSemesterId(api, store)
                val created = api.createEvent(
                    semesterId = semesterId,
                    request = AcademicMappers.toBackendEventRequest(event)
                )
                academicEventDao.insertEvent(created.toEntity())
                return@withContext created.id
            } catch (ex: HttpException) {
                reportSyncFailure("Could not save exam/event", ex)
                return@withContext -1L
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: event saved locally.")
            }
        }

        academicEventDao.insertEvent(event)
    }

    suspend fun updateAcademicEvent(event: AcademicEventEntity) = withContext(ioDispatcher) {
        academicEventDao.updateEvent(event)

        if (isRemoteReady()) {
            try {
                val updated = academicApi!!.updateEvent(
                    id = event.id,
                    request = AcademicMappers.toBackendEventRequest(event)
                )
                academicEventDao.insertEvent(updated.toEntity())
            } catch (ex: HttpException) {
                reportSyncFailure("Could not update exam/event", ex)
                syncFromBackend()
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: event changes saved locally.")
            }
        }
    }

    suspend fun deleteAcademicEvent(eventId: Long) = withContext(ioDispatcher) {
        academicEventDao.deleteEventById(eventId)

        if (isRemoteReady()) {
            try {
                val response = academicApi!!.deleteEvent(eventId)
                if (!response.isSuccessful && response.code() != 404) {
                    _syncMessages.tryEmit("Could not delete event on server (HTTP ${response.code()}).")
                    syncFromBackend()
                }
            } catch (ex: HttpException) {
                if (ex.code() != 404) {
                    reportSyncFailure("Could not delete event", ex)
                    syncFromBackend()
                }
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: event deleted locally.")
            }
        }
    }

    // ---- Grades & Averages ----

    suspend fun addGrade(subjectId: Long, value: Double, weight: Double): Long = withContext(ioDispatcher) {
        val safeScore = value.coerceIn(1.0, 10.0)
        val safeWeight = if (weight > 0.0) weight else 1.0

        if (isRemoteReady()) {
            try {
                val created = academicApi!!.addGrade(
                    subjectId = subjectId,
                    request = GradeRequestDto(score = safeScore, weight = safeWeight)
                )
                gradeDao.insertGrade(created.toEntity())
                return@withContext created.id
            } catch (ex: HttpException) {
                reportSyncFailure("Could not save grade", ex)
                return@withContext -1L
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: grade saved locally.")
            }
        }

        gradeDao.insertGrade(
            GradeEntity(
                subjectId = subjectId,
                value = safeScore,
                weight = safeWeight
            )
        )
    }

    suspend fun deleteGrade(gradeId: Long) = withContext(ioDispatcher) {
        gradeDao.deleteGradeById(gradeId)

        if (isRemoteReady()) {
            try {
                val response = academicApi!!.deleteGrade(gradeId)
                if (!response.isSuccessful && response.code() != 404) {
                    _syncMessages.tryEmit("Could not delete grade on server (HTTP ${response.code()}).")
                    syncFromBackend()
                }
            } catch (ex: HttpException) {
                if (ex.code() != 404) {
                    reportSyncFailure("Could not delete grade", ex)
                    syncFromBackend()
                }
            } catch (_: IOException) {
                _syncMessages.tryEmit("Offline: grade deleted locally.")
            }
        }
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

    /** Clears all cached user-specific academic tables (used on Logout / session expiry). */
    suspend fun clearAllUserData() {
        withContext(ioDispatcher) {
            database.clearAllTables()
        }
    }

    private fun reportSyncFailure(prefix: String, throwable: Throwable) {
        if (ApiErrorParser.isUnauthorized(throwable)) return
        val detail = ApiErrorParser.parse(throwable)
        _syncMessages.tryEmit("$prefix: $detail")
    }
}
