package com.example.sudosleep.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sudosleep.data.local.pojo.SubjectWithDetails
import com.example.sudosleep.ui.subject.SubjectDetailScreen
import com.example.sudosleep.ui.subject.SubjectListScreen
import com.example.sudosleep.ui.timetable.TimetableMainScreen
import com.example.sudosleep.ui.viewmodel.SubjectViewModel
import com.example.sudosleep.ui.viewmodel.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    subjectViewModel: SubjectViewModel,
    timetableViewModel: TimetableViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Timetable, 1 = Subjects
    var activeSubjectDetail by remember { mutableStateOf<SubjectWithDetails?>(null) }

    Scaffold(
        topBar = {
            if (activeSubjectDetail == null) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedTab == 0) "Sudo Sleep Timetable" else "Subjects & Gradebook",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (activeSubjectDetail == null) {
                NavigationBar(
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                if (selectedTab == 0) Icons.Default.Schedule else Icons.Default.CalendarToday,
                                contentDescription = "Timetable"
                            )
                        },
                        label = { Text("Timetable") }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                if (selectedTab == 1) Icons.Default.School else Icons.Default.Book,
                                contentDescription = "Subjects & Grades"
                            )
                        },
                        label = { Text("Subjects & Grades") }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (activeSubjectDetail != null) {
                val details = activeSubjectDetail!!
                val avg = subjectViewModel.calculateAverage(details.grades)

                SubjectDetailScreen(
                    subjectWithDetails = details,
                    calculatedAverage = avg,
                    onBack = { activeSubjectDetail = null },
                    onDeleteSubject = { subjectId ->
                        subjectViewModel.deleteSubject(subjectId)
                        activeSubjectDetail = null
                    },
                    onAddGrade = { valNum, weight ->
                        subjectViewModel.addGrade(details.subject.id, valNum, weight)
                    },
                    onDeleteGrade = { gradeId ->
                        subjectViewModel.deleteGrade(gradeId)
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> {
                        TimetableMainScreen(viewModel = timetableViewModel)
                    }
                    1 -> {
                        SubjectListScreen(
                            viewModel = subjectViewModel,
                            onSelectSubject = { activeSubjectDetail = it }
                        )
                    }
                }
            }
        }
    }
}
