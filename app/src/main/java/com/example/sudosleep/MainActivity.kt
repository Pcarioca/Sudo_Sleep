package com.example.sudosleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.sudosleep.data.auth.AuthManager
import com.example.sudosleep.data.local.AppDatabase
import com.example.sudosleep.data.preferences.UserPreferencesManager
import com.example.sudosleep.data.repository.SudoSleepRepository
import com.example.sudosleep.notifications.NotificationHelper
import com.example.sudosleep.ui.MainNavigationScreen
import com.example.sudosleep.ui.theme.SudoSleepTheme
import com.example.sudosleep.ui.viewmodel.SubjectViewModel
import com.example.sudosleep.ui.viewmodel.TimetableViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannelsCreated(applicationContext)

        val database = AppDatabase.getInstance(applicationContext)
        val repository = SudoSleepRepository(database)
        val preferencesManager = UserPreferencesManager(applicationContext)
        val authManager = AuthManager(preferencesManager)

        val subjectViewModel: SubjectViewModel by viewModels {
            SubjectViewModel.Factory(repository)
        }

        val timetableViewModel: TimetableViewModel by viewModels {
            TimetableViewModel.Factory(repository)
        }

        setContent {
            val prefs by preferencesManager.state.collectAsState()

            SudoSleepTheme(
                colorTheme = prefs.colorTheme,
                darkModeConfig = prefs.darkModeConfig
            ) {
                MainNavigationScreen(
                    subjectViewModel = subjectViewModel,
                    timetableViewModel = timetableViewModel,
                    preferencesManager = preferencesManager,
                    authManager = authManager
                )
            }
        }
    }
}
