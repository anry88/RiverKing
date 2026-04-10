package com.riverking.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val RiverBackdropBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0B1E26),
        Color(0xFF122A33),
        Color(0xFF08131A),
    )
)

private val RiverDarkScheme = darkColorScheme(
    primary = Color(0xFF43D17C),
    onPrimary = Color(0xFF062313),
    secondary = Color(0xFFF4C857),
    onSecondary = Color(0xFF241B02),
    tertiary = Color(0xFF63C8FF),
    background = Color(0xFF08131A),
    onBackground = Color(0xFFF3F7F8),
    surface = Color(0xFF10212A),
    onSurface = Color(0xFFF3F7F8),
    surfaceVariant = Color(0xFF173341),
    onSurfaceVariant = Color(0xFFB6C9D1),
    error = Color(0xFFFF7B7B),
)

@Composable
fun RiverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RiverDarkScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
