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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.Lesson
import ru.nstu.lyceum.schedule.data.api.ScheduleChangeItem
import ru.nstu.lyceum.schedule.data.api.Subgroup
import ru.nstu.lyceum.schedule.ui.theme.NikaTheme

fun formatGroup(label: String?): String {
    if (label.isNullOrBlank()) return ""
    val trimmed = label.trim()
    val digits = trimmed.filter { it.isDigit() }
    return if (digits.isNotEmpty()) "$digits группа" else trimmed
}

fun formatRoom(room: String?): String {
    if (room.isNullOrBlank()) return ""
    val trimmed = room.trim().removePrefix("(").removeSuffix(")")
    return if (trimmed.isNotBlank()) "($trimmed)" else ""
}

/**
 * Matches a subgroup to a change item accurately, preventing group bleeding
 * when multiple groups share the same subject (e.g. Foreign Language / English).
 */
fun findChangeForSubgroup(
    sg: Subgroup,
    changes: List<ScheduleChangeItem>,
    allSubgroups: List<Subgroup> = emptyList()
): ScheduleChangeItem? {
    if (changes.isEmpty()) return null

    val sgNum = sg.label?.filter { it.isDigit() }
    val sgSubjNorm = sg.subject?.let { ru.nstu.lyceum.schedule.data.api.normalizeSubject(it).lowercase() } ?: ""
    val sgTeacherNorm = sg.teacher?.trim()?.lowercase() ?: ""
    val sgRoomNorm = sg.room?.trim()?.removePrefix("(")?.removeSuffix(")")?.lowercase() ?: ""

    // 1. Exact match by group number digits
    if (!sgNum.isNullOrBlank()) {
        val groupMatch = changes.firstOrNull { c ->
            val cNum = (c.group ?: c.subgroup ?: c.label)?.filter { it.isDigit() }
            cNum == sgNum
        }
        if (groupMatch != null) return groupMatch
    }

    // 2. Match by exact teacher name
    if (sgTeacherNorm.isNotBlank()) {
        val teacherMatch = changes.firstOrNull { c ->
            c.teacher?.trim()?.lowercase() == sgTeacherNorm
        }
        if (teacherMatch != null) return teacherMatch
    }

    // 3. Match by exact room
    if (sgRoomNorm.isNotBlank()) {
        val roomMatch = changes.firstOrNull { c ->
            val cRoom = c.room?.trim()?.removePrefix("(")?.removeSuffix(")")?.lowercase() ?: ""
            cRoom == sgRoomNorm
        }
        if (roomMatch != null) return roomMatch
    }

    // 4. Fallback for single change with no group digits:
    // If the change specifies a teacher or room that belongs to ANOTHER subgroup, DO NOT match.
    val singleChange = changes.firstOrNull { c ->
        val cNum = (c.group ?: c.subgroup ?: c.label)?.filter { it.isDigit() }
        cNum.isNullOrBlank()
    }

    if (singleChange != null && allSubgroups.size > 1) {
        val changeTeacher = singleChange.teacher?.trim()?.lowercase() ?: ""
        val changeRoom = singleChange.room?.trim()?.removePrefix("(")?.removeSuffix(")")?.lowercase() ?: ""

        val belongsToOther = allSubgroups.any { other ->
            if (other === sg) return@any false
            val otherTeacher = other.teacher?.trim()?.lowercase() ?: ""
            val otherRoom = other.room?.trim()?.removePrefix("(")?.removeSuffix(")")?.lowercase() ?: ""
            (changeTeacher.isNotBlank() && otherTeacher == changeTeacher) ||
            (changeRoom.isNotBlank() && otherRoom == changeRoom)
        }

        if (belongsToOther) {
            return null
        }
    }

    return singleChange
}

@Composable
fun LessonCard(
    lesson: Lesson,
    change: ScheduleChangeItem? = null,
    changes: List<ScheduleChangeItem> = emptyList(),
    isActiveNow: Boolean = false,
    isTeacherMode: Boolean = false,
    notes: List<ru.nstu.lyceum.schedule.data.local.ScheduleNote> = emptyList(),
    onToggleNoteCompleted: (String) -> Unit = {},
    onTeacherClassClick: ((String) -> Unit)? = null,
    onRoomClick: ((room: String, lessonNum: Int) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val nika = NikaTheme.colors
    val allChanges = if (changes.isNotEmpty()) changes else listOfNotNull(change)

    // Total cancellation: cancelled for the whole class or all subgroups
    val isTotalCancelled: Boolean = when {
        allChanges.isEmpty() -> false
        lesson.subgroups.isEmpty() -> allChanges.any { it.isCancelledLesson }
        lesson.subgroups.size <= 1 -> {
            allChanges.any { it.isCancelledLesson }
        }
        else -> {
            val hasGeneralCancel = allChanges.any {
                it.isCancelledLesson && it.group.isNullOrBlank() && it.subgroup.isNullOrBlank() && it.label.isNullOrBlank()
            }
            if (hasGeneralCancel) {
                true
            } else {
                lesson.subgroups.all { sg ->
                    val c = findChangeForSubgroup(sg, allChanges, lesson.subgroups)
                    c?.isCancelledLesson == true
                }
            }
        }
    }

    val hasReplacement: Boolean = !isTotalCancelled && allChanges.isNotEmpty()

    val itemBg = when {
        isTotalCancelled -> nika.cancelBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        isActiveNow -> colorScheme.primaryContainer.copy(alpha = 0.35f)
        hasReplacement -> nika.changeBg.copy(alpha = if (nika.isDark) 0.85f else 0.70f)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemBg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Lesson Number (plain text like NIKA)
            Text(
                text = lesson.num.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = nika.lsnNum,
                modifier = Modifier.width(22.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 2. Lesson Time (stacked start and end time in green like NIKA)
            Column(
                modifier = Modifier.width(48.dp),
                horizontalAlignment = Alignment.Start
            ) {
                val times = lesson.time.split("–", "-")
                Text(
                    text = times.getOrNull(0)?.trim() ?: lesson.time,
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

            // 3. Content Column (Subject, Teacher, Group, Room)
            Column(modifier = Modifier.weight(1f)) {
                when {
                    // Empty Lesson with no cancellation
                    lesson.subgroups.isEmpty() && !isTotalCancelled -> {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.outline
                        )
                    }

                    // Total Cancelled Lesson
                    isTotalCancelled -> {
                        if (lesson.subgroups.isEmpty()) {
                            val cancelChange = allChanges.firstOrNull { it.isCancelledLesson }
                            renderSingleCancelledLesson(
                                subject = cancelChange?.subject?.takeIf { it.isNotBlank() && it != "F" && it != "$" && !it.startsWith("$") } ?: "Урок отменен",
                                teacher = cancelChange?.teacher,
                                room = cancelChange?.room,
                                className = cancelChange?.className,
                                group = cancelChange?.group ?: cancelChange?.subgroup ?: cancelChange?.label,
                                isTeacherMode = isTeacherMode,
                                lessonNum = lesson.num,
                                nika = nika,
                                colorScheme = colorScheme,
                                onTeacherClassClick = onTeacherClassClick,
                                onRoomClick = onRoomClick
                            )
                        } else {
                            Text(
                                text = lesson.displaySubject.takeIf { it != "—" } ?: "Урок отменен",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                ),
                                color = nika.cancel,
                                textDecoration = TextDecoration.LineThrough
                            )

                            lesson.subgroups.forEachIndexed { index, sg ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                val sgChange = findChangeForSubgroup(sg, allChanges, lesson.subgroups)
                                val teacher = (sg.teacher?.takeIf { it.isNotBlank() }
                                    ?: sgChange?.teacher
                                    ?: allChanges.firstOrNull { !it.teacher.isNullOrBlank() }?.teacher)?.takeIf { it.isNotBlank() }
                                val className = (sg.className?.takeIf { it.isNotBlank() }
                                    ?: sgChange?.className
                                    ?: allChanges.firstOrNull { !it.className.isNullOrBlank() }?.className)?.takeIf { it.isNotBlank() }
                                val rawRoom = (sg.room?.takeIf { it.isNotBlank() }
                                    ?: sgChange?.room
                                    ?: allChanges.firstOrNull { !it.room.isNullOrBlank() }?.room)
                                val room = formatRoom(rawRoom)
                                val cleanRoom = rawRoom?.trim()?.removePrefix("(")?.removeSuffix(")") ?: ""

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isTeacherMode) {
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
                                                    color = nika.cancel,
                                                    textDecoration = TextDecoration.LineThrough
                                                )
                                            }
                                        }

                                        val group = formatGroup(sg.label)
                                        if (group.isNotEmpty()) {
                                            Text(
                                                text = " - $group",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = nika.cancel,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                        }

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
                                                            Modifier.clickable { onRoomClick(cleanRoom, lesson.num) }
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
                                                    color = nika.cancel,
                                                    textDecoration = TextDecoration.LineThrough
                                                )
                                            }
                                        }
                                    } else {
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
                                                    color = nika.cancel,
                                                    textDecoration = TextDecoration.LineThrough
                                                )
                                            }
                                        }

                                        val group = formatGroup(sg.label)
                                        if (group.isNotEmpty()) {
                                            Text(
                                                text = " - $group",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = nika.cancel,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                        }

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
                                                            Modifier.clickable { onRoomClick(cleanRoom, lesson.num) }
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
                                                    color = nika.cancel,
                                                    textDecoration = TextDecoration.LineThrough
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Normal Lesson or Subgroup Change / Replacement
                    else -> {
                        lesson.subgroups.forEachIndexed { index, sg ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(5.dp))
                            }

                            val sgChange = findChangeForSubgroup(sg, allChanges, lesson.subgroups)

                            val isSgCancelled: Boolean
                            val isSubjectChanged: Boolean
                            val isTeacherChanged: Boolean
                            val isRoomChanged: Boolean
                            val isClassChanged: Boolean

                            if (sgChange != null) {
                                if (sgChange.isCancelledLesson) {
                                    isSgCancelled = true
                                    isSubjectChanged = false
                                    isTeacherChanged = false
                                    isRoomChanged = false
                                    isClassChanged = false
                                } else {
                                    isSgCancelled = false
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
                            val displaySubj = ru.nstu.lyceum.schedule.data.api.normalizeSubject(rawSubj)
                            val displayTeacher = when {
                                !sgChange?.teacher.isNullOrBlank() -> sgChange!!.teacher
                                !sg.teacher.isNullOrBlank() -> sg.teacher
                                lesson.subgroups.size <= 1 -> allChanges.firstOrNull { !it.teacher.isNullOrBlank() }?.teacher
                                else -> null
                            }
                            val displayRoom = when {
                                !sgChange?.room.isNullOrBlank() -> sgChange!!.room
                                !sg.room.isNullOrBlank() -> sg.room
                                lesson.subgroups.size <= 1 -> allChanges.firstOrNull { !it.room.isNullOrBlank() }?.room
                                else -> null
                            }
                            val displayClass = when {
                                !sgChange?.className.isNullOrBlank() -> sgChange!!.className
                                !sg.className.isNullOrBlank() -> sg.className
                                lesson.subgroups.size <= 1 -> allChanges.firstOrNull { !it.className.isNullOrBlank() }?.className
                                else -> null
                            }

                            val subjectColor = when {
                                isSgCancelled -> nika.cancel
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
                                    textDecoration = if (isSgCancelled) TextDecoration.LineThrough else null
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
                                                        isSgCancelled -> nika.cancel
                                                        isClassChanged -> nika.change
                                                        else -> nika.teacher
                                                    },
                                                    textDecoration = if (isSgCancelled) TextDecoration.LineThrough else null
                                                )
                                            }
                                        }

                                        val group = formatGroup(sg.label)
                                        if (group.isNotEmpty()) {
                                            Text(
                                                text = " - $group",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = if (isSgCancelled) nika.cancel else nika.group,
                                                textDecoration = if (isSgCancelled) TextDecoration.LineThrough else null
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
                                                            Modifier.clickable { onRoomClick(cleanRoom, lesson.num) }
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
                                                        isSgCancelled -> nika.cancel
                                                        isRoomChanged -> nika.change
                                                        else -> nika.room
                                                    },
                                                    textDecoration = if (isSgCancelled) TextDecoration.LineThrough else null
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
                                                        isSgCancelled -> nika.cancel
                                                        isTeacherChanged -> nika.change
                                                        else -> nika.teacher
                                                    },
                                                    textDecoration = if (isSgCancelled) TextDecoration.LineThrough else null
                                                )
                                            }
                                        }

                                        val group = formatGroup(sg.label)
                                        if (group.isNotEmpty()) {
                                            Text(
                                                text = " - $group",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = if (isSgCancelled) nika.cancel else nika.group,
                                                textDecoration = if (isSgCancelled) TextDecoration.LineThrough else null
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
                                                            Modifier.clickable { onRoomClick(cleanRoom, lesson.num) }
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
                                                        isSgCancelled -> nika.cancel
                                                        isRoomChanged -> nika.change
                                                        else -> nika.room
                                                    },
                                                    textDecoration = if (isSgCancelled) TextDecoration.LineThrough else null
                                                )
                                            }
                                        }
                                    }
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

        // Clean list divider between lessons
        HorizontalDivider(
            thickness = 0.5.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun renderSingleCancelledLesson(
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
