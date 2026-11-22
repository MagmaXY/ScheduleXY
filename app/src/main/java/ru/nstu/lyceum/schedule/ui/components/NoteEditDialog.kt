package ru.nstu.lyceum.schedule.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.normalizeSubject
import ru.nstu.lyceum.schedule.data.local.NoteType
import ru.nstu.lyceum.schedule.data.local.ScheduleNote
import ru.nstu.lyceum.schedule.data.local.SlotType
import ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditDialog(
    initialNote: ScheduleNote? = null,
    defaultType: NoteType = NoteType.HOMEWORK,
    targetName: String,
    isTeacherMode: Boolean,
    defaultDateIso: String,
    defaultLessonNum: Int? = null,
    defaultSubject: String? = null,
    availableLessons: List<Pair<Int, String>> = emptyList(), // lessonNum to subject name
    userSubgroup: Int = 1,
    userTechSubgroup: Int = 1,
    isMiddleSchool: Boolean = false,
    scheduleResponse: ScheduleResponse? = null,
    onSave: (ScheduleNote) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var noteType by remember(initialNote, defaultType) {
        mutableStateOf(initialNote?.type ?: defaultType)
    }

    var selectedDateIso by remember {
        val initial = initialNote?.date ?: defaultDateIso
        val today = NotesTimeHelper.getTodayIso()
        mutableStateOf(if (initial < today) today else initial)
    }

    var selectedSlotType by remember {
        mutableStateOf(initialNote?.slotType ?: if (noteType == NoteType.HOMEWORK) SlotType.LESSON else SlotType.DAY_NOTE)
    }

    var selectedLessonNum by remember {
        mutableStateOf(initialNote?.lessonNum ?: defaultLessonNum ?: availableLessons.firstOrNull()?.first ?: 1)
    }

    var isTransferPending by remember {
        mutableStateOf(initialNote?.transferPending ?: false)
    }

    var subjectText by remember {
        val initialSubj = initialNote?.subject ?: defaultSubject ?: if (scheduleResponse != null) {
            NotesTimeHelper.getEffectiveLessonSubject(
                schedule = scheduleResponse,
                dateIso = selectedDateIso,
                lessonNum = selectedLessonNum,
                userSubgroup = userSubgroup,
                userTechSubgroup = userTechSubgroup,
                isMiddleSchool = isMiddleSchool
            ) ?: ""
        } else {
            availableLessons.firstOrNull { it.first == selectedLessonNum }?.second ?: ""
        }
        mutableStateOf(initialSubj)
    }

    val currentAvailableLessons = remember(selectedDateIso, scheduleResponse, userSubgroup, userTechSubgroup, availableLessons) {
        if (scheduleResponse != null) {
            val list = NotesTimeHelper.getAvailableLessonsForDate(
                schedule = scheduleResponse,
                dateIso = selectedDateIso,
                userSubgroup = userSubgroup,
                userTechSubgroup = userTechSubgroup,
                isMiddleSchool = isMiddleSchool
            )
            if (list.isNotEmpty()) list else availableLessons
        } else {
            availableLessons
        }
    }

    // Determines if target lesson is split into subgroups: returns user's subgroup if split, or null if whole class
    fun determineSubgroupForTarget(dateIso: String, lsnNum: Int?, subj: String): Int? {
        if (subj.isBlank() || lsnNum == null || scheduleResponse == null) return null
        val norm = normalizeSubject(subj).lowercase().trim()
        val isTech = norm.contains("технолог") && isMiddleSchool
        val targetGroup = if (isTech) userTechSubgroup else userSubgroup

        val cal = Calendar.getInstance()
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
            return null
        }
        val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek > 5) return null

        val day = scheduleResponse.days.firstOrNull { it.dayIndex == dayOfWeek } ?: return null
        val lesson = day.lessons.firstOrNull { it.num == lsnNum } ?: return null

        val isSplit = lesson.subgroups.size > 1 || lesson.subgroups.any { sg ->
            !sg.label.isNullOrBlank() && sg.label!!.contains(Regex("\\d"))
        }
        return if (isSplit) targetGroup else null
    }

    var textFieldValue by remember {
        val txt = initialNote?.text ?: ""
        mutableStateOf(TextFieldValue(txt, selection = TextRange(txt.length)))
    }

    var showDatePicker by remember { mutableStateOf(false) }

    // DatePicker setup
    val todayUtc = remember {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = remember(selectedDateIso) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                sdf.parse(selectedDateIso)?.time ?: todayUtc
            } catch (_: Exception) {
                todayUtc
            }
        },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= todayUtc
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                        selectedDateIso = sdf.format(java.util.Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена", color = colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val isSlotInPast = remember(selectedDateIso, selectedSlotType, selectedLessonNum) {
        NotesTimeHelper.isSlotInPast(selectedDateIso, selectedSlotType, selectedLessonNum)
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        titleContentColor = colorScheme.onSurface,
        textContentColor = colorScheme.onSurfaceVariant,
        title = {
            if (initialNote != null) {
                Text(
                    text = "Редактировать запись",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = colorScheme.onSurface
                )
            } else if (!isTeacherMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isHw = noteType == NoteType.HOMEWORK
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHw) colorScheme.primary else colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                noteType = NoteType.HOMEWORK
                                if (selectedSlotType != SlotType.LESSON) {
                                    selectedSlotType = SlotType.LESSON
                                }
                            }
                    ) {
                        Text(
                            text = "Записать ДЗ",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (isHw) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp
                            ),
                            color = if (isHw) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isHw) colorScheme.primary else colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                noteType = NoteType.NOTE
                            }
                    ) {
                        Text(
                            text = "Своя заметка",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (!isHw) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp
                            ),
                            color = if (!isHw) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    text = if (noteType == NoteType.HOMEWORK) "Записать ДЗ" else "Своя заметка",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // 1. Date selection row + "К следующему уроку" button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Дата:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { showDatePicker = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = NotesTimeHelper.formatDateWithDay(selectedDateIso),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Automatic schedule option "К следующему уроку" (Point 9)
                if (noteType == NoteType.HOMEWORK && subjectText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val targetSg = if (subjectText.contains("технолог", ignoreCase = true) && isMiddleSchool) userTechSubgroup else userSubgroup
                                val nextInWeek = NotesTimeHelper.findNextLessonInCurrentWeek(
                                    schedule = scheduleResponse,
                                    subject = subjectText,
                                    targetSubgroup = targetSg,
                                    fromDateIso = selectedDateIso,
                                    afterLessonNum = selectedLessonNum,
                                    userTechSubgroup = userTechSubgroup,
                                    isMiddleSchool = isMiddleSchool
                                )
                                if (nextInWeek != null) {
                                    selectedDateIso = nextInWeek.first
                                    selectedLessonNum = nextInWeek.second
                                    isTransferPending = false
                                    Toast.makeText(
                                        context,
                                        "Выбран след. урок: ${NotesTimeHelper.formatDateWithDay(nextInWeek.first)} (${nextInWeek.second} урок)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    isTransferPending = true
                                    Toast.makeText(
                                        context,
                                        "На этой неделе больше нет уроков. Перенос запланирован на след. неделю (к ближайшему уроку)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Задать к следующему уроку",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Slot selection (Before school, Lesson, After school, Day note)
                if (noteType == NoteType.NOTE) {
                    Text(
                        text = "Привязка по времени:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val slots = listOf(
                            SlotType.DAY_NOTE to "На день",
                            SlotType.BEFORE_LESSONS to "До уроков",
                            SlotType.LESSON to "Урок",
                            SlotType.AFTER_LESSONS to "После уроков"
                        )
                        slots.forEach { (slot, label) ->
                            val isSlotDisabled = when (slot) {
                                SlotType.DAY_NOTE -> false
                                SlotType.BEFORE_LESSONS -> NotesTimeHelper.isSlotInPast(selectedDateIso, SlotType.BEFORE_LESSONS, null)
                                SlotType.LESSON -> (1..12).all { NotesTimeHelper.isSlotInPast(selectedDateIso, SlotType.LESSON, it) }
                                SlotType.AFTER_LESSONS -> NotesTimeHelper.isSlotInPast(selectedDateIso, SlotType.AFTER_LESSONS, null)
                            }
                            val isSel = selectedSlotType == slot
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSlotDisabled -> colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                                    isSel -> colorScheme.primary
                                    else -> colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = !isSlotDisabled) {
                                        selectedSlotType = slot
                                        if (slot == SlotType.LESSON && NotesTimeHelper.isSlotInPast(selectedDateIso, SlotType.LESSON, selectedLessonNum)) {
                                            val nextUpcoming = (1..12).firstOrNull { !NotesTimeHelper.isSlotInPast(selectedDateIso, SlotType.LESSON, it) } ?: 1
                                            selectedLessonNum = nextUpcoming
                                            val match = availableLessons.firstOrNull { it.first == nextUpcoming }
                                            if (match != null && match.second.isNotBlank()) {
                                                subjectText = match.second
                                            }
                                        }
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                    color = when {
                                        isSlotDisabled -> colorScheme.outlineVariant
                                        isSel -> colorScheme.onPrimary
                                        else -> colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3. Lesson selection: Horizontal scroll with all 12 potential lessons
                if (noteType == NoteType.HOMEWORK || selectedSlotType == SlotType.LESSON) {
                    Text(
                        text = "Урок:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..12).forEach { num ->
                            val isPast = NotesTimeHelper.isSlotInPast(selectedDateIso, SlotType.LESSON, num)
                            val isSel = selectedLessonNum == num
                            val match = currentAvailableLessons.firstOrNull { it.first == num }
                            val canClick = (noteType == NoteType.HOMEWORK) || !isPast
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSel -> colorScheme.primary
                                    isPast && noteType != NoteType.HOMEWORK -> colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                                    isPast && noteType == NoteType.HOMEWORK -> colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    else -> colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .width(44.dp)
                                    .clickable(enabled = canClick) {
                                        selectedLessonNum = num
                                        val eff = if (scheduleResponse != null) {
                                            NotesTimeHelper.getEffectiveLessonSubject(
                                                schedule = scheduleResponse,
                                                dateIso = selectedDateIso,
                                                lessonNum = num,
                                                userSubgroup = userSubgroup,
                                                userTechSubgroup = userTechSubgroup,
                                                isMiddleSchool = isMiddleSchool
                                            )
                                        } else {
                                            match?.second
                                        }
                                        if (!eff.isNullOrBlank()) {
                                            subjectText = eff
                                        }
                                    }
                            ) {
                                Text(
                                    text = num.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                    color = when {
                                        isSel -> colorScheme.onPrimary
                                        isPast && noteType != NoteType.HOMEWORK -> colorScheme.outlineVariant
                                        isPast && noteType == NoteType.HOMEWORK -> colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        else -> colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Subject Field (for Homework)
                    if (noteType == NoteType.HOMEWORK) {
                        Text(
                            text = "Предмет:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = subjectText,
                            onValueChange = { subjectText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Название предмета...", style = MaterialTheme.typography.bodyMedium, color = colorScheme.outline) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colorScheme.onSurface,
                                unfocusedTextColor = colorScheme.onSurface,
                                focusedContainerColor = colorScheme.surface,
                                unfocusedContainerColor = colorScheme.surface,
                                cursorColor = colorScheme.primary,
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outlineVariant
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // 4. Markdown Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    fun wrapSelection(prefix: String, suffix: String) {
                        val sel = textFieldValue.selection
                        val text = textFieldValue.text
                        val selected = text.substring(sel.min, sel.max)
                        val newText = text.substring(0, sel.min) + prefix + selected + suffix + text.substring(sel.max)
                        val newCursor = if (sel.collapsed) sel.min + prefix.length else sel.max + prefix.length + suffix.length
                        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
                    }

                    IconButton(onClick = { wrapSelection("**", "**") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Жирный", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { wrapSelection("*", "*") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Курсив", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { wrapSelection("~~", "~~") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.FormatStrikethrough, contentDescription = "Зачеркнутый", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = {
                        val sel = textFieldValue.selection
                        val text = textFieldValue.text
                        val newText = text.substring(0, sel.min) + "\n- " + text.substring(sel.min)
                        textFieldValue = TextFieldValue(newText, TextRange(sel.min + 3))
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Список", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 5. Note / Homework Text Field
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    placeholder = {
                        Text(
                            text = if (noteType == NoteType.HOMEWORK) "Задание, упр., параграф..." else "Текст заметки...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.outline
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface,
                        cursorColor = colorScheme.primary,
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant
                    )
                )

                if (isSlotInPast) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Выбранное время уже прошло. Нельзя сохранить запись на прошедшее время.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            val isSaveEnabled = textFieldValue.text.isNotBlank() && !isSlotInPast
            Button(
                onClick = {
                    val finalSlot = if (noteType == NoteType.HOMEWORK) SlotType.LESSON else selectedSlotType
                    val finalLessonNum = if (finalSlot == SlotType.LESSON) selectedLessonNum else null
                    val autoSubgrp = if (noteType == NoteType.HOMEWORK) {
                        determineSubgroupForTarget(
                            dateIso = selectedDateIso,
                            lsnNum = finalLessonNum,
                            subj = subjectText
                        )
                    } else null

                    val note = ScheduleNote(
                        id = initialNote?.id ?: java.util.UUID.randomUUID().toString(),
                        targetName = targetName,
                        type = noteType,
                        subject = if (noteType == NoteType.HOMEWORK) subjectText.trim().ifEmpty { null } else null,
                        subgroup = autoSubgrp,
                        date = selectedDateIso,
                        slotType = finalSlot,
                        lessonNum = finalLessonNum,
                        text = textFieldValue.text.trim(),
                        isCompleted = initialNote?.isCompleted ?: false,
                        transferPending = isTransferPending,
                        createdAt = initialNote?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(note)
                    onDismiss()
                },
                enabled = isSaveEnabled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    disabledContainerColor = colorScheme.surfaceVariant,
                    disabledContentColor = colorScheme.outlineVariant
                )
            ) {
                Text("Сохранить", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (initialNote != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(initialNote.id)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Удалить",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Отмена",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

