package com.example.sudosleep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.local.pojo.UnscheduledRequirementWithSubject
import com.example.sudosleep.data.repository.SudoSleepRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimetableViewModel(private val repository: SudoSleepRepository) : ViewModel() {

    val selectedDay = MutableStateFlow(1) // 1 = Monday ... 7 = Sunday

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

    val unscheduledRequirements: StateFlow<List<UnscheduledRequirementWithSubject>> =
        repository.unscheduledRequirements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSelectedDay(day: Int) {
        selectedDay.value = day
    }

    fun attemptScheduleRequirement(
        requirement: UnscheduledRequirementWithSubject,
        day: Int,
        startHour: Int,
        duration: Int,
        room: String?,
        teacher: String?,
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
                    teacher = teacher
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
        teacher: String?
    ) {
        viewModelScope.launch {
            repository.scheduleRequirement(
                requirementId = requirement.requirement.id,
                subjectId = requirement.subject.id,
                day = day,
                startHour = startHour,
                duration = duration,
                room = room,
                teacher = teacher
            )
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteTimetableEntry(entryId)
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
