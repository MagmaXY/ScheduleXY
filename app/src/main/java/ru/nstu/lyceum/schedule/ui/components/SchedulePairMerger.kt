package ru.nstu.lyceum.schedule.ui.components

import ru.nstu.lyceum.schedule.data.api.Lesson
import ru.nstu.lyceum.schedule.domain.PairSchedule

sealed class ScheduleItemUi {
    data class SingleLesson(val lesson: Lesson) : ScheduleItemUi()
    data class CombinedPair(
        val pairNum: Int,
        val pairRoman: String,
        val pairTimes: String,
        val firstLesson: Lesson,
        val secondLesson: Lesson
    ) : ScheduleItemUi()
}

object SchedulePairMerger {

    /**
     * Builds list of display items (single lessons or combined pairs).
     *
     * Pairs form for lessons (1,2) -> Pair I, (3,4) -> Pair II, (5,6) -> Pair III, (7,8) -> Pair IV.
     * Criteria for pairing:
     * - `showPairsSetting` is TRUE
     * - Target is senior class (9*, 10-*, 11-*) OR (target is teacher and both lessons have the same senior class)
     */
    fun processLessons(
        lessons: List<Lesson>,
        isTeacherMode: Boolean,
        targetName: String,
        showPairsSetting: Boolean
    ): List<ScheduleItemUi> {
        if (!showPairsSetting || lessons.isEmpty()) {
            return lessons.map { ScheduleItemUi.SingleLesson(it) }
        }

        val isSeniorClass = !isTeacherMode && (targetName.startsWith("9") || targetName.startsWith("10") || targetName.startsWith("11"))

        val result = mutableListOf<ScheduleItemUi>()
        val byNum = lessons.associateBy { it.num }

        val pairSlots = listOf(
            1 to 2,
            3 to 4,
            5 to 6,
            7 to 8
        )

        val processedNums = mutableSetOf<Int>()

        for ((pIndex, pair) in pairSlots.withIndex()) {
            val num1 = pair.first
            val num2 = pair.second
            val l1 = byNum[num1]
            val l2 = byNum[num2]
            val pairNum = pIndex + 1

            if (l1 != null && l2 != null) {
                val canPair = when {
                    isSeniorClass -> true
                    isTeacherMode -> {
                        // Check if teacher has lessons with the same senior class in both slots
                        val c1 = l1.subgroups.mapNotNull { it.className?.trim() }.filter { it.isNotBlank() }.distinct()
                        val c2 = l2.subgroups.mapNotNull { it.className?.trim() }.filter { it.isNotBlank() }.distinct()
                        val common = c1.intersect(c2.toSet())
                        common.isNotEmpty() && common.any { it.startsWith("9") || it.startsWith("10") || it.startsWith("11") }
                    }
                    else -> false
                }

                if (canPair) {
                    val roman = PairSchedule.getPairRomanNum(pairNum)
                    val times = PairSchedule.getPairTimeRange(pairNum)
                    result.add(
                        ScheduleItemUi.CombinedPair(
                            pairNum = pairNum,
                            pairRoman = roman,
                            pairTimes = times,
                            firstLesson = l1,
                            secondLesson = l2
                        )
                    )
                    processedNums.add(num1)
                    processedNums.add(num2)
                }
            }
        }

        // Add remaining lessons that weren't paired
        lessons.forEach { lesson ->
            if (lesson.num !in processedNums) {
                result.add(ScheduleItemUi.SingleLesson(lesson))
            }
        }

        // Return sorted by initial lesson appearance order
        return result.sortedBy { item ->
            when (item) {
                is ScheduleItemUi.SingleLesson -> item.lesson.num
                is ScheduleItemUi.CombinedPair -> item.firstLesson.num
            }
        }
    }
}
