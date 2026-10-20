package com.example.sudosleep.ui.timetable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.local.pojo.UnscheduledRequirementWithSubject
import com.example.sudosleep.ui.viewmodel.TimetableViewModel
import kotlinx.coroutines.launch

private data class PendingConflictSave(
    val req: UnscheduledRequirementWithSubject,
    val day: Int,
    val startHour: Int,
    val duration: Int,
    val room: String?,
    val teacher: String?,
    val conflictingEntry: TimetableEntryWithSubject
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableMainScreen(
    viewModel: TimetableViewModel
) {
    val selectedDay by viewModel.selectedDay.collectAsState()
    val entriesForDay by viewModel.entriesForSelectedDay.collectAsState()
    val unscheduledReqs by viewModel.unscheduledRequirements.collectAsState()

    var showScheduleSheet by remember { mutableStateOf(false) }
    var selectedEntryDetail by remember { mutableStateOf<TimetableEntryWithSubject?>(null) }
    var pendingConflictSave by remember { mutableStateOf<PendingConflictSave?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showScheduleSheet) {
        ScheduleEntryBottomSheet(
            sheetState = sheetState,
            unscheduledRequirements = unscheduledReqs,
            preselectedDay = selectedDay,
            onDismissRequest = { showScheduleSheet = false },
            onSaveSchedule = { req, day, startHour, duration, room, teacher ->
                viewModel.attemptScheduleRequirement(
                    requirement = req,
                    day = day,
                    startHour = startHour,
                    duration = duration,
                    room = room,
                    teacher = teacher,
                    onConflictDetected = { conflicting ->
                        pendingConflictSave = PendingConflictSave(
                            req = req,
                            day = day,
                            startHour = startHour,
                            duration = duration,
                            room = room,
                            teacher = teacher,
                            conflictingEntry = conflicting
                        )
                    },
                    onSuccess = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showScheduleSheet = false
                            }
                        }
                    }
                )
            }
        )
    }

    if (pendingConflictSave != null) {
        val conflictData = pendingConflictSave!!
        ConflictWarningDialog(
            conflictingEntry = conflictData.conflictingEntry,
            onCancel = { pendingConflictSave = null },
            onForceSave = {
                viewModel.forceScheduleRequirement(
                    requirement = conflictData.req,
                    day = conflictData.day,
                    startHour = conflictData.startHour,
                    duration = conflictData.duration,
                    room = conflictData.room,
                    teacher = conflictData.teacher
                )
                pendingConflictSave = null
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showScheduleSheet = false
                    }
                }
            }
        )
    }

    if (selectedEntryDetail != null) {
        TimetableEntryDetailDialog(
            item = selectedEntryDetail!!,
            onDismissRequest = { selectedEntryDetail = null },
            onDeleteEntry = { entryId ->
                viewModel.deleteEntry(entryId)
                selectedEntryDetail = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (unscheduledReqs.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showScheduleSheet = true },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Schedule Class",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Unscheduled Warning Banner at top
            if (unscheduledReqs.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    UnscheduledWarningBanner(
                        unscheduledCount = unscheduledReqs.size,
                        onClick = { showScheduleSheet = true }
                    )
                }
            }

            // Day Selector Tabs
            DaySelector(
                selectedDay = selectedDay,
                onDaySelected = { viewModel.setSelectedDay(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timetable Canvas
            TimetableGridCanvas(
                entries = entriesForDay,
                onEntryClick = { selectedEntryDetail = it },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
