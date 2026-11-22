package ru.nstu.lyceum.schedule.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.ui.util.ScheduleFormatter
import ru.nstu.lyceum.schedule.ui.components.PairCard
import ru.nstu.lyceum.schedule.ui.components.ScheduleItemUi
import ru.nstu.lyceum.schedule.ui.components.SchedulePairMerger
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.ClassItem
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.TeacherItem
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.ui.components.DaySelector
import ru.nstu.lyceum.schedule.ui.components.LessonCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ArchiveWeekOption(
    val weekParam: Int,
    val isoMonday: String,
    val label: String
)

fun formatArchiveWeekChip(isoMonday: String): String {
    try {
        val parts = isoMonday.split("-")
        if (parts.size == 3) {
            val cal = Calendar.getInstance()
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            val startDay = cal.get(Calendar.DAY_OF_MONTH)
            val startMonth = cal.get(Calendar.MONTH)

            cal.add(Calendar.DAY_OF_MONTH, 5) // Saturday
            val endDay = cal.get(Calendar.DAY_OF_MONTH)
            val endMonth = cal.get(Calendar.MONTH)

            val months = arrayOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
            return if (startMonth == endMonth) {
                "$startDay–$endDay ${months.getOrElse(startMonth) { "" }}"
            } else {
                "$startDay ${months.getOrElse(startMonth) { "" }}–$endDay ${months.getOrElse(endMonth) { "" }}"
            }
        }
    } catch (_: Exception) {}
    return isoMonday
}

fun getArchiveDateLabel(weekStartStr: String?, fallbackWeekOffset: Int, dayIndex: Int): String {
    val cal = Calendar.getInstance()
    if (!weekStartStr.isNullOrBlank()) {
        try {
            val parts = weekStartStr.split("-")
            if (parts.size == 3) {
                cal.set(Calendar.YEAR, parts[0].toInt())
                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                cal.add(Calendar.DAY_OF_MONTH, dayIndex)
                val sdf = SimpleDateFormat("d MMMM", Locale("ru"))
                return sdf.format(cal.time)
            }
        } catch (_: Exception) {}
    }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val d = (dayOfWeek + 5) % 7
    cal.add(Calendar.DAY_OF_MONTH, -d + (fallbackWeekOffset * 7) + dayIndex)
    val sdf = SimpleDateFormat("d MMMM", Locale("ru"))
    return sdf.format(cal.time)
}

fun getArchiveDateIso(weekStartStr: String?, fallbackWeekOffset: Int, dayIndex: Int): String {
    val cal = Calendar.getInstance()
    if (!weekStartStr.isNullOrBlank()) {
        try {
            val parts = weekStartStr.split("-")
            if (parts.size == 3) {
                cal.set(Calendar.YEAR, parts[0].toInt())
                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                cal.add(Calendar.DAY_OF_MONTH, dayIndex)
                return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            }
        } catch (_: Exception) {}
    }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val d = (dayOfWeek + 5) % 7
    cal.add(Calendar.DAY_OF_MONTH, -d + (fallbackWeekOffset * 7) + dayIndex)
    return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

@Composable
fun ArchiveScreen(
    archiveSchedule: ScheduleResponse?,
    archiveWeeks: List<String> = emptyList(),
    classesList: List<ClassItem>,
    teachersList: List<TeacherItem>,
    targetType: String,
    targetName: String,
    selectedWeek: Int, // -1, -2, -3...
    selectedDayIndex: Int,
    isLoading: Boolean,
    showPairsSchedule: Boolean,
    overlaySwipeAnimation: Boolean,
    favoriteClass: String? = null,
    favoriteTeacher: String? = null,
    classTeachers: List<String> = emptyList(),
    teacherClasses: List<String> = emptyList(),
    onWeekChanged: (Int) -> Unit,
    onTargetTypeChanged: (String) -> Unit,
    onTargetNameChanged: (String) -> Unit,
    onDaySelected: (Int) -> Unit,
    onNavigateToCalendar: (String) -> Unit = {}
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val colorScheme = MaterialTheme.colorScheme
    val isTeacherMode = targetType == PreferencesRepository.TYPE_TEACHER

    val weekOptions = remember(archiveWeeks) {
        archiveWeeks.take(3).mapIndexed { index, isoDate ->
            ArchiveWeekOption(
                weekParam = -(index + 1),
                isoMonday = isoDate,
                label = formatArchiveWeekChip(isoDate)
            )
        }
    }

    val activeWeekOption = weekOptions.firstOrNull { it.weekParam == selectedWeek }
    val activeWeekIso = activeWeekOption?.isoMonday?.ifBlank { null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Top Unified Header Bar for Archive
        val archiveDateLabel = remember(archiveSchedule?.weekStart, activeWeekIso, selectedWeek, selectedDayIndex) {
            getArchiveDateLabel(
                weekStartStr = archiveSchedule?.weekStart ?: activeWeekIso,
                fallbackWeekOffset = selectedWeek,
                dayIndex = selectedDayIndex
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Two flat rectangles without corners/borders
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val classSelected = !isTeacherMode
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clickable { onTargetTypeChanged(PreferencesRepository.TYPE_CLASS) }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Классы",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (classSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (classSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        val teacherSelected = isTeacherMode
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clickable { onTargetTypeChanged(PreferencesRepository.TYPE_TEACHER) }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Учителя",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (teacherSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (teacherSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right: Target Selector Box and Archive Week Switcher
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Target Selector Box: no borders, fill in color of header, icon 👥 or 👤
                        Box {
                            Surface(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { dropdownExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isTeacherMode) Icons.Default.Person else Icons.Default.Group,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = targetName.ifEmpty { if (isTeacherMode) "Учитель" else "Класс" },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Выбрать",
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = {
                                    dropdownExpanded = false
                                    filterQuery = ""
                                },
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(360.dp)
                                    .background(colorScheme.surfaceContainerHigh)
                            ) {
                                OutlinedTextField(
                                    value = filterQuery,
                                    onValueChange = { filterQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    placeholder = { Text("Поиск...", style = MaterialTheme.typography.bodyMedium, color = colorScheme.outline) },
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

                                val groups = remember(isTeacherMode, classesList, teachersList, favoriteClass, favoriteTeacher, classTeachers, teacherClasses) {
                                    if (isTeacherMode) {
                                        val allNames = teachersList.map { it.name }
                                        val favItem = favoriteTeacher?.takeIf { ft -> allNames.any { it.equals(ft, ignoreCase = true) } }
                                            ?.let { ft -> allNames.first { it.equals(ft, ignoreCase = true) } }
                                        val cTeachers = classTeachers.mapNotNull { ct ->
                                            allNames.firstOrNull { it.equals(ct, ignoreCase = true) || it.contains(ct, ignoreCase = true) }
                                        }.filter { it != favItem }.distinct()
                                        val other = allNames.filter { it != favItem && it !in cTeachers }

                                        buildList {
                                            if (favItem != null) {
                                                add("Избранный учитель" to listOf(favItem))
                                            }
                                            if (cTeachers.isNotEmpty()) {
                                                val header = if (!favoriteClass.isNullOrBlank()) "Учителя $favoriteClass" else "Учителя класса"
                                                add(header to cTeachers)
                                            }
                                            if (other.isNotEmpty()) {
                                                val header = if (favItem != null || cTeachers.isNotEmpty()) "Все учителя" else null
                                                add(header to other)
                                            }
                                        }
                                    } else {
                                        val allNames = classesList.map { it.name }
                                        val favItem = favoriteClass?.takeIf { it in allNames }
                                        val gradePrefix = favoriteClass?.takeWhile { it.isDigit() }
                                        val parallel = if (!gradePrefix.isNullOrBlank()) {
                                            allNames.filter { it != favItem && it.takeWhile { c -> c.isDigit() } == gradePrefix }
                                        } else emptyList()
                                        val tClasses = teacherClasses.filter { it in allNames && it != favItem && it !in parallel }
                                        val other = allNames.filter { it != favItem && it !in parallel && it !in tClasses }

                                        buildList {
                                            if (favItem != null) {
                                                add("Избранный класс" to listOf(favItem))
                                            }
                                            if (parallel.isNotEmpty()) {
                                                add("Параллель $gradePrefix классов" to parallel)
                                            }
                                            if (tClasses.isNotEmpty()) {
                                                val header = if (!favoriteTeacher.isNullOrBlank()) "Уроки: $favoriteTeacher" else "Классы учителя"
                                                add(header to tClasses)
                                            }
                                            if (other.isNotEmpty()) {
                                                val header = if (favItem != null || parallel.isNotEmpty() || tClasses.isNotEmpty()) "Все классы" else null
                                                add(header to other)
                                            }
                                        }
                                    }
                                }

                                @Composable
                                fun RenderDropdownItem(name: String) {
                                    val isFav = (isTeacherMode && name.equals(favoriteTeacher, ignoreCase = true)) ||
                                            (!isTeacherMode && name == favoriteClass)
                                    val isCurrent = name == targetName
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Default.Star else (if (isTeacherMode) Icons.Default.Person else Icons.Default.Group),
                                                    contentDescription = null,
                                                    tint = if (isCurrent) colorScheme.primary else colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (isCurrent) colorScheme.primary else colorScheme.onSurface
                                                )
                                            }
                                        },
                                        colors = MenuDefaults.itemColors(textColor = colorScheme.onSurface),
                                        onClick = {
                                            onTargetNameChanged(name)
                                            dropdownExpanded = false
                                            filterQuery = ""
                                        }
                                    )
                                }

                                if (filterQuery.isNotBlank()) {
                                    val query = filterQuery.trim()
                                    val filteredItems = groups.flatMap { it.second }.distinct().filter { it.contains(query, ignoreCase = true) }
                                    filteredItems.forEach { name ->
                                        RenderDropdownItem(name)
                                    }
                                } else {
                                    groups.forEach { (header, groupItems) ->
                                        if (!header.isNullOrBlank()) {
                                            Text(
                                                text = header,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                ),
                                                color = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                        groupItems.forEach { name ->
                                            RenderDropdownItem(name)
                                        }
                                    }
                                }
                            }
                        }

                        // Archive Weeks: flat tabs without borders/corners, divided like Classes and Teachers
                        Row(
                            modifier = Modifier.height(34.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            weekOptions.forEachIndexed { idx, opt ->
                                val isSelected = opt.weekParam == selectedWeek
                                if (idx > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .clickable { onWeekChanged(opt.weekParam) }
                                    .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Day navigation with arrows, label, and day selector in the unified header
                DaySelector(
                    selectedDayIndex = selectedDayIndex,
                    currentDayIndex = -1,
                    dateLabel = archiveDateLabel,
                    onDaySelected = onDaySelected,
                    onDateClick = {
                        val iso = getArchiveDateIso(archiveSchedule?.weekStart, selectedWeek, selectedDayIndex)
                        onNavigateToCalendar(iso)
                    }
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)

        // Lessons List Content with horizontal swipe
        var totalDragX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedDayIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onDragEnd = {
                            if (totalDragX < -60f) {
                                val next = if (selectedDayIndex < 5) selectedDayIndex + 1 else 0
                                onDaySelected(next)
                            } else if (totalDragX > 60f) {
                                val prev = if (selectedDayIndex > 0) selectedDayIndex - 1 else 5
                                onDaySelected(prev)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDragX += dragAmount
                        }
                    )
                }
        ) {
            if (isLoading && archiveSchedule == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            } else {
                AnimatedContent(
                    targetState = selectedDayIndex,
                    transitionSpec = {
                        if (overlaySwipeAnimation) {
                            val forward = if (initialState == 0 && targetState == 5) false else if (initialState == 5 && targetState == 0) true else targetState > initialState
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
                    label = "archive_day_content"
                ) { dayIdx ->
                    val daySchedule = archiveSchedule?.days?.firstOrNull { it.dayIndex == dayIdx }
                    val lessons = daySchedule?.lessons ?: emptyList()

                    if (lessons.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "В архиве на этот день занятий не найдено",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val itemsUi = remember(lessons, isTeacherMode, targetName, showPairsSchedule) {
                            SchedulePairMerger.processLessons(
                                lessons = lessons,
                                isTeacherMode = isTeacherMode,
                                targetName = targetName,
                                showPairsSetting = showPairsSchedule
                            )
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                items(itemsUi) { item ->
                                    when (item) {
                                        is ScheduleItemUi.SingleLesson -> {
                                            val lesson = item.lesson
                                            val changes = archiveSchedule?.getChangesForLesson(dayIdx, lesson.num) ?: emptyList()
                                            LessonCard(
                                                lesson = lesson,
                                                change = changes.firstOrNull(),
                                                changes = changes,
                                                isActiveNow = false,
                                                isTeacherMode = isTeacherMode
                                            )
                                        }
                                        is ScheduleItemUi.CombinedPair -> {
                                            val changes1 = archiveSchedule?.getChangesForLesson(dayIdx, item.firstLesson.num) ?: emptyList()
                                            val changes2 = archiveSchedule?.getChangesForLesson(dayIdx, item.secondLesson.num) ?: emptyList()
                                            PairCard(
                                                pairUi = item,
                                                changesL1 = changes1,
                                                changesL2 = changes2,
                                                isActiveNow = false,
                                                isTeacherMode = isTeacherMode
                                            )
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }

                            // Static copy button in top-right corner over schedule
                            val dayNames = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
                            val fullDayTitle = "${dayNames.getOrElse(dayIdx) { "Понедельник" }}, $archiveDateLabel"
                            IconButton(
                                onClick = {
                                    val textToCopy = ScheduleFormatter.formatScheduleForCopy(
                                        dayTitle = fullDayTitle,
                                        lessons = lessons,
                                        scheduleResponse = archiveSchedule,
                                        dayIndex = dayIdx,
                                        isTeacherMode = isTeacherMode,
                                        targetName = targetName,
                                        showPairsSetting = showPairsSchedule
                                    )
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                    Toast.makeText(context, "Расписание скопировано", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 6.dp, top = 2.dp)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_copy_content),
                                    contentDescription = "Скопировать расписание",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
