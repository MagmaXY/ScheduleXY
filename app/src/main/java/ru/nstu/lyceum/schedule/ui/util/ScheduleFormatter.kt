package ru.nstu.lyceum.schedule.ui.util

import ru.nstu.lyceum.schedule.data.api.Lesson
import ru.nstu.lyceum.schedule.data.api.ScheduleChangeItem
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.Subgroup
import ru.nstu.lyceum.schedule.data.api.normalizeSubject
import ru.nstu.lyceum.schedule.data.local.ScheduleNote
import ru.nstu.lyceum.schedule.data.local.SlotType
import ru.nstu.lyceum.schedule.ui.components.ScheduleItemUi
import ru.nstu.lyceum.schedule.ui.components.SchedulePairMerger
import ru.nstu.lyceum.schedule.ui.components.findChangeForSubgroup
import java.util.Calendar

object ScheduleFormatter {

    private val MONTHS_GENITIVE = arrayOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    private val DAY_NAMES = listOf(
        "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"
    )

    private fun resolveMondayCalendar(weekStartStr: String?, fallbackWeekOffset: Int = 0): Calendar {
        val cal = Calendar.getInstance()
        if (!weekStartStr.isNullOrBlank()) {
            try {
                val parts = weekStartStr.split("-")
                if (parts.size == 3) {
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    return cal
                }
            } catch (_: Exception) {}
        }
        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mon..6=Sun
        cal.add(Calendar.DAY_OF_MONTH, -dayOfWeek + (fallbackWeekOffset * 7))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun getDayDateGenitiveLabel(
        weekStartStr: String?,
        dayIndex: Int,
        fallbackWeekOffset: Int = 0
    ): String {
        val cal = resolveMondayCalendar(weekStartStr, fallbackWeekOffset)
        cal.add(Calendar.DAY_OF_MONTH, dayIndex)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH)
        return "$day ${MONTHS_GENITIVE.getOrElse(month) { "" }}"
    }

    fun getWeekRangeGenitiveLabel(
        weekStartStr: String?,
        fallbackWeekOffset: Int = 0
    ): String {
        val startCal = resolveMondayCalendar(weekStartStr, fallbackWeekOffset)
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)
        val startMonth = startCal.get(Calendar.MONTH)

        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 5)
        }
        val endDay = endCal.get(Calendar.DAY_OF_MONTH)
        val endMonth = endCal.get(Calendar.MONTH)

        return if (startMonth == endMonth) {
            "$startDay–$endDay ${MONTHS_GENITIVE.getOrElse(startMonth) { "" }}"
        } else {
            "$startDay ${MONTHS_GENITIVE.getOrElse(startMonth) { "" }} – $endDay ${MONTHS_GENITIVE.getOrElse(endMonth) { "" }}"
        }
    }

    /**
     * Formats lessons for sharing to clipboard with support for pair schedule mode.
     */
    fun formatScheduleForCopy(
        dayTitle: String,
        lessons: List<Lesson>,
        scheduleResponse: ScheduleResponse?,
        dayIndex: Int,
        isTeacherMode: Boolean = false,
        targetName: String = "",
        showPairsSetting: Boolean = false,
        notesForDay: List<ScheduleNote> = emptyList(),
        includeHomework: Boolean = false,
        includeNotes: Boolean = false,
        headerPrefix: String = "📆 "
    ): String {
        val sb = StringBuilder()
        sb.append(headerPrefix).append(dayTitle).append("\n\n")

        if (includeNotes) {
            val beforeNote = notesForDay.firstOrNull { it.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.BEFORE_LESSONS }
            if (beforeNote != null && beforeNote.text.isNotBlank()) {
                sb.append("📌 До уроков: ").append(beforeNote.text).append("\n\n")
            }
        }

        val uiItems = SchedulePairMerger.processLessons(
            lessons = lessons,
            isTeacherMode = isTeacherMode,
            targetName = targetName,
            showPairsSetting = showPairsSetting
        )

        uiItems.forEachIndexed { itemIndex, item ->
            when (item) {
                is ScheduleItemUi.SingleLesson -> {
                    val lNotes = notesForDay.filter {
                        it.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.LESSON && it.lessonNum == item.lesson.num
                    }
                    formatSingleLesson(
                        sb = sb,
                        lesson = item.lesson,
                        scheduleResponse = scheduleResponse,
                        dayIndex = dayIndex,
                        isTeacherMode = isTeacherMode,
                        notes = lNotes,
                        includeHomework = includeHomework,
                        includeNotes = includeNotes
                    )
                }
                is ScheduleItemUi.CombinedPair -> {
                    val pNotes = notesForDay.filter {
                        it.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.LESSON &&
                        (it.lessonNum == item.firstLesson.num || it.lessonNum == item.secondLesson.num)
                    }
                    formatCombinedPair(
                        sb = sb,
                        pairUi = item,
                        scheduleResponse = scheduleResponse,
                        dayIndex = dayIndex,
                        isTeacherMode = isTeacherMode,
                        notes = pNotes,
                        includeHomework = includeHomework,
                        includeNotes = includeNotes
                    )
                }
            }

            if (itemIndex < uiItems.size - 1) {
                sb.append("\n\n")
            }
        }

        if (includeNotes) {
            val afterNote = notesForDay.firstOrNull { it.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.AFTER_LESSONS }
            if (afterNote != null && afterNote.text.isNotBlank()) {
                sb.append("\n\n📌 После уроков: ").append(afterNote.text)
            }
        }

        return sb.toString().trimEnd()
    }

    private fun formatSingleLesson(
        sb: StringBuilder,
        lesson: Lesson,
        scheduleResponse: ScheduleResponse?,
        dayIndex: Int,
        isTeacherMode: Boolean,
        notes: List<ru.nstu.lyceum.schedule.data.local.ScheduleNote> = emptyList(),
        includeHomework: Boolean = false,
        includeNotes: Boolean = false
    ) {
        val changes = scheduleResponse?.getChangesForLesson(dayIndex, lesson.num) ?: emptyList()
        val lessonNumAndTime = "${lesson.num} (${lesson.time}):"
        val indent = " ".repeat(lessonNumAndTime.length + 1)

        if (lesson.subgroups.isEmpty()) {
            sb.append(lessonNumAndTime).append(" —")
        } else {
            val hasMultipleGroups = lesson.subgroups.size > 1

            lesson.subgroups.forEachIndexed { sgIndex, sg ->
                val details = formatSubgroupLine(
                    sg = sg,
                    sgIndex = sgIndex,
                    changes = changes,
                    allSubgroups = lesson.subgroups,
                    defaultSubject = lesson.displaySubject,
                    hasMultipleGroups = hasMultipleGroups,
                    isTeacherMode = isTeacherMode
                )

                if (sgIndex == 0) {
                    sb.append(lessonNumAndTime).append(" ").append(details)
                } else {
                    sb.append("\n").append(indent).append(details)
                }
            }
        }

        if (includeHomework) {
            val hwNotes = notes.filter { it.type == ru.nstu.lyceum.schedule.data.local.NoteType.HOMEWORK }
            hwNotes.forEach { hw ->
                val grpPrefix = if (hw.subgroup != null) "[${hw.subgroup} гр] " else ""
                sb.append("\n").append(indent).append("📝 ДЗ: ").append(grpPrefix).append(hw.text)
            }
        }
        if (includeNotes) {
            val genNotes = notes.filter { it.type == ru.nstu.lyceum.schedule.data.local.NoteType.NOTE }
            genNotes.forEach { n ->
                sb.append("\n").append(indent).append("📌 Заметка: ").append(n.text)
            }
        }
    }

    private fun formatCombinedPair(
        sb: StringBuilder,
        pairUi: ScheduleItemUi.CombinedPair,
        scheduleResponse: ScheduleResponse?,
        dayIndex: Int,
        isTeacherMode: Boolean,
        notes: List<ru.nstu.lyceum.schedule.data.local.ScheduleNote> = emptyList(),
        includeHomework: Boolean = false,
        includeNotes: Boolean = false
    ) {
        val l1 = pairUi.firstLesson
        val l2 = pairUi.secondLesson
        val ch1 = scheduleResponse?.getChangesForLesson(dayIndex, l1.num) ?: emptyList()
        val ch2 = scheduleResponse?.getChangesForLesson(dayIndex, l2.num) ?: emptyList()

        val pairPrefix = "${pairUi.pairRoman} (${pairUi.pairTimes}):"
        val indent = " ".repeat(pairPrefix.length + 1)

        val areIdentical = SchedulePairMerger.areLessonsIdentical(l1, l2, ch1, ch2)

        if (areIdentical) {
            val allChanges = ch1.ifEmpty { ch2 }
            if (l1.subgroups.isEmpty()) {
                sb.append(pairPrefix).append(" —")
            } else {
                val hasMultipleGroups = l1.subgroups.size > 1
                l1.subgroups.forEachIndexed { sgIndex, sg ->
                    val details = formatSubgroupLine(
                        sg = sg,
                        sgIndex = sgIndex,
                        changes = allChanges,
                        allSubgroups = l1.subgroups,
                        defaultSubject = l1.displaySubject,
                        hasMultipleGroups = hasMultipleGroups,
                        isTeacherMode = isTeacherMode
                    )

                    if (sgIndex == 0) {
                        sb.append(pairPrefix).append(" ").append(details)
                    } else {
                        sb.append("\n").append(indent).append(details)
                    }
                }
            }
        } else {
            // Different subjects: section for each lesson with its time, no group labels
            val l1Details = l1.subgroups.mapIndexed { idx, sg ->
                formatSubgroupLine(
                    sg = sg,
                    sgIndex = idx,
                    changes = ch1,
                    allSubgroups = l1.subgroups,
                    defaultSubject = l1.displaySubject,
                    hasMultipleGroups = false,
                    isTeacherMode = isTeacherMode
                )
            }.joinToString(", ")

            val l2Details = l2.subgroups.mapIndexed { idx, sg ->
                formatSubgroupLine(
                    sg = sg,
                    sgIndex = idx,
                    changes = ch2,
                    allSubgroups = l2.subgroups,
                    defaultSubject = l2.displaySubject,
                    hasMultipleGroups = false,
                    isTeacherMode = isTeacherMode
                )
            }.joinToString(", ")

            sb.append(pairPrefix).append(" ").append("(${l1.time}) ").append(l1Details)
            sb.append("\n").append(indent).append("(${l2.time}) ").append(l2Details)
        }

        if (includeHomework) {
            val hwNotes = notes.filter { it.type == ru.nstu.lyceum.schedule.data.local.NoteType.HOMEWORK }
            hwNotes.forEach { hw ->
                val grpPrefix = if (hw.subgroup != null) "[${hw.subgroup} гр] " else ""
                sb.append("\n").append(indent).append("📝 ДЗ: ").append(grpPrefix).append(hw.text)
            }
        }
        if (includeNotes) {
            val genNotes = notes.filter { it.type == ru.nstu.lyceum.schedule.data.local.NoteType.NOTE }
            genNotes.forEach { n ->
                sb.append("\n").append(indent).append("📌 Заметка: ").append(n.text)
            }
        }
    }

    private fun formatSubgroupLine(
        sg: Subgroup,
        sgIndex: Int,
        changes: List<ScheduleChangeItem>,
        allSubgroups: List<Subgroup>,
        defaultSubject: String,
        hasMultipleGroups: Boolean,
        isTeacherMode: Boolean
    ): String {
        val sgChange = findChangeForSubgroup(sg, changes, allSubgroups)
        val isCancelled = sgChange?.isCancelledLesson == true
        val isReplaced = sgChange != null && !isCancelled

        val rawSubj = when {
            isReplaced && !sgChange?.subject.isNullOrBlank() -> sgChange!!.subject!!
            else -> sg.subject ?: defaultSubject
        }
        var subjectStr = normalizeSubject(rawSubj)
        if (isCancelled) {
            subjectStr = "$subjectStr (отменен)"
        }

        val teacherOrClass = when {
            isTeacherMode -> {
                when {
                    isReplaced && !sgChange?.className.isNullOrBlank() -> sgChange!!.className!!
                    else -> sg.className ?: ""
                }
            }
            else -> {
                when {
                    isReplaced && !sgChange?.teacher.isNullOrBlank() -> sgChange!!.teacher!!
                    else -> sg.teacher ?: ""
                }
            }
        }

        val rawRoom = when {
            isReplaced && !sgChange?.room.isNullOrBlank() -> sgChange!!.room!!
            else -> sg.room ?: ""
        }
        val roomClean = rawRoom.trim().removePrefix("(").removeSuffix(")")

        val groupPrefix = if (hasMultipleGroups || !sg.label.isNullOrBlank()) {
            val labelDigits = sg.label?.filter { it.isDigit() }
            val grpNum = if (!labelDigits.isNullOrBlank()) labelDigits else (sgIndex + 1).toString()
            "Группа $grpNum: "
        } else {
            ""
        }

        val parts = mutableListOf<String>()
        if (subjectStr.isNotBlank()) parts.add(subjectStr)
        if (teacherOrClass.isNotBlank()) parts.add(teacherOrClass)
        if (roomClean.isNotBlank()) parts.add(roomClean)

        return groupPrefix + parts.joinToString(", ")
    }

    /**
     * Formats the whole week schedule (Mon-Sat) for clipboard sharing.
     * Uses genitive Russian month names and "День недели, дата" headers per day.
     */
    fun formatWeekScheduleForCopy(
        scheduleResponse: ScheduleResponse?,
        fallbackWeekOffset: Int = 0,
        isTeacherMode: Boolean = false,
        targetName: String = "",
        showPairsSetting: Boolean = false,
        notesByDayIndex: Map<Int, List<ScheduleNote>> = emptyMap(),
        includeHomework: Boolean = false,
        includeNotes: Boolean = false
    ): String {
        val weekStart = scheduleResponse?.weekStart
        val weekRangeLabel = getWeekRangeGenitiveLabel(weekStart, fallbackWeekOffset)

        val sb = StringBuilder()
        sb.append("📆 Вся неделя, ").append(weekRangeLabel).append("\n\n")

        for (dayIdx in 0..5) {
            if (dayIdx > 0) {
                sb.append("\n\n")
            }
            val dayName = DAY_NAMES.getOrElse(dayIdx) { "День" }
            val dateLabel = getDayDateGenitiveLabel(weekStart, dayIdx, fallbackWeekOffset)
            val fullDayTitle = "$dayName, $dateLabel"

            val lessons = scheduleResponse?.days?.firstOrNull { it.dayIndex == dayIdx }?.lessons ?: emptyList()
            val notesForDay = notesByDayIndex[dayIdx] ?: emptyList()

            val hasIncludedBeforeOrAfter = includeNotes && notesForDay.any {
                (it.slotType == SlotType.BEFORE_LESSONS || it.slotType == SlotType.AFTER_LESSONS) && it.text.isNotBlank()
            }

            if (lessons.isEmpty() && !hasIncludedBeforeOrAfter) {
                sb.append("📅 ").append(fullDayTitle).append("\n")
                sb.append("Занятий нет")
            } else {
                sb.append(
                    formatScheduleForCopy(
                        dayTitle = fullDayTitle,
                        lessons = lessons,
                        scheduleResponse = scheduleResponse,
                        dayIndex = dayIdx,
                        isTeacherMode = isTeacherMode,
                        targetName = targetName,
                        showPairsSetting = showPairsSetting,
                        notesForDay = notesForDay,
                        includeHomework = includeHomework,
                        includeNotes = includeNotes,
                        headerPrefix = "📅 "
                    )
                )
            }
        }

        return sb.toString().trimEnd()
    }
}
