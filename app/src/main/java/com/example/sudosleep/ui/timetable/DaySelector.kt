package com.example.sudosleep.ui.timetable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val DAYS_OF_WEEK = listOf(
    1 to "Monday",
    2 to "Tuesday",
    3 to "Wednesday",
    4 to "Thursday",
    5 to "Friday",
    6 to "Saturday",
    7 to "Sunday"
)

val SHORT_DAYS_OF_WEEK = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

@Composable
fun DaySelector(
    selectedDay: Int, // 1 to 7
    onDaySelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = (selectedDay - 1).coerceIn(0, 6),
        edgePadding = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        SHORT_DAYS_OF_WEEK.forEachIndexed { index, shortName ->
            val dayNumber = index + 1
            val isSelected = selectedDay == dayNumber

            Tab(
                selected = isSelected,
                onClick = { onDaySelected(dayNumber) },
                text = {
                    Text(
                        text = shortName,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}
