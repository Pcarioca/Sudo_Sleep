package com.example.sudosleep.ui.timetable

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sudosleep.data.local.pojo.AcademicEventWithSubject
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.local.pojo.UnscheduledRequirementWithSubject
import com.example.sudosleep.data.preferences.UserPreferencesManager
import com.example.sudosleep.data.repository.SudoSleepRepository
import com.example.sudosleep.ui.exams.AddOrEditExamBottomSheet
import com.example.sudosleep.ui.viewmodel.CalendarViewMode
import com.example.sudosleep.ui.viewmodel.TimetableViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

private sealed interface PendingConflictAction {
    val conflictingEntry: TimetableEntryWithSubject

    data class RequirementConflict(
        val req: UnscheduledRequirementWithSubject,
        val day: Int,
        val startHour: Int,
        val duration: Int,
        val room: String?,
        val teacher: String?,
        val notes: String?,
        val notificationsEnabled: Boolean,
        val reminderOffsetsCsv: String,
        override val conflictingEntry: TimetableEntryWithSubject
    ) : PendingConflictAction

    data class QuickActivityConflict(
        val existingSubjectId: Long?,
        val newSubjectName: String?,
        val newSubjectColorHex: String,
        val activityType: String,
        val customTypeName: String?,
        val day: Int,
        val startHour: Int,
        val duration: Int,
        val room: String?,
        val teacher: String?,
        val notes: String?,
        val notificationsEnabled: Boolean,
        val reminderOffsetsCsv: String,
        override val conflictingEntry: TimetableEntryWithSubject
    ) : PendingConflictAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableMainScreen(
    viewModel: TimetableViewModel,
    preferencesManager: UserPreferencesManager
) {
    val selectedDay by viewModel.selectedDay.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val allEntries by viewModel.allTimetableEntries.collectAsState()
    val allEvents by viewModel.allAcademicEvents.collectAsState()
    val allSubjects by viewModel.allSubjects.collectAsState()
    val unscheduledReqs by viewModel.unscheduledRequirements.collectAsState()
    val prefs by preferencesManager.state.collectAsState()

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Sheet & Dialog states
    var showScheduleSheet by remember { mutableStateOf(false) }
    var sheetPreselectedDay by remember { mutableIntStateOf(selectedDay) }
    var sheetPreselectedHour by remember { mutableIntStateOf(8) }
    var sheetPreferUnscheduled by remember { mutableStateOf(false) }

    var selectedEntryDetail by remember { mutableStateOf<TimetableEntryWithSubject?>(null) }
    var selectedExamDetail by remember { mutableStateOf<AcademicEventWithSubject?>(null) }
    var addExamOnEpochDay by remember { mutableStateOf<Long?>(null) }
    var pendingConflict by remember { mutableStateOf<PendingConflictAction?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Synchronize HorizontalPager with selectedDay (1..7 <-> page 0..6)
    val pagerState = rememberPagerState(
        initialPage = (selectedDay - 1).coerceIn(0, 6),
        pageCount = { 7 }
    )

    LaunchedEffect(pagerState.currentPage) {
        val targetDay = pagerState.currentPage + 1
        if (selectedDay != targetDay) {
            viewModel.setSelectedDay(targetDay)
        }
    }

    LaunchedEffect(selectedDay) {
        val targetPage = (selectedDay - 1).coerceIn(0, 6)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Schedule Entry Bottom Sheet (Pre-filled when tapping any empty grid slot!)
    if (showScheduleSheet) {
        ScheduleEntryBottomSheet(
            sheetState = sheetState,
            unscheduledRequirements = unscheduledReqs,
            allSubjects = allSubjects,
            preselectedDay = sheetPreselectedDay,
            preselectedStartHour = sheetPreselectedHour,
            preferUnscheduledTab = sheetPreferUnscheduled,
            defaultReminderOffsetsCsv = prefs.defaultReminderOffsetsCsv,
            onDismissRequest = { showScheduleSheet = false },
            onSaveSchedule = { req, day, startHour, duration, room, teacher, notes, notifEnabled, offsetsCsv ->
                viewModel.attemptScheduleRequirement(
                    requirement = req,
                    day = day,
                    startHour = startHour,
                    duration = duration,
                    room = room,
                    teacher = teacher,
                    notes = notes,
                    notificationsEnabled = notifEnabled,
                    reminderOffsetsCsv = offsetsCsv,
                    onConflictDetected = { conflicting ->
                        pendingConflict = PendingConflictAction.RequirementConflict(
                            req = req,
                            day = day,
                            startHour = startHour,
                            duration = duration,
                            room = room,
                            teacher = teacher,
                            notes = notes,
                            notificationsEnabled = notifEnabled,
                            reminderOffsetsCsv = offsetsCsv,
                            conflictingEntry = conflicting
                        )
                    },
                    onSuccess = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showScheduleSheet = false
                        }
                    }
                )
            },
            onSaveQuickActivity = { existingSubId, newSubName, newSubColor, actType, customType, day, startHour, duration, room, teacher, notes, notifEnabled, offsetsCsv ->
                viewModel.attemptScheduleQuickActivity(
                    existingSubjectId = existingSubId,
                    newSubjectName = newSubName,
                    newSubjectColorHex = newSubColor,
                    activityType = actType,
                    customTypeName = customType,
                    day = day,
                    startHour = startHour,
                    duration = duration,
                    room = room,
                    teacher = teacher,
                    notes = notes,
                    notificationsEnabled = notifEnabled,
                    reminderOffsetsCsv = offsetsCsv,
                    onConflictDetected = { conflicting ->
                        pendingConflict = PendingConflictAction.QuickActivityConflict(
                            existingSubjectId = existingSubId,
                            newSubjectName = newSubName,
                            newSubjectColorHex = newSubColor,
                            activityType = actType,
                            customTypeName = customType,
                            day = day,
                            startHour = startHour,
                            duration = duration,
                            room = room,
                            teacher = teacher,
                            notes = notes,
                            notificationsEnabled = notifEnabled,
                            reminderOffsetsCsv = offsetsCsv,
                            conflictingEntry = conflicting
                        )
                    },
                    onSuccess = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showScheduleSheet = false
                        }
                    }
                )
            },
            onSaveExamEvent = { examEntity ->
                viewModel.addAcademicEvent(examEntity)
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) showScheduleSheet = false
                }
            }
        )
    }

    // Conflict Warning Dialog
    if (pendingConflict != null) {
        val conflictAction = pendingConflict!!
        ConflictWarningDialog(
            conflictingEntry = conflictAction.conflictingEntry,
            onCancel = { pendingConflict = null },
            onForceSave = {
                when (conflictAction) {
                    is PendingConflictAction.RequirementConflict -> {
                        viewModel.forceScheduleRequirement(
                            requirement = conflictAction.req,
                            day = conflictAction.day,
                            startHour = conflictAction.startHour,
                            duration = conflictAction.duration,
                            room = conflictAction.room,
                            teacher = conflictAction.teacher,
                            notes = conflictAction.notes,
                            notificationsEnabled = conflictAction.notificationsEnabled,
                            reminderOffsetsCsv = conflictAction.reminderOffsetsCsv
                        )
                    }
                    is PendingConflictAction.QuickActivityConflict -> {
                        viewModel.forceScheduleQuickActivity(
                            existingSubjectId = conflictAction.existingSubjectId,
                            newSubjectName = conflictAction.newSubjectName,
                            newSubjectColorHex = conflictAction.newSubjectColorHex,
                            activityType = conflictAction.activityType,
                            customTypeName = conflictAction.customTypeName,
                            day = conflictAction.day,
                            startHour = conflictAction.startHour,
                            duration = conflictAction.duration,
                            room = conflictAction.room,
                            teacher = conflictAction.teacher,
                            notes = conflictAction.notes,
                            notificationsEnabled = conflictAction.notificationsEnabled,
                            reminderOffsetsCsv = conflictAction.reminderOffsetsCsv
                        )
                    }
                }
                pendingConflict = null
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) showScheduleSheet = false
                }
            }
        )
    }

    // Activity Quick-View / Edit / Single-Week Cancel Sheet
    if (selectedEntryDetail != null) {
        TimetableEntryDetailDialog(
            item = selectedEntryDetail!!,
            onDismissRequest = { selectedEntryDetail = null },
            onUpdateEntry = { updated ->
                viewModel.updateEntry(updated)
                selectedEntryDetail = null
            },
            onToggleCanceledThisWeek = { entryId, canceled ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setEntryCanceledThisWeek(entryId, canceled)
            },
            onDeleteEntry = { entryId ->
                viewModel.deleteEntry(entryId)
                selectedEntryDetail = null
            }
        )
    }

    // Exam Detail / Edit Sheet
    if (selectedExamDetail != null || addExamOnEpochDay != null) {
        AddOrEditExamBottomSheet(
            initialEvent = selectedExamDetail?.event,
            subjects = allSubjects,
            defaultReminderOffsetsCsv = prefs.defaultReminderOffsetsCsv,
            preselectedDateEpochDay = addExamOnEpochDay,
            onDismiss = {
                selectedExamDetail = null
                addExamOnEpochDay = null
            },
            onSave = { entity ->
                if (entity.id == 0L) viewModel.addAcademicEvent(entity)
                else viewModel.updateAcademicEvent(entity)
                selectedExamDetail = null
                addExamOnEpochDay = null
            },
            onDelete = { eventId ->
                viewModel.deleteAcademicEvent(eventId)
                selectedExamDetail = null
                addExamOnEpochDay = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    sheetPreselectedDay = selectedDay
                    sheetPreselectedHour = 8
                    sheetPreferUnscheduled = false
                    showScheduleSheet = true
                },
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
                        text = "Add Activity",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Controls Row: View Mode Switcher (Day / Week / Month) + Reset Zoom chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.weight(1f)
                ) {
                    CalendarViewMode.entries.forEachIndexed { idx, mode ->
                        val icon = when (mode) {
                            CalendarViewMode.DAY -> Icons.Default.CalendarViewDay
                            CalendarViewMode.WEEK -> Icons.Default.CalendarViewWeek
                            CalendarViewMode.MONTH -> Icons.Default.CalendarViewMonth
                        }
                        SegmentedButton(
                            selected = viewMode == mode,
                            onClick = { viewModel.setViewMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = CalendarViewMode.entries.size
                            ),
                            icon = {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (viewMode != CalendarViewMode.MONTH && abs(prefs.hourHeightDp - 68f) > 4f) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            preferencesManager.setHourHeightDp(68f)
                        },
                        label = { Text("100%") },
                        leadingIcon = {
                            Icon(Icons.Default.ZoomInMap, contentDescription = "Reset Zoom", modifier = Modifier.size(15.dp))
                        }
                    )
                }
            }

            // Unscheduled Requirements Banner (if any)
            if (unscheduledReqs.isNotEmpty() && viewMode == CalendarViewMode.DAY) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    UnscheduledWarningBanner(
                        unscheduledCount = unscheduledReqs.size,
                        onClick = {
                            sheetPreselectedDay = selectedDay
                            sheetPreselectedHour = 8
                            sheetPreferUnscheduled = true
                            showScheduleSheet = true
                        }
                    )
                }
            }

            when (viewMode) {
                CalendarViewMode.DAY -> {
                    // "Next Up" / Status Pill for Today
                    val todayDow = LocalDate.now().dayOfWeek.value
                    val currentHour = LocalTime.now().hour
                    val nextTodayClass = allEntries
                        .filter {
                            it.entry.day == todayDow &&
                                !SudoSleepRepository.isCanceledForWeek(it.entry) &&
                                (it.entry.startHour + it.entry.duration) > currentHour
                        }
                        .minByOrNull { it.entry.startHour }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val statusText = if (nextTodayClass != null) {
                                val startH = nextTodayClass.entry.startHour
                                val prefix = if (startH <= currentHour) "Happening now" else "Next up today (${String.format(Locale.US, "%02d:00", startH)})"
                                "$prefix: ${nextTodayClass.subject.name}"
                            } else {
                                "No more classes today — enjoy your free time! ✨"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                        }
                    }

                    // Day Selector Tabs
                    DaySelector(
                        selectedDay = selectedDay,
                        onDaySelected = { day ->
                            viewModel.setSelectedDay(day)
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Swipeable 7-Day HorizontalPager wrapping the Pinch-to-Zoom TimetableGridCanvas
                    val mondayOfCurrentWeek = remember {
                        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { pageIndex ->
                        val pageDay = pageIndex + 1
                        val pageDate = mondayOfCurrentWeek.plusDays(pageIndex.toLong())
                        val dayEntries = allEntries.filter { it.entry.day == pageDay }
                        val dayEvents = allEvents.filter { it.event.dateEpochDay == pageDate.toEpochDay() }

                        TimetableGridCanvas(
                            entries = dayEntries,
                            eventsForDay = dayEvents,
                            hourHeight = prefs.hourHeightDp.dp,
                            onHourHeightChange = { newDp ->
                                preferencesManager.setHourHeightDp(newDp)
                            },
                            onEmptySlotClick = { clickedHour ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sheetPreselectedDay = pageDay
                                sheetPreselectedHour = clickedHour
                                sheetPreferUnscheduled = false
                                showScheduleSheet = true
                            },
                            onEntryClick = { selectedEntryDetail = it },
                            onEventClick = { selectedExamDetail = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                CalendarViewMode.WEEK -> {
                    TimetableWeekView(
                        allEntries = allEntries,
                        allEvents = allEvents,
                        hourHeight = prefs.hourHeightDp.dp,
                        onHourHeightChange = { newDp ->
                            preferencesManager.setHourHeightDp(newDp)
                        },
                        onEmptySlotClick = { day, startHour ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sheetPreselectedDay = day
                            sheetPreselectedHour = startHour
                            sheetPreferUnscheduled = false
                            showScheduleSheet = true
                        },
                        onEntryClick = { selectedEntryDetail = it },
                        onEventClick = { selectedExamDetail = it },
                        onDayHeaderClick = { day ->
                            viewModel.setSelectedDay(day)
                            viewModel.setViewMode(CalendarViewMode.DAY)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                CalendarViewMode.MONTH -> {
                    TimetableMonthView(
                        allEntries = allEntries,
                        allEvents = allEvents,
                        onJumpToDayView = { dayOfWeek ->
                            viewModel.setSelectedDay(dayOfWeek)
                            viewModel.setViewMode(CalendarViewMode.DAY)
                        },
                        onAddEventOnDate = { epochDay ->
                            addExamOnEpochDay = epochDay
                        },
                        onEntryClick = { selectedEntryDetail = it },
                        onEventClick = { selectedExamDetail = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
