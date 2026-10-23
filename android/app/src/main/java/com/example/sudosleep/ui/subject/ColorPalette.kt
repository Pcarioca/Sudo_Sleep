package com.example.sudosleep.ui.subject

import androidx.compose.ui.graphics.Color

data class ColorOption(
    val name: String,
    val hex: String,
    val color: Color
)

val SUBJECT_COLOR_OPTIONS = listOf(
    ColorOption("Cyan Blue", "#0288D1", Color(0xFF0288D1)),
    ColorOption("Deep Purple", "#7B1FA2", Color(0xFF7B1FA2)),
    ColorOption("Vibrant Orange", "#E65100", Color(0xFFE65100)),
    ColorOption("Teal Green", "#00897B", Color(0xFF00897B)),
    ColorOption("Berry Pink", "#D81B60", Color(0xFFD81B60)),
    ColorOption("Amber Yellow", "#F57F17", Color(0xFFF57F17)),
    ColorOption("Indigo Blue", "#3949AB", Color(0xFF3949AB)),
    ColorOption("Forest Green", "#43A047", Color(0xFF43A047)),
    ColorOption("Crimson Red", "#C62828", Color(0xFFC62828)),
    ColorOption("Slate Gray", "#546E7A", Color(0xFF546E7A))
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF0288D1)): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorLong = if (cleanHex.length == 6) {
            "FF$cleanHex".toLong(16)
        } else {
            cleanHex.toLong(16)
        }
        Color(colorLong)
    } catch (e: Exception) {
        fallback
    }
}
