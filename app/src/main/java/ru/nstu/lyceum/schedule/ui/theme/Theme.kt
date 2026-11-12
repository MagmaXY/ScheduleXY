package ru.nstu.lyceum.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NikaSubject,
    onPrimary = Color.White,
    primaryContainer = NikaHeaderStart,
    onPrimaryContainer = NikaSubject,
    secondary = NikaTeacher,
    onSecondary = Color.White,
    secondaryContainer = NikaCalExists,
    onSecondaryContainer = NikaTeacher,
    background = NikaBg,
    onBackground = Color(0xFF1E293B),
    surface = NikaSurface,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = NikaTimeBg,
    onSurfaceVariant = Color(0xFF475569),
    outline = NikaBorder
)

@Composable
fun ScheduleXYTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
