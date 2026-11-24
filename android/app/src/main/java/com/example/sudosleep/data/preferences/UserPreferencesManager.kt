package com.example.sudosleep.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppColorTheme(val displayName: String, val emoji: String, val subtitle: String) {
    PASTEL_DREAM("Pastel Dream", "🌸", "Soft lavender & cotton-candy indigo"),
    MATCHA_MINT("Matcha Mint", "🍵", "Cozy botanical greens & warm cream"),
    SAKURA_BLOSSOM("Sakura Blossom", "🌷", "Sweet cherry pink & rose quartz"),
    OCEAN_BREEZE("Ocean Breeze", "🌊", "Crisp sky blue & coastal teal"),
    SUNSET_PEACH("Sunset Peach", "🍑", "Warm coral, amber & golden hour"),
    DYNAMIC_SYSTEM("Dynamic Material You", "✨", "Adapts to your phone wallpaper")
}

enum class DarkModeConfig(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Always Light ☀️"),
    DARK("Always Cozy Dark 🌙")
}

data class UserPreferencesState(
    val isLoggedIn: Boolean = false,
    val userDisplayName: String = "Sleepy Scholar",
    val userEmail: String = "",
    val authProvider: String = "LOCAL",
    val hasCompletedOnboarding: Boolean = false,
    val globalNotificationsEnabled: Boolean = true,
    val defaultReminderOffsetsCsv: String = "15,60",
    val colorTheme: AppColorTheme = AppColorTheme.PASTEL_DREAM,
    val darkModeConfig: DarkModeConfig = DarkModeConfig.SYSTEM,
    val hourHeightDp: Float = 68f
) {
    fun getDefaultReminderOffsets(): List<Int> {
        if (defaultReminderOffsetsCsv.isBlank()) return emptyList()
        return defaultReminderOffsetsCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .sorted()
    }
}

class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("sudo_sleep_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<UserPreferencesState> = _state.asStateFlow()

    private fun loadState(): UserPreferencesState {
        val themeName = prefs.getString("color_theme", AppColorTheme.PASTEL_DREAM.name)
        val darkName = prefs.getString("dark_mode_config", DarkModeConfig.SYSTEM.name)
        return UserPreferencesState(
            isLoggedIn = prefs.getBoolean("is_logged_in", false),
            userDisplayName = prefs.getString("user_display_name", "Sleepy Scholar") ?: "Sleepy Scholar",
            userEmail = prefs.getString("user_email", "") ?: "",
            authProvider = prefs.getString("auth_provider", "LOCAL") ?: "LOCAL",
            hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", false),
            globalNotificationsEnabled = prefs.getBoolean("global_notifications_enabled", true),
            defaultReminderOffsetsCsv = prefs.getString("default_reminder_offsets", "15,60") ?: "15,60",
            colorTheme = runCatching { AppColorTheme.valueOf(themeName!!) }.getOrDefault(AppColorTheme.PASTEL_DREAM),
            darkModeConfig = runCatching { DarkModeConfig.valueOf(darkName!!) }.getOrDefault(DarkModeConfig.SYSTEM),
            hourHeightDp = prefs.getFloat("hour_height_dp", 68f).coerceIn(38f, 115f)
        )
    }

    fun setLoggedIn(
        displayName: String,
        email: String,
        provider: String,
        hasCompletedOnboarding: Boolean? = null,
        defaultReminderOffsetsCsv: String? = null
    ) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_display_name", displayName)
            putString("user_email", email)
            putString("auth_provider", provider)
            if (hasCompletedOnboarding != null) {
                putBoolean("has_completed_onboarding", hasCompletedOnboarding)
            }
            if (defaultReminderOffsetsCsv != null) {
                putString("default_reminder_offsets", defaultReminderOffsetsCsv)
            }
            apply()
        }
        _state.value = loadState()
    }

    fun updateUserProfile(
        displayName: String,
        email: String? = null,
        provider: String? = null,
        hasCompletedOnboarding: Boolean? = null,
        defaultReminderOffsetsCsv: String? = null
    ) {
        prefs.edit().apply {
            putString("user_display_name", displayName)
            if (email != null) putString("user_email", email)
            if (provider != null) putString("auth_provider", provider)
            if (hasCompletedOnboarding != null) {
                putBoolean("has_completed_onboarding", hasCompletedOnboarding)
            }
            if (defaultReminderOffsetsCsv != null) {
                putString("default_reminder_offsets", defaultReminderOffsetsCsv)
            }
            apply()
        }
        _state.value = loadState()
    }

    fun logOut() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("user_display_name")
            .remove("user_email")
            .remove("auth_provider")
            .remove("has_completed_onboarding")
            .apply()
        _state.value = loadState()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit()
            .putBoolean("has_completed_onboarding", completed)
            .apply()
        _state.value = loadState()
    }

    fun setGlobalNotificationsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean("global_notifications_enabled", enabled)
            .apply()
        _state.value = loadState()
    }

    fun setDefaultReminderOffsets(offsets: List<Int>) {
        val csv = offsets.distinct().sorted().joinToString(",")
        prefs.edit()
            .putString("default_reminder_offsets", csv)
            .apply()
        _state.value = loadState()
    }

    fun setColorTheme(theme: AppColorTheme) {
        prefs.edit()
            .putString("color_theme", theme.name)
            .apply()
        _state.value = loadState()
    }

    fun setDarkModeConfig(config: DarkModeConfig) {
        prefs.edit()
            .putString("dark_mode_config", config.name)
            .apply()
        _state.value = loadState()
    }

    fun setHourHeightDp(heightDp: Float) {
        val clamped = heightDp.coerceIn(38f, 115f)
        prefs.edit()
            .putFloat("hour_height_dp", clamped)
            .apply()
        _state.value = _state.value.copy(hourHeightDp = clamped)
    }
}
