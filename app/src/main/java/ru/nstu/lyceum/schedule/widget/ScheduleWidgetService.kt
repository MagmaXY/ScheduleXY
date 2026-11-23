package ru.nstu.lyceum.schedule.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.data.api.Lesson
import ru.nstu.lyceum.schedule.data.api.ScheduleChangeItem
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.Subgroup
import ru.nstu.lyceum.schedule.data.local.CacheManager
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.ui.MainActivity
import ru.nstu.lyceum.schedule.ui.components.ScheduleItemUi
import ru.nstu.lyceum.schedule.ui.components.SchedulePairMerger
import java.util.Calendar

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ScheduleRemoteViewsFactory(applicationContext)
    }
}

sealed class WidgetRow {
    data class SlotNote(val title: String, val text: String) : WidgetRow()
    data class Lesson(
        val itemUi: ScheduleItemUi,
        val homeworkText: String?,
        val noteText: String?
    ) : WidgetRow()
}

class ScheduleRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var widgetRows: List<WidgetRow> = emptyList()
    private var currentSchedule: ScheduleResponse? = null
    private var targetDayIdx: Int = 0
    private var targetType: String = PreferencesRepository.TYPE_CLASS
    private var targetName: String = ""
    private var isTeacherMode: Boolean = false
    private var isDark: Boolean = false
    private var themePreset: String = PreferencesRepository.THEME_CLASSIC

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val prefs = PreferencesRepository(context)
        val cache = CacheManager(context)

        isDark = prefs.isDarkMode
        themePreset = prefs.themePreset
        targetType = prefs.targetType
        targetName = prefs.targetName
        isTeacherMode = targetType == PreferencesRepository.TYPE_TEACHER

        val defaultDay = BellSchedule.getDefaultScheduleDayIndex()
        val dIdx = if (prefs.widgetDayIndex in 0..5) prefs.widgetDayIndex else defaultDay
        targetDayIdx = dIdx

        val sched = cache.getSchedule(targetType, targetName, 0)
        currentSchedule = sched
        val dayLessons = sched?.days?.firstOrNull { it.dayIndex == targetDayIdx }?.lessons ?: emptyList()

        val rows = mutableListOf<WidgetRow>()

        // Load notes from lyceum_notes.json
        val targetDateIso = ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper.getDateIsoForDayIndex(sched?.weekStart, targetDayIdx)
        val notesFile = java.io.File(context.filesDir, "lyceum_notes.json")
        val dayNotes = try {
            if (notesFile.exists()) {
                val json = notesFile.readText()
                val type = object : com.google.gson.reflect.TypeToken<List<ru.nstu.lyceum.schedule.data.local.ScheduleNote>>() {}.type
                val list = com.google.gson.Gson().fromJson<List<ru.nstu.lyceum.schedule.data.local.ScheduleNote>>(json, type) ?: emptyList()
                list.filter { it.targetName == targetName && it.date == targetDateIso && !it.isDeleted }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val beforeNote = dayNotes.firstOrNull { it.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.BEFORE_LESSONS }
        if (beforeNote != null && beforeNote.text.isNotBlank()) {
            rows.add(WidgetRow.SlotNote("До уроков", beforeNote.text))
        }

        val processedUi = SchedulePairMerger.processLessons(
            lessons = dayLessons,
            isTeacherMode = isTeacherMode,
            targetName = targetName,
            showPairsSetting = prefs.showPairsSchedule
        )

        processedUi.forEach { uiItem ->
            val (hwList, noteList) = when (uiItem) {
                is ScheduleItemUi.SingleLesson -> {
                    val matching = dayNotes.filter { it.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.LESSON && it.lessonNum == uiItem.lesson.num }
                    Pair(
                        matching.filter { it.type == ru.nstu.lyceum.schedule.data.local.NoteType.HOMEWORK },
                        matching.filter { it.type == ru.nstu.lyceum.schedule.data.local.NoteType.NOTE }
                    )
                }
                is ScheduleItemUi.CombinedPair -> {
                    val matching = dayNotes.filter {
                        it.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.LESSON &&
                        (it.lessonNum == uiItem.firstLesson.num || it.lessonNum == uiItem.secondLesson.num)
                    }
                    Pair(
                        matching.filter { it.type == ru.nstu.lyceum.schedule.data.local.NoteType.HOMEWORK },
                        matching.filter { it.type == ru.nstu.lyceum.schedule.data.local.NoteType.NOTE }
                    )
                }
            }

            val hwText = if (hwList.isNotEmpty()) {
                hwList.joinToString("; ") { hw ->
                    hw.text
                }
            } else null

            val noteText = if (noteList.isNotEmpty()) {
                noteList.joinToString("; ") { it.text }
            } else null

            rows.add(WidgetRow.Lesson(uiItem, hwText, noteText))
        }

        val afterNote = dayNotes.firstOrNull { it.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.AFTER_LESSONS }
        if (afterNote != null && afterNote.text.isNotBlank()) {
            rows.add(WidgetRow.SlotNote("После уроков", afterNote.text))
        }

        widgetRows = rows
    }

    override fun onDestroy() {}

    override fun getCount(): Int = widgetRows.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= widgetRows.size) return RemoteViews(context.packageName, R.layout.widget_item_lesson)

        val row = widgetRows[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item_lesson)

        val dynamicPrimary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themePreset == PreferencesRepository.THEME_DYNAMIC) {
            context.getColor(if (isDark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600)
        } else null

        // Divider color
        val dividerColor = when {
            themePreset == PreferencesRepository.THEME_OLED && isDark -> Color.parseColor("#27272A")
            themePreset == PreferencesRepository.THEME_EMERALD && isDark -> Color.parseColor("#1C2722")
            isDark -> Color.parseColor("#334155")
            themePreset == PreferencesRepository.THEME_EMERALD -> Color.parseColor("#BFC9C3")
            themePreset == PreferencesRepository.THEME_OLED -> Color.parseColor("#E4E4E7")
            else -> Color.parseColor("#E2E8F0")
        }
        views.setInt(R.id.item_divider, "setBackgroundColor", dividerColor)

        if (row is WidgetRow.SlotNote) {
            views.setViewVisibility(R.id.widget_item_container, View.GONE)
            views.setViewVisibility(R.id.item_slot_note_container, View.VISIBLE)
            views.setTextViewText(R.id.item_slot_note_title, row.title)
            views.setTextViewText(R.id.item_slot_note_text, row.text)
            val slotTitleColor = when {
                dynamicPrimary != null -> dynamicPrimary
                themePreset == PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#6EDBB0") else Color.parseColor("#006C4C")
                themePreset == PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#FAFAFA") else Color.parseColor("#18181B")
                else -> if (isDark) Color.parseColor("#9999FF") else Color.parseColor("#222288")
            }
            val slotTextColor = when {
                themePreset == PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#89938E") else Color.parseColor("#4D6357")
                themePreset == PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#A1A1AA") else Color.parseColor("#71717A")
                else -> if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#555555")
            }
            views.setTextColor(R.id.item_slot_note_title, slotTitleColor)
            views.setTextColor(R.id.item_slot_note_text, slotTextColor)
            return views
        }

        views.setViewVisibility(R.id.widget_item_container, View.VISIBLE)
        views.setViewVisibility(R.id.item_slot_note_container, View.GONE)

        val itemUi = (row as WidgetRow.Lesson).itemUi

        val hwNoteColor = when {
            themePreset == PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#89938E") else Color.parseColor("#4D6357")
            themePreset == PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#A1A1AA") else Color.parseColor("#71717A")
            else -> if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
        }

        if (!row.homeworkText.isNullOrBlank()) {
            views.setViewVisibility(R.id.item_lsn_homework, View.VISIBLE)
            views.setTextViewText(R.id.item_lsn_homework, "ДЗ: " + row.homeworkText)
            views.setTextColor(R.id.item_lsn_homework, hwNoteColor)
        } else {
            views.setViewVisibility(R.id.item_lsn_homework, View.GONE)
        }

        if (!row.noteText.isNullOrBlank()) {
            views.setViewVisibility(R.id.item_lsn_note, View.VISIBLE)
            views.setTextViewText(R.id.item_lsn_note, "Заметка: " + row.noteText)
            views.setTextColor(R.id.item_lsn_note, hwNoteColor)
        } else {
            views.setViewVisibility(R.id.item_lsn_note, View.GONE)
        }

        // Palette tokens matching active theme
        val numColor = when (themePreset) {
            PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#71717A") else Color.parseColor("#A1A1AA")
            PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#89938E") else Color.parseColor("#6F7974")
            else -> if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#AAAAAA")
        }
        val timeColor = when {
            themePreset == PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#6EDBB0") else Color.parseColor("#006C4C")
            themePreset == PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#E2E8F0") else Color.parseColor("#18181B")
            dynamicPrimary != null -> dynamicPrimary
            else -> if (isDark) Color.parseColor("#33CC33") else Color.parseColor("#008000")
        }
        val cancelColor = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
        val changeColor = if (isDark) Color.parseColor("#F87171") else Color.parseColor("#DD2222")
        val subjColor = when {
            dynamicPrimary != null -> dynamicPrimary
            themePreset == PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#6EDBB0") else Color.parseColor("#006C4C")
            themePreset == PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#FAFAFA") else Color.parseColor("#18181B")
            else -> if (isDark) Color.parseColor("#9999FF") else Color.parseColor("#222288")
        }
        val teacherColor = when {
            themePreset == PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#7DD3B6") else Color.parseColor("#1E6B52")
            themePreset == PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#A1A1AA") else Color.parseColor("#3F3F46")
            else -> if (isDark) Color.parseColor("#5EEAD4") else Color.parseColor("#226666")
        }
        val groupColor = when {
            dynamicPrimary != null -> dynamicPrimary
            themePreset == PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#6EDBB0") else Color.parseColor("#006C4C")
            themePreset == PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#E4E4E7") else Color.parseColor("#27272A")
            else -> if (isDark) Color.parseColor("#8099FF") else Color.parseColor("#0000FF")
        }
        val roomColor = when (themePreset) {
            PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
            PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#DEE4DF") else Color.parseColor("#171D1A")
            else -> if (isDark) Color.parseColor("#F1F5F9") else Color.parseColor("#000000")
        }
        val classColor = roomColor

        when (itemUi) {
            is ScheduleItemUi.SingleLesson -> {
                val lesson = itemUi.lesson

                views.setTextViewText(R.id.item_lsn_num, lesson.num.toString())
                views.setTextColor(R.id.item_lsn_num, numColor)

                val times = lesson.time.split("–", "-")
                val timeFormatted = if (times.size >= 2) "${times[0].trim()}\n${times[1].trim()}" else lesson.time
                views.setTextViewText(R.id.item_lsn_time, timeFormatted)
                views.setTextColor(R.id.item_lsn_time, timeColor)

                val allChanges = currentSchedule?.getChangesForLesson(targetDayIdx, lesson.num) ?: emptyList()
                val isTotalCancelled = when {
                    allChanges.isEmpty() -> false
                    lesson.subgroups.isEmpty() -> false
                    lesson.subgroups.size <= 1 -> allChanges.any { it.isCancelledLesson }
                    else -> {
                        val hasGeneralCancel = allChanges.any {
                            it.isCancelledLesson && it.group.isNullOrBlank() && it.subgroup.isNullOrBlank() && it.label.isNullOrBlank()
                        }
                        if (hasGeneralCancel) true
                        else lesson.subgroups.all { sg ->
                            val c = findWidgetChangeForSubgroup(sg, allChanges, lesson.subgroups)
                            c?.isCancelledLesson == true
                        }
                    }
                }

                if (lesson.subgroups.isEmpty()) {
                    views.setTextViewText(R.id.item_lsn_subject, "—")
                    views.setTextColor(R.id.item_lsn_subject, numColor)
                    views.setViewVisibility(R.id.item_lsn_room, View.GONE)
                    views.setTextViewText(R.id.item_lsn_sub, "")
                    views.setViewVisibility(R.id.item_sg2_container, View.GONE)
                } else if (isTotalCancelled) {
                    val subjText = lesson.displaySubject.takeIf { it != "—" } ?: "Урок отменен"
                    val spanSubj = SpannableString(subjText)
                    spanSubj.setSpan(StrikethroughSpan(), 0, spanSubj.length, 0)
                    views.setTextViewText(R.id.item_lsn_subject, spanSubj)
                    views.setTextColor(R.id.item_lsn_subject, cancelColor)

                    val firstSg = lesson.subgroups.firstOrNull()
                    val rawRoom1 = firstSg?.room?.takeIf { it.isNotBlank() } ?: allChanges.firstOrNull { !it.room.isNullOrBlank() }?.room
                    val roomStr1 = formatWidgetRoom(rawRoom1)
                    if (roomStr1.isNotBlank()) {
                        val spanRoom1 = SpannableString(roomStr1)
                        spanRoom1.setSpan(StrikethroughSpan(), 0, spanRoom1.length, 0)
                        views.setTextViewText(R.id.item_lsn_room, spanRoom1)
                        views.setTextColor(R.id.item_lsn_room, cancelColor)
                        views.setViewVisibility(R.id.item_lsn_room, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.item_lsn_room, View.GONE)
                    }

                    val subline = buildCancelledSubline(firstSg, isTeacherMode, cancelColor, allChanges.firstOrNull())
                    views.setTextViewText(R.id.item_lsn_sub, subline)

                    if (lesson.subgroups.size > 1) {
                        val secondSg = lesson.subgroups[1]
                        views.setViewVisibility(R.id.item_sg2_container, View.VISIBLE)
                        val subj2Text = secondSg.subject ?: lesson.displaySubject
                        val spanSubj2 = SpannableString(subj2Text)
                        spanSubj2.setSpan(StrikethroughSpan(), 0, spanSubj2.length, 0)
                        views.setTextViewText(R.id.item_lsn_subject_2, spanSubj2)
                        views.setTextColor(R.id.item_lsn_subject_2, cancelColor)

                        val roomStr2 = formatWidgetRoom(secondSg.room)
                        if (roomStr2.isNotBlank()) {
                            val spanRoom2 = SpannableString(roomStr2)
                            spanRoom2.setSpan(StrikethroughSpan(), 0, spanRoom2.length, 0)
                            views.setTextViewText(R.id.item_lsn_room_2, spanRoom2)
                            views.setTextColor(R.id.item_lsn_room_2, cancelColor)
                            views.setViewVisibility(R.id.item_lsn_room_2, View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.item_lsn_room_2, View.GONE)
                        }

                        val subline2 = buildCancelledSubline(secondSg, isTeacherMode, cancelColor)
                        views.setTextViewText(R.id.item_lsn_sub_2, subline2)
                    } else {
                        views.setViewVisibility(R.id.item_sg2_container, View.GONE)
                    }
                } else {
                    val sg1 = lesson.subgroups[0]
                    val sgChange1 = findWidgetChangeForSubgroup(sg1, allChanges, lesson.subgroups)
                    val (isCancelled1, isReplaced1) = evaluateWidgetSubgroupChange(sg1, allChanges, lesson.subgroups)
                    renderSubgroup(
                        views = views,
                        subjectViewId = R.id.item_lsn_subject,
                        subViewId = R.id.item_lsn_sub,
                        roomViewId = R.id.item_lsn_room,
                        sg = sg1,
                        sgChange = sgChange1,
                        isCancelled = isCancelled1,
                        isReplaced = isReplaced1,
                        lessonDefaultSubj = lesson.displaySubject,
                        isTeacherMode = isTeacherMode,
                        subjColor = subjColor,
                        changeColor = changeColor,
                        cancelColor = cancelColor,
                        teacherColor = teacherColor,
                        groupColor = groupColor,
                        roomColor = roomColor,
                        classColor = classColor
                    )

                    if (lesson.subgroups.size > 1) {
                        views.setViewVisibility(R.id.item_sg2_container, View.VISIBLE)
                        val sg2 = lesson.subgroups[1]
                        val sgChange2 = findWidgetChangeForSubgroup(sg2, allChanges, lesson.subgroups)
                        val (isCancelled2, isReplaced2) = evaluateWidgetSubgroupChange(sg2, allChanges, lesson.subgroups)
                        renderSubgroup(
                            views = views,
                            subjectViewId = R.id.item_lsn_subject_2,
                            subViewId = R.id.item_lsn_sub_2,
                            roomViewId = R.id.item_lsn_room_2,
                            sg = sg2,
                            sgChange = sgChange2,
                            isCancelled = isCancelled2,
                            isReplaced = isReplaced2,
                            lessonDefaultSubj = lesson.displaySubject,
                            isTeacherMode = isTeacherMode,
                            subjColor = subjColor,
                            changeColor = changeColor,
                            cancelColor = cancelColor,
                            teacherColor = teacherColor,
                            groupColor = groupColor,
                            roomColor = roomColor,
                            classColor = classColor
                        )
                    } else {
                        views.setViewVisibility(R.id.item_sg2_container, View.GONE)
                    }
                }
            }

            is ScheduleItemUi.CombinedPair -> {
                // Roman Pair Number
                views.setTextViewText(R.id.item_lsn_num, itemUi.pairRoman)
                views.setTextColor(R.id.item_lsn_num, numColor)

                // Pair Times (e.g. "8:30\n10:00")
                val times = itemUi.pairTimes.split("–", "-")
                val timeFormatted = if (times.size >= 2) "${times[0].trim()}\n${times[1].trim()}" else itemUi.pairTimes
                views.setTextViewText(R.id.item_lsn_time, timeFormatted)
                views.setTextColor(R.id.item_lsn_time, timeColor)

                val l1 = itemUi.firstLesson
                val l2 = itemUi.secondLesson
                val changes1 = currentSchedule?.getChangesForLesson(targetDayIdx, l1.num) ?: emptyList()
                val changes2 = currentSchedule?.getChangesForLesson(targetDayIdx, l2.num) ?: emptyList()

                // Check if both lessons are identical
                val areIdentical = areLessonsIdenticalForWidget(l1, l2)

                if (areIdentical) {
                    val allChanges = changes1.ifEmpty { changes2 }
                    val isTotalCancelled = when {
                        allChanges.isEmpty() -> false
                        l1.subgroups.isEmpty() -> allChanges.any { it.isCancelledLesson }
                        l1.subgroups.size <= 1 -> allChanges.any { it.isCancelledLesson }
                        else -> {
                            val hasGeneralCancel = allChanges.any {
                                it.isCancelledLesson && it.group.isNullOrBlank() && it.subgroup.isNullOrBlank() && it.label.isNullOrBlank()
                            }
                            if (hasGeneralCancel) true
                            else l1.subgroups.all { sg ->
                                val c = findWidgetChangeForSubgroup(sg, allChanges, l1.subgroups)
                                c?.isCancelledLesson == true
                            }
                        }
                    }

                    if (l1.subgroups.isEmpty()) {
                        views.setTextViewText(R.id.item_lsn_subject, "—")
                        views.setTextColor(R.id.item_lsn_subject, numColor)
                        views.setViewVisibility(R.id.item_lsn_room, View.GONE)
                        views.setTextViewText(R.id.item_lsn_sub, "")
                        views.setViewVisibility(R.id.item_sg2_container, View.GONE)
                    } else if (isTotalCancelled) {
                        val subjText = l1.displaySubject.takeIf { it != "—" } ?: "Пара отменена"
                        val spanSubj = SpannableString(subjText)
                        spanSubj.setSpan(StrikethroughSpan(), 0, spanSubj.length, 0)
                        views.setTextViewText(R.id.item_lsn_subject, spanSubj)
                        views.setTextColor(R.id.item_lsn_subject, cancelColor)

                        val firstSg = l1.subgroups.firstOrNull()
                        val rawRoom1 = firstSg?.room?.takeIf { it.isNotBlank() } ?: allChanges.firstOrNull { !it.room.isNullOrBlank() }?.room
                        val roomStr1 = formatWidgetRoom(rawRoom1)
                        if (roomStr1.isNotBlank()) {
                            val spanRoom1 = SpannableString(roomStr1)
                            spanRoom1.setSpan(StrikethroughSpan(), 0, spanRoom1.length, 0)
                            views.setTextViewText(R.id.item_lsn_room, spanRoom1)
                            views.setTextColor(R.id.item_lsn_room, cancelColor)
                            views.setViewVisibility(R.id.item_lsn_room, View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.item_lsn_room, View.GONE)
                        }

                        val subline = buildCancelledSubline(firstSg, isTeacherMode, cancelColor, allChanges.firstOrNull())
                        views.setTextViewText(R.id.item_lsn_sub, subline)

                        if (l1.subgroups.size > 1) {
                            val secondSg = l1.subgroups[1]
                            views.setViewVisibility(R.id.item_sg2_container, View.VISIBLE)
                            val subj2Text = secondSg.subject ?: l1.displaySubject
                            val spanSubj2 = SpannableString(subj2Text)
                            spanSubj2.setSpan(StrikethroughSpan(), 0, spanSubj2.length, 0)
                            views.setTextViewText(R.id.item_lsn_subject_2, spanSubj2)
                            views.setTextColor(R.id.item_lsn_subject_2, cancelColor)

                            val roomStr2 = formatWidgetRoom(secondSg.room)
                            if (roomStr2.isNotBlank()) {
                                val spanRoom2 = SpannableString(roomStr2)
                                spanRoom2.setSpan(StrikethroughSpan(), 0, spanRoom2.length, 0)
                                views.setTextViewText(R.id.item_lsn_room_2, spanRoom2)
                                views.setTextColor(R.id.item_lsn_room_2, cancelColor)
                                views.setViewVisibility(R.id.item_lsn_room_2, View.VISIBLE)
                            } else {
                                views.setViewVisibility(R.id.item_lsn_room_2, View.GONE)
                            }

                            val subline2 = buildCancelledSubline(secondSg, isTeacherMode, cancelColor)
                            views.setTextViewText(R.id.item_lsn_sub_2, subline2)
                        } else {
                            views.setViewVisibility(R.id.item_sg2_container, View.GONE)
                        }
                    } else {
                        // Render subgroups of unified pair
                        val sg1 = l1.subgroups[0]
                        val sgChange1 = findWidgetChangeForSubgroup(sg1, allChanges, l1.subgroups)
                        val (isCancelled1, isReplaced1) = evaluateWidgetSubgroupChange(sg1, allChanges, l1.subgroups)
                        renderSubgroup(
                            views = views,
                            subjectViewId = R.id.item_lsn_subject,
                            subViewId = R.id.item_lsn_sub,
                            roomViewId = R.id.item_lsn_room,
                            sg = sg1,
                            sgChange = sgChange1,
                            isCancelled = isCancelled1,
                            isReplaced = isReplaced1,
                            lessonDefaultSubj = l1.displaySubject,
                            isTeacherMode = isTeacherMode,
                            subjColor = subjColor,
                            changeColor = changeColor,
                            cancelColor = cancelColor,
                            teacherColor = teacherColor,
                            groupColor = groupColor,
                            roomColor = roomColor,
                            classColor = classColor
                        )

                        if (l1.subgroups.size > 1) {
                            views.setViewVisibility(R.id.item_sg2_container, View.VISIBLE)
                            val sg2 = l1.subgroups[1]
                            val sgChange2 = findWidgetChangeForSubgroup(sg2, allChanges, l1.subgroups)
                            val (isCancelled2, isReplaced2) = evaluateWidgetSubgroupChange(sg2, allChanges, l1.subgroups)
                            renderSubgroup(
                                views = views,
                                subjectViewId = R.id.item_lsn_subject_2,
                                subViewId = R.id.item_lsn_sub_2,
                                roomViewId = R.id.item_lsn_room_2,
                                sg = sg2,
                                sgChange = sgChange2,
                                isCancelled = isCancelled2,
                                isReplaced = isReplaced2,
                                lessonDefaultSubj = l1.displaySubject,
                                isTeacherMode = isTeacherMode,
                                subjColor = subjColor,
                                changeColor = changeColor,
                                cancelColor = cancelColor,
                                teacherColor = teacherColor,
                                groupColor = groupColor,
                                roomColor = roomColor,
                                classColor = classColor
                            )
                        } else {
                            views.setViewVisibility(R.id.item_sg2_container, View.GONE)
                        }
                    }
                } else {
                    // Different subjects: Render lesson 1 in slot 1 and lesson 2 in slot 2 with time prefixes
                    val sg1 = l1.subgroups.firstOrNull()
                    val sg2 = l2.subgroups.firstOrNull()

                    if (sg1 != null) {
                        val sgChange1 = findWidgetChangeForSubgroup(sg1, changes1, l1.subgroups)
                        val (isCancelled1, isReplaced1) = evaluateWidgetSubgroupChange(sg1, changes1, l1.subgroups)
                        renderSubgroup(
                            views = views,
                            subjectViewId = R.id.item_lsn_subject,
                            subViewId = R.id.item_lsn_sub,
                            roomViewId = R.id.item_lsn_room,
                            sg = sg1,
                            sgChange = sgChange1,
                            isCancelled = isCancelled1,
                            isReplaced = isReplaced1,
                            lessonDefaultSubj = l1.displaySubject,
                            isTeacherMode = isTeacherMode,
                            subjColor = subjColor,
                            changeColor = changeColor,
                            cancelColor = cancelColor,
                            teacherColor = teacherColor,
                            groupColor = groupColor,
                            roomColor = roomColor,
                            classColor = classColor,
                            timePrefix = "${l1.time} "
                        )
                    }

                    if (sg2 != null) {
                        views.setViewVisibility(R.id.item_sg2_container, View.VISIBLE)
                        val sgChange2 = findWidgetChangeForSubgroup(sg2, changes2, l2.subgroups)
                        val (isCancelled2, isReplaced2) = evaluateWidgetSubgroupChange(sg2, changes2, l2.subgroups)
                        renderSubgroup(
                            views = views,
                            subjectViewId = R.id.item_lsn_subject_2,
                            subViewId = R.id.item_lsn_sub_2,
                            roomViewId = R.id.item_lsn_room_2,
                            sg = sg2,
                            sgChange = sgChange2,
                            isCancelled = isCancelled2,
                            isReplaced = isReplaced2,
                            lessonDefaultSubj = l2.displaySubject,
                            isTeacherMode = isTeacherMode,
                            subjColor = subjColor,
                            changeColor = changeColor,
                            cancelColor = cancelColor,
                            teacherColor = teacherColor,
                            groupColor = groupColor,
                            roomColor = roomColor,
                            classColor = classColor,
                            timePrefix = "${l2.time} "
                        )
                    } else {
                        views.setViewVisibility(R.id.item_sg2_container, View.GONE)
                    }
                }
            }
        }

        // Fill-in intent to open MainActivity on this day when a lesson is clicked
        val fillInIntent = Intent().apply {
            putExtra(MainActivity.EXTRA_TAB_INDEX, 0)
            putExtra(MainActivity.EXTRA_DAY_INDEX, targetDayIdx)
            putExtra(MainActivity.EXTRA_TARGET_TYPE, targetType)
            putExtra(MainActivity.EXTRA_TARGET_NAME, targetName)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}

private fun areLessonsIdenticalForWidget(l1: Lesson, l2: Lesson): Boolean {
    if (l1.subgroups.size != l2.subgroups.size) return false
    for (i in l1.subgroups.indices) {
        val sg1 = l1.subgroups[i]
        val sg2 = l2.subgroups[i]
        val subj1 = sg1.subject?.trim()?.lowercase() ?: ""
        val subj2 = sg2.subject?.trim()?.lowercase() ?: ""
        if (subj1 != subj2) return false

        val teacher1 = sg1.teacher?.trim()?.lowercase() ?: ""
        val teacher2 = sg2.teacher?.trim()?.lowercase() ?: ""
        if (teacher1 != teacher2) return false

        val class1 = sg1.className?.trim()?.lowercase() ?: ""
        val class2 = sg2.className?.trim()?.lowercase() ?: ""
        if (class1 != class2) return false
    }
    return true
}

private fun evaluateWidgetSubgroupChange(
    sg: Subgroup,
    changes: List<ScheduleChangeItem>,
    allSubgroups: List<Subgroup>
): Pair<Boolean, Boolean> { // Pair(isCancelled, isReplaced)
    val sgChange = findWidgetChangeForSubgroup(sg, changes, allSubgroups)
    if (sgChange != null) {
        if (sgChange.isCancelledLesson) {
            return Pair(true, false)
        }
        val normSgSubj = sg.subject?.let { ru.nstu.lyceum.schedule.data.api.normalizeSubject(it).lowercase().trim() } ?: ""
        val normChangeSubj = sgChange.subject?.let { ru.nstu.lyceum.schedule.data.api.normalizeSubject(it).lowercase().trim() } ?: ""
        val sameSubject = normSgSubj.isNotEmpty() && normChangeSubj.isNotEmpty() && normSgSubj == normChangeSubj

        val normSgTeacher = sg.teacher?.lowercase()?.trim() ?: ""
        val normChangeTeacher = sgChange.teacher?.lowercase()?.trim() ?: ""
        val sameTeacher = normSgTeacher.isEmpty() || normChangeTeacher.isEmpty() || normSgTeacher == normChangeTeacher

        val isSameAsBase = sameSubject && sameTeacher
        return if (isSameAsBase) {
            Pair(false, false)
        } else {
            Pair(false, true)
        }
    } else {
        return Pair(false, false)
    }
}

private fun renderSubgroup(
    views: RemoteViews,
    subjectViewId: Int,
    subViewId: Int,
    roomViewId: Int,
    sg: Subgroup,
    sgChange: ScheduleChangeItem?,
    isCancelled: Boolean,
    isReplaced: Boolean,
    lessonDefaultSubj: String,
    isTeacherMode: Boolean,
    subjColor: Int,
    changeColor: Int,
    cancelColor: Int,
    teacherColor: Int,
    groupColor: Int,
    roomColor: Int,
    classColor: Int,
    timePrefix: String = ""
) {
    val normSgSubj = sg.subject?.let { ru.nstu.lyceum.schedule.data.api.normalizeSubject(it).lowercase().trim() } ?: ""
    val normChangeSubj = sgChange?.subject?.let { ru.nstu.lyceum.schedule.data.api.normalizeSubject(it).lowercase().trim() } ?: ""
    val isSubjectChanged = !isCancelled && sgChange != null && normChangeSubj.isNotEmpty() && normSgSubj != normChangeSubj

    val normSgTeacher = sg.teacher?.lowercase()?.trim() ?: ""
    val normChangeTeacher = sgChange?.teacher?.lowercase()?.trim() ?: ""
    val isTeacherChanged = !isCancelled && sgChange != null && normChangeTeacher.isNotEmpty() && normSgTeacher != normChangeTeacher

    val normSgRoom = sg.room?.trim()?.removePrefix("(")?.removeSuffix(")")?.lowercase() ?: ""
    val normChangeRoom = sgChange?.room?.trim()?.removePrefix("(")?.removeSuffix(")")?.lowercase() ?: ""
    val isRoomChanged = !isCancelled && sgChange != null && normChangeRoom.isNotEmpty() && normSgRoom != normChangeRoom

    val normSgClass = sg.className?.trim()?.lowercase() ?: ""
    val normChangeClass = sgChange?.className?.trim()?.lowercase() ?: ""
    val isClassChanged = !isCancelled && sgChange != null && normChangeClass.isNotEmpty() && normSgClass != normChangeClass

    val rawSubj = when {
        !sgChange?.subject.isNullOrBlank() && (isReplaced || isSubjectChanged) -> sgChange!!.subject!!
        else -> sg.subject ?: lessonDefaultSubj
    }
    val displaySubj = timePrefix + ru.nstu.lyceum.schedule.data.api.normalizeSubject(rawSubj)

    val currentSubjColor = when {
        isCancelled -> cancelColor
        isSubjectChanged -> changeColor
        else -> subjColor
    }

    if (isCancelled) {
        val spanSubj = SpannableString(displaySubj)
        spanSubj.setSpan(StrikethroughSpan(), 0, spanSubj.length, 0)
        views.setTextViewText(subjectViewId, spanSubj)
    } else {
        views.setTextViewText(subjectViewId, displaySubj)
    }
    views.setTextColor(subjectViewId, currentSubjColor)

    // Room on the right of the subject (wrap_content, never truncated)
    val displayRoom = when {
        !sgChange?.room.isNullOrBlank() -> sgChange!!.room!!
        else -> sg.room ?: ""
    }
    val roomStr = formatWidgetRoom(displayRoom)
    if (roomStr.isNotBlank()) {
        val currentRoomColor = when {
            isCancelled -> cancelColor
            isRoomChanged -> changeColor
            else -> roomColor
        }
        if (isCancelled) {
            val spanRoom = SpannableString(roomStr)
            spanRoom.setSpan(StrikethroughSpan(), 0, spanRoom.length, 0)
            views.setTextViewText(roomViewId, spanRoom)
        } else {
            views.setTextViewText(roomViewId, roomStr)
        }
        views.setTextColor(roomViewId, currentRoomColor)
        views.setViewVisibility(roomViewId, View.VISIBLE)
    } else {
        views.setViewVisibility(roomViewId, View.GONE)
    }

    // Subline formatting: Teacher / Class + Group (without room, full horizontal width)
    val displayTeacher = when {
        !sgChange?.teacher.isNullOrBlank() -> sgChange!!.teacher!!
        else -> sg.teacher ?: ""
    }
    val displayClass = when {
        !sgChange?.className.isNullOrBlank() -> sgChange!!.className!!
        else -> sg.className ?: ""
    }
    val groupStr = formatWidgetGroup(sg.label)

    val sb = SpannableStringBuilder()
    val mainWho = if (isTeacherMode) displayClass else displayTeacher
    val isWhoChanged = if (isTeacherMode) isClassChanged else isTeacherChanged

    if (mainWho.isNotBlank()) {
        val start = sb.length
        sb.append(mainWho)
        val end = sb.length
        if (isCancelled) {
            sb.setSpan(ForegroundColorSpan(cancelColor), start, end, 0)
            sb.setSpan(StrikethroughSpan(), start, end, 0)
        } else if (isWhoChanged) {
            sb.setSpan(ForegroundColorSpan(changeColor), start, end, 0)
            if (isTeacherMode) {
                sb.setSpan(StyleSpan(Typeface.BOLD), start, end, 0)
            } else {
                sb.setSpan(StyleSpan(Typeface.ITALIC), start, end, 0)
            }
        } else {
            sb.setSpan(ForegroundColorSpan(if (isTeacherMode) classColor else teacherColor), start, end, 0)
            if (isTeacherMode) {
                sb.setSpan(StyleSpan(Typeface.BOLD), start, end, 0)
            } else {
                sb.setSpan(StyleSpan(Typeface.ITALIC), start, end, 0)
            }
        }
    }

    if (groupStr.isNotBlank()) {
        val start = sb.length
        sb.append(if (sb.isNotEmpty()) " - $groupStr" else groupStr)
        val end = sb.length
        if (isCancelled) {
            sb.setSpan(ForegroundColorSpan(cancelColor), start, end, 0)
            sb.setSpan(StrikethroughSpan(), start, end, 0)
        } else {
            sb.setSpan(ForegroundColorSpan(groupColor), start, end, 0)
        }
    }

    views.setTextViewText(subViewId, sb)
}

private fun buildCancelledSubline(sg: Subgroup?, isTeacherMode: Boolean, cancelColor: Int, fallbackChange: ScheduleChangeItem? = null): SpannableStringBuilder {
    val sb = SpannableStringBuilder()
    val who = if (isTeacherMode) {
        sg?.className?.takeIf { it.isNotBlank() } ?: fallbackChange?.className ?: ""
    } else {
        sg?.teacher?.takeIf { it.isNotBlank() } ?: fallbackChange?.teacher ?: ""
    }
    val groupStr = formatWidgetGroup(sg?.label?.takeIf { it.isNotBlank() } ?: fallbackChange?.group ?: fallbackChange?.subgroup ?: fallbackChange?.label)

    if (who.isNotBlank()) sb.append(who)
    if (groupStr.isNotBlank()) sb.append(if (sb.isNotEmpty()) " - $groupStr" else groupStr)

    if (sb.isNotEmpty()) {
        sb.setSpan(ForegroundColorSpan(cancelColor), 0, sb.length, 0)
        sb.setSpan(StrikethroughSpan(), 0, sb.length, 0)
    }
    return sb
}

private fun formatWidgetGroup(label: String?): String {
    if (label.isNullOrBlank()) return ""
    val trimmed = label.trim()
    val digits = trimmed.filter { it.isDigit() }
    return if (digits.isNotEmpty()) "$digits группа" else trimmed
}

private fun formatWidgetRoom(room: String?): String {
    if (room.isNullOrBlank()) return ""
    val trimmed = room.trim().removePrefix("(").removeSuffix(")")
    return if (trimmed.isNotBlank()) "($trimmed)" else ""
}

private fun findWidgetChangeForSubgroup(
    sg: Subgroup,
    changesList: List<ScheduleChangeItem>,
    allSubgroups: List<Subgroup> = emptyList()
): ScheduleChangeItem? {
    if (changesList.isEmpty()) return null
    val sgDigits = sg.label?.filter { it.isDigit() }
    val byGroup = changesList.firstOrNull { c ->
        val cGrp = c.group ?: c.subgroup ?: c.label
        val cDigits = cGrp?.filter { it.isDigit() }
        !cDigits.isNullOrEmpty() && !sgDigits.isNullOrEmpty() && cDigits == sgDigits
    }
    if (byGroup != null) return byGroup

    val candidatesWithoutOtherGroup = changesList.filter { c ->
        val cGrp = c.group ?: c.subgroup ?: c.label
        val cDigits = cGrp?.filter { it.isDigit() }
        cDigits.isNullOrEmpty() || (sgDigits != null && cDigits == sgDigits)
    }
    if (candidatesWithoutOtherGroup.isEmpty()) return null

    if (!sg.teacher.isNullOrBlank()) {
        val byTeacher = candidatesWithoutOtherGroup.firstOrNull { c ->
            c.teacher?.trim()?.equals(sg.teacher.trim(), ignoreCase = true) == true
        }
        if (byTeacher != null) return byTeacher
    }

    if (!sg.room.isNullOrBlank()) {
        val byRoom = candidatesWithoutOtherGroup.firstOrNull { c ->
            c.room?.trim()?.equals(sg.room.trim(), ignoreCase = true) == true
        }
        if (byRoom != null) return byRoom
    }

    val otherSubgroups = allSubgroups.filter { it != sg }
    val belongsToOther = candidatesWithoutOtherGroup.any { c ->
        otherSubgroups.any { otherSg ->
            (!c.teacher.isNullOrBlank() && c.teacher?.trim()?.equals(otherSg.teacher?.trim(), ignoreCase = true) == true) ||
            (!c.room.isNullOrBlank() && c.room?.trim()?.equals(otherSg.room?.trim(), ignoreCase = true) == true)
        }
    }
    if (belongsToOther) return null

    if (allSubgroups.size <= 1 && candidatesWithoutOtherGroup.size == 1) {
        return candidatesWithoutOtherGroup[0]
    }
    return null
}
