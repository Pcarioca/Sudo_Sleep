package com.example.sudosleep.ui.exams

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.sudosleep.data.local.pojo.AcademicEventWithSubject
import com.example.sudosleep.notifications.NotificationHelper
import com.example.sudosleep.ui.subject.SUBJECT_COLOR_OPTIONS
import com.example.sudosleep.ui.subject.parseHexColor
import com.example.sudosleep.ui.viewmodel.TimetableViewModel

val EVENT_TYPES = listOf("Exam", "Midterm", "Project Deadline", "Quiz", "Custom Event")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsAndDeadlinesScreen(
    viewModel: TimetableViewModel,
    defaultReminderOffsetsCsv: String = "15,60,1440"
) {
    val events by viewModel.allAcademicEvents.collectAsState()
    val subjects by viewModel.allSubjects.collectAsState()

    var showAddOrEditSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<AcademicEventWithSubject?>(null) }

    val todayEpoch = LocalDate.now().toEpochDay()
    val upcomingEvents = events.filter { it.event.dateEpochDay >= todayEpoch }
    val pastEvents = events.filter { it.event.dateEpochDay < todayEpoch }

    if (showAddOrEditSheet) {
        AddOrEditExamBottomSheet(
            initialEvent = editingEvent?.event,
            subjects = subjects,
            defaultReminderOffsetsCsv = defaultReminderOffsetsCsv,
            onDismiss = {
                showAddOrEditSheet = false
                editingEvent = null
            },
            onSave = { entity ->
                if (entity.id == 0L) {
                    viewModel.addAcademicEvent(entity)
                } else {
                    viewModel.updateAcademicEvent(entity)
                }
                showAddOrEditSheet = false
                editingEvent = null
            },
            onDelete = { eventId ->
                viewModel.deleteAcademicEvent(eventId)
                showAddOrEditSheet = false
                editingEvent = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEvent = null
                    showAddOrEditSheet = true
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
                    Text("Schedule Exam / Deadline", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val nextEvent = upcomingEvents.firstOrNull()
                val daysUntilNext = nextEvent?.let {
                    ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.ofEpochDay(it.event.dateEpochDay))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🎯 Exam & Deadline Radar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (nextEvent != null) {
                                Text(
                                    text = nextEvent.event.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val countdownText = when (daysUntilNext) {
                                    0L -> "Happening Today! 🔥"
                                    1L -> "Tomorrow • ${String.format(Locale.US, "%02d:00", nextEvent.event.startHour)}"
                                    else -> "In $daysUntilNext days • ${
                                        LocalDate.ofEpochDay(nextEvent.event.dateEpochDay)
                                            .format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
                                    }"
                                }
                                Text(
                                    text = countdownText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            } else {
                                Text(
                                    text = "No upcoming exams! 🎉",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Tap '+ Schedule Exam / Deadline' below to add one.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Upcoming Exams & Deadlines (${upcomingEvents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (upcomingEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✨ All clear! No upcoming exams or deadlines scheduled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(upcomingEvents, key = { it.event.id }) { item ->
                    ExamEventCard(
                        item = item,
                        onClick = {
                            editingEvent = item
                            showAddOrEditSheet = true
                        }
                    )
                }
            }

            if (pastEvents.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completed / Past Events (${pastEvents.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(pastEvents, key = { it.event.id }) { item ->
                    ExamEventCard(
                        item = item,
                        onClick = {
                            editingEvent = item
                            showAddOrEditSheet = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}

@Composable
fun ExamEventCard(
    item: AcademicEventWithSubject,
    onClick: () -> Unit
) {
    val event = item.event
    val accentColor = parseHexColor(item.subject?.colorHex ?: event.colorHex)
    val eventDate = LocalDate.ofEpochDay(event.dateEpochDay)
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), eventDate)

    val countdownLabel = when {
        daysLeft < 0 -> "Completed"
        daysLeft == 0L -> "Today!"
        daysLeft == 1L -> "1 day left"
        else -> "$daysLeft days left"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "🎯 ${event.eventType.uppercase(Locale.US)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (item.subject != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.subject.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (daysLeft in 0..3) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = countdownLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (daysLeft in 0..3) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = eventDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%02d:00 (%dh)", event.startHour, event.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!event.room.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.room,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddOrEditExamBottomSheet(
    initialEvent: AcademicEventEntity?,
    subjects: List<SubjectEntity>,
    defaultReminderOffsetsCsv: String = "15,60,1440",
    preselectedDateEpochDay: Long? = null,
    preselectedStartHour: Int? = null,
    onDismiss: () -> Unit,
    onSave: (AcademicEventEntity) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var eventType by remember { mutableStateOf(initialEvent?.eventType ?: "Exam") }
    var selectedSubjectId by remember { mutableStateOf(initialEvent?.subjectId ?: subjects.firstOrNull()?.id) }
    var dateEpochDay by remember {
        mutableLongStateOf(
            initialEvent?.dateEpochDay ?: preselectedDateEpochDay ?: LocalDate.now().plusDays(1).toEpochDay()
        )
    }
    var startHour by remember { mutableIntStateOf(initialEvent?.startHour ?: preselectedStartHour ?: 9) }
    var duration by remember { mutableIntStateOf(initialEvent?.duration ?: 2) }
    var room by remember { mutableStateOf(initialEvent?.room ?: "") }
    var notes by remember { mutableStateOf(initialEvent?.notes ?: "") }
    var colorHex by remember {
        mutableStateOf(initialEvent?.colorHex ?: SUBJECT_COLOR_OPTIONS.getOrNull(4)?.hex ?: "#D81B60")
    }
    var notificationsEnabled by remember { mutableStateOf(initialEvent?.notificationsEnabled ?: true) }

    val initialOffsets = remember(initialEvent, defaultReminderOffsetsCsv) {
        val csv = initialEvent?.reminderOffsetsCsv ?: defaultReminderOffsetsCsv
        csv.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
    val selectedOffsets = remember { mutableStateListOf<Int>().apply { addAll(initialOffsets) } }

    var showDatePicker by remember { mutableStateOf(false) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var hourMenuExpanded by remember { mutableStateOf(false) }
    var showDangerZone by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val initialMillis = LocalDate.ofEpochDay(dateEpochDay)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            dateEpochDay = pickedDate.toEpochDay()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirmDialog && initialEvent != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Permanently Delete ${initialEvent.title}?") },
            text = { Text("This exam/deadline and its scheduled reminders will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(initialEvent.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Keep It")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialEvent == null) "🎯 Schedule Exam / Deadline" else "✏️ Edit Exam / Deadline",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    if (initialEvent != null && onDelete != null) {
                        IconButton(onClick = { showDangerZone = !showDangerZone }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Event Type Chips
            Text("Event Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EVENT_TYPES.forEach { type ->
                    val selected = eventType == type
                    FilterChip(
                        selected = selected,
                        onClick = { eventType = type },
                        label = { Text(type) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Title / Assessment Name") },
                placeholder = { Text("e.g. Mobile Systems Midterm Exam") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Linked Subject Dropdown (Optional)
            Text("Linked Subject (Optional)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = subjectMenuExpanded,
                onExpandedChange = { subjectMenuExpanded = !subjectMenuExpanded }
            ) {
                val selectedSubject = subjects.firstOrNull { it.id == selectedSubjectId }
                OutlinedTextField(
                    value = selectedSubject?.name ?: "Standalone / General Event",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                    shape = RoundedCornerShape(14.dp)
                )
                ExposedDropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = { subjectMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Standalone / General Event") },
                        onClick = {
                            selectedSubjectId = null
                            subjectMenuExpanded = false
                        }
                    )
                    subjects.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text(sub.name) },
                            onClick = {
                                selectedSubjectId = sub.id
                                colorHex = sub.colorHex
                                subjectMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date & Start Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("Date", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val formattedDate = LocalDate.ofEpochDay(dateEpochDay)
                        .format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US))
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(formattedDate, maxLines = 1)
                    }
                }

                Column(modifier = Modifier.weight(0.8f)) {
                    Text("Start Hour", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = hourMenuExpanded,
                        onExpandedChange = { hourMenuExpanded = !hourMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = String.format(Locale.US, "%02d:00", startHour),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = hourMenuExpanded,
                            onDismissRequest = { hourMenuExpanded = false }
                        ) {
                            (8..21).forEach { h ->
                                DropdownMenuItem(
                                    text = { Text(String.format(Locale.US, "%02d:00", h)) },
                                    onClick = {
                                        startHour = h
                                        if (startHour + duration > 22) duration = (22 - startHour).coerceAtLeast(1)
                                        hourMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Duration", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 4).forEach { dur ->
                    val enabled = startHour + dur <= 22
                    FilterChip(
                        selected = duration == dur,
                        onClick = { if (enabled) duration = dur },
                        enabled = enabled,
                        label = { Text("${dur}h") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Room / Location (Optional)") },
                placeholder = { Text("e.g. Aula Magna / Online Portal") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Study Notes / Topics (Optional)") },
                placeholder = { Text("e.g. Chapters 1–5, bring student ID") },
                leadingIcon = { Icon(Icons.Default.EventNote, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Smart Reminders Section
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Exam Reminders", fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }

                    AnimatedVisibility(visible = notificationsEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select one or more countdown reminders:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NotificationHelper.AVAILABLE_REMINDER_OFFSETS.forEach { (mins, label) ->
                                    val selected = selectedOffsets.contains(mins)
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            if (selected) selectedOffsets.remove(mins)
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Please enter a title for the exam or deadline"
                        return@Button
                    }
                    onSave(
                        AcademicEventEntity(
                            id = initialEvent?.id ?: 0L,
                            subjectId = selectedSubjectId,
                            title = title.trim(),
                            eventType = eventType,
                            dateEpochDay = dateEpochDay,
                            startHour = startHour,
                            duration = duration,
                            room = room.trim().ifEmpty { null },
                            notes = notes.trim().ifEmpty { null },
                            colorHex = colorHex,
                            notificationsEnabled = notificationsEnabled,
                            reminderOffsetsCsv = selectedOffsets.sorted().joinToString(",")
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (initialEvent == null) "Save Exam / Deadline ✨" else "Save Changes ✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Deeper Safe Deletion Section (only when editing & toggled via 3-dot menu or bottom link)
            if (initialEvent != null && onDelete != null) {
                Spacer(modifier = Modifier.height(16.dp))
                if (!showDangerZone) {
                    TextButton(
                        onClick = { showDangerZone = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Advanced / Remove Options…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Exam / Deadline Permanently")
                    }
                }
            }
        }
    }
}
