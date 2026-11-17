package com.example.sudosleep.ui.subject

import androidx.compose.ui.graphics.Color

data class ColorOption(
    val name: String,
    val hex: String,
    val color: Color
)

val SUBJECT_COLOR_OPTIONS = listOf(
    ColorOption("Sky Berry", "#0288D1", Color(0xFF0288D1)),
    ColorOption("LavenderGrape", "#7B1FA2", Color(0xFF7B1FA2)),
    ColorOption("Tangerine Pop", "#E65100", Color(0xFFE65100)),
    ColorOption("Minty Teal", "#00897B", Color(0xFF00897B)),
    ColorOption("Sakura Rose", "#D81B60", Color(0xFFD81B60)),
    ColorOption("Honey Amber", "#F57F17", Color(0xFFF57F17)),
    ColorOption("Periwinkle Indigo", "#3949AB", Color(0xFF3949AB)),
    ColorOption("Matcha Leaf", "#43A047", Color(0xFF43A047)),
    ColorOption("Coral Crimson", "#C62828", Color(0xFFC62828)),
    ColorOption("Cozy Slate", "#546E7A", Color(0xFF546E7A)),
    ColorOption("Lilac Dream", "#8E24AA", Color(0xFF8E24AA)),
    ColorOption("Ocean Cobalt", "#1E88E5", Color(0xFF1E88E5))
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
