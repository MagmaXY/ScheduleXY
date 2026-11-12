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
    data class Break(val nextNum: Int, val minutesLeft: Int, val nextStart: String) : BellState()
    object AfterSchool : BellState()
    object Weekend : BellState()

    val displayText: String
        get() = when (this) {
            is BeforeSchool -> "До 1 урока: $minutesUntil мин (нач. в $firstLessonStart)"
            is InLesson -> "Идёт $num урок (до звонка: $minutesLeft мин)"
            is Break -> "Перемена до $nextNum урока (осталось: $minutesLeft мин)"
            is AfterSchool -> "Уроки на сегодня завершены"
            is Weekend -> "Сегодня выходной"
        }

    val compactText: String
        get() = when (this) {
            is BeforeSchool -> "До уроков $minutesUntil м."
            is InLesson -> "Урок $num ($minutesLeft м.)"
            is Break -> "Перемена ($minutesLeft м.)"
            is AfterSchool -> "Конец уроков"
            is Weekend -> "Выходной"
        }

    val activeLessonNum: Int?
        get() = (this as? InLesson)?.num
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
        LessonBell(8, 14, 55, 15, 35)
    )

    fun getCurrentBellState(cal: Calendar = Calendar.getInstance()): BellState {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) {
            return BellState.Weekend
        }

        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val firstBell = BELLS.first()
        if (currentMinutes < firstBell.startMinutes) {
            return BellState.BeforeSchool(
                firstLessonStart = String.format("%02d:%02d", firstBell.startHour, firstBell.startMinute),
                minutesUntil = firstBell.startMinutes - currentMinutes
            )
        }

        val lastBell = BELLS.last()
        if (currentMinutes >= lastBell.endMinutes) {
            return BellState.AfterSchool
        }

        for (i in BELLS.indices) {
            val bell = BELLS[i]
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

            if (i < BELLS.size - 1) {
                val nextBell = BELLS[i + 1]
                if (currentMinutes in bell.endMinutes until nextBell.startMinutes) {
                    return BellState.Break(
                        nextNum = nextBell.num,
                        minutesLeft = nextBell.startMinutes - currentMinutes,
                        nextStart = String.format("%02d:%02d", nextBell.startHour, nextBell.startMinute)
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
}
