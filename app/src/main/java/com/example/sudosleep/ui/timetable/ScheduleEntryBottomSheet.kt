package com.example.sudosleep.ui.timetable

import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sudosleep.data.local.pojo.UnscheduledRequirementWithSubject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleEntryBottomSheet(
    sheetState: SheetState,
    unscheduledRequirements: List<UnscheduledRequirementWithSubject>,
    preselectedDay: Int = 1,
    onDismissRequest: () -> Unit,
    onSaveSchedule: (
        requirement: UnscheduledRequirementWithSubject,
        day: Int,
        startHour: Int,
        duration: Int,
        room: String?,
        teacher: String?
    ) -> Unit
) {
    var selectedReq by remember {
        mutableStateOf(unscheduledRequirements.firstOrNull())
    }
    var selectedDay by remember { mutableIntStateOf(preselectedDay) }
    var selectedStartHour by remember { mutableIntStateOf(8) }
    var selectedDuration by remember { mutableIntStateOf(2) }
    var roomText by remember { mutableStateOf("") }
    var teacherText by remember { mutableStateOf("") }

    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var startHourDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val daysList = listOf(
        1 to "Monday",
        2 to "Tuesday",
        3 to "Wednesday",
        4 to "Thursday",
        5 to "Friday",
        6 to "Saturday",
        7 to "Sunday"
    )

    val validStartHours = (8..21).toList()
    val availableDurations = listOf(1, 2, 3, 4)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Schedule Activity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unscheduled Requirements Queue Chips
            Text(
                text = "Select Unscheduled Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (unscheduledRequirements.isEmpty()) {
                Text(
                    text = "All required activities are scheduled! Awesome job.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(unscheduledRequirements) { item ->
                        val reqName = item.requirement.customTypeName ?: item.requirement.activityType
                        val chipText = "${item.subject.name} - $reqName"
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

            Spacer(modifier = Modifier.height(20.dp))

            // Day Selection Dropdown
            Text(
                text = "Day of Week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = dayDropdownExpanded,
                onExpandedChange = { dayDropdownExpanded = !dayDropdownExpanded }
            ) {
                val currentDayLabel = daysList.firstOrNull { it.first == selectedDay }?.second ?: "Monday"
                OutlinedTextField(
                    value = currentDayLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = dayDropdownExpanded,
                    onDismissRequest = { dayDropdownExpanded = false }
                ) {
                    daysList.forEach { (dNum, dName) ->
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

            Spacer(modifier = Modifier.height(16.dp))

            // Start Hour & Duration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Start Hour
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
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
                            shape = RoundedCornerShape(12.dp)
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
                                        // Adjust duration if startHour + duration > 22
                                        if (selectedStartHour + selectedDuration > 22) {
                                            selectedDuration = 22 - selectedStartHour
                                        }
                                        startHourDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Duration
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
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

            Spacer(modifier = Modifier.height(16.dp))

            // Optional Room
            OutlinedTextField(
                value = roomText,
                onValueChange = { roomText = it },
                label = { Text("Room / Hall (Optional)") },
                placeholder = { Text("e.g. Hall A101 / Lab 3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Teacher / Instructor
            OutlinedTextField(
                value = teacherText,
                onValueChange = { teacherText = it },
                label = { Text("Teacher / Instructor (Optional)") },
                placeholder = { Text("e.g. Prof. Alan Turing") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Schedule Button
            Button(
                onClick = {
                    val req = selectedReq
                    if (req == null) {
                        errorMessage = "Please select an activity requirement to schedule"
                        return@Button
                    }
                    if (selectedStartHour + selectedDuration > 22) {
                        errorMessage = "Activity end time cannot exceed 22:00"
                        return@Button
                    }

                    onSaveSchedule(
                        req,
                        selectedDay,
                        selectedStartHour,
                        selectedDuration,
                        roomText.trim().ifEmpty { null },
                        teacherText.trim().ifEmpty { null }
                    )
                },
                enabled = selectedReq != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Save to Timetable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
