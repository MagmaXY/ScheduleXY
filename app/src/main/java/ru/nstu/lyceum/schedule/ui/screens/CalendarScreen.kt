package ru.nstu.lyceum.schedule.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import ru.nstu.lyceum.schedule.data.local.NoteType
import ru.nstu.lyceum.schedule.data.local.NotesRepository
import ru.nstu.lyceum.schedule.data.local.ScheduleNote
import ru.nstu.lyceum.schedule.ui.components.NoteEditDialog
import ru.nstu.lyceum.schedule.ui.util.MarkdownRenderer
import ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.CalendarEvent
import ru.nstu.lyceum.schedule.data.api.EventCategory
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.ui.theme.EventBlue
import ru.nstu.lyceum.schedule.ui.theme.EventCyan
import ru.nstu.lyceum.schedule.ui.theme.EventGreen
import ru.nstu.lyceum.schedule.ui.theme.EventOrange
import ru.nstu.lyceum.schedule.ui.theme.EventPurple
import ru.nstu.lyceum.schedule.ui.theme.EventRed
import ru.nstu.lyceum.schedule.ui.theme.EventYellow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

fun resolveScheduleWeekOffset(
    cellCal: Calendar,
    currentScheduleWeekStart: String?,
    archiveWeeks: List<String>
): Pair<Int, Int>? {
    val dayOfWeek = (cellCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mon..6=Sun
    val cellDayIdx = if (dayOfWeek in 0..5) dayOfWeek else 0
    val cellMondayCal = Calendar.getInstance().apply {
        timeInMillis = cellCal.timeInMillis
        add(Calendar.DAY_OF_MONTH, -dayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val cellMondayIso = String.format(Locale.US, "%04d-%02d-%02d",
        cellMondayCal.get(Calendar.YEAR),
        cellMondayCal.get(Calendar.MONTH) + 1,
        cellMondayCal.get(Calendar.DAY_OF_MONTH)
    )

    val activeMondayCal = Calendar.getInstance()
    var parsedSuccess = false
    if (!currentScheduleWeekStart.isNullOrBlank()) {
        try {
            val parts = currentScheduleWeekStart.split("-")
            if (parts.size == 3) {
                activeMondayCal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                activeMondayCal.set(Calendar.MILLISECOND, 0)
                parsedSuccess = true
            }
        } catch (_: Exception) {}
    }
    if (!parsedSuccess) {
        val nowDayOfWeek = activeMondayCal.get(Calendar.DAY_OF_WEEK)
        val nowHour = activeMondayCal.get(Calendar.HOUR_OF_DAY)
        val d = (nowDayOfWeek + 5) % 7
        activeMondayCal.add(Calendar.DAY_OF_MONTH, -d)
        if (nowDayOfWeek == Calendar.SUNDAY || (nowDayOfWeek == Calendar.SATURDAY && nowHour >= 15)) {
            activeMondayCal.add(Calendar.DAY_OF_MONTH, 7) // upcoming Monday on weekend
        }
        activeMondayCal.set(Calendar.HOUR_OF_DAY, 0)
        activeMondayCal.set(Calendar.MINUTE, 0)
        activeMondayCal.set(Calendar.SECOND, 0)
        activeMondayCal.set(Calendar.MILLISECOND, 0)
    }
    val activeMondayIso = String.format(Locale.US, "%04d-%02d-%02d",
        activeMondayCal.get(Calendar.YEAR),
        activeMondayCal.get(Calendar.MONTH) + 1,
        activeMondayCal.get(Calendar.DAY_OF_MONTH)
    )

    if (cellMondayIso == activeMondayIso) {
        return Pair(0, cellDayIdx)
    }

    // Match strictly against available archive weeks from the API
    val archiveIdx = archiveWeeks.indexOf(cellMondayIso)
    if (archiveIdx != -1) {
        return Pair(-(archiveIdx + 1), cellDayIdx)
    }

    return null
}

@Composable
fun CalendarScreen(
    currentSchedule: ScheduleResponse?,
    archiveWeeks: List<String> = emptyList(),
    events: List<CalendarEvent>,
    categories: List<EventCategory>,
    notesRepository: NotesRepository? = null,
    targetName: String = "",
    isTeacherMode: Boolean = false,
    userSubgroup: Int = 1,
    userTechSubgroup: Int = 1,
    isMiddleSchool: Boolean = false,
    targetDateFocus: String? = null,
    overlaySwipeAnimation: Boolean = true,
    onNavigateToSchedule: (weekOffset: Int, dayIndex: Int) -> Unit
) {
    val context = LocalContext.current
    var calendarMonthOffset by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedDateString by remember { mutableStateOf<String?>(null) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var noteToEdit by remember { mutableStateOf<ScheduleNote?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(targetDateFocus) {
        if (!targetDateFocus.isNullOrBlank()) {
            try {
                val parts = targetDateFocus.split("-")
                if (parts.size == 3) {
                    val targetCal = Calendar.getInstance().apply {
                        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val nowCal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val monthDiff = (targetCal.get(Calendar.YEAR) - nowCal.get(Calendar.YEAR)) * 12 +
                            (targetCal.get(Calendar.MONTH) - nowCal.get(Calendar.MONTH))
                    calendarMonthOffset = monthDiff
                    selectedDateString = targetDateFocus
                }
            } catch (_: Exception) {}
        }
    }

    val allNotes = notesRepository?.notes?.collectAsState()?.value ?: emptyList()
    val currentNotes = remember(allNotes, targetName) {
        allNotes.filter { it.targetName == targetName }
    }
    val todayIso = remember { NotesTimeHelper.getTodayIso() }
    val totalDebts = remember(currentNotes, todayIso) {
        currentNotes.count { it.type == NoteType.HOMEWORK && !it.isCompleted && it.date < todayIso }
    }

    val selectedScheduleNav = remember(selectedDateString, currentSchedule?.weekStart, archiveWeeks) {
        if (selectedDateString != null) {
            try {
                val parts = selectedDateString!!.split("-")
                if (parts.size == 3) {
                    val c = Calendar.getInstance().apply {
                        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    resolveScheduleWeekOffset(c, currentSchedule?.weekStart, archiveWeeks)
                } else null
            } catch (_: Exception) { null }
        } else null
    }

    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    val todayMonth = cal.get(Calendar.MONTH)
    val todayYear = cal.get(Calendar.YEAR)

    cal.add(Calendar.MONTH, calendarMonthOffset)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val currentMonthName = SimpleDateFormat("LLLL yyyy", Locale("ru")).format(cal.time)
        .replaceFirstChar { it.titlecase(Locale("ru")) }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0 = Mon, 6 = Sun
    val displayedMonth = cal.get(Calendar.MONTH)
    val displayedYear = cal.get(Calendar.YEAR)

    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .pointerInput(calendarMonthOffset) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        if (totalDragX < -60f) {
                            // Swipe left -> next month
                            calendarMonthOffset++
                        } else if (totalDragX > 60f) {
                            // Swipe right -> previous month
                            calendarMonthOffset--
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    }
                )
            }
    ) {
        // Unified Header Bar: Month Navigation & Days of Week
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Month Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { calendarMonthOffset-- },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Предыдущий месяц",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = currentMonthName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { calendarMonthOffset++ },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Следующий месяц",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Days of week header under month row
                val weekHeaders = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    weekHeaders.forEachIndexed { i, h ->
                        Text(
                            text = h,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = when (i) {
                                5 -> colorScheme.secondary
                                6 -> colorScheme.error
                                else -> colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)

        // Calendar Grid
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            AnimatedContent(
                targetState = calendarMonthOffset,
                transitionSpec = {
                    if (overlaySwipeAnimation) {
                        val forward = targetState > initialState
                        if (forward) {
                            (slideInHorizontally(
                                animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                            ) { fullWidth -> fullWidth } + fadeIn(tween(150)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                                    ) { fullWidth -> -fullWidth } + fadeOut(tween(150))
                                )
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                            ) { fullWidth -> -fullWidth } + fadeIn(tween(150)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                                    ) { fullWidth -> fullWidth } + fadeOut(tween(150))
                                )
                        }
                    } else {
                        EnterTransition.None.togetherWith(ExitTransition.None)
                    }
                },
                label = "calendar_month_grid"
            ) { monthOffset ->
                val mCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, monthOffset)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val mDaysInMonth = mCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val mFirstDayOfWeek = (mCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val mDisplayedMonth = mCal.get(Calendar.MONTH)
                val mDisplayedYear = mCal.get(Calendar.YEAR)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Calendar cells: 6 rows of 7 days
                    for (row in 0 until 6) {
                        val rowStart = row * 7
                        if (rowStart >= mFirstDayOfWeek + mDaysInMonth) break

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - mFirstDayOfWeek + 1

                                if (dayNumber in 1..mDaysInMonth) {
                                    val isToday = dayNumber == todayDay && mDisplayedMonth == todayMonth && mDisplayedYear == todayYear
                                    val isSaturday = col == 5
                                    val isSunday = col == 6

                                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", mDisplayedYear, mDisplayedMonth + 1, dayNumber)
                                    val hasEvents = events.any { it.date == dateStr }
                                    val dayNotes = currentNotes.filter { it.date == dateStr }
                                    val hasNotes = dayNotes.isNotEmpty()
                                    val hasDebts = dayNotes.any { it.type == NoteType.HOMEWORK && !it.isCompleted && dateStr < todayIso }
                                    val isSelected = selectedDateString == dateStr

                                    val cellBg = when {
                                        isSelected -> colorScheme.primary
                                        isToday -> colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        isSunday -> colorScheme.errorContainer.copy(alpha = 0.25f)
                                        isSaturday -> colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                        else -> colorScheme.surfaceContainerLow
                                    }

                                    val cellBorder = when {
                                        isToday && !isSelected -> colorScheme.primary
                                        isSelected -> colorScheme.primary
                                        else -> colorScheme.outlineVariant
                                    }

                                    val textColor = when {
                                        isSelected -> colorScheme.onPrimary
                                        isSunday -> colorScheme.error
                                        isSaturday -> colorScheme.secondary
                                        isToday -> colorScheme.primary
                                        else -> colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.15f)
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cellBg)
                                            .border(
                                                width = if (isToday || isSelected) 1.2.dp else 0.5.dp,
                                                color = cellBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                val cellCal = Calendar.getInstance().apply {
                                                    set(mDisplayedYear, mDisplayedMonth, dayNumber, 0, 0, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                val nav = resolveScheduleWeekOffset(cellCal, currentSchedule?.weekStart, archiveWeeks)
                                                if (selectedDateString != dateStr) {
                                                    selectedDateString = dateStr
                                                } else {
                                                    // Double click immediately opens schedule
                                                    if (nav != null) {
                                                        onNavigateToSchedule(nav.first, nav.second)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 17.sp,
                                                    lineHeight = 18.sp
                                                ),
                                                color = textColor,
                                                textAlign = TextAlign.Center
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.height(7.dp)
                                            ) {
                                                if (hasEvents) {
                                                    val dotColor = when {
                                                        isSelected -> colorScheme.onPrimary
                                                        isSunday -> colorScheme.error
                                                        isSaturday -> colorScheme.secondary
                                                        else -> colorScheme.onSurface
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(dotColor)
                                                    )
                                                }
                                                if (hasNotes) {
                                                    if (hasEvents) Spacer(modifier = Modifier.width(2.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null,
                                                        tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface,
                                                        modifier = Modifier.size(6.5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1.2f))
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)

        // Full-width search bar across entire screen
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            placeholder = {
                Text(
                    text = "Поиск по событиям и заметкам...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Поиск",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface,
                focusedContainerColor = colorScheme.surfaceContainerLow,
                unfocusedContainerColor = colorScheme.surfaceContainerLow,
                cursorColor = colorScheme.primary,
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        // Filtered notes and events
        val isNoteCategory = selectedCategory in listOf("CAT_HW", "CAT_NOTES", "CAT_DEBTS")
        val isEventColorCategory = selectedCategory != null && !isNoteCategory

        val filteredNotes = remember(currentNotes, selectedCategory, selectedDateString, todayIso, searchQuery) {
            if (isEventColorCategory) emptyList()
            else {
                val base = currentNotes.filter { note ->
                    val matchesDate = selectedDateString == null || note.date == selectedDateString
                    val matchesCat = when (selectedCategory) {
                        "CAT_HW" -> note.type == NoteType.HOMEWORK
                        "CAT_NOTES" -> note.type == NoteType.NOTE
                        "CAT_DEBTS" -> note.type == NoteType.HOMEWORK && !note.isCompleted && note.date < todayIso
                        else -> true
                    }
                    matchesDate && matchesCat
                }
                ru.nstu.lyceum.schedule.ui.util.SmartSearchEngine.searchNotes(base, searchQuery)
            }
        }

        val filteredEvents = remember(events, selectedCategory, selectedDateString, isNoteCategory, searchQuery) {
            if (isNoteCategory) emptyList()
            else {
                val base = events.filter { event ->
                    val matchesCat = (selectedCategory == null || event.color == selectedCategory)
                    val matchesDate = (selectedDateString == null || event.date == selectedDateString)
                    matchesCat && matchesDate
                }
                ru.nstu.lyceum.schedule.ui.util.SmartSearchEngine.searchEvents(base, searchQuery)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (selectedScheduleNav != null) {
                item {
                    val (weekOffset, dayIdx) = selectedScheduleNav
                    val isCurWeek = weekOffset == 0
                    val isSunday = try {
                        val parts = selectedDateString!!.split("-")
                        val c = Calendar.getInstance().apply { set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt()) }
                        c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    } catch (_: Exception) { false }

                    val btnText = when {
                        isCurWeek && isSunday -> "Открыть расписание на понедельник"
                        isCurWeek -> "Открыть расписание на этот день"
                        else -> {
                            val archiveIdx = -(weekOffset + 1)
                            val chipLabel = archiveWeeks.getOrNull(archiveIdx)?.let { formatArchiveWeekChip(it) }
                            if (!chipLabel.isNullOrBlank()) {
                                "Открыть архив: $chipLabel"
                            } else {
                                "Открыть архив расписания"
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            onNavigateToSchedule(weekOffset, dayIdx)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = btnText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else if (selectedDateString != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = colorScheme.surfaceContainerLow,
                        border = BorderStroke(0.5.dp, colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Расписание на эту неделю недоступно в архиве",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Categories horizontal list below the schedule button / archive info
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allSelected = selectedCategory == null
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clickable { selectedCategory = null }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Все",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (allSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isTeacherMode) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val isHwSelected = selectedCategory == "CAT_HW"
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clickable { selectedCategory = if (isHwSelected) null else "CAT_HW" }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ДЗ",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isHwSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isHwSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    val isNotesSelected = selectedCategory == "CAT_NOTES"
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clickable { selectedCategory = if (isNotesSelected) null else "CAT_NOTES" }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Заметки",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isNotesSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (isNotesSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isTeacherMode && totalDebts > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val isDebtsSelected = selectedCategory == "CAT_DEBTS"
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clickable { selectedCategory = if (isDebtsSelected) null else "CAT_DEBTS" }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Долги ($totalDebts)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isDebtsSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isDebtsSelected) colorScheme.error else colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    categories.forEach { cat ->
                        Spacer(modifier = Modifier.width(4.dp))
                        val isCatSelected = selectedCategory == cat.color
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clickable { selectedCategory = if (isCatSelected) null else cat.color }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (isCatSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

            item {
                Text(
                    text = if (selectedDateString != null) "События и заметки на $selectedDateString" else "События лицея и заметки",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }

            if (filteredNotes.isEmpty() && filteredEvents.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) "По запросу «$searchQuery» ничего не найдено" else "В выбранной категории записей не найдено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Notes first
                items(filteredNotes, key = { "note_${it.id}" }) { note ->
                    val isDebt = note.type == NoteType.HOMEWORK && !note.isCompleted && note.date < todayIso
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.5.dp)
                            .clickable { noteToEdit = note },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isDebt) colorScheme.errorContainer.copy(alpha = 0.15f) else colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 0.6.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isDebt) colorScheme.error.copy(alpha = 0.5f) else colorScheme.outlineVariant
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isDebt) colorScheme.error else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                val title = when {
                                    note.type == NoteType.HOMEWORK -> {
                                        val lsn = if (note.lessonNum != null) " (${note.lessonNum} урок)" else ""
                                        "ДЗ: ${note.subject ?: ""}$lsn"
                                    }
                                    note.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.BEFORE_LESSONS -> "До уроков"
                                    note.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.AFTER_LESSONS -> "После уроков"
                                    note.slotType == ru.nstu.lyceum.schedule.data.local.SlotType.LESSON -> "Заметка (${note.lessonNum} урок)"
                                    else -> "Заметка"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = if (isDebt) colorScheme.error else colorScheme.onSurface
                                    )
                                    if (isDebt) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = colorScheme.errorContainer
                                        ) {
                                            Text(
                                                text = "Не сдано",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = NotesTimeHelper.formatDateWithDay(note.date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                val parsedText = MarkdownRenderer.parseMarkdown(note.text)
                                Text(
                                    text = parsedText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        textDecoration = if (note.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                    ),
                                    color = if (note.isCompleted) colorScheme.outlineVariant else colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = {
                                    notesRepository?.deleteNote(note.id)
                                    Toast.makeText(context, "Запись удалена", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Удалить",
                                    tint = colorScheme.error.copy(alpha = 0.75f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Lyceum events next
                items(filteredEvents) { ev ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.5.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 0.6.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outlineVariant)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(getCategoryColor(ev.color))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ev.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = colorScheme.onSurface
                                )
                                val label = ev.sourceLabel?.takeIf {
                                    !it.contains("файл", ignoreCase = true) && !it.contains("сайт", ignoreCase = true)
                                }
                                Text(
                                    text = if (!label.isNullOrBlank()) "${ev.date} • $label" else ev.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (noteToEdit != null) {
        NoteEditDialog(
            initialNote = noteToEdit,
            targetName = targetName,
            isTeacherMode = isTeacherMode,
            defaultDateIso = noteToEdit!!.date,
            defaultLessonNum = noteToEdit!!.lessonNum,
            defaultSubject = noteToEdit!!.subject,
            userSubgroup = userSubgroup,
            userTechSubgroup = userTechSubgroup,
            isMiddleSchool = isMiddleSchool,
            scheduleResponse = currentSchedule,
            onSave = { updated ->
                notesRepository?.updateNote(updated)
                noteToEdit = null
            },
            onDelete = { noteId ->
                notesRepository?.deleteNote(noteId)
                Toast.makeText(context, "Запись удалена", Toast.LENGTH_SHORT).show()
                noteToEdit = null
            },
            onDismiss = { noteToEdit = null }
        )
    }
}

fun getCategoryColor(colorName: String?): Color {
    return when (colorName?.lowercase()) {
        "blue" -> EventBlue
        "green" -> EventGreen
        "red" -> EventRed
        "orange" -> EventOrange
        "purple" -> EventPurple
        "yellow" -> EventYellow
        "cyan" -> EventCyan
        else -> EventBlue
    }
}
