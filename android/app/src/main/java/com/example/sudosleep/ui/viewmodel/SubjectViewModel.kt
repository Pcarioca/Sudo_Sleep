package com.example.sudosleep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sudosleep.data.local.entity.GradeEntity
import com.example.sudosleep.data.local.entity.SubjectEntity
import com.example.sudosleep.data.local.pojo.SubjectWithDetails
import com.example.sudosleep.data.repository.SudoSleepRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubjectViewModel(private val repository: SudoSleepRepository) : ViewModel() {

    val allSubjectsWithDetails: StateFlow<List<SubjectWithDetails>> =
        repository.allSubjectsWithDetails.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun addSubject(
        name: String,
        colorHex: String,
        requirements: List<Pair<String, String?>>
    ) {
        viewModelScope.launch {
            repository.addSubjectWithRequirements(name, colorHex, requirements)
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(subjectId: Long) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
        }
    }

    fun addGrade(subjectId: Long, value: Double, weight: Double) {
        viewModelScope.launch {
            repository.addGrade(subjectId, value, weight)
        }
    }

    fun deleteGrade(gradeId: Long) {
        viewModelScope.launch {
            repository.deleteGrade(gradeId)
        }
    }

    fun calculateAverage(grades: List<GradeEntity>): Double {
        return repository.calculateAverage(grades)
    }

    fun calculateOverallGpa(): Double {
        return repository.calculateOverallGpa(allSubjectsWithDetails.value)
    }

    class Factory(private val repository: SudoSleepRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SubjectViewModel::class.java)) {
                return SubjectViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
