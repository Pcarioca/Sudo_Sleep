package com.example.sudosleep.ui.timetable

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudosleep.data.local.pojo.AcademicEventWithSubject
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.data.repository.SudoSleepRepository
import com.example.sudosleep.ui.subject.parseHexColor

/**
 * Bird's-Eye 7-Day Week View with Pinch-to-Zoom, Direct Tap-to-Add on any empty cell,
 * and week-offset navigation.
 */
@Composable
fun TimetableWeekView(
    allEntries: List<TimetableEntryWithSubject>,
    allEvents: List<AcademicEventWithSubject>,
    hourHeight: Dp,
    onHourHeightChange: (Float) -> Unit,
    onEmptySlotClick: (day: Int, startHour: Int) -> Unit,
    onEntryClick: (TimetableEntryWithSubject) -> Unit,
    onEventClick: (AcademicEventWithSubject) -> Unit,
    onDayHeaderClick: (day: Int) -> Unit,
    modifier: Modifier = Modifier,
    startHour: Int = 8,
    endHour: Int = 22
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val mondayOfSelectedWeek = remember(weekOffset, today) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOffset.toLong())
    }
    val sundayOfSelectedWeek = mondayOfSelectedWeek.plusDays(6)

    val totalHours = endHour - startHour
    val totalHeight = hourHeight * totalHours
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        // Week Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { weekOffset -= 1 }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val rangeStr = "${
                    mondayOfSelectedWeek.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
                } – ${
                    sundayOfSelectedWeek.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                }"
                Text(
                    text = if (weekOffset == 0) "This Week • $rangeStr" else rangeStr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any cell to add • Pinch to zoom",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (weekOffset != 0) {
                    IconButton(onClick = { weekOffset = 0 }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "This Week", modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = { weekOffset += 1 }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
                }
            }
        }

        // 7-Day Column Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 6.dp)
        ) {
            Spacer(modifier = Modifier.width(42.dp))
            SHORT_DAYS_OF_WEEK.forEachIndexed { idx, shortName ->
                val dayNum = idx + 1
                val cellDate = mondayOfSelectedWeek.plusDays(idx.toLong())
                val isToday = cellDate == today

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDayHeaderClick(dayNum) }
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = shortName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cellDate.dayOfMonth.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Scrollable & Zoomable 7-Day Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
            ) {
                // Left Time Column
                Column(modifier = Modifier.width(42.dp)) {
                    for (h in startHour until endHour) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hourHeight),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d", h),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // 7 Day Columns
                for (dayNum in 1..7) {
                    val colDate = mondayOfSelectedWeek.plusDays((dayNum - 1).toLong())
                    val dayEntries = allEntries.filter { it.entry.day == dayNum }
                    val dayEvents = allEvents.filter { it.event.dateEpochDay == colDate.toEpochDay() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // Interactive Hour Cells for Direct Tap-to-Add
                        Column(modifier = Modifier.fillMaxSize()) {
                            for (h in startHour until endHour) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(hourHeight)
                                        .clickable { onEmptySlotClick(dayNum, h) }
                                ) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }

                        // Recurring Timetable Entries
                        dayEntries.forEach { item ->
                            val entry = item.entry
                            val isCanceled = SudoSleepRepository.isCanceledForWeek(entry, colDate)
                            val topOffset = hourHeight * (entry.startHour - startHour)
                            val blockHeight = hourHeight * entry.duration
                            val baseColor = parseHexColor(item.subject.colorHex)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCanceled) baseColor.copy(alpha = 0.32f) else baseColor.copy(alpha = 0.90f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = topOffset)
                                    .height(blockHeight - 2.dp)
                                    .padding(horizontal = 1.5.dp, vertical = 1.dp)
                                    .clickable { onEntryClick(item) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                ) {
                                    Text(
                                        text = item.subject.name,
                                        fontSize = 9.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textDecoration = if (isCanceled) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!entry.room.isNullOrBlank() && blockHeight >= 48.dp) {
                                        Text(
                                            text = entry.room,
                                            fontSize = 8.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Date-specific Exams/Deadlines falling on this day of the week
                        dayEvents.forEach { evItem ->
                            val ev = evItem.event
                            val topOffset = hourHeight * (ev.startHour - startHour)
                            val blockHeight = hourHeight * ev.duration
                            val evColor = parseHexColor(evItem.subject?.colorHex ?: ev.colorHex)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = evColor,
                                border = BorderStroke(1.5.dp, Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = topOffset)
                                    .height(blockHeight - 2.dp)
                                    .padding(horizontal = 1.5.dp, vertical = 1.dp)
                                    .clickable { onEventClick(evItem) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                ) {
                                    Text(
                                        text = "🎯 ${ev.title}",
                                        fontSize = 9.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Monthly Calendar View highlighting days with Exams/Deadlines and recurring classes,
 * plus an interactive selected-date agenda below.
 */
@Composable
fun TimetableMonthView(
    allEntries: List<TimetableEntryWithSubject>,
    allEvents: List<AcademicEventWithSubject>,
    onJumpToDayView: (dayOfWeek: Int) -> Unit,
    onAddEventOnDate: (epochDay: Long) -> Unit,
    onEntryClick: (TimetableEntryWithSubject) -> Unit,
    onEventClick: (AcademicEventWithSubject) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val firstDayOfMonth = currentYearMonth.atDay(1)
    val leadingEmptyCells = (firstDayOfMonth.dayOfWeek.value - 1).coerceIn(0, 6)
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7

    val selectedDayEvents = allEvents.filter { it.event.dateEpochDay == selectedDate.toEpochDay() }
    val selectedDayClasses = allEntries.filter { it.entry.day == selectedDate.dayOfWeek.value }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Month Header Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                        }

                        Text(
                            text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Row {
                            IconButton(
                                onClick = {
                                    currentYearMonth = YearMonth.now()
                                    selectedDate = LocalDate.now()
                                }
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Today", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Weekday Header Labels
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SHORT_DAYS_OF_WEEK.forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid Rows
                    val rows = totalCells / 7
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (c in 0 until 7) {
                                val cellIdx = r * 7 + c
                                val dayNum = cellIdx - leadingEmptyCells + 1
                                if (dayNum in 1..daysInMonth) {
                                    val cellDate = currentYearMonth.atDay(dayNum)
                                    val isSelected = cellDate == selectedDate
                                    val isToday = cellDate == LocalDate.now()
                                    val dateEvents = allEvents.filter { it.event.dateEpochDay == cellDate.toEpochDay() }
                                    val dayClasses = allEntries.filter { it.entry.day == cellDate.dayOfWeek.value }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    dateEvents.isNotEmpty() -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> MaterialTheme.colorScheme.surface
                                                }
                                            )
                                            .border(
                                                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedDate = cellDate }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface
                                            )

                                            if (dateEvents.isNotEmpty()) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.error
                                                ) {
                                                    Text(
                                                        text = "🎯${dateEvents.size}",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onError,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            } else if (dayClasses.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    dayClasses.take(3).forEach { cls ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                    else parseHexColor(cls.subject.colorHex)
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).height(52.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Header & Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedDayEvents.size} exam(s)/deadline(s) • ${selectedDayClasses.size} recurring class(es)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onAddEventOnDate(selectedDate.toEpochDay()) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exam")
                    }

                    Button(
                        onClick = { onJumpToDayView(selectedDate.dayOfWeek.value) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Day View")
                    }
                }
            }
        }

        // Exams on Selected Date
        items(selectedDayEvents, key = { "ev_${it.event.id}" }) { evItem ->
            val daysDiff = ChronoUnit.DAYS.between(LocalDate.now(), selectedDate)
            val badge = when {
                daysDiff < 0 -> "Past"
                daysDiff == 0L -> "Today!"
                daysDiff == 1L -> "Tomorrow"
                else -> "In $daysDiff days"
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEventClick(evItem) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎯 ${evItem.event.eventType}: ${evItem.event.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%02d:00 - %02d:00%s",
                                evItem.event.startHour,
                                evItem.event.startHour + evItem.event.duration,
                                evItem.event.room?.let { " • $it" } ?: ""
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Classes on Selected Date
        items(selectedDayClasses, key = { "cls_${it.entry.id}" }) { clsItem ->
            val isCanceled = SudoSleepRepository.isCanceledForWeek(clsItem.entry, selectedDate)
            val color = parseHexColor(clsItem.subject.colorHex)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEntryClick(clsItem) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = clsItem.subject.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (isCanceled) TextDecoration.LineThrough else TextDecoration.None
                        )
                        val actName = clsItem.requirement.customTypeName ?: clsItem.requirement.activityType
                        Text(
                            text = String.format(
                                Locale.US,
                                "%s • %02d:00 - %02d:00%s",
                                actName,
                                clsItem.entry.startHour,
                                clsItem.entry.startHour + clsItem.entry.duration,
                                clsItem.entry.room?.let { " • $it" } ?: ""
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isCanceled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Canceled",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}
