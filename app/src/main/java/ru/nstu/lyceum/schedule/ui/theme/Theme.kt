package ru.nstu.lyceum.schedule.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository

data class NikaThemeColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderLight: Color,
    val headerStart: Color,
    val headerEnd: Color,
    val subject: Color,
    val teacher: Color,
    val room: Color,
    val group: Color,
    val lsnNum: Color,
    val lsnTime: Color,
    val cancel: Color,
    val cancelBg: Color,
    val cancelBorder: Color,
    val change: Color,
    val changeBg: Color,
    val changeBorder: Color,
    val activeNowBg: Color,
    val activeNowBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val gold: Color,
    val goldBg: Color
)

val LightNikaThemeColors = NikaThemeColors(
    isDark = false,
    bg = Slate50,
    surface = Color.White,
    surfaceVariant = Slate100,
    border = Slate300,
    borderLight = Slate200,
    headerStart = Color.White,
    headerEnd = Slate50,
    subject = Color(0xFF222288),
    teacher = Color(0xFF226666),
    room = Color(0xFF000000),
    group = Color(0xFF0000FF),
    lsnNum = Color(0xFFAAAAAA),
    lsnTime = Color(0xFF008000),
    cancel = Slate500,
    cancelBg = Slate100,
    cancelBorder = Slate200,
    change = Color(0xFFDD2222),
    changeBg = Color(0xFFFFD4D7),
    changeBorder = Color(0xFFFCA5A5),
    activeNowBg = Color(0xFFD7EBFA).copy(alpha = 0.5f),
    activeNowBorder = Color(0xFF008000),
    textPrimary = Slate900,
    textSecondary = Slate600,
    textMuted = Slate400,
    gold = LyceumGold,
    goldBg = LyceumGoldContainer
)

val DarkNikaThemeColors = NikaThemeColors(
    isDark = true,
    bg = DarkOled,
    surface = Color(0xFF111827),
    surfaceVariant = Slate800,
    border = Slate700,
    borderLight = Color(0xFF283548),
    headerStart = Color(0xFF111827),
    headerEnd = DarkOled,
    subject = Color(0xFF9999FF),
    teacher = Color(0xFF5EEAD4),
    room = Color(0xFFF1F5F9),
    group = Color(0xFF8099FF),
    lsnNum = Color(0xFF94A3B8),
    lsnTime = Color(0xFF33CC33),
    cancel = Slate400,
    cancelBg = Color(0xFF141C2B),
    cancelBorder = Slate800,
    change = Color(0xFFF87171),
    changeBg = Color(0xFF450A0A),
    changeBorder = Color(0xFF7F1D1D),
    activeNowBg = Color(0xFF1E265A).copy(alpha = 0.5f),
    activeNowBorder = Color(0xFF33CC33),
    textPrimary = Slate50,
    textSecondary = Slate300,
    textMuted = Slate500,
    gold = LyceumGoldDark,
    goldBg = LyceumGoldContainerDark
)

val LocalNikaColors = staticCompositionLocalOf { LightNikaThemeColors }

val EmeraldLightNikaColors = LightNikaThemeColors.copy(
    subject = Color(0xFF005A3E),
    teacher = Color(0xFF1E6B52),
    lsnTime = Color(0xFF006C4C),
    group = Color(0xFF006C4C),
    gold = Color(0xFF059669),
    goldBg = Color(0xFFCEE9D8),
    activeNowBorder = Color(0xFF006C4C)
)

val EmeraldDarkNikaColors = DarkNikaThemeColors.copy(
    bg = Color(0xFF0E1512),
    surface = Color(0xFF121B17),
    surfaceVariant = Color(0xFF1C2722),
    subject = Color(0xFF6EDBB0),
    teacher = Color(0xFF7DD3B6),
    lsnTime = Color(0xFF5EEAD4),
    group = Color(0xFF6EDBB0),
    gold = Color(0xFF34D399),
    goldBg = Color(0xFF16211C),
    activeNowBorder = Color(0xFF6EDBB0)
)

val OledDarkNikaColors = DarkNikaThemeColors.copy(
    bg = Color(0xFF000000),
    surface = Color(0xFF080808),
    surfaceVariant = Color(0xFF141414),
    border = Color(0xFF27272A),
    borderLight = Color(0xFF18181B),
    headerStart = Color(0xFF080808),
    headerEnd = Color(0xFF000000),
    subject = Color(0xFFFAFAFA),
    teacher = Color(0xFFA1A1AA),
    room = Color(0xFFFFFFFF),
    group = Color(0xFFE4E4E7),
    lsnNum = Color(0xFF71717A),
    lsnTime = Color(0xFFE2E8F0),
    cancel = Color(0xFF71717A),
    cancelBg = Color(0xFF141414),
    cancelBorder = Color(0xFF27272A),
    activeNowBg = Color(0xFF27272A).copy(alpha = 0.5f),
    activeNowBorder = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF71717A),
    gold = Color(0xFFD4D4D8),
    goldBg = Color(0xFF18181B)
)

val OledLightNikaColors = LightNikaThemeColors.copy(
    bg = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF4F4F5),
    border = Color(0xFFE4E4E7),
    borderLight = Color(0xFFF4F4F5),
    subject = Color(0xFF18181B),
    teacher = Color(0xFF3F3F46),
    room = Color(0xFF09090B),
    group = Color(0xFF27272A),
    lsnTime = Color(0xFF18181B),
    gold = Color(0xFF52525B),
    goldBg = Color(0xFFE4E4E7)
)

object NikaTheme {
    val colors: NikaThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNikaColors.current
}

// 1. Classic Lyceum Theme (Сине-золотой Лицей)
private val LightColorScheme = lightColorScheme(
    primary = LyceumBlue,
    onPrimary = Color.White,
    primaryContainer = LyceumBlueContainer,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = LyceumGreen,
    onSecondary = Color.White,
    secondaryContainer = LyceumGreenContainer,
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = LyceumGold,
    onTertiary = Color.White,
    tertiaryContainer = LyceumGoldContainer,
    onTertiaryContainer = Color(0xFF78350F),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate200,
    surfaceContainerHighest = Slate300,
    outline = Slate300,
    outlineVariant = Slate200,
    error = LyceumRed,
    onError = Color.White,
    errorContainer = LyceumRedContainer,
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColorScheme = darkColorScheme(
    primary = LyceumBlueDark,
    onPrimary = DarkOled,
    primaryContainer = LyceumBlueContainerDark,
    onPrimaryContainer = LyceumBlueContainer,
    secondary = LyceumGreenDark,
    onSecondary = DarkOled,
    secondaryContainer = LyceumGreenContainerDark,
    onSecondaryContainer = LyceumGreenContainer,
    tertiary = LyceumGoldDark,
    onTertiary = DarkOled,
    tertiaryContainer = LyceumGoldContainerDark,
    onTertiaryContainer = LyceumGoldContainer,
    background = DarkOled,
    onBackground = Slate50,
    surface = Color(0xFF111827),
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    surfaceContainerLowest = Color(0xFF070A10),
    surfaceContainerLow = Color(0xFF0F172A),
    surfaceContainer = Slate800,
    surfaceContainerHigh = Color(0xFF283548),
    surfaceContainerHighest = Slate700,
    outline = Slate700,
    outlineVariant = Color(0xFF283548),
    error = LyceumRedDark,
    onError = Color(0xFF450A0A),
    errorContainer = LyceumRedContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

// 2. Emerald Theme (Хвойно-изумрудный акцент)
private val EmeraldLightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8BF8C7),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEE9D8),
    onSecondaryContainer = Color(0xFF092015),
    tertiary = Color(0xFF386567),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBEB),
    onTertiaryContainer = Color(0xFF002021),
    background = Color(0xFFF5FBF6),
    onBackground = Color(0xFF171D1A),
    surface = Color.White,
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DF),
    onSurfaceVariant = Color(0xFF3F4945),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEFF5F0),
    surfaceContainer = Color(0xFFE9F0EB),
    surfaceContainerHigh = Color(0xFFE3EAE5),
    surfaceContainerHighest = Color(0xFFDDE4DF),
    outline = Color(0xFF6F7974),
    outlineVariant = Color(0xFFBFC9C3),
    error = LyceumRed,
    onError = Color.White,
    errorContainer = LyceumRedContainer,
    onErrorContainer = Color(0xFF991B1B)
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6EDBB0),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF005238),
    onPrimaryContainer = Color(0xFF8BF8C7),
    secondary = Color(0xFFB3CCBD),
    onSecondary = Color(0xFF1E352A),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCEE9D8),
    tertiary = Color(0xFFA1CED0),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF1E4D4F),
    onTertiaryContainer = Color(0xFFBCEBEB),
    background = Color(0xFF0E1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF121B17),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C3),
    surfaceContainerLowest = Color(0xFF090F0D),
    surfaceContainerLow = Color(0xFF121B17),
    surfaceContainer = Color(0xFF16211C),
    surfaceContainerHigh = Color(0xFF1C2722),
    surfaceContainerHighest = Color(0xFF27322D),
    outline = Color(0xFF89938E),
    outlineVariant = Color(0xFF3F4945),
    error = LyceumRedDark,
    onError = Color(0xFF450A0A),
    errorContainer = LyceumRedContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

// 3. OLED Theme (100% глубокий чёрный ночью / чистый серебристо-графитовый днём)
private val OledDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF27272A),
    onPrimaryContainer = Color(0xFFF4F4F5),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF18181B),
    onSecondaryContainer = Color(0xFFE4E4E7),
    tertiary = Color(0xFFD4D4D8),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF27272A),
    onTertiaryContainer = Color(0xFFE4E4E7),
    background = Color(0xFF000000),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF080808),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF18181B),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0C0C0C),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF18181B),
    surfaceContainerHighest = Color(0xFF222222),
    outline = Color(0xFF3F3F46),
    outlineVariant = Color(0xFF27272A),
    error = LyceumRedDark,
    onError = Color(0xFF450A0A),
    errorContainer = LyceumRedContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

private val OledLightColorScheme = lightColorScheme(
    primary = Color(0xFF18181B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E4E7),
    onPrimaryContainer = Color(0xFF18181B),
    secondary = Color(0xFF52525B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF27272A),
    tertiary = Color(0xFF3F3F46),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE4E4E7),
    onTertiaryContainer = Color(0xFF18181B),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF18181B),
    surface = Color.White,
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F4F5),
    surfaceContainer = Color(0xFFEDEDED),
    surfaceContainerHigh = Color(0xFFE4E4E7),
    surfaceContainerHighest = Color(0xFFD4D4D8),
    outline = Color(0xFFA1A1AA),
    outlineVariant = Color(0xFFE4E4E7),
    error = LyceumRed,
    onError = Color.White,
    errorContainer = LyceumRedContainer,
    onErrorContainer = Color(0xFF991B1B)
)

private fun extractHue(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

private fun boostColor(
    color: Color,
    minSaturation: Float,
    maxSaturation: Float,
    targetValue: Float
): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = (hsv[1] * 1.4f).coerceIn(minSaturation, maxSaturation)
    hsv[2] = targetValue.coerceIn(0.2f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun hsvColor(hue: Float, saturation: Float, value: Float): Color {
    val hsv = floatArrayOf(hue, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun enhanceDynamicDarkColorScheme(base: androidx.compose.material3.ColorScheme): androidx.compose.material3.ColorScheme {
    val primaryHue = extractHue(base.primary)

    // Make the extracted wallpaper color vibrant and crisp (e.g. glowing neon purple or emerald green)
    val vibrantPrimary = boostColor(base.primary, minSaturation = 0.55f, maxSaturation = 0.90f, targetValue = 0.92f)
    val vibrantSecondary = boostColor(base.secondary, minSaturation = 0.40f, maxSaturation = 0.75f, targetValue = 0.88f)
    val vibrantTertiary = boostColor(base.tertiary, minSaturation = 0.45f, maxSaturation = 0.80f, targetValue = 0.90f)

    // Deep ambient surfaces tinted with the wallpaper's primary hue
    val bg = hsvColor(primaryHue, 0.20f, 0.07f)
    val surf = hsvColor(primaryHue, 0.18f, 0.11f)
    val surfLow = hsvColor(primaryHue, 0.19f, 0.09f)
    val surfLowest = hsvColor(primaryHue, 0.22f, 0.05f)
    val surfContainer = hsvColor(primaryHue, 0.18f, 0.14f)
    val surfHigh = hsvColor(primaryHue, 0.20f, 0.18f)
    val surfHighest = hsvColor(primaryHue, 0.22f, 0.23f)
    val surfVar = hsvColor(primaryHue, 0.18f, 0.18f)
    val outlineC = hsvColor(primaryHue, 0.24f, 0.35f)
    val outlineVarC = hsvColor(primaryHue, 0.20f, 0.22f)

    return base.copy(
        primary = vibrantPrimary,
        onPrimary = Color(0xFF0F172A),
        primaryContainer = hsvColor(primaryHue, 0.50f, 0.28f),
        onPrimaryContainer = hsvColor(primaryHue, 0.20f, 0.95f),
        secondary = vibrantSecondary,
        secondaryContainer = hsvColor(primaryHue, 0.35f, 0.25f),
        tertiary = vibrantTertiary,
        tertiaryContainer = hsvColor(primaryHue, 0.40f, 0.26f),
        background = bg,
        onBackground = Color(0xFFF1F5F9),
        surface = surf,
        onSurface = Color(0xFFF1F5F9),
        surfaceVariant = surfVar,
        onSurfaceVariant = Color(0xFFCBD5E1),
        surfaceContainerLowest = surfLowest,
        surfaceContainerLow = surfLow,
        surfaceContainer = surfContainer,
        surfaceContainerHigh = surfHigh,
        surfaceContainerHighest = surfHighest,
        outline = outlineC,
        outlineVariant = outlineVarC,
        error = Color(0xFFFF453A),
        onError = Color(0xFF450A0A),
        errorContainer = Color(0xFF5A081D),
        onErrorContainer = Color(0xFFFFD4DA)
    )
}

private fun enhanceDynamicLightColorScheme(base: androidx.compose.material3.ColorScheme): androidx.compose.material3.ColorScheme {
    val primaryHue = extractHue(base.primary)

    // Deep, saturated wallpaper tone readable on light backgrounds
    val vibrantPrimary = boostColor(base.primary, minSaturation = 0.65f, maxSaturation = 0.90f, targetValue = 0.45f)
    val vibrantSecondary = boostColor(base.secondary, minSaturation = 0.50f, maxSaturation = 0.80f, targetValue = 0.40f)
    val vibrantTertiary = boostColor(base.tertiary, minSaturation = 0.55f, maxSaturation = 0.85f, targetValue = 0.42f)

    // Soft pastel surfaces tinted with the wallpaper hue
    val bg = hsvColor(primaryHue, 0.05f, 0.98f)
    val surf = Color.White
    val surfContainer = hsvColor(primaryHue, 0.08f, 0.95f)
    val surfHigh = hsvColor(primaryHue, 0.12f, 0.91f)
    val surfHighest = hsvColor(primaryHue, 0.15f, 0.86f)
    val surfVar = hsvColor(primaryHue, 0.10f, 0.93f)
    val outlineC = hsvColor(primaryHue, 0.25f, 0.65f)
    val outlineVarC = hsvColor(primaryHue, 0.12f, 0.88f)

    return base.copy(
        primary = vibrantPrimary,
        onPrimary = Color.White,
        primaryContainer = hsvColor(primaryHue, 0.22f, 0.93f),
        onPrimaryContainer = hsvColor(primaryHue, 0.80f, 0.25f),
        secondary = vibrantSecondary,
        secondaryContainer = hsvColor(primaryHue, 0.18f, 0.92f),
        tertiary = vibrantTertiary,
        tertiaryContainer = hsvColor(primaryHue, 0.20f, 0.92f),
        background = bg,
        onBackground = Color(0xFF0F172A),
        surface = surf,
        onSurface = Color(0xFF0F172A),
        surfaceVariant = surfVar,
        onSurfaceVariant = Color(0xFF334155),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = bg,
        surfaceContainer = surfContainer,
        surfaceContainerHigh = surfHigh,
        surfaceContainerHighest = surfHighest,
        outline = outlineC,
        outlineVariant = outlineVarC,
        error = Color(0xFFDC2626),
        onError = Color.White,
        errorContainer = Color(0xFFFFE4E6),
        onErrorContainer = Color(0xFF9F1239)
    )
}

@Composable
fun ScheduleXYTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePreset: String = PreferencesRepository.THEME_CLASSIC,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        themePreset == PreferencesRepository.THEME_EMERALD -> {
            if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
        }
        themePreset == PreferencesRepository.THEME_OLED -> {
            if (darkTheme) OledDarkColorScheme else OledLightColorScheme
        }
        themePreset == PreferencesRepository.THEME_DYNAMIC || dynamicColor -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) enhanceDynamicDarkColorScheme(dynamicDarkColorScheme(context))
                else enhanceDynamicLightColorScheme(dynamicLightColorScheme(context))
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        else -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }

    val nikaColors = when {
        themePreset == PreferencesRepository.THEME_EMERALD -> if (darkTheme) EmeraldDarkNikaColors else EmeraldLightNikaColors
        themePreset == PreferencesRepository.THEME_OLED -> if (darkTheme) OledDarkNikaColors else OledLightNikaColors
        themePreset == PreferencesRepository.THEME_DYNAMIC || dynamicColor -> if (darkTheme) {
            DarkNikaThemeColors.copy(
                bg = colorScheme.background,
                surface = colorScheme.surface,
                surfaceVariant = colorScheme.surfaceVariant,
                border = colorScheme.outline,
                borderLight = colorScheme.outlineVariant,
                subject = colorScheme.primary,
                teacher = colorScheme.secondary,
                group = colorScheme.tertiary,
                lsnTime = Color(0xFF4ADE80),
                change = Color(0xFFFF2A55),
                changeBg = Color(0xFF5A081D),
                changeBorder = Color(0xFFF43F5E),
                cancel = Slate400,
                cancelBg = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                cancelBorder = colorScheme.outline.copy(alpha = 0.35f),
                textPrimary = colorScheme.onSurface,
                textSecondary = colorScheme.onSurfaceVariant
            )
        } else {
            LightNikaThemeColors.copy(
                bg = colorScheme.background,
                surface = colorScheme.surface,
                surfaceVariant = colorScheme.surfaceVariant,
                border = colorScheme.outline,
                borderLight = colorScheme.outlineVariant,
                subject = colorScheme.primary,
                teacher = colorScheme.secondary,
                group = colorScheme.tertiary,
                lsnTime = Color(0xFF16A34A),
                change = Color(0xFFBE123C),
                changeBg = Color(0xFFFFE4E6),
                changeBorder = Color(0xFFFB7185),
                cancel = Slate500,
                cancelBg = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                cancelBorder = colorScheme.outline.copy(alpha = 0.35f),
                textPrimary = colorScheme.onSurface,
                textSecondary = colorScheme.onSurfaceVariant
            )
        }
        else -> if (darkTheme) DarkNikaThemeColors else LightNikaThemeColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalNikaColors provides nikaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
