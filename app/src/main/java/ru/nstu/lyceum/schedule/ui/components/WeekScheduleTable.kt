package ru.nstu.lyceum.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.Lesson
import ru.nstu.lyceum.schedule.data.api.ScheduleChangeItem
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.Subgroup
import ru.nstu.lyceum.schedule.data.api.normalizeSubject
import ru.nstu.lyceum.schedule.data.local.NoteType
import ru.nstu.lyceum.schedule.data.local.ScheduleNote
import ru.nstu.lyceum.schedule.data.local.SlotType
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.domain.PairSchedule
import ru.nstu.lyceum.schedule.ui.theme.NikaTheme
import ru.nstu.lyceum.schedule.ui.theme.NikaThemeColors
import ru.nstu.lyceum.schedule.ui.util.MarkdownRenderer
import ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper
import ru.nstu.lyceum.schedule.ui.util.ScheduleFormatter
import java.util.Calendar

private sealed class WeekTableRow {
    data object BeforeLessonsRow : WeekTableRow()
    data class PairRow(
        val pairNum: Int,
        val pairRoman: String,
        val pairTimes: String,
        val num1: Int,
        val num2: Int
    ) : WeekTableRow()

    data class SingleRow(
        val lessonNum: Int,
        val timeRange: String
    ) : WeekTableRow()

    data object AfterLessonsRow : WeekTableRow()
}

private val FULL_DAY_NAMES = listOf(
    "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekScheduleTable(
    schedule: ScheduleResponse?,
    weekStartFallbackOffset: Int = 0,
    isTeacherMode: Boolean,
    targetName: String,
    showPairsSchedule: Boolean,
    activeDayIndex: Int = -1,
    activeLessonNum: Int? = null,
    activePairNum: Int? = null,
    notesByDayIndex: Map<Int, List<ScheduleNote>> = emptyMap(),
    userSubgroup: Int = 1,
    userTechSubgroup: Int = 1,
    emptyText: String = "На эту неделю занятий нет",
    onToggleNoteCompleted: (String) -> Unit = {},
    onDayClick: (Int) -> Unit,
    onDateClick: (String) -> Unit,
    onTeacherClassClick: ((String) -> Unit)? = null,
    onRoomClick: ((dayIndex: Int, room: String, lessonNum: Int) -> Unit)? = null,
    onLessonCellClick: ((dayIndex: Int, lessonNum: Int) -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val nika = NikaTheme.colors
    val dividerColor = colorScheme.outlineVariant.copy(alpha = 0.7f)
    val rowHeaderWidth = 58.dp

    val dayLessonsMap = remember(schedule) {
        (0..5).associateWith { d ->
            schedule?.days?.firstOrNull { it.dayIndex == d }?.lessons ?: emptyList()
        }
    }

    val dayItemsMap = remember(dayLessonsMap, isTeacherMode, targetName, showPairsSchedule) {
        (0..5).associateWith { d ->
            SchedulePairMerger.processLessons(
                lessons = dayLessonsMap[d].orEmpty(),
                isTeacherMode = isTeacherMode,
                targetName = targetName,
                showPairsSetting = showPairsSchedule
            )
        }
    }

    val filteredLessonNotesMap = remember(notesByDayIndex, userSubgroup, userTechSubgroup) {
        (0..5).associateWith { d ->
            notesByDayIndex[d].orEmpty().filter { note ->
                note.slotType == SlotType.LESSON && note.lessonNum != null &&
                    (note.subgroup == null ||
                        note.subgroup == userSubgroup ||
                        (note.subject?.contains("технолог", ignoreCase = true) == true && note.subgroup == userTechSubgroup))
            }
        }
    }

    val beforeNotesMap = remember(notesByDayIndex) {
        (0..5).associateWith { d ->
            notesByDayIndex[d].orEmpty().firstOrNull { it.slotType == SlotType.BEFORE_LESSONS }
        }
    }
    val afterNotesMap = remember(notesByDayIndex) {
        (0..5).associateWith { d ->
            notesByDayIndex[d].orEmpty().firstOrNull { it.slotType == SlotType.AFTER_LESSONS }
        }
    }

    val rows = remember(dayLessonsMap, dayItemsMap, filteredLessonNotesMap, beforeNotesMap, afterNotesMap, showPairsSchedule) {
        buildList {
            if (beforeNotesMap.values.any { it != null }) {
                add(WeekTableRow.BeforeLessonsRow)
            }

            val pairedNumsInWeek = if (showPairsSchedule) {
                (0..5).flatMap { d ->
                    dayItemsMap[d].orEmpty()
                        .filterIsInstance<ScheduleItemUi.CombinedPair>()
                        .map { it.pairNum }
                }.toSet()
            } else {
                emptySet()
            }

            val allPresentNums = ((0..5).flatMap { d ->
                dayLessonsMap[d].orEmpty().map { it.num }
            } + (0..5).flatMap { d ->
                filteredLessonNotesMap[d].orEmpty().mapNotNull { it.lessonNum }
            }).toSet()

            val maxNum = allPresentNums.maxOrNull() ?: 0
            val addedPairs = mutableSetOf<Int>()

            for (num in 1..maxNum) {
                if (num in 1..8) {
                    val pairNum = (num + 1) / 2
                    if (pairNum in pairedNumsInWeek) {
                        if (pairNum !in addedPairs) {
                            addedPairs.add(pairNum)
                            add(
                                WeekTableRow.PairRow(
                                    pairNum = pairNum,
                                    pairRoman = PairSchedule.getPairRomanNum(pairNum),
                                    pairTimes = PairSchedule.getPairTimeRange(pairNum),
                                    num1 = pairNum * 2 - 1,
                                    num2 = pairNum * 2
                                )
                            )
                        }
                        continue
                    }
                }

                if (num in allPresentNums) {
                    val resolvedTime = (0..5).firstNotNullOfOrNull { d ->
                        dayLessonsMap[d]?.firstOrNull { it.num == num }?.time?.takeIf { it.isNotBlank() }
                    } ?: BellSchedule.BELLS.firstOrNull { it.num == num }?.timeRange ?: ""
                    add(WeekTableRow.SingleRow(lessonNum = num, timeRange = resolvedTime))
                }
            }

            if (afterNotesMap.values.any { it != null }) {
                add(WeekTableRow.AfterLessonsRow)
            }
        }
    }

    val weekStart = schedule?.weekStart
    val dayDateLabels = remember(weekStart, weekStartFallbackOffset) {
        (0..5).map { d -> ScheduleFormatter.getDayDateGenitiveLabel(weekStart, d, weekStartFallbackOffset) }
    }
    val dayDateIsos = remember(weekStart, weekStartFallbackOffset) {
        (0..5).map { d ->
            if (weekStart != null) {
                NotesTimeHelper.getDateIsoForDayIndex(weekStart, d)
            } else {
                val cal = Calendar.getInstance()
                val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                cal.add(Calendar.DAY_OF_MONTH, -dow + (weekStartFallbackOffset * 7) + d)
                String.format(
                    java.util.Locale.US,
                    "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
                )
            }
        }
    }

    if (rows.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Table Header Row: Empty corner cell + 6 Day Column Headers
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Top-left corner cell: intentionally empty (no "УРОК/ПАРА" label)
                Box(
                    modifier = Modifier
                        .width(rowHeaderWidth)
                        .fillMaxHeight()
                )

                for (dayIdx in 0..5) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .background(dividerColor)
                    )

                    val isToday = dayIdx == activeDayIndex
                    val isSaturday = dayIdx == 5
                    val dayName = FULL_DAY_NAMES[dayIdx]
                    val dateLabel = dayDateLabels[dayIdx]
                    val dateIso = dayDateIsos[dayIdx]

                    val dayNameColor = when {
                        isToday -> colorScheme.primary
                        isSaturday -> colorScheme.secondary
                        else -> colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (isToday) colorScheme.primaryContainer.copy(alpha = 0.28f) else Color.Transparent
                            )
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$dayName, ",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = dayNameColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onDayClick(dayIdx) }
                                    .padding(horizontal = 1.dp)
                            )
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                ),
                                color = if (isToday) colorScheme.primary else colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onDateClick(dateIso) }
                                    .padding(horizontal = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // Shared Left Row Header
                    Box(
                        modifier = Modifier
                            .width(rowHeaderWidth)
                            .fillMaxHeight()
                            .background(colorScheme.surface.copy(alpha = 0.6f))
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (row) {
                            is WeekTableRow.BeforeLessonsRow -> {
                                Text(
                                    text = "До\nуроков",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp,
                                        lineHeight = 12.sp
                                    ),
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            is WeekTableRow.AfterLessonsRow -> {
                                Text(
                                    text = "После\nуроков",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp,
                                        lineHeight = 12.sp
                                    ),
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            is WeekTableRow.PairRow -> {
                                TableRowHeaderContent(
                                    numLabel = row.pairRoman,
                                    timeRange = row.pairTimes,
                                    nika = nika
                                )
                            }

                            is WeekTableRow.SingleRow -> {
                                TableRowHeaderContent(
                                    numLabel = row.lessonNum.toString(),
                                    timeRange = row.timeRange,
                                    nika = nika
                                )
                            }
                        }
                    }

                    // 6 Day Columns sharing identical row height
                    for (dayIdx in 0..5) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(0.5.dp)
                                .background(dividerColor)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            when (row) {
                                is WeekTableRow.BeforeLessonsRow -> {
                                    val note = beforeNotesMap[dayIdx]
                                    if (note != null) {
                                        TableSlotNoteCell(
                                            note = note,
                                            onToggleCompleted = { onToggleNoteCompleted(note.id) }
                                        )
                                    }
                                }

                                is WeekTableRow.AfterLessonsRow -> {
                                    val note = afterNotesMap[dayIdx]
                                    if (note != null) {
                                        TableSlotNoteCell(
                                            note = note,
                                            onToggleCompleted = { onToggleNoteCompleted(note.id) }
                                        )
                                    }
                                }

                                is WeekTableRow.SingleRow -> {
                                    val lesson = dayLessonsMap[dayIdx]?.firstOrNull { it.num == row.lessonNum }
                                    val notes = filteredLessonNotesMap[dayIdx].orEmpty()
                                        .filter { it.lessonNum == row.lessonNum }
                                    val changes = schedule?.getChangesForLesson(dayIdx, row.lessonNum).orEmpty()
                                    val isToday = dayIdx == activeDayIndex
                                    val isActive = isToday && activeLessonNum == row.lessonNum

                                    if (lesson != null) {
                                        TableSingleLessonCell(
                                            lesson = lesson,
                                            changes = changes,
                                            isActiveNow = isActive,
                                            isTeacherMode = isTeacherMode,
                                            notes = notes,
                                            dayIdx = dayIdx,
                                            nika = nika,
                                            colorScheme = colorScheme,
                                            onToggleNoteCompleted = onToggleNoteCompleted,
                                            onTeacherClassClick = onTeacherClassClick,
                                            onRoomClick = onRoomClick,
                                            onCellClick = if (onLessonCellClick != null) {
                                                { onLessonCellClick(dayIdx, lesson.num) }
                                            } else null
                                        )
                                    } else if (notes.isNotEmpty()) {
                                        TableLessonNotesList(
                                            notes = notes,
                                            onToggleCompleted = onToggleNoteCompleted,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 5.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                is WeekTableRow.PairRow -> {
                                    val l1 = dayLessonsMap[dayIdx]?.firstOrNull { it.num == row.num1 }
                                    val l2 = dayLessonsMap[dayIdx]?.firstOrNull { it.num == row.num2 }
                                    val ch1 = schedule?.getChangesForLesson(dayIdx, row.num1).orEmpty()
                                    val ch2 = schedule?.getChangesForLesson(dayIdx, row.num2).orEmpty()
                                    val notes1 = filteredLessonNotesMap[dayIdx].orEmpty().filter { it.lessonNum == row.num1 }
                                    val notes2 = filteredLessonNotesMap[dayIdx].orEmpty().filter { it.lessonNum == row.num2 }
                                    val isToday = dayIdx == activeDayIndex
                                    val isPairCombinedOnDay = dayItemsMap[dayIdx].orEmpty()
                                        .any { it is ScheduleItemUi.CombinedPair && it.pairNum == row.pairNum }

                                    val (split1, split2) = remember(row.pairNum, row.pairTimes) {
                                        PairSchedule.getSplitLessonTimes(row.pairNum, row.pairTimes)
                                    }
                                    val (seg1Min, seg2Min) = remember(row.pairNum, row.pairTimes) {
                                        PairSchedule.getSplitLessonMinutes(row.pairNum, row.pairTimes)
                                    }
                                    val curMin = remember(activeLessonNum, activePairNum) {
                                        val cal = Calendar.getInstance()
                                        cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                                    }
                                    val isPairActive = isToday && (activePairNum == row.pairNum || activeLessonNum == row.num1 || activeLessonNum == row.num2)
                                    val isSeg1Active = isToday && (activeLessonNum == row.num1 || (isPairActive && curMin in seg1Min.first until seg1Min.second))
                                    val isSeg2Active = isToday && (activeLessonNum == row.num2 || (isPairActive && curMin in seg2Min.first until seg2Min.second))

                                    if (l1 != null && l2 != null && isPairCombinedOnDay &&
                                        SchedulePairMerger.areLessonsIdentical(l1, l2, ch1, ch2)
                                    ) {
                                        val allChanges = ch1.ifEmpty { ch2 }
                                        TableSingleLessonCell(
                                            lesson = l1,
                                            changes = allChanges,
                                            isActiveNow = isPairActive,
                                            isTeacherMode = isTeacherMode,
                                            notes = notes1 + notes2,
                                            dayIdx = dayIdx,
                                            nika = nika,
                                            colorScheme = colorScheme,
                                            emptyCancelText = "Пара отменена",
                                            onToggleNoteCompleted = onToggleNoteCompleted,
                                            onTeacherClassClick = onTeacherClassClick,
                                            onRoomClick = onRoomClick,
                                            onCellClick = if (onLessonCellClick != null) {
                                                { onLessonCellClick(dayIdx, l1.num) }
                                            } else null
                                        )
                                    } else if (l1 != null || l2 != null || notes1.isNotEmpty() || notes2.isNotEmpty()) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            if (l1 != null) {
                                                val halfTime = if (isPairCombinedOnDay) "${split1.first}–${split1.second}" else l1.time
                                                TableSingleLessonCell(
                                                    lesson = l1,
                                                    changes = ch1,
                                                    isActiveNow = isSeg1Active,
                                                    isTeacherMode = isTeacherMode,
                                                    notes = notes1,
                                                    dayIdx = dayIdx,
                                                    nika = nika,
                                                    colorScheme = colorScheme,
                                                    subTimeHeader = "${l1.num} ($halfTime)",
                                                    onToggleNoteCompleted = onToggleNoteCompleted,
                                                    onTeacherClassClick = onTeacherClassClick,
                                                    onRoomClick = onRoomClick,
                                                    onCellClick = if (onLessonCellClick != null) {
                                                        { onLessonCellClick(dayIdx, l1.num) }
                                                    } else null,
                                                    modifier = if (l2 == null && notes2.isEmpty()) Modifier.fillMaxHeight() else Modifier
                                                )
                                            } else if (notes1.isNotEmpty()) {
                                                TableLessonNotesList(
                                                    notes = notes1,
                                                    onToggleCompleted = onToggleNoteCompleted,
                                                    headerLabel = "${row.num1} (${split1.first}–${split1.second})",
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                                                )
                                            }

                                            if ((l1 != null || notes1.isNotEmpty()) && (l2 != null || notes2.isNotEmpty())) {
                                                HorizontalDivider(
                                                    thickness = 0.5.dp,
                                                    color = dividerColor.copy(alpha = 0.5f)
                                                )
                                            }

                                            if (l2 != null) {
                                                val halfTime = if (isPairCombinedOnDay) "${split2.first}–${split2.second}" else l2.time
                                                TableSingleLessonCell(
                                                    lesson = l2,
                                                    changes = ch2,
                                                    isActiveNow = isSeg2Active,
                                                    isTeacherMode = isTeacherMode,
                                                    notes = notes2,
                                                    dayIdx = dayIdx,
                                                    nika = nika,
                                                    colorScheme = colorScheme,
                                                    subTimeHeader = "${l2.num} ($halfTime)",
                                                    onToggleNoteCompleted = onToggleNoteCompleted,
                                                    onTeacherClassClick = onTeacherClassClick,
                                                    onRoomClick = onRoomClick,
                                                    onCellClick = if (onLessonCellClick != null) {
                                                        { onLessonCellClick(dayIdx, l2.num) }
                                                    } else null,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else if (notes2.isNotEmpty()) {
                                                TableLessonNotesList(
                                                    notes = notes2,
                                                    onToggleCompleted = onToggleNoteCompleted,
                                                    headerLabel = "${row.num2} (${split2.first}–${split2.second})",
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TableRowHeaderContent(
    numLabel: String,
    timeRange: String,
    nika: NikaThemeColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = numLabel,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            ),
            color = nika.lsnNum,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(18.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Column(horizontalAlignment = Alignment.Start) {
            val times = timeRange.split("–", "-")
            Text(
                text = times.getOrNull(0)?.trim() ?: timeRange,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                ),
                color = nika.lsnTime
            )
            val end = times.getOrNull(1)?.trim() ?: ""
            if (end.isNotEmpty()) {
                Text(
                    text = end,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    ),
                    color = nika.lsnTime
                )
            }
        }
    }
}

@Composable
private fun TableSlotNoteCell(
    note: ScheduleNote,
    onToggleCompleted: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val parsedText = MarkdownRenderer.parseMarkdown(note.text)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onToggleCompleted)
            .padding(horizontal = 5.dp, vertical = 5.dp)
    ) {
        Text(
            text = parsedText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (note.isCompleted) colorScheme.outlineVariant else colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableSingleLessonCell(
    lesson: Lesson,
    changes: List<ScheduleChangeItem>,
    isActiveNow: Boolean,
    isTeacherMode: Boolean,
    notes: List<ScheduleNote>,
    dayIdx: Int,
    nika: NikaThemeColors,
    colorScheme: ColorScheme,
    subTimeHeader: String? = null,
    emptyCancelText: String = "Урок отменен",
    onToggleNoteCompleted: (String) -> Unit,
    onTeacherClassClick: ((String) -> Unit)?,
    onRoomClick: ((dayIndex: Int, room: String, lessonNum: Int) -> Unit)?,
    onCellClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isTotalCancelled: Boolean = when {
        changes.isEmpty() -> false
        lesson.subgroups.isEmpty() -> changes.any { it.isCancelledLesson }
        lesson.subgroups.size <= 1 -> changes.any { it.isCancelledLesson }
        else -> {
            val hasGeneralCancel = changes.any {
                it.isCancelledLesson && it.group.isNullOrBlank() && it.subgroup.isNullOrBlank() && it.label.isNullOrBlank()
            }
            if (hasGeneralCancel) true
            else lesson.subgroups.all { sg ->
                findChangeForSubgroup(sg, changes, lesson.subgroups)?.isCancelledLesson == true
            }
        }
    }
    val hasReplacement = !isTotalCancelled && changes.isNotEmpty()

    val cellBg = when {
        isTotalCancelled -> nika.cancelBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        isActiveNow -> colorScheme.primaryContainer.copy(alpha = 0.35f)
        hasReplacement -> nika.changeBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (subTimeHeader == null) Modifier.fillMaxHeight() else Modifier)
            .background(cellBg)
            .then(if (onCellClick != null) Modifier.clickable(onClick = onCellClick) else Modifier)
            .padding(horizontal = 5.dp, vertical = 5.dp)
    ) {
        if (!subTimeHeader.isNullOrBlank()) {
            Text(
                text = subTimeHeader,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.sp
                ),
                color = nika.lsnTime
            )
            Spacer(modifier = Modifier.height(1.dp))
        }

        when {
            lesson.subgroups.isEmpty() && !isTotalCancelled -> {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.outline
                )
            }

            isTotalCancelled && lesson.subgroups.isEmpty() -> {
                val cancelChange = changes.firstOrNull { it.isCancelledLesson }
                val rawSubj = cancelChange?.subject?.takeIf {
                    it.isNotBlank() && it != "F" && it != "$" && !it.startsWith("$")
                } ?: emptyCancelText
                Text(
                    text = normalizeSubject(rawSubj),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        lineHeight = 13.5.sp
                    ),
                    color = nika.cancel,
                    textDecoration = TextDecoration.LineThrough
                )
                val who = if (isTeacherMode) cancelChange?.className?.takeIf { it.isNotBlank() } else cancelChange?.teacher?.takeIf { it.isNotBlank() }
                val grp = formatGroup(cancelChange?.group ?: cancelChange?.subgroup ?: cancelChange?.label)
                val formattedRoom = formatRoom(cancelChange?.room)
                val cleanRoom = cancelChange?.room?.trim()?.removePrefix("(")?.removeSuffix(")") ?: ""
                if (who != null || grp.isNotEmpty() || formattedRoom.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    TableMetadataFlowRow(
                        who = who,
                        whoColor = nika.cancel,
                        isTeacherMode = isTeacherMode,
                        isCancelled = true,
                        groupText = grp,
                        formattedRoom = formattedRoom,
                        cleanRoom = cleanRoom,
                        roomColor = nika.cancel,
                        lessonNum = lesson.num,
                        dayIdx = dayIdx,
                        nika = nika,
                        colorScheme = colorScheme,
                        onTeacherClassClick = onTeacherClassClick,
                        onRoomClick = onRoomClick
                    )
                }
            }

            else -> {
                lesson.subgroups.forEachIndexed { index, sg ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    val sgChange = findChangeForSubgroup(sg, changes, lesson.subgroups)

                    val isSgCancelled: Boolean
                    val isSubjectChanged: Boolean
                    val isTeacherChanged: Boolean
                    val isRoomChanged: Boolean
                    val isClassChanged: Boolean

                    if (isTotalCancelled || sgChange?.isCancelledLesson == true) {
                        isSgCancelled = true
                        isSubjectChanged = false
                        isTeacherChanged = false
                        isRoomChanged = false
                        isClassChanged = false
                    } else if (sgChange != null) {
                        isSgCancelled = false
                        val normSgSubj = sg.subject?.let { normalizeSubject(it).lowercase().trim() } ?: ""
                        val normChangeSubj = sgChange.subject?.let { normalizeSubject(it).lowercase().trim() } ?: ""
                        isSubjectChanged = normChangeSubj.isNotEmpty() && normSgSubj != normChangeSubj

                        val normSgTeacher = sg.teacher?.lowercase()?.trim() ?: ""
                        val normChangeTeacher = sgChange.teacher?.lowercase()?.trim() ?: ""
                        isTeacherChanged = normChangeTeacher.isNotEmpty() && normSgTeacher != normChangeTeacher

                        val normSgRoom = sg.room?.trim()?.removePrefix("(")?.removeSuffix(")")?.lowercase() ?: ""
                        val normChangeRoom = sgChange.room?.trim()?.removePrefix("(")?.removeSuffix(")")?.lowercase() ?: ""
                        isRoomChanged = normChangeRoom.isNotEmpty() && normSgRoom != normChangeRoom

                        val normSgClass = sg.className?.trim()?.lowercase() ?: ""
                        val normChangeClass = sgChange.className?.trim()?.lowercase() ?: ""
                        isClassChanged = normChangeClass.isNotEmpty() && normSgClass != normChangeClass
                    } else {
                        isSgCancelled = false
                        isSubjectChanged = false
                        isTeacherChanged = false
                        isRoomChanged = false
                        isClassChanged = false
                    }

                    val rawSubj = when {
                        isSubjectChanged && !sgChange?.subject.isNullOrBlank() -> sgChange!!.subject!!
                        !sg.subject.isNullOrBlank() -> sg.subject!!
                        !sgChange?.subject.isNullOrBlank() && sgChange?.isCancelledLesson == false -> sgChange!!.subject!!
                        else -> "Без названия"
                    }
                    val displaySubj = normalizeSubject(rawSubj)
                    val displayTeacher = when {
                        !sgChange?.teacher.isNullOrBlank() -> sgChange!!.teacher
                        !sg.teacher.isNullOrBlank() -> sg.teacher
                        lesson.subgroups.size <= 1 -> changes.firstOrNull { !it.teacher.isNullOrBlank() }?.teacher
                        else -> null
                    }
                    val displayRoom = when {
                        !sgChange?.room.isNullOrBlank() -> sgChange!!.room
                        !sg.room.isNullOrBlank() -> sg.room
                        lesson.subgroups.size <= 1 -> changes.firstOrNull { !it.room.isNullOrBlank() }?.room
                        else -> null
                    }
                    val displayClass = when {
                        !sgChange?.className.isNullOrBlank() -> sgChange!!.className
                        !sg.className.isNullOrBlank() -> sg.className
                        lesson.subgroups.size <= 1 -> changes.firstOrNull { !it.className.isNullOrBlank() }?.className
                        else -> null
                    }

                    val subjectColor = when {
                        isSgCancelled -> nika.cancel
                        isSubjectChanged -> nika.change
                        else -> nika.subject
                    }

                    Text(
                        text = displaySubj,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            lineHeight = 13.5.sp
                        ),
                        color = subjectColor,
                        textDecoration = if (isSgCancelled) TextDecoration.LineThrough else null
                    )

                    val who = if (isTeacherMode) {
                        displayClass?.takeIf { it.isNotBlank() }
                    } else {
                        displayTeacher?.takeIf { it.isNotBlank() }
                    }
                    val whoColor = when {
                        isSgCancelled -> nika.cancel
                        isTeacherMode && isClassChanged -> nika.change
                        !isTeacherMode && isTeacherChanged -> nika.change
                        else -> nika.teacher
                    }
                    val grp = formatGroup(sg.label)
                    val formattedRoom = formatRoom(displayRoom)
                    val cleanRoom = displayRoom?.trim()?.removePrefix("(")?.removeSuffix(")") ?: ""
                    val roomColor = when {
                        isSgCancelled -> nika.cancel
                        isRoomChanged -> nika.change
                        else -> nika.room
                    }

                    if (who != null || grp.isNotEmpty() || formattedRoom.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        TableMetadataFlowRow(
                            who = who,
                            whoColor = whoColor,
                            isTeacherMode = isTeacherMode,
                            isCancelled = isSgCancelled,
                            groupText = grp,
                            formattedRoom = formattedRoom,
                            cleanRoom = cleanRoom,
                            roomColor = roomColor,
                            lessonNum = lesson.num,
                            dayIdx = dayIdx,
                            nika = nika,
                            colorScheme = colorScheme,
                            onTeacherClassClick = onTeacherClassClick,
                            onRoomClick = onRoomClick
                        )
                    }
                }
            }
        }

        if (notes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            TableLessonNotesList(
                notes = notes,
                onToggleCompleted = onToggleNoteCompleted
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableMetadataFlowRow(
    who: String?,
    whoColor: Color,
    isTeacherMode: Boolean,
    isCancelled: Boolean,
    groupText: String,
    formattedRoom: String,
    cleanRoom: String,
    roomColor: Color,
    lessonNum: Int,
    dayIdx: Int,
    nika: NikaThemeColors,
    colorScheme: ColorScheme,
    onTeacherClassClick: ((String) -> Unit)?,
    onRoomClick: ((dayIndex: Int, room: String, lessonNum: Int) -> Unit)?
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (who != null) {
            Box(
                modifier = Modifier
                    .border(
                        width = 0.5.dp,
                        color = colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .clip(RoundedCornerShape(3.dp))
                    .then(
                        if (onTeacherClassClick != null) {
                            Modifier.clickable { onTeacherClassClick(who) }
                        } else Modifier
                    )
                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = who,
                    style = if (isTeacherMode) {
                        MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    } else {
                        MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, fontSize = 10.sp)
                    },
                    color = whoColor,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                )
            }
        }

        if (groupText.isNotEmpty()) {
            Text(
                text = if (who != null) "- $groupText" else groupText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = if (isCancelled) nika.cancel else nika.group,
                textDecoration = if (isCancelled) TextDecoration.LineThrough else null
            )
        }

        if (formattedRoom.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .border(
                        width = 0.5.dp,
                        color = colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .clip(RoundedCornerShape(3.dp))
                    .then(
                        if (onRoomClick != null && cleanRoom.isNotBlank()) {
                            Modifier.clickable { onRoomClick(dayIdx, cleanRoom, lessonNum) }
                        } else Modifier
                    )
                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = formattedRoom,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = roomColor,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                )
            }
        }
    }
}

@Composable
private fun TableLessonNotesList(
    notes: List<ScheduleNote>,
    onToggleCompleted: (String) -> Unit,
    headerLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val sortedNotes = notes.sortedBy { if (it.type == NoteType.HOMEWORK) 0 else 1 }
    val colorScheme = MaterialTheme.colorScheme
    val nika = NikaTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (!headerLabel.isNullOrBlank()) {
            Text(
                text = headerLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.sp
                ),
                color = nika.lsnTime
            )
        }
        sortedNotes.forEach { note ->
            val prefix = if (note.type == NoteType.HOMEWORK) "ДЗ: " else "Заметка: "
            val parsedText = MarkdownRenderer.parseMarkdown(note.text)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCompleted(note.id) },
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    ),
                    color = if (note.isCompleted) colorScheme.outlineVariant else colorScheme.onSurfaceVariant
                )
                Text(
                    text = parsedText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (note.isCompleted) colorScheme.outlineVariant else colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
