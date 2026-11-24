package com.example.sudosleep.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sudosleep.data.auth.AuthManager
import com.example.sudosleep.data.auth.FacebookLoginAuthHelper
import com.example.sudosleep.data.auth.GoogleCredentialAuthHelper
import com.example.sudosleep.data.local.pojo.SubjectWithDetails
import com.example.sudosleep.data.preferences.UserPreferencesManager
import com.example.sudosleep.notifications.NotificationHelper
import com.example.sudosleep.ui.auth.LoginScreen
import com.example.sudosleep.ui.exams.ExamsAndDeadlinesScreen
import com.example.sudosleep.ui.export.TimetableExportScreen
import com.example.sudosleep.ui.onboarding.OnboardingCoachMarkOverlay
import com.example.sudosleep.ui.settings.SettingsAndNotificationsScreen
import com.example.sudosleep.ui.subject.SubjectDetailScreen
import com.example.sudosleep.ui.subject.SubjectListScreen
import com.example.sudosleep.ui.timetable.TimetableMainScreen
import com.example.sudosleep.ui.viewmodel.CalendarViewMode
import com.example.sudosleep.ui.viewmodel.SubjectViewModel
import com.example.sudosleep.ui.viewmodel.TimetableViewModel
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppDestination(val title: String) {
    TIMETABLE("Timetable"),
    SUBJECTS("Subjects & Gradebook"),
    EXAMS("Exams & Deadlines"),
    EXPORT("Wallpaper & PDF Export"),
    SETTINGS("Reminders & Cute Themes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    subjectViewModel: SubjectViewModel,
    timetableViewModel: TimetableViewModel,
    preferencesManager: UserPreferencesManager,
    authManager: AuthManager,
    googleAuthHelper: GoogleCredentialAuthHelper,
    facebookAuthHelper: FacebookLoginAuthHelper
) {
    val prefs by preferencesManager.state.collectAsState()
    val allEntries by timetableViewModel.allTimetableEntries.collectAsState()
    val allEvents by timetableViewModel.allAcademicEvents.collectAsState()
    val allSubjectsWithDetails by subjectViewModel.allSubjectsWithDetails.collectAsState()
    val currentViewMode by timetableViewModel.viewMode.collectAsState()
    val isSyncing by timetableViewModel.isSyncing.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDestination by remember { mutableStateOf(AppDestination.TIMETABLE) }
    var activeSubjectDetail by remember { mutableStateOf<SubjectWithDetails?>(null) }
    var lastBackPressMillis by remember { mutableLongStateOf(0L) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Validate persisted session on launch AND pull latest user data whenever authenticated
    LaunchedEffect(prefs.isLoggedIn) {
        if (prefs.isLoggedIn) {
            val valid = authManager.restoreSessionIfAvailable()
            if (valid) {
                timetableViewModel.refreshFromBackend()
            }
        }
    }

    // Surface background sync errors / offline notices cleanly via Snackbar
    LaunchedEffect(Unit) {
        timetableViewModel.syncMessages.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Keep activeSubjectDetail reactive when grades/details change in Room
    val resolvedSubjectDetail = remember(activeSubjectDetail, allSubjectsWithDetails) {
        activeSubjectDetail?.let { current ->
            allSubjectsWithDetails.firstOrNull { it.subject.id == current.subject.id } ?: current
        }
    }

    // Synchronize AlarmManager local notifications only while authenticated
    LaunchedEffect(prefs.isLoggedIn, allEntries, allEvents, prefs.globalNotificationsEnabled) {
        if (prefs.isLoggedIn) {
            NotificationHelper.syncAllAlarms(
                context = context,
                globalEnabled = prefs.globalNotificationsEnabled,
                entries = allEntries,
                events = allEvents
            )
        }
    }

    // 1. Show Login / Sign Up Screen if not authenticated
    if (!prefs.isLoggedIn) {
        LoginScreen(
            authManager = authManager,
            googleAuthHelper = googleAuthHelper,
            facebookAuthHelper = facebookAuthHelper,
            onLoginSuccess = { welcomeNotice ->
                activeSubjectDetail = null
                currentDestination = AppDestination.TIMETABLE
                if (!welcomeNotice.isNullOrBlank()) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = welcomeNotice,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        )
        return
    }

    // 2. Comprehensive System Back Gesture / Back Button Management
    BackHandler(enabled = true) {
        when {
            drawerState.isOpen -> {
                scope.launch { drawerState.close() }
            }
            activeSubjectDetail != null -> {
                activeSubjectDetail = null
            }
            currentDestination != AppDestination.TIMETABLE -> {
                currentDestination = AppDestination.TIMETABLE
            }
            currentViewMode != CalendarViewMode.DAY -> {
                timetableViewModel.setViewMode(CalendarViewMode.DAY)
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressMillis < 2200L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressMillis = now
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Press back again to exit Sudo Sleep 💤",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    val overallGpa = subjectViewModel.calculateOverallGpa()

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(310.dp),
                    drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        // Drawer Header with User Profile (tap to edit display name), GPA Badge & Active Theme
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                )
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    showEditProfileDialog = true
                                }
                                .padding(horizontal = 20.dp, vertical = 28.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Bedtime,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prefs.userDisplayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = prefs.userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            showEditProfileDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit display name",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    ) {
                                        Text(
                                            text = if (overallGpa > 0.0) String.format(Locale.US, "🎓 GPA %.2f", overallGpa)
                                            else "🎓 Sudo Scholar",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    ) {
                                        Text(
                                            text = "${prefs.colorTheme.emoji} ${prefs.colorTheme.displayName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Drawer Items
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            label = { Text("Timetable (Day / Week / Month)") },
                            selected = currentDestination == AppDestination.TIMETABLE && activeSubjectDetail == null,
                            onClick = {
                                activeSubjectDetail = null
                                currentDestination = AppDestination.TIMETABLE
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.School, contentDescription = null) },
                            label = { Text("Subjects & Gradebook") },
                            selected = currentDestination == AppDestination.SUBJECTS || activeSubjectDetail != null,
                            onClick = {
                                activeSubjectDetail = null
                                currentDestination = AppDestination.SUBJECTS
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.EventAvailable, contentDescription = null) },
                            label = { Text("Exams & Deadlines") },
                            badge = {
                                if (allEvents.isNotEmpty()) {
                                    Badge { Text(allEvents.size.toString()) }
                                }
                            },
                            selected = currentDestination == AppDestination.EXAMS,
                            onClick = {
                                activeSubjectDetail = null
                                currentDestination = AppDestination.EXAMS
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                            label = { Text("Export Wallpaper / PDF") },
                            selected = currentDestination == AppDestination.EXPORT,
                            onClick = {
                                activeSubjectDetail = null
                                currentDestination = AppDestination.EXPORT
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                            label = { Text("Notifications & Cute Themes") },
                            selected = currentDestination == AppDestination.SETTINGS,
                            onClick = {
                                activeSubjectDetail = null
                                currentDestination = AppDestination.SETTINGS
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Sync, contentDescription = null) },
                            label = { Text(if (isSyncing) "Syncing..." else "Sync Schedule Now") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                timetableViewModel.refreshFromBackend(showSuccessMessage = true)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
                            label = { Text("Interactive Tutorial") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                preferencesManager.setOnboardingCompleted(false)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            label = {
                                Text(
                                    "Log Out",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    NotificationHelper.cancelAllAlarms(
                                        context = context,
                                        entries = allEntries,
                                        events = allEvents
                                    )
                                    activeSubjectDetail = null
                                    currentDestination = AppDestination.TIMETABLE
                                    authManager.logout()
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (resolvedSubjectDetail == null) {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Navigation Drawer")
                                }
                            },
                            title = {
                                Text(
                                    text = currentDestination.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        activeSubjectDetail = null
                                        currentDestination = AppDestination.EXPORT
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wallpaper,
                                        contentDescription = "Export Wallpaper or PDF"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        activeSubjectDetail = null
                                        currentDestination = AppDestination.SETTINGS
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Themes & Reminders"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                bottomBar = {
                    if (resolvedSubjectDetail == null) {
                        NavigationBar(
                            modifier = Modifier.clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.TIMETABLE,
                                onClick = { currentDestination = AppDestination.TIMETABLE },
                                icon = {
                                    Icon(
                                        if (currentDestination == AppDestination.TIMETABLE) Icons.Default.Schedule
                                        else Icons.Default.CalendarToday,
                                        contentDescription = "Timetable"
                                    )
                                },
                                label = { Text("Timetable") }
                            )

                            NavigationBarItem(
                                selected = currentDestination == AppDestination.SUBJECTS,
                                onClick = { currentDestination = AppDestination.SUBJECTS },
                                icon = {
                                    Icon(
                                        if (currentDestination == AppDestination.SUBJECTS) Icons.Default.School
                                        else Icons.Default.Book,
                                        contentDescription = "Subjects"
                                    )
                                },
                                label = { Text("Subjects") }
                            )

                            NavigationBarItem(
                                selected = currentDestination == AppDestination.EXAMS,
                                onClick = { currentDestination = AppDestination.EXAMS },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (allEvents.isNotEmpty()) {
                                                Badge { Text(allEvents.size.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = "Exams & Deadlines"
                                        )
                                    }
                                },
                                label = { Text("Exams") }
                            )

                            NavigationBarItem(
                                selected = currentDestination == AppDestination.EXPORT,
                                onClick = { currentDestination = AppDestination.EXPORT },
                                icon = {
                                    Icon(
                                        Icons.Default.Wallpaper,
                                        contentDescription = "Export Wallpaper & PDF"
                                    )
                                },
                                label = { Text("Export") }
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
                    if (resolvedSubjectDetail != null) {
                        val avg = subjectViewModel.calculateAverage(resolvedSubjectDetail.grades)
                        SubjectDetailScreen(
                            subjectWithDetails = resolvedSubjectDetail,
                            calculatedAverage = avg,
                            onBack = { activeSubjectDetail = null },
                            onDeleteSubject = { subjectId ->
                                subjectViewModel.deleteSubject(subjectId)
                                activeSubjectDetail = null
                            },
                            onAddGrade = { valNum, weight ->
                                subjectViewModel.addGrade(resolvedSubjectDetail.subject.id, valNum, weight)
                            },
                            onDeleteGrade = { gradeId ->
                                subjectViewModel.deleteGrade(gradeId)
                            }
                        )
                    } else {
                        when (currentDestination) {
                            AppDestination.TIMETABLE -> {
                                TimetableMainScreen(
                                    viewModel = timetableViewModel,
                                    preferencesManager = preferencesManager
                                )
                            }
                            AppDestination.SUBJECTS -> {
                                SubjectListScreen(
                                    viewModel = subjectViewModel,
                                    onSelectSubject = { activeSubjectDetail = it }
                                )
                            }
                            AppDestination.EXAMS -> {
                                ExamsAndDeadlinesScreen(
                                    viewModel = timetableViewModel,
                                    defaultReminderOffsetsCsv = prefs.defaultReminderOffsetsCsv
                                )
                            }
                            AppDestination.EXPORT -> {
                                TimetableExportScreen(
                                    entries = allEntries,
                                    studentName = prefs.userDisplayName,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                            AppDestination.SETTINGS -> {
                                SettingsAndNotificationsScreen(
                                    preferencesManager = preferencesManager,
                                    onReplayTutorial = {
                                        preferencesManager.setOnboardingCompleted(false)
                                        currentDestination = AppDestination.TIMETABLE
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSyncing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // 3. First-Install Interactive Coach-Marks Tutorial Overlay
        if (!prefs.hasCompletedOnboarding) {
            OnboardingCoachMarkOverlay(
                onComplete = {
                    preferencesManager.setOnboardingCompleted(true)
                    scope.launch {
                        authManager.syncProfilePreferences(hasCompletedOnboarding = true)
                    }
                }
            )
        }
    }

    if (showEditProfileDialog) {
        EditDisplayNameDialog(
            currentName = prefs.userDisplayName,
            email = prefs.userEmail,
            authManager = authManager,
            onDismiss = { showEditProfileDialog = false },
            onSaved = { updatedName ->
                showEditProfileDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Display name updated to $updatedName ✨")
                }
            }
        )
    }
}

@Composable
private fun EditDisplayNameDialog(
    currentName: String,
    email: String,
    authManager: AuthManager,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf(currentName) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = "Edit Profile", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        errorMessage = null
                    },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    enabled = !isSaving,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        val result = authManager.updateDisplayName(displayName)
                        isSaving = false
                        if (result.success) {
                            onSaved(result.displayName)
                        } else {
                            errorMessage = result.errorMessage
                        }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    )
}
