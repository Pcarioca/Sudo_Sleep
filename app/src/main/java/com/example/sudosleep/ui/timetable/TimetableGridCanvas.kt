package com.example.sudosleep.ui.timetable

import java.time.LocalTime
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudosleep.data.local.pojo.AcademicEventWithSubject
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.repository.SudoSleepRepository
import com.example.sudosleep.ui.subject.parseHexColor

@Composable
fun TimetableGridCanvas(
    entries: List<TimetableEntryWithSubject>,
    eventsForDay: List<AcademicEventWithSubject> = emptyList(),
    onEntryClick: (TimetableEntryWithSubject) -> Unit,
    onEventClick: (AcademicEventWithSubject) -> Unit = {},
    onEmptySlotClick: (startHour: Int) -> Unit = {},
    onHourHeightChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    hourHeight: Dp = 68.dp,
    startHour: Int = 8,
    endHour: Int = 22
) {
    val totalHours = endHour - startHour
    val totalHeight = hourHeight * totalHours
    val scrollState = rememberScrollState()
    val currentTime = LocalTime.now()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(hourHeight) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                onHourHeightChange(hourHeight.value * zoom)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .verticalScroll(scrollState)
    ) {
        // 1. Interactive Hourly Grid Rows (Direct Tap-to-Add on any slot!)
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            for (h in startHour until endHour) {
                val isSlotOccupied = entries.any { h >= it.entry.startHour && h < it.entry.startHour + it.entry.duration } ||
                    eventsForDay.any { h >= it.event.startHour && h < it.event.startHour + it.event.duration }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeight)
                        .clickable { onEmptySlotClick(h) },
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d:00", h),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(54.dp)
                            .padding(top = 4.dp, start = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )

                        // Subtle "+ Tap to schedule" hint on empty rows when zoomed in enough
                        if (!isSlotOccupied && hourHeight >= 54.dp) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tap to add at ${String.format(Locale.US, "%02d:00", h)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Live Current Time Indicator Line (if within 08:00–22:00)
        if (currentTime.hour in startHour until endHour) {
            val fractionalHours = (currentTime.hour - startHour) + (currentTime.minute / 60f)
            val lineOffset = hourHeight * fractionalHours
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = lineOffset)
                    .padding(start = 46.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                HorizontalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // 3. Scheduled Class & Exam Cards Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .padding(start = 58.dp, end = 10.dp)
        ) {
            // Recurring Class Cards
            entries.forEach { item ->
                val entry = item.entry
                val subject = item.subject
                val req = item.requirement
                val isCanceledThisWeek = SudoSleepRepository.isCanceledForWeek(entry)

                val topOffset = hourHeight * (entry.startHour - startHour)
                val blockHeight = hourHeight * entry.duration
                val themeColor = parseHexColor(subject.colorHex)

                val activityTypeName = req.customTypeName ?: req.activityType
                val timeSpanText = String.format(
                    Locale.US,
                    "%02d:00 - %02d:00",
                    entry.startHour,
                    entry.startHour + entry.duration
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = topOffset)
                        .height(blockHeight - 4.dp)
                        .padding(vertical = 2.dp)
                        .clickable { onEntryClick(item) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCanceledThisWeek) {
                            themeColor.copy(alpha = 0.38f)
                        } else {
                            themeColor.copy(alpha = 0.93f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isCanceledThisWeek) 0.dp else 3.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textDecoration = if (isCanceledThisWeek) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (entry.notificationsEnabled && !isCanceledThisWeek) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Reminders active",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCanceledThisWeek) Color.Black.copy(alpha = 0.35f)
                                    else Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = if (isCanceledThisWeek) "Canceled this week" else activityTypeName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCanceledThisWeek) {
                                Icon(
                                    imageVector = Icons.Default.EventBusy,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = timeSpanText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        if (blockHeight >= 86.dp) {
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!entry.room.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.88f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = entry.room,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 1
                                    )
                                }
                            }

                            if (!entry.teacher.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.88f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = entry.teacher,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Date-Specific Exam / Deadline Cards for Today's Weekday in Current Week
            eventsForDay.forEach { evItem ->
                val ev = evItem.event
                val topOffset = hourHeight * (ev.startHour - startHour)
                val blockHeight = hourHeight * ev.duration
                val accentColor = parseHexColor(evItem.subject?.colorHex ?: ev.colorHex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = topOffset)
                        .height(blockHeight - 4.dp)
                        .padding(vertical = 2.dp)
                        .clickable { onEventClick(evItem) },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, Color.White),
                    colors = CardDefaults.cardColors(
                        containerColor = accentColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 ${ev.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.28f)
                            ) {
                                Text(
                                    text = ev.eventType.uppercase(Locale.US),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = String.format(
                                Locale.US,
                                "%02d:00 - %02d:00%s",
                                ev.startHour,
                                ev.startHour + ev.duration,
                                ev.room?.let { " • $it" } ?: ""
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }
    }
}
