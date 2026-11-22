package ru.nstu.lyceum.schedule.domain

import java.util.Calendar

data class LessonBell(
    val num: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    val startMinutes: Int get() = startHour * 60 + startMinute
    val endMinutes: Int get() = endHour * 60 + endMinute
    val timeRange: String get() = String.format("%02d:%02d–%02d:%02d", startHour, startMinute, endHour, endMinute)
}

sealed class BellState {
    data class BeforeSchool(val firstLessonStart: String, val minutesUntil: Int) : BellState()
    data class InLesson(val num: Int, val timeRange: String, val minutesLeft: Int, val progress: Float) : BellState()
    data class Break(val nextNum: Int, val minutesLeft: Int, val nextStart: String, val progress: Float = 0f) : BellState()
    object AfterSchool : BellState()
    object Weekend : BellState()
    object Vacation : BellState()

    val displayText: String
        get() = when (this) {
            is BeforeSchool -> "До 1 урока: $minutesUntil мин"
            is InLesson -> "$num урок (до звонка: $minutesLeft мин)"
            is Break -> "Перемена до $nextNum урока (осталось: $minutesLeft мин)"
            is AfterSchool -> "Уроки на сегодня завершены"
            is Weekend -> "Занятий нет"
            is Vacation -> "Занятий нет"
        }

    fun getDisplayText(isPair: Boolean): String = when (this) {
        is BeforeSchool -> if (isPair) "До 1 пары: $minutesUntil мин" else "До 1 урока: $minutesUntil мин"
        is InLesson -> if (isPair) "$num пара (до звонка: $minutesLeft мин)" else "$num урок (до звонка: $minutesLeft мин)"
        is Break -> if (isPair) "Перемена до $nextNum пары (осталось: $minutesLeft мин)" else "Перемена до $nextNum урока (осталось: $minutesLeft мин)"
        is AfterSchool -> if (isPair) "Пары на сегодня завершены" else "Уроки на сегодня завершены"
        is Weekend -> "Занятий нет"
        is Vacation -> "Занятий нет"
    }

    val widgetText: String
        get() = when (this) {
            is BeforeSchool -> "$minutesUntil мин"
            is InLesson -> "$minutesLeft мин"
            is Break -> "$minutesLeft мин"
            is AfterSchool -> "Завершено"
            is Weekend -> "Занятий нет"
            is Vacation -> "Занятий нет"
        }

    val compactText: String
        get() = when (this) {
            is BeforeSchool -> "До уроков $minutesUntil м."
            is InLesson -> "Урок $num ($minutesLeft м.)"
            is Break -> "Перемена ($minutesLeft м.)"
            is AfterSchool -> "Конец уроков"
            is Weekend -> "Занятий нет"
            is Vacation -> "Занятий нет"
        }

    val activeLessonNum: Int?
        get() = (this as? InLesson)?.num

    val currentProgress: Float?
        get() = when (this) {
            is InLesson -> progress
            is Break -> progress
            else -> null
        }
}

object BellSchedule {

    val BELLS = listOf(
        LessonBell(1, 8, 15, 9, 0),
        LessonBell(2, 9, 10, 9, 55),
        LessonBell(3, 10, 15, 11, 0),
        LessonBell(4, 11, 15, 12, 0),
        LessonBell(5, 12, 10, 12, 55),
        LessonBell(6, 13, 5, 13, 50),
        LessonBell(7, 14, 0, 14, 40),
        LessonBell(8, 14, 55, 15, 35),
        LessonBell(9, 15, 50, 16, 30),
        LessonBell(10, 16, 40, 17, 20),
        LessonBell(11, 17, 30, 18, 10),
        LessonBell(12, 18, 20, 19, 0)
    )

    fun getCurrentBellState(cal: Calendar = Calendar.getInstance(), isVacation: Boolean = false): BellState {
        if (isVacation) {
            return BellState.Vacation
        }

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) {
            return BellState.Weekend
        }

        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val isSaturday = dayOfWeek == Calendar.SATURDAY
        val activeBells = if (isSaturday) BELLS.take(8) else BELLS

        val firstBell = activeBells.first()
        if (currentMinutes < firstBell.startMinutes) {
            return BellState.BeforeSchool(
                firstLessonStart = String.format("%02d:%02d", firstBell.startHour, firstBell.startMinute),
                minutesUntil = firstBell.startMinutes - currentMinutes
            )
        }

        val lastBell = activeBells.last()
        if (currentMinutes >= lastBell.endMinutes) {
            return BellState.AfterSchool
        }

        for (i in activeBells.indices) {
            val bell = activeBells[i]
            if (currentMinutes in bell.startMinutes until bell.endMinutes) {
                val totalDuration = bell.endMinutes - bell.startMinutes
                val elapsed = currentMinutes - bell.startMinutes
                val progress = elapsed.toFloat() / totalDuration.toFloat()
                return BellState.InLesson(
                    num = bell.num,
                    timeRange = bell.timeRange,
                    minutesLeft = bell.endMinutes - currentMinutes,
                    progress = progress.coerceIn(0f, 1f)
                )
            }

            if (i < activeBells.size - 1) {
                val nextBell = activeBells[i + 1]
                if (currentMinutes in bell.endMinutes until nextBell.startMinutes) {
                    val breakDuration = nextBell.startMinutes - bell.endMinutes
                    val breakElapsed = currentMinutes - bell.endMinutes
                    val breakProgress = if (breakDuration > 0) (breakElapsed.toFloat() / breakDuration.toFloat()).coerceIn(0f, 1f) else 0f
                    return BellState.Break(
                        nextNum = nextBell.num,
                        minutesLeft = nextBell.startMinutes - currentMinutes,
                        nextStart = String.format("%02d:%02d", nextBell.startHour, nextBell.startMinute),
                        progress = breakProgress
                    )
                }
            }
        }

        return BellState.AfterSchool
    }

    /**
     * Converts Calendar DAY_OF_WEEK to Russian school schedule index:
     * Monday = 0, Tuesday = 1, Wednesday = 2, Thursday = 3, Friday = 4, Saturday = 5, Sunday = 6
     */
    fun getScheduleDayIndex(cal: Calendar = Calendar.getInstance()): Int {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    /**
     * Returns the default day index (0..5, Mon..Sat) to display:
     * On Saturday evening (>= 15:00) or Sunday, defaults to Monday (0) for the upcoming week.
     * On Monday..Friday or Saturday daytime (< 15:00), defaults to the current day (0..5).
     */
    fun getDefaultScheduleDayIndex(cal: Calendar = Calendar.getInstance()): Int {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (dayOfWeek == Calendar.SUNDAY) {
            return 0 // Monday
        }
        if (dayOfWeek == Calendar.SATURDAY && hour >= 15) {
            return 0 // Monday for upcoming week
        }

        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val isMorningOrNight = currentMinutes < 8 * 60 + 15

        val bellState = getCurrentBellState(cal)
        val isAfterSchool = !isMorningOrNight && (bellState is BellState.AfterSchool)
        val dayIdx = getScheduleDayIndex(cal)

        if (isAfterSchool) {
            // Lessons ended today, default to next school day
            return when (dayIdx) {
                0 -> 1 // Mon -> Tue
                1 -> 2 // Tue -> Wed
                2 -> 3 // Wed -> Thu
                3 -> 4 // Thu -> Fri
                4 -> 5 // Fri -> Sat
                5 -> 0 // Sat -> Mon
                else -> 0
            }
        }

        return dayIdx.coerceIn(0, 5)
    }

    /**
     * Checks if the given schedule's weekStart corresponds to the current calendar week.
     */
    fun isCurrentScheduleWeek(weekStartStr: String?, cal: Calendar = Calendar.getInstance()): Boolean {
        if (weekStartStr.isNullOrBlank()) return true
        return try {
            val c = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
            val dayOfWeek = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mon..6=Sun
            c.add(Calendar.DAY_OF_MONTH, -dayOfWeek)
            val curMonday = String.format(
                java.util.Locale.US,
                "%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)
            )
            weekStartStr == curMonday
        } catch (_: Exception) {
            true
        }
    }
}
