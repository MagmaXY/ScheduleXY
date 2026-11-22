package ru.nstu.lyceum.schedule.domain

import java.util.Calendar

data class LessonPair(
    val pairNum: Int,             // 1..4 (I, II, III, IV)
    val timeRange: String,        // e.g. "8:30 – 10:00"
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    val romanNum: String
        get() = when (pairNum) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            else -> pairNum.toString()
        }

    val startMinutes: Int get() = startHour * 60 + startMinute
    val endMinutes: Int get() = endHour * 60 + endMinute
}

object PairSchedule {

    val PAIRS = listOf(
        LessonPair(1, "8:30 – 10:00", 8, 30, 10, 0),
        LessonPair(2, "10:15 – 11:45", 10, 15, 11, 45),
        LessonPair(3, "12:10 – 13:40", 12, 10, 13, 40),
        LessonPair(4, "14:00 – 15:30", 14, 0, 15, 30)
    )

    fun getCurrentPairState(cal: Calendar = Calendar.getInstance(), isVacation: Boolean = false): BellState {
        if (isVacation) return BellState.Vacation
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) return BellState.Weekend

        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val activePairs = PAIRS

        val firstPair = activePairs.first()
        if (currentMinutes < firstPair.startMinutes) {
            return BellState.BeforeSchool(
                firstLessonStart = String.format("%02d:%02d", firstPair.startHour, firstPair.startMinute),
                minutesUntil = firstPair.startMinutes - currentMinutes
            )
        }

        val lastPair = activePairs.last()
        if (currentMinutes >= lastPair.endMinutes) {
            return BellState.AfterSchool
        }

        for (i in activePairs.indices) {
            val pair = activePairs[i]
            if (currentMinutes in pair.startMinutes until pair.endMinutes) {
                val totalDuration = pair.endMinutes - pair.startMinutes
                val elapsed = currentMinutes - pair.startMinutes
                val progress = elapsed.toFloat() / totalDuration.toFloat()
                return BellState.InLesson(
                    num = pair.pairNum,
                    timeRange = pair.timeRange,
                    minutesLeft = pair.endMinutes - currentMinutes,
                    progress = progress.coerceIn(0f, 1f)
                )
            }

            if (i < activePairs.size - 1) {
                val nextPair = activePairs[i + 1]
                if (currentMinutes in pair.endMinutes until nextPair.startMinutes) {
                    val breakDuration = nextPair.startMinutes - pair.endMinutes
                    val breakElapsed = currentMinutes - pair.endMinutes
                    val breakProgress = if (breakDuration > 0) (breakElapsed.toFloat() / breakDuration.toFloat()).coerceIn(0f, 1f) else 0f
                    return BellState.Break(
                        nextNum = nextPair.pairNum,
                        minutesLeft = nextPair.startMinutes - currentMinutes,
                        nextStart = String.format("%02d:%02d", nextPair.startHour, nextPair.startMinute),
                        progress = breakProgress
                    )
                }
            }
        }

        return BellState.AfterSchool
    }

    /**
     * Checks if a target is a senior class (9*, 10-*, 11-*).
     */
    fun isSeniorTarget(targetType: String, targetName: String): Boolean {
        if (targetType != "class") return false
        val clean = targetName.trim()
        return clean.startsWith("9") || clean.startsWith("10") || clean.startsWith("11")
    }

    /**
     * Pair time range string for 1st, 2nd, 3rd, 4th pair.
     */
    fun getPairTimeRange(pairIndex: Int): String {
        return PAIRS.getOrNull(pairIndex - 1)?.timeRange ?: ""
    }

    fun getPairRomanNum(pairIndex: Int): String {
        return PAIRS.getOrNull(pairIndex - 1)?.romanNum ?: pairIndex.toString()
    }

    /**
     * Splits a pair into two equal 45-minute lesson segments.
     * Returns: (start1 to end1) to (start2 to end2).
     */
    fun getSplitLessonTimes(pairIndex: Int, pairTimes: String): Pair<Pair<String, String>, Pair<String, String>> {
        val pair = PAIRS.getOrNull(pairIndex - 1)
        val startMinutes = if (pair != null) {
            pair.startMinutes
        } else {
            val parts = pairTimes.split("–", "-")
            val s = parts.getOrNull(0)?.trim() ?: "8:30"
            val sParts = s.split(":")
            val sh = sParts.getOrNull(0)?.toIntOrNull() ?: 8
            val sm = sParts.getOrNull(1)?.toIntOrNull() ?: 30
            sh * 60 + sm
        }

        val l1Start = startMinutes
        val l1End = startMinutes + 45
        val l2Start = l1End
        val l2End = startMinutes + 90

        fun formatMin(min: Int): String {
            val h = min / 60
            val m = min % 60
            return String.format("%d:%02d", h, m)
        }

        return Pair(
            formatMin(l1Start) to formatMin(l1End),
            formatMin(l2Start) to formatMin(l2End)
        )
    }

    /**
     * Minute ranges for split lessons: (start1 to end1) to (start2 to end2).
     */
    fun getSplitLessonMinutes(pairIndex: Int, pairTimes: String): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        val pair = PAIRS.getOrNull(pairIndex - 1)
        val startMinutes = if (pair != null) {
            pair.startMinutes
        } else {
            val parts = pairTimes.split("–", "-")
            val s = parts.getOrNull(0)?.trim() ?: "8:30"
            val sParts = s.split(":")
            val sh = sParts.getOrNull(0)?.toIntOrNull() ?: 8
            val sm = sParts.getOrNull(1)?.toIntOrNull() ?: 30
            sh * 60 + sm
        }
        val mid = startMinutes + 45
        val end = startMinutes + 90
        return (startMinutes to mid) to (mid to end)
    }
}
