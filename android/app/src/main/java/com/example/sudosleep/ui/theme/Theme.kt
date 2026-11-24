package com.example.sudosleep.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.sudosleep.data.preferences.AppColorTheme
import com.example.sudosleep.data.preferences.DarkModeConfig

private val PastelDreamDarkScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceContainerHigh = SurfaceContainerHighDark
)

private val PastelDreamLightScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceContainerHigh = SurfaceContainerHighLight
)

private fun buildCustomScheme(theme: AppColorTheme, darkTheme: Boolean): ColorScheme {
    return when (theme) {
        AppColorTheme.PASTEL_DREAM, AppColorTheme.DYNAMIC_SYSTEM -> {
            if (darkTheme) PastelDreamDarkScheme else PastelDreamLightScheme
        }
        AppColorTheme.MATCHA_MINT -> {
            if (darkTheme) {
                PastelDreamDarkScheme.copy(
                    primary = MatchaPrimaryDark,
                    primaryContainer = MatchaContainerDark,
                    onPrimaryContainer = Color(0xFFDCFCE7)
                )
            } else {
                PastelDreamLightScheme.copy(
                    primary = MatchaPrimaryLight,
                    primaryContainer = MatchaContainerLight,
                    onPrimaryContainer = Color(0xFF052E16),
                    secondary = MatchaSecondaryLight,
                    surface = MatchaSurfaceLight
                )
            }
        }
        AppColorTheme.SAKURA_BLOSSOM -> {
            if (darkTheme) {
                PastelDreamDarkScheme.copy(
                    primary = SakuraPrimaryDark,
                    primaryContainer = SakuraContainerDark,
                    onPrimaryContainer = Color(0xFFFCE7F3)
                )
            } else {
                PastelDreamLightScheme.copy(
                    primary = SakuraPrimaryLight,
                    primaryContainer = SakuraContainerLight,
                    onPrimaryContainer = Color(0xFF500724),
                    secondary = SakuraSecondaryLight,
                    surface = SakuraSurfaceLight
                )
            }
        }
        AppColorTheme.OCEAN_BREEZE -> {
            if (darkTheme) {
                PastelDreamDarkScheme.copy(
                    primary = OceanPrimaryDark,
                    primaryContainer = OceanContainerDark,
                    onPrimaryContainer = Color(0xFFE0F2FE)
                )
            } else {
                PastelDreamLightScheme.copy(
                    primary = OceanPrimaryLight,
                    primaryContainer = OceanContainerLight,
                    onPrimaryContainer = Color(0xFF082F49),
                    secondary = OceanSecondaryLight,
                    surface = OceanSurfaceLight
                )
            }
        }
        AppColorTheme.SUNSET_PEACH -> {
            if (darkTheme) {
                PastelDreamDarkScheme.copy(
                    primary = SunsetPrimaryDark,
                    primaryContainer = SunsetContainerDark,
                    onPrimaryContainer = Color(0xFFFFEDD5)
                )
            } else {
                PastelDreamLightScheme.copy(
                    primary = SunsetPrimaryLight,
                    primaryContainer = SunsetContainerLight,
                    onPrimaryContainer = Color(0xFF431407),
                    secondary = SunsetSecondaryLight,
                    surface = SunsetSurfaceLight
                )
            }
        }
    }
}

@Composable
fun SudoSleepTheme(
    colorTheme: AppColorTheme = AppColorTheme.PASTEL_DREAM,
    darkModeConfig: DarkModeConfig = DarkModeConfig.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (darkModeConfig) {
        DarkModeConfig.SYSTEM -> systemDark
        DarkModeConfig.LIGHT -> false
        DarkModeConfig.DARK -> true
    }

    val colorScheme = when {
        colorTheme == AppColorTheme.DYNAMIC_SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> buildCustomScheme(colorTheme, useDark)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
