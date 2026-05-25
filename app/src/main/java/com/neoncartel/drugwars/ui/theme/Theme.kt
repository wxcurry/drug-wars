package com.neoncartel.drugwars.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeonColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF28E6FF),
    onPrimary = Color(0xFF001015),
    secondary = Color(0xFFFF3FB4),
    onSecondary = Color(0xFF190010),
    tertiary = Color(0xFFFDE047),
    background = Color(0xFF05060A),
    onBackground = Color(0xFFEFF6FF),
    surface = Color(0xFF101827),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF172033),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF240000),
)

@Composable
fun DrugWarsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
