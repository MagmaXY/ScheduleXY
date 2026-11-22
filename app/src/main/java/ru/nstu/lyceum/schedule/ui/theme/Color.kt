package ru.nstu.lyceum.schedule.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Lyceum Material Design 3 Minimalist Palette
// Colors: White, Gray, Black, Gold, Blue, Green
// ==========================================

// Neutrals
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)
val DarkOled = Color(0xFF0B0F17)

// Lyceum Blue from m.schedule.html (#222288 / #0000FF / #9999FF for dark)
val LyceumBlue = Color(0xFF222288)           // Nika Subject Deep Blue (#228)
val LyceumBlueDark = Color(0xFF9999FF)       // Bright Soft Blue for dark mode (#99F)
val LyceumBlueContainer = Color(0xFFD7EBFA)  // Nika Exists Light Blue Container
val LyceumBlueContainerDark = Color(0xFF1E265A)

// Lyceum Gold (Tertiary / Highlights / Favorite)
val LyceumGold = Color(0xFFD97706)           // Warm Amber Gold
val LyceumGoldDark = Color(0xFFFBBF24)       // Bright Luminous Gold
val LyceumGoldContainer = Color(0xFFFEF3C7)  // Soft Gold container
val LyceumGoldContainerDark = Color(0xFF78350F)

// Lyceum Green from m.schedule.html (green #008000 / Saturday #009900 / #00CC00 for dark)
val LyceumGreen = Color(0xFF008000)          // Nika Lesson Time & Saturday Green
val LyceumGreenDark = Color(0xFF33CC33)      // Bright Green for dark mode
val LyceumGreenContainer = Color(0xFFD7F5D7) // Soft Green container
val LyceumGreenContainerDark = Color(0xFF0A3D0A)

// Soft Red for Changes & Cancellations
val LyceumRed = Color(0xFFDC2626)
val LyceumRedDark = Color(0xFFF87171)
val LyceumRedContainer = Color(0xFFFEE2E2)
val LyceumRedContainerDark = Color(0xFF450A0A)

// Legacy compatibility aliases with modernized minimal values
val NikaHeaderStart = Color(0xFFFFFFFF)
val NikaHeaderEnd = Color(0xFFF8FAFC)
val NikaTimeBg = Color(0xFFF1F5F9)
val NikaBg = Slate50
val NikaSurface = Color(0xFFFFFFFF)
val NikaBorder = Slate300
val NikaBorderLight = Slate200

// Nika Theme exact matches from https://lyceum.nstu.ru/rasp/m.schedule.html
val NikaLsnNum = Color(0xFFAAAAAA)         // #AAA
val NikaLsnTime = Color(0xFF008000)        // green
val NikaSubject = Color(0xFF222288)        // #228
val NikaTeacher = Color(0xFF226666)        // #266
val NikaRoom = Color(0xFF000000)           // #000
val NikaGroup = Color(0xFF0000FF)          // #00F
val NikaChange = Color(0xFFDD2222)         // #D22
val NikaCancel = Color(0xFFDD2222)         // #D22
val NikaCancelBorder = Slate300
val NikaCancelBg = Slate100
val NikaTimeAccent = Color(0xFFAA281E)      // rgb(170,40,30)

// Calendar specific
val NikaCalExists = Color(0xFFD7EBFA)      // #D7EBFA
val NikaCalTodayBorder = Color(0xFF8888CC) // #88C
val NikaCalChange = Color(0xFFFFD4D7)      // #FFD4D7
val NikaCalMethod = Color(0xFFFECBA5)      // #FECBA5
val NikaSaturday = Color(0xFF009900)       // #090
val NikaSunday = Color(0xFFCC2222)         // #C22

// Category Colors for Lyceum Events
val EventBlue = LyceumBlue
val EventGreen = LyceumGreen
val EventRed = LyceumRed
val EventOrange = LyceumGold
val EventPurple = Color(0xFF7C3AED)
val EventYellow = Color(0xFFEAB308)
val EventCyan = Color(0xFF0891B2)
