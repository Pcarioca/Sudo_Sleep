package com.example.sudosleep.ui.viewmodel

import java.time.LocalDate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sudosleep.data.local.entity.AcademicEventEntity
import com.example.sudosleep.data.local.entity.SubjectEntity
import com.example.sudosleep.data.local.entity.TimetableEntryEntity
import com.example.sudosleep.data.local.pojo.AcademicEventWithSubject
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.local.pojo.UnscheduledRequirementWithSubject
import com.example.sudosleep.data.repository.SudoSleepRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CalendarViewMode(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month")
}

class TimetableViewModel(private val repository: SudoSleepRepository) : ViewModel() {

    val selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value) // 1 = Monday ... 7 = Sunday

    private val _viewMode = MutableStateFlow(CalendarViewMode.DAY)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val entriesForSelectedDay: StateFlow<List<TimetableEntryWithSubject>> =
        selectedDay.flatMapLatest { day ->
            repository.getTimetableEntriesForDay(day)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTimetableEntries: StateFlow<List<TimetableEntryWithSubject>> =
        repository.allTimetableEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allAcademicEvents: StateFlow<List<AcademicEventWithSubject>> =
        repository.allAcademicEvents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSubjects: StateFlow<List<SubjectEntity>> =
        repository.allSubjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unscheduledRequirements: StateFlow<List<UnscheduledRequirementWithSubject>> =
        repository.unscheduledRequirements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isSyncing: StateFlow<Boolean> = repository.isSyncing

    val syncMessages = repository.syncMessages

    fun refreshFromBackend(showSuccessMessage: Boolean = false) {
        viewModelScope.launch {
            repository.syncFromBackend(showSuccessMessage = showSuccessMessage)
        }
    }

    fun setSelectedDay(day: Int) {
        selectedDay.value = day.coerceIn(1, 7)
    }

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun attemptScheduleRequirement(
        requirement: UnscheduledRequirementWithSubject,
        day: Int,
        startHour: Int,
        duration: Int,
        room: String?,
        teacher: String?,
        notes: String? = null,
        notificationsEnabled: Boolean = true,
        reminderOffsetsCsv: String = "15",
        onConflictDetected: (TimetableEntryWithSubject) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val conflict = repository.checkOverlapConflict(
                day = day,
                startHour = startHour,
                duration = duration
            )
            if (conflict != null) {
                onConflictDetected(conflict)
            } else {
                forceScheduleRequirement(
                    requirement = requirement,
                    day = day,
                    startHour = startHour,
                    duration = duration,
                    room = room,
                    teacher = teacher,
                    notes = notes,
                    notificationsEnabled = notificationsEnabled,
                    reminderOffsetsCsv = reminderOffsetsCsv
                )
                onSuccess()
            }
        }
    }

    fun forceScheduleRequirement(
        requirement: UnscheduledRequirementWithSubject,
        day: Int,
        startHour: Int,
        duration: Int,
        room: String?,
        teacher: String?,
        notes: String? = null,
        notificationsEnabled: Boolean = true,
        reminderOffsetsCsv: String = "15"
    ) {
        viewModelScope.launch {
            repository.scheduleRequirement(
                requirementId = requirement.requirement.id,
                subjectId = requirement.subject.id,
                day = day,
                startHour = startHour,
                duration = duration,
                room = room,
                teacher = teacher,
                notes = notes,
                notificationsEnabled = notificationsEnabled,
                reminderOffsetsCsv = reminderOffsetsCsv
            )
        }
    }

    fun attemptScheduleQuickActivity(
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
        reminderOffsetsCsv: String,
        onConflictDetected: (TimetableEntryWithSubject) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val conflict = repository.checkOverlapConflict(
                day = day,
                startHour = startHour,
                duration = duration
            )
            if (conflict != null) {
                onConflictDetected(conflict)
            } else {
                forceScheduleQuickActivity(
                    existingSubjectId = existingSubjectId,
                    newSubjectName = newSubjectName,
                    newSubjectColorHex = newSubjectColorHex,
                    activityType = activityType,
                    customTypeName = customTypeName,
                    day = day,
                    startHour = startHour,
                    duration = duration,
                    room = room,
                    teacher = teacher,
                    notes = notes,
                    notificationsEnabled = notificationsEnabled,
                    reminderOffsetsCsv = reminderOffsetsCsv
                )
                onSuccess()
            }
        }
    }

    fun forceScheduleQuickActivity(
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
    ) {
        viewModelScope.launch {
            repository.scheduleQuickActivity(
                existingSubjectId = existingSubjectId,
                newSubjectName = newSubjectName,
                newSubjectColorHex = newSubjectColorHex,
                activityType = activityType,
                customTypeName = customTypeName,
                day = day,
                startHour = startHour,
                duration = duration,
                room = room,
                teacher = teacher,
                notes = notes,
                notificationsEnabled = notificationsEnabled,
                reminderOffsetsCsv = reminderOffsetsCsv
            )
        }
    }

    fun updateEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            repository.updateTimetableEntry(entry)
        }
    }

    fun setEntryCanceledThisWeek(entryId: Long, canceled: Boolean) {
        viewModelScope.launch {
            repository.setEntryCanceledThisWeek(entryId, canceled)
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteTimetableEntry(entryId)
        }
    }

    fun addAcademicEvent(event: AcademicEventEntity) {
        viewModelScope.launch {
            repository.addAcademicEvent(event)
        }
    }

    fun updateAcademicEvent(event: AcademicEventEntity) {
        viewModelScope.launch {
            repository.updateAcademicEvent(event)
        }
    }

    fun deleteAcademicEvent(eventId: Long) {
        viewModelScope.launch {
            repository.deleteAcademicEvent(eventId)
        }
    }

    class Factory(private val repository: SudoSleepRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
                return TimetableViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
