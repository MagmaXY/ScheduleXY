package ru.nstu.lyceum.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import ru.nstu.lyceum.schedule.data.api.Lesson
import ru.nstu.lyceum.schedule.data.api.ScheduleChangeItem
import ru.nstu.lyceum.schedule.data.api.Subgroup
import ru.nstu.lyceum.schedule.domain.PairSchedule
import ru.nstu.lyceum.schedule.ui.theme.NikaTheme

@Composable
fun PairCard(
    pairUi: ScheduleItemUi.CombinedPair,
    changesL1: List<ScheduleChangeItem>,
    changesL2: List<ScheduleChangeItem>,
    isActiveNow: Boolean,
    isActiveL1: Boolean = false,
    isActiveL2: Boolean = false,
    isTeacherMode: Boolean,
    notes: List<ru.nstu.lyceum.schedule.data.local.ScheduleNote> = emptyList(),
    onToggleNoteCompleted: (String) -> Unit = {},
    onTeacherClassClick: ((String) -> Unit)? = null,
    onRoomClick: ((room: String, lessonNum: Int) -> Unit)? = null,
    onLessonClick: ((lessonNum: Int, subject: String) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val nika = NikaTheme.colors

    val l1 = pairUi.firstLesson
    val l2 = pairUi.secondLesson

    // Check if both lessons have identical subject/groups
    val areSubjectsIdentical = SchedulePairMerger.areLessonsIdentical(l1, l2, changesL1, changesL2)

    val (splitTimes1, splitTimes2) = remember(pairUi.pairNum, pairUi.pairTimes) {
        PairSchedule.getSplitLessonTimes(pairUi.pairNum, pairUi.pairTimes)
    }

    val (seg1Min, seg2Min) = remember(pairUi.pairNum, pairUi.pairTimes) {
        PairSchedule.getSplitLessonMinutes(pairUi.pairNum, pairUi.pairTimes)
    }

    val cal = Calendar.getInstance()
    val curMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val isSeg1Active = isActiveL1 || (isActiveNow && !areSubjectsIdentical && curMin in seg1Min.first until seg1Min.second)
    val isSeg2Active = isActiveL2 || (isActiveNow && !areSubjectsIdentical && curMin in seg2Min.first until seg2Min.second)

    val allChanges = changesL1.ifEmpty { changesL2 }
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
                val c = findChangeForSubgroup(sg, allChanges, l1.subgroups)
                c?.isCancelledLesson == true
            }
        }
    }
    val hasReplacement = areSubjectsIdentical && (changesL1.isNotEmpty() || changesL2.isNotEmpty()) && !isTotalCancelled

    val itemBg = when {
        isTotalCancelled -> nika.cancelBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        isActiveNow && areSubjectsIdentical -> colorScheme.primaryContainer.copy(alpha = 0.35f)
        hasReplacement -> nika.changeBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        else -> Color.Transparent
    }

    val l1IsCancelled = when {
        changesL1.isEmpty() -> false
        l1.subgroups.isEmpty() -> changesL1.any { it.isCancelledLesson }
        l1.subgroups.size <= 1 -> changesL1.any { it.isCancelledLesson }
        else -> {
            val hasGeneralCancel = changesL1.any {
                it.isCancelledLesson && it.group.isNullOrBlank() && it.subgroup.isNullOrBlank() && it.label.isNullOrBlank()
            }
            if (hasGeneralCancel) true
            else l1.subgroups.all { sg ->
                val c = findChangeForSubgroup(sg, changesL1, l1.subgroups)
                c?.isCancelledLesson == true
            }
        }
    }
    val l1HasChange = changesL1.isNotEmpty() && !l1IsCancelled

    val l1Bg = when {
        l1IsCancelled -> nika.cancelBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        isSeg1Active -> colorScheme.primaryContainer.copy(alpha = 0.35f)
        l1HasChange -> nika.changeBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        else -> Color.Transparent
    }

    val l2IsCancelled = when {
        changesL2.isEmpty() -> false
        l2.subgroups.isEmpty() -> changesL2.any { it.isCancelledLesson }
        l2.subgroups.size <= 1 -> changesL2.any { it.isCancelledLesson }
        else -> {
            val hasGeneralCancel = changesL2.any {
                it.isCancelledLesson && it.group.isNullOrBlank() && it.subgroup.isNullOrBlank() && it.label.isNullOrBlank()
            }
            if (hasGeneralCancel) true
            else l2.subgroups.all { sg ->
                val c = findChangeForSubgroup(sg, changesL2, l2.subgroups)
                c?.isCancelledLesson == true
            }
        }
    }
    val l2HasChange = changesL2.isNotEmpty() && !l2IsCancelled

    val l2Bg = when {
        l2IsCancelled -> nika.cancelBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        isSeg2Active -> colorScheme.primaryContainer.copy(alpha = 0.35f)
        l2HasChange -> nika.changeBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        else -> Color.Transparent
    }

    val cardClickModifier = if (areSubjectsIdentical) {
        if (onLessonClick != null) {
            Modifier.clickable { onLessonClick(l1.num, l1.displaySubject) }
        } else if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else Modifier
    } else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemBg)
            .then(cardClickModifier)
    ) {
        if (areSubjectsIdentical) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Roman Pair Number (I, II, III, IV)
                Text(
                    text = pairUi.pairRoman,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = nika.lsnNum,
                    modifier = Modifier.width(22.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // 2. Pair Overall Time (stacked: e.g. 8:30 / 10:00)
                Column(
                    modifier = Modifier.width(48.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val times = pairUi.pairTimes.split("–", "-")
                    Text(
                        text = times.getOrNull(0)?.trim() ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = nika.lsnTime
                    )
                    Text(
                        text = times.getOrNull(1)?.trim() ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = nika.lsnTime
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 3. Content Column
                Column(modifier = Modifier.weight(1f)) {
                    if (isTotalCancelled) {
                        if (l1.subgroups.isEmpty()) {
                            val cancelChange = allChanges.firstOrNull { it.isCancelledLesson }
                            renderSingleCancelledRow(
                                subject = cancelChange?.subject?.takeIf { it.isNotBlank() && it != "F" && it != "$" && !it.startsWith("$") } ?: "Пара отменена",
                                teacher = cancelChange?.teacher,
                                room = cancelChange?.room,
                                className = cancelChange?.className,
                                group = cancelChange?.group ?: cancelChange?.subgroup ?: cancelChange?.label,
                                isTeacherMode = isTeacherMode,
                                lessonNum = l1.num,
                                nika = nika,
                                colorScheme = colorScheme,
                                onTeacherClassClick = onTeacherClassClick,
                                onRoomClick = onRoomClick
                            )
                        } else {
                            l1.subgroups.forEachIndexed { index, sg ->
                                if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                                renderSubgroupRow(
                                    sg = sg,
                                    changes = allChanges,
                                    allSubgroups = l1.subgroups,
                                    isTeacherMode = isTeacherMode,
                                    lessonNum = l1.num,
                                    nika = nika,
                                    colorScheme = colorScheme,
                                    onTeacherClassClick = onTeacherClassClick,
                                    onRoomClick = onRoomClick,
                                    forceCancelled = true
                                )
                            }
                        }
                    } else {
                        l1.subgroups.forEachIndexed { index, sg ->
                            if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                            renderSubgroupRow(sg, allChanges, l1.subgroups, isTeacherMode, l1.num, nika, colorScheme, onTeacherClassClick, onRoomClick)
                        }
                    }
                }
            }
        } else {
            // Split pair into 2 lessons: Time split into 2 compact groups of 2 numbers right after Roman pair number
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Roman Pair Number: Centered vertically across both lessons
                Text(
                    text = pairUi.pairRoman,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = nika.lsnNum,
                    modifier = Modifier.width(22.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // 2. Both split lessons in a Column
                Column(modifier = Modifier.weight(1f)) {
                    // Lesson 1 Row
                    val l1ClickMod = if (onLessonClick != null) {
                        Modifier.clickable { onLessonClick(l1.num, l1.displaySubject) }
                    } else if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else Modifier

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(l1Bg, shape = RoundedCornerShape(6.dp))
                            .then(l1ClickMod)
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lesson 1 Time: compact group of 2 numbers
                        Column(
                            modifier = Modifier.width(48.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = splitTimes1.first,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = nika.lsnTime
                            )
                            Text(
                                text = splitTimes1.second,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = nika.lsnTime
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Lesson 1 Content
                        Column(modifier = Modifier.weight(1f)) {
                            if (l1IsCancelled) {
                                if (l1.subgroups.isEmpty()) {
                                    val cancelChange = changesL1.firstOrNull { it.isCancelledLesson }
                                    renderSingleCancelledRow(
                                        subject = cancelChange?.subject?.takeIf { it.isNotBlank() && it != "F" && it != "$" && !it.startsWith("$") } ?: "Урок отменен",
                                        teacher = cancelChange?.teacher,
                                        room = cancelChange?.room,
                                        className = cancelChange?.className,
                                        group = cancelChange?.group ?: cancelChange?.subgroup ?: cancelChange?.label,
                                        isTeacherMode = isTeacherMode,
                                        lessonNum = l1.num,
                                        nika = nika,
                                        colorScheme = colorScheme,
                                        onTeacherClassClick = onTeacherClassClick,
                                        onRoomClick = onRoomClick
                                    )
                                } else {
                                    l1.subgroups.forEachIndexed { index, sg ->
                                        if (index > 0) Spacer(modifier = Modifier.height(3.dp))
                                        renderSubgroupRow(
                                            sg = sg,
                                            changes = changesL1,
                                            allSubgroups = l1.subgroups,
                                            isTeacherMode = isTeacherMode,
                                            lessonNum = l1.num,
                                            nika = nika,
                                            colorScheme = colorScheme,
                                            onTeacherClassClick = onTeacherClassClick,
                                            onRoomClick = onRoomClick,
                                            forceCancelled = true
                                        )
                                    }
                                }
                            } else {
                                l1.subgroups.forEachIndexed { index, sg ->
                                    if (index > 0) Spacer(modifier = Modifier.height(3.dp))
                                    renderSubgroupRow(sg, changesL1, l1.subgroups, isTeacherMode, l1.num, nika, colorScheme, onTeacherClassClick, onRoomClick)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Lesson 2 Row
                    val l2ClickMod = if (onLessonClick != null) {
                        Modifier.clickable { onLessonClick(l2.num, l2.displaySubject) }
                    } else if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else Modifier

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(l2Bg, shape = RoundedCornerShape(6.dp))
                            .then(l2ClickMod)
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lesson 2 Time: compact group of 2 numbers
                        Column(
                            modifier = Modifier.width(48.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = splitTimes2.first,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = nika.lsnTime
                            )
                            Text(
                                text = splitTimes2.second,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = nika.lsnTime
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Lesson 2 Content
                        Column(modifier = Modifier.weight(1f)) {
                            if (l2IsCancelled) {
                                if (l2.subgroups.isEmpty()) {
                                    val cancelChange = changesL2.firstOrNull { it.isCancelledLesson }
                                    renderSingleCancelledRow(
                                        subject = cancelChange?.subject?.takeIf { it.isNotBlank() && it != "F" && it != "$" && !it.startsWith("$") } ?: "Урок отменен",
                                        teacher = cancelChange?.teacher,
                                        room = cancelChange?.room,
                                        className = cancelChange?.className,
                                        group = cancelChange?.group ?: cancelChange?.subgroup ?: cancelChange?.label,
                                        isTeacherMode = isTeacherMode,
                                        lessonNum = l2.num,
                                        nika = nika,
                                        colorScheme = colorScheme,
                                        onTeacherClassClick = onTeacherClassClick,
                                        onRoomClick = onRoomClick
                                    )
                                } else {
                                    l2.subgroups.forEachIndexed { index, sg ->
                                        if (index > 0) Spacer(modifier = Modifier.height(3.dp))
                                        renderSubgroupRow(
                                            sg = sg,
                                            changes = changesL2,
                                            allSubgroups = l2.subgroups,
                                            isTeacherMode = isTeacherMode,
                                            lessonNum = l2.num,
                                            nika = nika,
                                            colorScheme = colorScheme,
                                            onTeacherClassClick = onTeacherClassClick,
                                            onRoomClick = onRoomClick,
                                            forceCancelled = true
                                        )
                                    }
                                }
                            } else {
                                l2.subgroups.forEachIndexed { index, sg ->
                                    if (index > 0) Spacer(modifier = Modifier.height(3.dp))
                                    renderSubgroupRow(sg, changesL2, l2.subgroups, isTeacherMode, l2.num, nika, colorScheme, onTeacherClassClick, onRoomClick)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (notes.isNotEmpty()) {
            LessonNotesSection(
                notes = notes,
                onToggleCompleted = onToggleNoteCompleted
            )
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun renderSubgroupRow(
    sg: Subgroup,
    changes: List<ScheduleChangeItem>,
    allSubgroups: List<Subgroup>,
    isTeacherMode: Boolean,
    lessonNum: Int,
    nika: ru.nstu.lyceum.schedule.ui.theme.NikaThemeColors,
    colorScheme: androidx.compose.material3.ColorScheme,
    onTeacherClassClick: ((String) -> Unit)? = null,
    onRoomClick: ((room: String, lessonNum: Int) -> Unit)? = null,
    forceCancelled: Boolean = false
) {
    val sgChange = findChangeForSubgroup(sg, changes, allSubgroups)

    val isCancelled: Boolean
    val isSubjectChanged: Boolean
    val isTeacherChanged: Boolean
    val isRoomChanged: Boolean
    val isClassChanged: Boolean

    if (forceCancelled) {
        isCancelled = true
        isSubjectChanged = false
        isTeacherChanged = false
        isRoomChanged = false
        isClassChanged = false
    } else if (sgChange != null) {
        if (sgChange.isCancelledLesson) {
            isCancelled = true
            isSubjectChanged = false
            isTeacherChanged = false
            isRoomChanged = false
            isClassChanged = false
        } else {
            isCancelled = false
            val normSgSubj = sg.subject?.let { ru.nstu.lyceum.schedule.data.api.normalizeSubject(it).lowercase().trim() } ?: ""
            val normChangeSubj = sgChange.subject?.let { ru.nstu.lyceum.schedule.data.api.normalizeSubject(it).lowercase().trim() } ?: ""
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
        }
    } else {
        isCancelled = false
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
    val displaySubj = ru.nstu.lyceum.schedule.data.api.normalizeSubject(rawSubj)
    val displayTeacher = when {
        !sgChange?.teacher.isNullOrBlank() -> sgChange!!.teacher
        !sg.teacher.isNullOrBlank() -> sg.teacher
        allSubgroups.size <= 1 -> changes.firstOrNull { !it.teacher.isNullOrBlank() }?.teacher
        else -> null
    }
    val displayRoom = when {
        !sgChange?.room.isNullOrBlank() -> sgChange!!.room
        !sg.room.isNullOrBlank() -> sg.room
        allSubgroups.size <= 1 -> changes.firstOrNull { !it.room.isNullOrBlank() }?.room
        else -> null
    }
    val displayClass = when {
        !sgChange?.className.isNullOrBlank() -> sgChange!!.className
        !sg.className.isNullOrBlank() -> sg.className
        allSubgroups.size <= 1 -> changes.firstOrNull { !it.className.isNullOrBlank() }?.className
        else -> null
    }

    val subjectColor = when {
        isCancelled -> nika.cancel
        isSubjectChanged -> nika.change
        else -> nika.subject
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = displaySubj,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp
            ),
            color = subjectColor,
            textDecoration = if (isCancelled) TextDecoration.LineThrough else null
        )

        Spacer(modifier = Modifier.height(1.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTeacherMode) {
                val className = displayClass?.takeIf { it.isNotBlank() }
                if (className != null) {
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
                                    Modifier.clickable { onTeacherClassClick(className) }
                                } else Modifier
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = className,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = when {
                                isCancelled -> nika.cancel
                                isClassChanged -> nika.change
                                else -> nika.teacher
                            },
                            textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                        )
                    }
                }

                val group = formatGroup(sg.label)
                if (group.isNotEmpty()) {
                    Text(
                        text = " - $group",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (isCancelled) nika.cancel else nika.group,
                        textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                    )
                }

                val room = formatRoom(displayRoom)
                val cleanRoom = displayRoom?.trim()?.removePrefix("(")?.removeSuffix(")") ?: ""
                if (room.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
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
                                    Modifier.clickable { onRoomClick(cleanRoom, lessonNum) }
                                } else Modifier
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = room,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = when {
                                isCancelled -> nika.cancel
                                isRoomChanged -> nika.change
                                else -> nika.room
                            },
                            textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                        )
                    }
                }
            } else {
                val teacher = displayTeacher?.takeIf { it.isNotBlank() }
                if (teacher != null) {
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
                                    Modifier.clickable { onTeacherClassClick(teacher) }
                                } else Modifier
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = teacher,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp
                            ),
                            color = when {
                                isCancelled -> nika.cancel
                                isTeacherChanged -> nika.change
                                else -> nika.teacher
                            },
                            textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                        )
                    }
                }

                val group = formatGroup(sg.label)
                if (group.isNotEmpty()) {
                    Text(
                        text = " - $group",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (isCancelled) nika.cancel else nika.group,
                        textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                    )
                }

                val room = formatRoom(displayRoom)
                val cleanRoom = displayRoom?.trim()?.removePrefix("(")?.removeSuffix(")") ?: ""
                if (room.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
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
                                    Modifier.clickable { onRoomClick(cleanRoom, lessonNum) }
                                } else Modifier
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = room,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = when {
                                isCancelled -> nika.cancel
                                isRoomChanged -> nika.change
                                else -> nika.room
                            },
                            textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun renderSingleCancelledRow(
    subject: String,
    teacher: String?,
    room: String?,
    className: String?,
    group: String?,
    isTeacherMode: Boolean,
    lessonNum: Int,
    nika: ru.nstu.lyceum.schedule.ui.theme.NikaThemeColors,
    colorScheme: androidx.compose.material3.ColorScheme,
    onTeacherClassClick: ((String) -> Unit)? = null,
    onRoomClick: ((room: String, lessonNum: Int) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = ru.nstu.lyceum.schedule.data.api.normalizeSubject(subject),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp
            ),
            color = nika.cancel,
            textDecoration = TextDecoration.LineThrough
        )

        val who = if (isTeacherMode) className?.takeIf { it.isNotBlank() } else teacher?.takeIf { it.isNotBlank() }
        val grp = formatGroup(group)
        val formattedRoom = formatRoom(room)
        val cleanRoom = room?.trim()?.removePrefix("(")?.removeSuffix(")") ?: ""

        if (who != null || grp.isNotEmpty() || formattedRoom.isNotEmpty()) {
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = who,
                            style = if (isTeacherMode) {
                                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, fontSize = 12.sp)
                            },
                            color = nika.cancel,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }

                if (grp.isNotEmpty()) {
                    Text(
                        text = if (who != null) " - $grp" else grp,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = nika.cancel,
                        textDecoration = TextDecoration.LineThrough
                    )
                }

                if (formattedRoom.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
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
                                    Modifier.clickable { onRoomClick(cleanRoom, lessonNum) }
                                } else Modifier
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = formattedRoom,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = nika.cancel,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}
