package com.example.sudosleep.ui.timetable

import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.sudosleep.data.local.entity.TimetableEntryEntity
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.repository.SudoSleepRepository
import com.example.sudosleep.notifications.NotificationHelper
import com.example.sudosleep.ui.subject.parseHexColor

/**
 * Polished Activity Quick-View & Edit Bottom Sheet:
 * - Includes "Canceled this week only" switch (dims activity & suppresses reminders for current week).
 * - Supports editing room, instructor, notes, duration, and multiple reminder timings.
 * - Safe Deletion UX: No accidental one-tap delete on the initial card; Delete is tucked inside Edit mode ->
 *   Danger Zone / More menu with a mandatory confirmation AlertDialog.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimetableEntryDetailDialog(
    item: TimetableEntryWithSubject,
    onDismissRequest: () -> Unit,
    onUpdateEntry: (TimetableEntryEntity) -> Unit = {},
    onToggleCanceledThisWeek: (entryId: Long, canceled: Boolean) -> Unit = { _, _ -> },
    onDeleteEntry: (entryId: Long) -> Unit
) {
    val entry = item.entry
    val subject = item.subject
    val req = item.requirement
    val themeColor = parseHexColor(subject.colorHex)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isEditMode by remember { mutableStateOf(false) }
    var showDangerZone by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    var isCanceledThisWeek by remember(entry) {
        mutableStateOf(SudoSleepRepository.isCanceledForWeek(entry))
    }
    var roomText by remember(entry) { mutableStateOf(entry.room ?: "") }
    var teacherText by remember(entry) { mutableStateOf(entry.teacher ?: "") }
    var notesText by remember(entry) { mutableStateOf(entry.notes ?: "") }
    var startHour by remember(entry) { mutableIntStateOf(entry.startHour) }
    var duration by remember(entry) { mutableIntStateOf(entry.duration) }
    var notificationsEnabled by remember(entry) { mutableStateOf(entry.notificationsEnabled) }

    val selectedOffsets = remember(entry) {
        mutableStateListOf<Int>().apply { addAll(entry.getReminderOffsetsMinutes()) }
    }

    val activityTypeName = req.customTypeName ?: req.activityType
    val dayName = DAYS_OF_WEEK.firstOrNull { it.first == entry.day }?.second ?: "Monday"
    val timeSpanText = String.format(
        Locale.US,
        "%s • %02d:00 - %02d:00 (%dh)",
        dayName,
        startHour,
        startHour + duration,
        duration
    )

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("Remove ${subject.name} ($activityTypeName)?") },
            text = {
                Text(
                    "Are you sure you want to permanently remove this class from your weekly timetable? " +
                        "If it was only canceled once, use 'Canceled this week only' instead."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteDialog = false
                        onDeleteEntry(entry.id)
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove Permanently")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Keep Class")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(themeColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = themeColor.copy(alpha = 0.16f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = activityTypeName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = themeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row {
                    if (isEditMode) {
                        IconButton(onClick = { showDangerZone = !showDangerZone }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Canceled This Week Only" Quick Toggle Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCanceledThisWeek) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = if (isCanceledThisWeek) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Canceled this week only",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isCanceledThisWeek) {
                                    "Class is paused & reminders muted for this week 💤"
                                } else {
                                    "Skip this week without deleting from your schedule"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isCanceledThisWeek,
                        onCheckedChange = { checked ->
                            isCanceledThisWeek = checked
                            onToggleCanceledThisWeek(entry.id, checked)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isEditMode) {
                // Quick-View Details Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = timeSpanText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = entry.room?.takeIf { it.isNotBlank() } ?: "No room specified",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = entry.teacher?.takeIf { it.isNotBlank() } ?: "No instructor specified",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val remLabels = entry.getReminderOffsetsMinutes()
                                .joinToString(", ") { NotificationHelper.formatOffsetLabel(it) }
                                .ifEmpty { "None" }
                            Text(
                                text = if (entry.notificationsEnabled) "Reminders: $remLabels" else "Reminders disabled",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (!entry.notes.isNullOrBlank()) {
                            HorizontalDivider()
                            Text(
                                text = "📝 Notes: ${entry.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Done")
                    }

                    Button(
                        onClick = { isEditMode = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Activity")
                    }
                }
            } else {
                // EDIT MODE (Room, Teacher, Notes, Duration, Reminders, and Deeper Delete Option)
                OutlinedTextField(
                    value = roomText,
                    onValueChange = { roomText = it },
                    label = { Text("Room / Location") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = teacherText,
                    onValueChange = { teacherText = it },
                    label = { Text("Teacher / Instructor") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Session Notes") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Duration", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 4).forEach { dur ->
                        val allowed = startHour + dur <= 22
                        FilterChip(
                            selected = duration == dur,
                            onClick = { if (allowed) duration = dur },
                            enabled = allowed,
                            label = { Text("${dur}h") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reminder Toggles
                Card(
                    shape = RoundedCornerShape(16.dp),
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
                            Text("Enable Activity Reminders", fontWeight = FontWeight.Bold)
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
                            )
                        }
                        AnimatedVisibility(visible = notificationsEnabled) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 8.dp)
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

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val updated = entry.copy(
                            room = roomText.trim().ifEmpty { null },
                            teacher = teacherText.trim().ifEmpty { null },
                            notes = notesText.trim().ifEmpty { null },
                            duration = duration,
                            notificationsEnabled = notificationsEnabled,
                            reminderOffsetsCsv = selectedOffsets.sorted().joinToString(","),
                            canceledWeekEpochId = if (isCanceledThisWeek) {
                                SudoSleepRepository.computeWeekEpochId()
                            } else {
                                null
                            }
                        )
                        onUpdateEntry(updated)
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Save Changes ✨", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Deeper Safe Deletion Section (Protected behind overflow / toggle + confirmation dialog)
                if (!showDangerZone) {
                    TextButton(
                        onClick = { showDangerZone = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Advanced / Remove from Timetable…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedButton(
                        onClick = { showConfirmDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Permanently Remove Activity from Schedule")
                    }
                }
            }
        }
    }
}
