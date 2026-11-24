package com.example.sudosleep.ui.timetable

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sudosleep.data.local.entity.AcademicEventEntity
import com.example.sudosleep.data.local.entity.SubjectEntity
import com.example.sudosleep.data.local.pojo.UnscheduledRequirementWithSubject
import com.example.sudosleep.notifications.NotificationHelper
import com.example.sudosleep.ui.exams.EVENT_TYPES
import com.example.sudosleep.ui.subject.SUBJECT_COLOR_OPTIONS

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleEntryBottomSheet(
    sheetState: SheetState,
    unscheduledRequirements: List<UnscheduledRequirementWithSubject>,
    allSubjects: List<SubjectEntity> = emptyList(),
    preselectedDay: Int = 1,
    preselectedStartHour: Int = 8,
    preferUnscheduledTab: Boolean = false,
    defaultReminderOffsetsCsv: String = "15,60",
    onDismissRequest: () -> Unit,
    onSaveSchedule: (
        requirement: UnscheduledRequirementWithSubject,
        day: Int,
        startHour: Int,
        duration: Int,
        room: String?,
        teacher: String?,
        notes: String?,
        notificationsEnabled: Boolean,
        reminderOffsetsCsv: String
    ) -> Unit,
    onSaveQuickActivity: (
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onSaveExamEvent: (AcademicEventEntity) -> Unit = {}
) {
    // Mode: 0 = Quick Activity, 1 = Pending Requirement, 2 = Exam / Deadline
    var selectedMode by remember {
        mutableIntStateOf(
            if (preferUnscheduledTab && unscheduledRequirements.isNotEmpty()) 1 else 0
        )
    }

    var selectedReq by remember { mutableStateOf(unscheduledRequirements.firstOrNull()) }
    var selectedDay by remember { mutableIntStateOf(preselectedDay.coerceIn(1, 7)) }
    var selectedStartHour by remember { mutableIntStateOf(preselectedStartHour.coerceIn(8, 21)) }
    var selectedDuration by remember {
        mutableIntStateOf(if (preselectedStartHour + 2 <= 22) 2 else 1)
    }

    // Quick Activity state
    var selectedSubjectId by remember { mutableStateOf(allSubjects.firstOrNull()?.id) }
    var createNewSubjectInline by remember { mutableStateOf(allSubjects.isEmpty()) }
    var newSubjectName by remember { mutableStateOf("") }
    var newSubjectColorHex by remember { mutableStateOf(SUBJECT_COLOR_OPTIONS.first().hex) }
    var quickActivityType by remember { mutableStateOf("Lecture") }
    var customActivityName by remember { mutableStateOf("") }

    // Exam / Deadline state
    var examTitle by remember { mutableStateOf("") }
    var examCategory by remember { mutableStateOf("Exam") }

    // Shared fields
    var roomText by remember { mutableStateOf("") }
    var teacherText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val initialOffsets = remember(defaultReminderOffsetsCsv) {
        defaultReminderOffsetsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(15) }
    }
    val selectedOffsets = remember { mutableStateListOf<Int>().apply { addAll(initialOffsets) } }

    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var startHourDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val validStartHours = (8..21).toList()
    val availableDurations = listOf(1, 2, 3, 4)
    val standardActivityTypes = listOf("Lecture", "Laboratory", "Seminar", "Practical Work", "Study Session", "Custom")

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add to Timetable ✨",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val dayLabel = DAYS_OF_WEEK.firstOrNull { it.first == selectedDay }?.second ?: "Monday"
                    Text(
                        text = String.format(Locale.US, "%s • Starting at %02d:00", dayLabel, selectedStartHour),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Switcher: Quick Class | Unscheduled Queue | Exam/Deadline
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    0 to "Quick Class",
                    1 to "Queue (${unscheduledRequirements.size})",
                    2 to "🎯 Exam"
                )
                modes.forEachIndexed { idx, (modeVal, label) ->
                    SegmentedButton(
                        selected = selectedMode == modeVal,
                        onClick = {
                            selectedMode = modeVal
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size)
                    ) {
                        Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedMode) {
                // MODE 0: Quick Class / Activity
                0 -> {
                    Text(
                        text = "Subject / Course",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (allSubjects.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = !createNewSubjectInline,
                                onClick = { createNewSubjectInline = false },
                                label = { Text("Existing Subject") }
                            )
                            FilterChip(
                                selected = createNewSubjectInline,
                                onClick = { createNewSubjectInline = true },
                                label = { Text("+ New Subject / Standalone") }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!createNewSubjectInline && allSubjects.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = subjectDropdownExpanded,
                            onExpandedChange = { subjectDropdownExpanded = !subjectDropdownExpanded }
                        ) {
                            val currentSubject = allSubjects.firstOrNull { it.id == selectedSubjectId } ?: allSubjects.first()
                            OutlinedTextField(
                                value = currentSubject.name,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = subjectDropdownExpanded,
                                onDismissRequest = { subjectDropdownExpanded = false }
                            ) {
                                allSubjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            selectedSubjectId = sub.id
                                            subjectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = newSubjectName,
                            onValueChange = {
                                newSubjectName = it
                                errorMessage = null
                            },
                            label = { Text("Subject or Activity Title") },
                            placeholder = { Text("e.g. Artificial Intelligence / Gym / Study") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(SUBJECT_COLOR_OPTIONS) { option ->
                                val isSel = option.hex == newSubjectColorHex
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(option.color)
                                        .border(
                                            width = if (isSel) 2.5.dp else 0.dp,
                                            color = if (isSel) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { newSubjectColorHex = option.hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Activity Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        standardActivityTypes.forEach { type ->
                            val isSel = quickActivityType == type
                            FilterChip(
                                selected = isSel,
                                onClick = { quickActivityType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    if (quickActivityType == "Custom") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customActivityName,
                            onValueChange = { customActivityName = it },
                            label = { Text("Custom Activity Label") },
                            placeholder = { Text("e.g. Consultation / Tutoring") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // MODE 1: Unscheduled Requirement Queue
                1 -> {
                    Text(
                        text = "Select Unscheduled Requirement",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (unscheduledRequirements.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎉 All required subject components are scheduled! Switch to 'Quick Class' or '🎯 Exam' above to add more.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(unscheduledRequirements) { item ->
                                val reqName = item.requirement.customTypeName ?: item.requirement.activityType
                                val chipText = "${item.subject.name} • $reqName"
                                val isSelected = selectedReq?.requirement?.id == item.requirement.id

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedReq = item
                                        errorMessage = null
                                    },
                                    label = { Text(chipText, maxLines = 1) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // MODE 2: Exam / Deadline on this Slot
                2 -> {
                    Text(
                        text = "Exam / Deadline Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EVENT_TYPES.forEach { cat ->
                            FilterChip(
                                selected = examCategory == cat,
                                onClick = { examCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = examTitle,
                        onValueChange = {
                            examTitle = it
                            errorMessage = null
                        },
                        label = { Text("Exam or Assessment Title") },
                        placeholder = { Text("e.g. Software Architecture Midterm") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day Selection Dropdown
            Text(
                text = "Day of Week",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = dayDropdownExpanded,
                onExpandedChange = { dayDropdownExpanded = !dayDropdownExpanded }
            ) {
                val currentDayLabel = DAYS_OF_WEEK.firstOrNull { it.first == selectedDay }?.second ?: "Monday"
                OutlinedTextField(
                    value = currentDayLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                    shape = RoundedCornerShape(14.dp)
                )
                ExposedDropdownMenu(
                    expanded = dayDropdownExpanded,
                    onDismissRequest = { dayDropdownExpanded = false }
                ) {
                    DAYS_OF_WEEK.forEach { (dNum, dName) ->
                        DropdownMenuItem(
                            text = { Text(dName) },
                            onClick = {
                                selectedDay = dNum
                                dayDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start Hour & Duration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = startHourDropdownExpanded,
                        onExpandedChange = { startHourDropdownExpanded = !startHourDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = String.format(Locale.US, "%02d:00", selectedStartHour),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startHourDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = startHourDropdownExpanded,
                            onDismissRequest = { startHourDropdownExpanded = false }
                        ) {
                            validStartHours.forEach { hour ->
                                DropdownMenuItem(
                                    text = { Text(String.format(Locale.US, "%02d:00", hour)) },
                                    onClick = {
                                        selectedStartHour = hour
                                        if (selectedStartHour + selectedDuration > 22) {
                                            selectedDuration = (22 - selectedStartHour).coerceAtLeast(1)
                                        }
                                        startHourDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableDurations.forEach { dur ->
                            val isAllowed = selectedStartHour + dur <= 22
                            val isSelected = selectedDuration == dur

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isAllowed) selectedDuration = dur
                                },
                                enabled = isAllowed,
                                label = { Text("${dur}h") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Room & Teacher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = roomText,
                    onValueChange = { roomText = it },
                    label = { Text("Room (Optional)") },
                    placeholder = { Text("Hall A101") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = teacherText,
                    onValueChange = { teacherText = it },
                    label = { Text("Instructor (Optional)") },
                    placeholder = { Text("Prof. Turing") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes / Topic (Optional)") },
                placeholder = { Text("Bring laptop, Chapter 4 exercises…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Multiple Reminder Chips Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reminders Before Start",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }

                    AnimatedVisibility(visible = notificationsEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NotificationHelper.AVAILABLE_REMINDER_OFFSETS.forEach { (mins, label) ->
                                    val isSel = selectedOffsets.contains(mins)
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            if (isSel) selectedOffsets.remove(mins)
                                            else selectedOffsets.add(mins)
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    val offsetsCsv = selectedOffsets.sorted().joinToString(",")
                    when (selectedMode) {
                        0 -> {
                            val existingId = if (createNewSubjectInline || allSubjects.isEmpty()) {
                                null
                            } else {
                                selectedSubjectId ?: allSubjects.firstOrNull()?.id
                            }
                            if (existingId == null && newSubjectName.isBlank()) {
                                errorMessage = "Please enter a Subject or Activity title"
                                return@Button
                            }
                            onSaveQuickActivity(
                                existingId,
                                newSubjectName.trim().ifEmpty { null },
                                newSubjectColorHex,
                                quickActivityType,
                                customActivityName.trim().ifEmpty { null },
                                selectedDay,
                                selectedStartHour,
                                selectedDuration,
                                roomText.trim().ifEmpty { null },
                                teacherText.trim().ifEmpty { null },
                                notesText.trim().ifEmpty { null },
                                notificationsEnabled,
                                offsetsCsv
                            )
                        }
                        1 -> {
                            val req = selectedReq
                            if (req == null) {
                                errorMessage = "Please select an unscheduled requirement or switch to 'Quick Class'"
                                return@Button
                            }
                            onSaveSchedule(
                                req,
                                selectedDay,
                                selectedStartHour,
                                selectedDuration,
                                roomText.trim().ifEmpty { null },
                                teacherText.trim().ifEmpty { null },
                                notesText.trim().ifEmpty { null },
                                notificationsEnabled,
                                offsetsCsv
                            )
                        }
                        2 -> {
                            if (examTitle.isBlank()) {
                                errorMessage = "Please enter a title for the exam or deadline"
                                return@Button
                            }
                            val targetDow = DayOfWeek.of(selectedDay.coerceIn(1, 7))
                            val nextDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(targetDow))
                            onSaveExamEvent(
                                AcademicEventEntity(
                                    subjectId = selectedSubjectId,
                                    title = examTitle.trim(),
                                    eventType = examCategory,
                                    dateEpochDay = nextDate.toEpochDay(),
                                    startHour = selectedStartHour,
                                    duration = selectedDuration,
                                    room = roomText.trim().ifEmpty { null },
                                    notes = notesText.trim().ifEmpty { null },
                                    colorHex = "#D81B60",
                                    notificationsEnabled = notificationsEnabled,
                                    reminderOffsetsCsv = offsetsCsv
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "Save to Schedule ✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
