package com.example.sudosleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.sudosleep.data.auth.AuthManager
import com.example.sudosleep.data.auth.EncryptedTokenStore
import com.example.sudosleep.data.auth.FacebookLoginAuthHelper
import com.example.sudosleep.data.auth.GoogleCredentialAuthHelper
import com.example.sudosleep.data.local.AppDatabase
import com.example.sudosleep.data.preferences.UserPreferencesManager
import com.example.sudosleep.data.remote.ApiClient
import com.example.sudosleep.data.repository.SudoSleepRepository
import com.example.sudosleep.notifications.NotificationHelper
import com.example.sudosleep.ui.MainNavigationScreen
import com.example.sudosleep.ui.theme.SudoSleepTheme
import com.example.sudosleep.ui.viewmodel.SubjectViewModel
import com.example.sudosleep.ui.viewmodel.TimetableViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannelsCreated(applicationContext)

        val database = AppDatabase.getInstance(applicationContext)
        val preferencesManager = UserPreferencesManager(applicationContext)
        val tokenStore = EncryptedTokenStore(applicationContext)
        val googleAuthHelper = GoogleCredentialAuthHelper(applicationContext)
        val facebookAuthHelper = FacebookLoginAuthHelper()

        lateinit var authManager: AuthManager
        val apiClient = ApiClient(
            tokenStore = tokenStore,
            baseUrl = BuildConfig.API_BASE_URL,
            onSessionExpired = {
                lifecycleScope.launch {
                    authManager.handleSessionExpired()
                }
            }
        )
        val repository = SudoSleepRepository(
            database = database,
            academicApi = apiClient.academicApi,
            tokenStore = tokenStore
        )
        authManager = AuthManager(
            authApi = apiClient.authApi,
            tokenStore = tokenStore,
            preferencesManager = preferencesManager,
            onClearLocalUserData = {
                val cachedEntries = runCatching { repository.allTimetableEntries.first() }.getOrDefault(emptyList())
                val cachedEvents = runCatching { repository.allAcademicEvents.first() }.getOrDefault(emptyList())
                NotificationHelper.cancelAllAlarms(
                    context = applicationContext,
                    entries = cachedEntries,
                    events = cachedEvents
                )
                repository.clearAllUserData()
            },
            onClearSocialSessions = {
                googleAuthHelper.clearCredentialState()
                facebookAuthHelper.logOut()
            }
        )

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
                    authManager = authManager,
                    googleAuthHelper = googleAuthHelper,
                    facebookAuthHelper = facebookAuthHelper
                )
            }
        }
    }
}
