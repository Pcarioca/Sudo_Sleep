package com.example.sudosleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.sudosleep.data.local.AppDatabase
import com.example.sudosleep.data.repository.SudoSleepRepository
import com.example.sudosleep.ui.MainNavigationScreen
import com.example.sudosleep.ui.theme.SudoSleepTheme
import com.example.sudosleep.ui.viewmodel.SubjectViewModel
import com.example.sudosleep.ui.viewmodel.TimetableViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = SudoSleepRepository(database)

        val subjectViewModel: SubjectViewModel by viewModels {
            SubjectViewModel.Factory(repository)
        }

        val timetableViewModel: TimetableViewModel by viewModels {
            TimetableViewModel.Factory(repository)
        }

        setContent {
            SudoSleepTheme {
                MainNavigationScreen(
                    subjectViewModel = subjectViewModel,
                    timetableViewModel = timetableViewModel
                )
            }
        }
    }
}
