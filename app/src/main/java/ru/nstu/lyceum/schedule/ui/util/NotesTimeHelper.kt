package ru.nstu.lyceum.schedule.ui.util

import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.normalizeSubject
import ru.nstu.lyceum.schedule.data.local.SlotType
import ru.nstu.lyceum.schedule.domain.BellSchedule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotesTimeHelper {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val russianDateFormat = SimpleDateFormat("d MMMM", Locale("ru"))
    private val russianDateWithDayFormat = SimpleDateFormat("EEE, d MMM", Locale("ru"))

    fun getTodayIso(): String {
        return isoFormat.format(Calendar.getInstance().time)
    }

    fun getTodayStartUtcMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getDateIsoForDayIndex(weekStart: String?, dayIndex: Int): String {
        val cal = Calendar.getInstance()
        if (!weekStart.isNullOrBlank()) {
            try {
                val parts = weekStart.split("-")
                if (parts.size == 3) {
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.add(Calendar.DAY_OF_MONTH, dayIndex)
                    return isoFormat.format(cal.time)
                }
            } catch (_: Exception) {}
        }
        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -dayOfWeek + dayIndex)
        return isoFormat.format(cal.time)
    }

    fun isSlotInPast(selectedDateIso: String, slotType: SlotType, lessonNum: Int?): Boolean {
        val todayIso = getTodayIso()
        if (selectedDateIso < todayIso) return true
        if (selectedDateIso > todayIso) return false

        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        return when (slotType) {
            SlotType.BEFORE_LESSONS -> nowMinutes >= (8 * 60 + 30) // After 8:30 AM
            SlotType.LESSON -> {
                if (lessonNum == null) false
                else {
                    val bell = BellSchedule.BELLS.firstOrNull { it.num == lessonNum }
                    if (bell != null) {
                        nowMinutes >= bell.startMinutes
                    } else {
                        false
                    }
                }
            }
            SlotType.AFTER_LESSONS -> nowMinutes >= (21 * 60) // After 21:00
            SlotType.DAY_NOTE -> false
        }
    }

    fun formatDateRussian(dateIso: String): String {
        return try {
            val d = isoFormat.parse(dateIso)
            if (d != null) russianDateFormat.format(d) else dateIso
        } catch (_: Exception) {
            dateIso
        }
    }

    fun formatDateWithDay(dateIso: String): String {
        return try {
            val d = isoFormat.parse(dateIso)
            if (d != null) {
                val res = russianDateWithDayFormat.format(d)
                res.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
            } else dateIso
        } catch (_: Exception) {
            dateIso
        }
    }

    /**
     * Resolves the effective subject for a lesson taking into account changes,
     * cancellations, replacements, and subgroups. Returns null if the lesson
     * or subgroup is cancelled or empty.
     */
    fun getEffectiveLessonSubject(
        schedule: ScheduleResponse?,
        dateIso: String,
        lessonNum: Int,
        userSubgroup: Int = 1,
        userTechSubgroup: Int = 1,
        isMiddleSchool: Boolean = false
    ): String? {
        if (schedule == null || schedule.days.isEmpty()) return null

        val cal = Calendar.getInstance()
        try {
            val d = isoFormat.parse(dateIso) ?: return null
            cal.time = d
        } catch (_: Exception) {
            return null
        }

        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mon..5=Sat, 6=Sun
        if (dayOfWeek > 5) return null

        val daySchedule = schedule.days.firstOrNull { it.dayIndex == dayOfWeek } ?: return null
        val lesson = daySchedule.lessons.firstOrNull { it.num == lessonNum } ?: return null

        val changes = schedule.getChangesForDateAndLesson(dateIso, lessonNum)

        // Check total cancellation
        val isTotalCancelled = when {
            changes.isEmpty() -> false
            lesson.subgroups.isEmpty() -> changes.any { it.isCancelledLesson }
            lesson.subgroups.size <= 1 -> changes.any { it.isCancelledLesson }
            else -> {
                val hasGeneralCancel = changes.any {
                    it.isCancelledLesson && it.group.isNullOrBlank() && it.subgroup.isNullOrBlank() && it.label.isNullOrBlank()
                }
                if (hasGeneralCancel) true
                else {
                    lesson.subgroups.all { sg ->
                        val c = ru.nstu.lyceum.schedule.ui.components.findChangeForSubgroup(sg, changes, lesson.subgroups)
                        c?.isCancelledLesson == true
                    }
                }
            }
        }

        if (isTotalCancelled) return null

        // Determine target subgroup for student
        val isTech = lesson.subgroups.any {
            it.subject?.contains("технолог", ignoreCase = true) == true
        } && isMiddleSchool
        val targetGroup = if (isTech) userTechSubgroup else userSubgroup

        // Find subgroup matching user
        val userSg = if (lesson.subgroups.size > 1) {
            lesson.subgroups.firstOrNull { sg ->
                val digit = sg.label?.filter { it.isDigit() }?.toIntOrNull()
                digit == targetGroup
            } ?: lesson.subgroups.firstOrNull()
        } else {
            lesson.subgroups.firstOrNull()
        }

        if (userSg != null) {
            val sgChange = ru.nstu.lyceum.schedule.ui.components.findChangeForSubgroup(userSg, changes, lesson.subgroups)
            if (sgChange != null) {
                if (sgChange.isCancelledLesson) return null
                if (!sgChange.subject.isNullOrBlank() && sgChange.subject != "F" && sgChange.subject != "$" && !sgChange.subject.startsWith("$")) {
                    return sgChange.subject
                }
            }
            val subj = userSg.subject?.takeIf {
                it.isNotBlank() && it != "—" && !it.equals("Урок отменен", ignoreCase = true)
            }
            return subj
        }

        // Fallback: check replacement change if no subgroups
        val replacementChange = changes.firstOrNull { !it.isCancelledLesson && !it.subject.isNullOrBlank() }
        if (replacementChange != null) {
            return replacementChange.subject
        }

        val display = lesson.displaySubject.takeIf {
            it.isNotBlank() && it != "—" && !it.equals("Урок отменен", ignoreCase = true)
        }
        return display
    }

    /**
     * Returns a list of active (non-cancelled) lesson numbers and their effective subjects
     * for a given date.
     */
    fun getAvailableLessonsForDate(
        schedule: ScheduleResponse?,
        dateIso: String,
        userSubgroup: Int = 1,
        userTechSubgroup: Int = 1,
        isMiddleSchool: Boolean = false
    ): List<Pair<Int, String>> {
        if (schedule == null || schedule.days.isEmpty()) return emptyList()

        val cal = Calendar.getInstance()
        try {
            val d = isoFormat.parse(dateIso) ?: return emptyList()
            cal.time = d
        } catch (_: Exception) {
            return emptyList()
        }

        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek > 5) return emptyList()

        val daySchedule = schedule.days.firstOrNull { it.dayIndex == dayOfWeek } ?: return emptyList()

        val list = mutableListOf<Pair<Int, String>>()
        for (lesson in daySchedule.lessons.sortedBy { it.num }) {
            val subj = getEffectiveLessonSubject(
                schedule = schedule,
                dateIso = dateIso,
                lessonNum = lesson.num,
                userSubgroup = userSubgroup,
                userTechSubgroup = userTechSubgroup,
                isMiddleSchool = isMiddleSchool
            )
            if (!subj.isNullOrBlank()) {
                list.add(Pair(lesson.num, subj))
            }
        }
        return list
    }

    /**
     * Finds the next non-cancelled lesson of the given subject in the remainder of the current week.
     */
    fun findNextLessonInCurrentWeek(
        schedule: ScheduleResponse?,
        subject: String,
        targetSubgroup: Int? = null,
        fromDateIso: String,
        afterLessonNum: Int? = null,
        includeSameDayBefore: Boolean = false,
        userTechSubgroup: Int = 1,
        isMiddleSchool: Boolean = false
    ): Pair<String, Int>? {
        if (schedule == null || schedule.days.isEmpty()) return null
        val normTargetSubj = normalizeSubject(subject).lowercase().trim()

        val cal = Calendar.getInstance()
        try {
            val d = isoFormat.parse(fromDateIso) ?: return null
            cal.time = d
        } catch (_: Exception) {
            return null
        }

        val fromDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        if (fromDayOfWeek > 5) {
            return null // Sunday
        }

        val todayIso = getTodayIso()

        for (dayOfWeek in fromDayOfWeek..5) {
            val checkCal = Calendar.getInstance().apply {
                time = cal.time
                add(Calendar.DAY_OF_MONTH, dayOfWeek - fromDayOfWeek)
            }
            val dateIso = isoFormat.format(checkCal.time)
            val daySchedule = schedule.days.firstOrNull { it.dayIndex == dayOfWeek } ?: continue

            for (lesson in daySchedule.lessons.sortedBy { it.num }) {
                if (dayOfWeek == fromDayOfWeek) {
                    if (!includeSameDayBefore && afterLessonNum != null && lesson.num <= afterLessonNum) {
                        continue
                    }
                    if (dateIso == todayIso && isSlotInPast(todayIso, SlotType.LESSON, lesson.num)) {
                        continue
                    }
                    if (afterLessonNum != null && lesson.num == afterLessonNum) {
                        continue
                    }
                }

                val effSubj = getEffectiveLessonSubject(
                    schedule = schedule,
                    dateIso = dateIso,
                    lessonNum = lesson.num,
                    userSubgroup = targetSubgroup ?: 1,
                    userTechSubgroup = userTechSubgroup,
                    isMiddleSchool = isMiddleSchool
                )

                if (effSubj != null) {
                    val normSubj = normalizeSubject(effSubj).lowercase().trim()
                    val matches = normSubj == normTargetSubj || normSubj.contains(normTargetSubj) || normTargetSubj.contains(normSubj)
                    if (matches) {
                        return Pair(dateIso, lesson.num)
                    }
                }
            }
        }
        return null
    }

    /**
     * Checks all uncompleted homework notes for the schedule's target class/teacher.
     * If a lesson on or after today was cancelled or replaced with a different subject
     * (e.g. mid-week schedule changes), the homework is automatically relocated to the
     * next non-cancelled occurrence of this subject (either later this week or marked
     * transferPending = true for the next week).
     *
     * Returns a list of Pair(note, message) for any relocated notes.
     */
    fun checkAndRelocateCancelledHomework(
        notesRepository: ru.nstu.lyceum.schedule.data.local.NotesRepository,
        schedule: ScheduleResponse?,
        userSubgroup: Int = 1,
        userTechSubgroup: Int = 1,
        isMiddleSchool: Boolean = false
    ): List<Pair<ru.nstu.lyceum.schedule.data.local.ScheduleNote, String>> {
        if (schedule == null || schedule.days.isEmpty()) return emptyList()
        val target = schedule.className ?: schedule.teacherName ?: return emptyList()
        val todayIso = getTodayIso()

        val allNotes = notesRepository.notes.value
        val activeHwNotes = allNotes.filter { note ->
            note.targetName.equals(target, ignoreCase = true) &&
            note.type == ru.nstu.lyceum.schedule.data.local.NoteType.HOMEWORK &&
            !note.isCompleted &&
            !note.transferPending &&
            note.date >= todayIso &&
            !note.subject.isNullOrBlank() &&
            note.lessonNum != null
        }

        if (activeHwNotes.isEmpty()) return emptyList()

        val updatedNotes = mutableListOf<ru.nstu.lyceum.schedule.data.local.ScheduleNote>()
        val relocatedInfo = mutableListOf<Pair<ru.nstu.lyceum.schedule.data.local.ScheduleNote, String>>()

        for (note in activeHwNotes) {
            val subj = note.subject ?: continue
            val lsnNum = note.lessonNum ?: continue
            val targetSg = note.subgroup ?: if (subj.contains("технолог", ignoreCase = true) && isMiddleSchool) userTechSubgroup else userSubgroup

            // Check what is currently at this lesson slot in the updated schedule
            val currentEffSubj = getEffectiveLessonSubject(
                schedule = schedule,
                dateIso = note.date,
                lessonNum = lsnNum,
                userSubgroup = targetSg,
                userTechSubgroup = userTechSubgroup,
                isMiddleSchool = isMiddleSchool
            )

            val normSubj = normalizeSubject(subj).lowercase().trim()
            val isStillSameSubject = if (currentEffSubj != null) {
                val normCurrent = normalizeSubject(currentEffSubj).lowercase().trim()
                normCurrent == normSubj || normCurrent.contains(normSubj) || normSubj.contains(normCurrent)
            } else false

            // If the lesson was cancelled or replaced with another subject -> relocate!
            if (!isStillSameSubject) {
                val nextInWeek = findNextLessonInCurrentWeek(
                    schedule = schedule,
                    subject = subj,
                    targetSubgroup = targetSg,
                    fromDateIso = note.date,
                    afterLessonNum = if (note.date == todayIso) lsnNum else null,
                    includeSameDayBefore = (note.date > todayIso),
                    userTechSubgroup = userTechSubgroup,
                    isMiddleSchool = isMiddleSchool
                )

                if (nextInWeek != null) {
                    val updated = note.copy(
                        date = nextInWeek.first,
                        lessonNum = nextInWeek.second,
                        transferPending = false
                    )
                    updatedNotes.add(updated)
                    val dateLabel = formatDateRussian(nextInWeek.first)
                    val msg = "ДЗ по $subj перенесено на $dateLabel (${nextInWeek.second} урок) из-за отмены урока"
                    relocatedInfo.add(Pair(updated, msg))
                } else {
                    val updated = note.copy(
                        transferPending = true
                    )
                    updatedNotes.add(updated)
                    val msg = "ДЗ по $subj перенесено на следующую неделю (урок отменён)"
                    relocatedInfo.add(Pair(updated, msg))
                }
            } else {
                // If the lesson still exists, check if an earlier lesson appeared due to a schedule change
                val earliestInWeek = findNextLessonInCurrentWeek(
                    schedule = schedule,
                    subject = subj,
                    targetSubgroup = targetSg,
                    fromDateIso = todayIso,
                    afterLessonNum = null,
                    includeSameDayBefore = true,
                    userTechSubgroup = userTechSubgroup,
                    isMiddleSchool = isMiddleSchool
                )

                if (earliestInWeek != null) {
                    val isEarlier = earliestInWeek.first < note.date ||
                            (earliestInWeek.first == note.date && earliestInWeek.second < lsnNum)

                    if (isEarlier) {
                        val changesAtEarlier = schedule.getChangesForDateAndLesson(earliestInWeek.first, earliestInWeek.second)
                        val appearedDueToChange = changesAtEarlier.any { c ->
                            if (c.isCancelledLesson) return@any false
                            val cSubj = c.subject?.let { normalizeSubject(it).lowercase().trim() } ?: ""
                            cSubj == normSubj || cSubj.contains(normSubj) || normSubj.contains(cSubj)
                        }

                        if (appearedDueToChange) {
                            val updated = note.copy(
                                date = earliestInWeek.first,
                                lessonNum = earliestInWeek.second,
                                transferPending = false
                            )
                            updatedNotes.add(updated)
                            val dateLabel = formatDateRussian(earliestInWeek.first)
                            val msg = "ДЗ по $subj перенесено раньше на $dateLabel (${earliestInWeek.second} урок) из-за изменения в расписании"
                            relocatedInfo.add(Pair(updated, msg))
                        }
                    }
                }
            }
        }

        if (updatedNotes.isNotEmpty()) {
            notesRepository.updateNotes(updatedNotes)
        }

        return relocatedInfo
    }

    /**
     * Finds the closest non-cancelled occurrence of a subject in a full week's schedule.
     */
    fun findClosestLessonInWeek(
        schedule: ScheduleResponse,
        subject: String,
        targetSubgroup: Int? = null,
        fromDayIndex: Int = 0,
        userSubgroup: Int = 1,
        userTechSubgroup: Int = 1,
        isMiddleSchool: Boolean = false
    ): Pair<String, Int>? {
        if (schedule.days.isEmpty()) return null
        val normTargetSubj = normalizeSubject(subject).lowercase().trim()

        for (dayIdx in fromDayIndex..5) {
            val dateIso = getDateIsoForDayIndex(schedule.weekStart, dayIdx)
            val daySchedule = schedule.days.firstOrNull { it.dayIndex == dayIdx } ?: continue

            for (lesson in daySchedule.lessons.sortedBy { it.num }) {
                val effSubj = getEffectiveLessonSubject(
                    schedule = schedule,
                    dateIso = dateIso,
                    lessonNum = lesson.num,
                    userSubgroup = targetSubgroup ?: userSubgroup,
                    userTechSubgroup = userTechSubgroup,
                    isMiddleSchool = isMiddleSchool
                )

                if (effSubj != null) {
                    val normSubj = normalizeSubject(effSubj).lowercase().trim()
                    val matches = normSubj == normTargetSubj || normSubj.contains(normTargetSubj) || normTargetSubj.contains(normSubj)
                    if (matches) {
                        return Pair(dateIso, lesson.num)
                    }
                }
            }
        }
        return null
    }

    /**
     * Resolves pending weekly transfers against the provided schedule.
     * If the subject is found in an upcoming non-cancelled lesson this week,
     * the note date and lesson number are updated and transferPending is cleared.
     * If not found (e.g. week parity alternation), the note remains transferPending = true
     * to be resolved on the subsequent week.
     */
    fun resolvePendingTransfers(
        notesRepository: ru.nstu.lyceum.schedule.data.local.NotesRepository,
        schedule: ScheduleResponse?,
        userSubgroup: Int = 1,
        userTechSubgroup: Int = 1,
        isMiddleSchool: Boolean = false
    ) {
        if (schedule == null) return
        val target = schedule.className ?: schedule.teacherName ?: return
        val allNotes = notesRepository.notes.value
        val pendingNotes = allNotes.filter { it.targetName == target && it.transferPending && !it.isCompleted }
        if (pendingNotes.isEmpty()) return

        val isCurrentWeek = BellSchedule.isCurrentScheduleWeek(schedule.weekStart)
        val fromDay = if (isCurrentWeek) {
            val cal = Calendar.getInstance()
            val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            dow.coerceIn(0, 5)
        } else 0

        val resolvedList = mutableListOf<ru.nstu.lyceum.schedule.data.local.ScheduleNote>()

        for (note in pendingNotes) {
            val subj = note.subject
            if (subj.isNullOrBlank()) continue

            val closest = findClosestLessonInWeek(
                schedule = schedule,
                subject = subj,
                targetSubgroup = note.subgroup,
                fromDayIndex = fromDay,
                userSubgroup = userSubgroup,
                userTechSubgroup = userTechSubgroup,
                isMiddleSchool = isMiddleSchool
            )

            if (closest != null) {
                resolvedList.add(
                    note.copy(
                        date = closest.first,
                        lessonNum = closest.second,
                        transferPending = false
                    )
                )
            }
        }

        if (resolvedList.isNotEmpty()) {
            notesRepository.updateNotes(resolvedList)
        }
    }

    /**
     * Finds the next date and lesson number for a given subject in the schedule.
     */
    fun findNextLessonOccurrence(
        schedule: ScheduleResponse?,
        subject: String,
        targetSubgroup: Int?,
        fromDateIso: String
    ): Pair<String, Int>? {
        return findNextLessonInCurrentWeek(
            schedule = schedule,
            subject = subject,
            targetSubgroup = targetSubgroup,
            fromDateIso = fromDateIso
        )
    }
}
