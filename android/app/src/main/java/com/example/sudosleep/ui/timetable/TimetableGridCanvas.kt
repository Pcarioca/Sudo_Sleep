package com.example.sudosleep.ui.timetable

import java.util.Locale
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.ui.subject.parseHexColor

@Composable
fun TimetableGridCanvas(
    entries: List<TimetableEntryWithSubject>,
    onEntryClick: (TimetableEntryWithSubject) -> Unit,
    modifier: Modifier = Modifier,
    hourHeight: Dp = 68.dp,
    startHour: Int = 8,
    endHour: Int = 22
) {
    val totalHours = endHour - startHour
    val totalHeight = hourHeight * totalHours
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Grid lines and time labels
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            for (h in startHour until endHour) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeight),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d:00", h),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(52.dp)
                            .padding(top = 2.dp, start = 4.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Event Blocks Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .padding(start = 56.dp, end = 8.dp)
        ) {
            entries.forEach { item ->
                val entry = item.entry
                val subject = item.subject
                val req = item.requirement

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
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = themeColor.copy(alpha = 0.92f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = activityTypeName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = timeSpanText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        if (blockHeight >= 100.dp) {
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!entry.room.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = entry.room ?: "",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                }
                            }

                            if (!entry.teacher.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = entry.teacher ?: "",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
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
