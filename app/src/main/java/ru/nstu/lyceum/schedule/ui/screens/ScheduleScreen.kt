package ru.nstu.lyceum.schedule.ui.screens

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import ru.nstu.lyceum.schedule.ui.components.WeekScheduleTable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import ru.nstu.lyceum.schedule.data.local.NoteType
import ru.nstu.lyceum.schedule.data.local.NotesRepository
import ru.nstu.lyceum.schedule.data.local.ScheduleNote
import ru.nstu.lyceum.schedule.data.local.SlotType
import ru.nstu.lyceum.schedule.ui.components.NoteEditDialog
import ru.nstu.lyceum.schedule.ui.components.SlotNoteCard
import ru.nstu.lyceum.schedule.ui.components.StandaloneLessonNoteCard
import ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper
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
import ru.nstu.lyceum.schedule.ui.components.PairCard
import ru.nstu.lyceum.schedule.ui.components.ScheduleItemUi
import ru.nstu.lyceum.schedule.ui.components.SchedulePairMerger
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.ui.util.ScheduleFormatter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.ClassItem
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.TeacherItem
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.domain.BellState
import ru.nstu.lyceum.schedule.domain.PairSchedule
import ru.nstu.lyceum.schedule.domain.RealtimeSyncManager
import ru.nstu.lyceum.schedule.domain.VacationChecker
import ru.nstu.lyceum.schedule.ui.components.DayCycleHelper
import ru.nstu.lyceum.schedule.ui.components.DaySelector
import ru.nstu.lyceum.schedule.ui.components.LessonCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun getScheduleDateLabel(weekStartStr: String?, dayIndex: Int): String {
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
    val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mon..6=Sun
    cal.add(Calendar.DAY_OF_MONTH, -dayOfWeek + dayIndex)
    val sdf = SimpleDateFormat("d MMMM", Locale("ru"))
    return sdf.format(cal.time)
}

fun getScheduleDayDateIso(weekStartStr: String?, dayIndex: Int): String {
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
    val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mon..6=Sun
    cal.add(Calendar.DAY_OF_MONTH, -dayOfWeek + dayIndex)
    return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

@Composable
fun ScheduleScreen(
    currentSchedule: ScheduleResponse?,
    classesList: List<ClassItem>,
    teachersList: List<TeacherItem>,
    targetType: String,
    targetName: String,
    isFavorite: Boolean,
    selectedDayIndex: Int,
    realtimeState: RealtimeSyncManager.RealtimeState,
    isLoading: Boolean,
    showPairsSchedule: Boolean,
    statusBarPairsMode: Boolean,
    overlaySwipeAnimation: Boolean,
    notesRepository: NotesRepository? = null,
    copyIncludeHomework: Boolean = false,
    copyIncludeNotes: Boolean = false,
    userSubgroup: Int = 1,
    userTechSubgroup: Int = 1,
    favoriteClass: String? = null,
    favoriteTeacher: String? = null,
    classTeachers: List<String> = emptyList(),
    teacherClasses: List<String> = emptyList(),
    onTargetTypeChanged: (String) -> Unit,
    onTargetNameChanged: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDaySelected: (Int) -> Unit,
    onTeacherClassClick: ((String) -> Unit)? = null,
    onRoomClick: ((room: String, lessonNum: Int) -> Unit)? = null,
    onTableRoomClick: ((dayIndex: Int, room: String, lessonNum: Int) -> Unit)? = null,
    onNavigateToCalendar: (String) -> Unit = {},
    onNavigateToNotes: (String) -> Unit = {}
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    var noteMenuExpanded by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var dialogDefaultType by remember { mutableStateOf(NoteType.HOMEWORK) }
    var dialogDefaultLessonNum by remember { mutableStateOf<Int?>(null) }
    var dialogDefaultSubject by remember { mutableStateOf<String?>(null) }
    var dialogTargetDayIndex by remember { mutableIntStateOf(if (selectedDayIndex in 0..5) selectedDayIndex else 0) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var lastSingleDayIndex by rememberSaveable { mutableIntStateOf(if (selectedDayIndex in 0..5) selectedDayIndex else 0) }

    LaunchedEffect(selectedDayIndex) {
        if (selectedDayIndex in 0..5) {
            lastSingleDayIndex = selectedDayIndex
        }
    }
    LaunchedEffect(isLandscape, selectedDayIndex) {
        if (!isLandscape && selectedDayIndex == 6) {
            onDaySelected(lastSingleDayIndex)
        }
    }

    val allNotes = notesRepository?.notes?.collectAsState()?.value ?: emptyList()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val colorScheme = MaterialTheme.colorScheme
    val isTeacherMode = targetType == PreferencesRepository.TYPE_TEACHER

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Top Unified Header Bar: [ Классы | Учителя ] ... [ 👥 10-1 ▼ ] [ ★ ] + Day Switching
        val scheduleDateLabel = remember(currentSchedule?.weekStart, selectedDayIndex, isLandscape) {
            if (isLandscape && selectedDayIndex == 6) {
                ScheduleFormatter.getWeekRangeGenitiveLabel(currentSchedule?.weekStart, 0)
            } else {
                getScheduleDateLabel(currentSchedule?.weekStart, selectedDayIndex)
            }
        }
        val notesByDayIndex = remember(allNotes, targetName, currentSchedule?.weekStart) {
            (0..5).associateWith { d ->
                val dIso = NotesTimeHelper.getDateIsoForDayIndex(currentSchedule?.weekStart, d)
                allNotes.filter { it.targetName == targetName && it.date == dIso }
            }
        }
        val isThisWeek = remember(currentSchedule?.weekStart) {
            BellSchedule.isCurrentScheduleWeek(currentSchedule?.weekStart)
        }
        val activeDayIndex = remember(isThisWeek, realtimeState.dayIndex) {
            if (isThisWeek) {
                val cal = Calendar.getInstance()
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                if ((dow == Calendar.SATURDAY && hour >= 15) || dow == Calendar.SUNDAY) {
                    -1
                } else {
                    realtimeState.dayIndex
                }
            } else {
                -1
            }
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
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Two flat rectangles of background color without borders/corners
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

                    // Right: Target Selector Box and Star Favorite Button
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
                                        maxLines = 1
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
                                    .fillMaxWidth(0.85f)
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

                        // Favorite Star Button: fill in color of header, no borders/box outline, dark gray star
                        val favIconColor = colorScheme.onSurfaceVariant

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Избранное",
                                tint = favIconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Day navigation with arrows, label, and day selector in the unified header
                DaySelector(
                    selectedDayIndex = selectedDayIndex,
                    currentDayIndex = activeDayIndex,
                    dateLabel = scheduleDateLabel,
                    showAllOption = isLandscape,
                    onDaySelected = onDaySelected,
                    onDateClick = {
                        val focusDayIdx = if (selectedDayIndex in 0..5) selectedDayIndex else 0
                        val iso = getScheduleDayDateIso(currentSchedule?.weekStart, focusDayIdx)
                        onNavigateToCalendar(iso)
                    },
                    trailingActionContent = if (isLandscape && selectedDayIndex == 6) {
                        {
                            IconButton(
                                onClick = {
                                    val textToCopy = ScheduleFormatter.formatWeekScheduleForCopy(
                                        scheduleResponse = currentSchedule,
                                        fallbackWeekOffset = 0,
                                        isTeacherMode = isTeacherMode,
                                        targetName = targetName,
                                        showPairsSetting = showPairsSchedule,
                                        notesByDayIndex = notesByDayIndex,
                                        includeHomework = copyIncludeHomework,
                                        includeNotes = copyIncludeNotes
                                    )
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                    Toast.makeText(context, "Расписание недели скопировано", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_copy_content),
                                    contentDescription = "Скопировать расписание недели",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (notesRepository != null) {
                                Box {
                                    IconButton(
                                        onClick = { noteMenuExpanded = true },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Заметки",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = noteMenuExpanded,
                                        onDismissRequest = { noteMenuExpanded = false },
                                        modifier = Modifier.background(colorScheme.surfaceContainerHigh)
                                    ) {
                                        val fallbackDay = if (activeDayIndex in 0..5) activeDayIndex else lastSingleDayIndex
                                        val fallbackDateIso = NotesTimeHelper.getDateIsoForDayIndex(currentSchedule?.weekStart, fallbackDay)
                                        if (!isTeacherMode) {
                                            DropdownMenuItem(
                                                text = { Text("Записать ДЗ", style = MaterialTheme.typography.bodyMedium) },
                                                onClick = {
                                                    noteMenuExpanded = false
                                                    dialogTargetDayIndex = fallbackDay
                                                    dialogDefaultType = NoteType.HOMEWORK
                                                    dialogDefaultLessonNum = null
                                                    dialogDefaultSubject = null
                                                    showNoteDialog = true
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Своя заметка", style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                noteMenuExpanded = false
                                                dialogTargetDayIndex = fallbackDay
                                                dialogDefaultType = NoteType.NOTE
                                                dialogDefaultLessonNum = null
                                                dialogDefaultSubject = null
                                                showNoteDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Управление заметками", style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                noteMenuExpanded = false
                                                onNavigateToNotes(fallbackDateIso)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else null
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)

        // Lessons List Content with horizontal swipe support
        var totalDragX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedDayIndex, isLandscape) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onDragEnd = {
                            if (totalDragX < -60f) {
                                onDaySelected(DayCycleHelper.nextDay(selectedDayIndex, isLandscape))
                            } else if (totalDragX > 60f) {
                                onDaySelected(DayCycleHelper.prevDay(selectedDayIndex, isLandscape))
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDragX += dragAmount
                        }
                    )
                }
        ) {
            if (isLoading && currentSchedule == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            } else {
                AnimatedContent(
                    targetState = selectedDayIndex,
                    transitionSpec = {
                        if (overlaySwipeAnimation) {
                            val forward = DayCycleHelper.isForwardTransition(initialState, targetState, isLandscape)
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
                    label = "schedule_day_content"
                ) { dayIdx ->
                    if (dayIdx == 6 && isLandscape) {
                        val isVacation = VacationChecker.isVacation(context)
                        val currentLessonBell = remember(realtimeState) {
                            BellSchedule.getCurrentBellState(Calendar.getInstance(), isVacation)
                        }
                        val currentPairBell = remember(realtimeState) {
                            PairSchedule.getCurrentPairState(Calendar.getInstance(), isVacation)
                        }
                        val activeLessonNum = (currentLessonBell as? BellState.InLesson)?.num
                        val activePairNum = (currentPairBell as? BellState.InLesson)?.num

                        WeekScheduleTable(
                            schedule = currentSchedule,
                            weekStartFallbackOffset = 0,
                            isTeacherMode = isTeacherMode,
                            targetName = targetName,
                            showPairsSchedule = showPairsSchedule,
                            activeDayIndex = activeDayIndex,
                            activeLessonNum = activeLessonNum,
                            activePairNum = activePairNum,
                            notesByDayIndex = notesByDayIndex,
                            userSubgroup = userSubgroup,
                            userTechSubgroup = userTechSubgroup,
                            emptyText = "На эту неделю занятий нет",
                            onToggleNoteCompleted = { notesRepository?.toggleCompleted(it) },
                            onDayClick = { clickedDay -> onDaySelected(clickedDay) },
                            onDateClick = { dateIso -> onNavigateToCalendar(dateIso) },
                            onTeacherClassClick = onTeacherClassClick,
                            onRoomClick = if (onTableRoomClick != null) {
                                onTableRoomClick
                            } else if (onRoomClick != null) {
                                { _, room, lsn -> onRoomClick(room, lsn) }
                            } else null,
                            onLessonCellClick = if (notesRepository != null && !isTeacherMode) {
                                { clickedDay, lsnNum ->
                                    val dayIso = NotesTimeHelper.getDateIsoForDayIndex(currentSchedule?.weekStart, clickedDay)
                                    val isMiddle = Regex("^[5-7][а-яА-Яa-zA-Z].*").matches(targetName.trim())
                                    val effSubj = NotesTimeHelper.getEffectiveLessonSubject(
                                        schedule = currentSchedule,
                                        dateIso = dayIso,
                                        lessonNum = lsnNum,
                                        userSubgroup = userSubgroup,
                                        userTechSubgroup = userTechSubgroup,
                                        isMiddleSchool = isMiddle
                                    )
                                    dialogTargetDayIndex = clickedDay
                                    dialogDefaultType = NoteType.HOMEWORK
                                    dialogDefaultLessonNum = lsnNum
                                    dialogDefaultSubject = effSubj
                                    showNoteDialog = true
                                }
                            } else null
                        )
                    } else {
                    val daySchedule = currentSchedule?.days?.firstOrNull { it.dayIndex == dayIdx }
                    val lessons = daySchedule?.lessons ?: emptyList()

                    if (lessons.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "В этот день занятий нет",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = colorScheme.outline
                            )
                        }
                    } else {
                        val isToday = dayIdx == realtimeState.dayIndex && isThisWeek
                        val isVacation = VacationChecker.isVacation(context)
                        val currentLessonBell = remember(realtimeState) {
                            BellSchedule.getCurrentBellState(Calendar.getInstance(), isVacation)
                        }
                        val currentPairBell = remember(realtimeState) {
                            PairSchedule.getCurrentPairState(Calendar.getInstance(), isVacation)
                        }
                        val activeLessonNum = (currentLessonBell as? BellState.InLesson)?.num
                        val activePairNum = (currentPairBell as? BellState.InLesson)?.num

                        val canShowPencilButton = notesRepository != null

                        val selectedDateIso = remember(currentSchedule?.weekStart, dayIdx) {
                            NotesTimeHelper.getDateIsoForDayIndex(currentSchedule?.weekStart, dayIdx)
                        }
                        val notesForDay = remember(allNotes, targetName, selectedDateIso) {
                            allNotes.filter { it.targetName == targetName && it.date == selectedDateIso }
                        }
                        val beforeNote = notesForDay.firstOrNull { it.slotType == SlotType.BEFORE_LESSONS }
                        val afterNote = notesForDay.firstOrNull { it.slotType == SlotType.AFTER_LESSONS }

                        // Subgroup filtering for lesson homework/notes
                        fun filterNotesForLesson(lsnNum: Int): List<ScheduleNote> {
                            return notesForDay.filter {
                                it.slotType == SlotType.LESSON && it.lessonNum == lsnNum &&
                                (it.subgroup == null || it.subgroup == userSubgroup || (it.subject?.contains("технолог", ignoreCase = true) == true && it.subgroup == userTechSubgroup))
                            }
                        }

                        val itemsUi = remember(lessons, isTeacherMode, targetName, showPairsSchedule) {
                            SchedulePairMerger.processLessons(
                                lessons = lessons,
                                isTeacherMode = isTeacherMode,
                                targetName = targetName,
                                showPairsSetting = showPairsSchedule
                            )
                        }

                        // Notes on slots not present in the schedule (windows or teacher's free day)
                        val scheduledLessonNums = remember(lessons) { lessons.map { it.num }.toSet() }
                        val orphanLessonNotes = remember(notesForDay, scheduledLessonNums, userSubgroup, userTechSubgroup) {
                            notesForDay.filter {
                                it.slotType == SlotType.LESSON && it.lessonNum != null && it.lessonNum !in scheduledLessonNums &&
                                (it.subgroup == null || it.subgroup == userSubgroup || (it.subject?.contains("технолог", ignoreCase = true) == true && it.subgroup == userTechSubgroup))
                            }.sortedBy { it.lessonNum }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                if (beforeNote != null) {
                                    item {
                                        SlotNoteCard(
                                            note = beforeNote,
                                            title = "До уроков",
                                            onToggleCompleted = { notesRepository?.toggleCompleted(beforeNote.id) }
                                        )
                                    }
                                }

                                if (itemsUi.isEmpty() && orphanLessonNotes.isEmpty() && beforeNote == null && afterNote == null) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Занятий нет",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                items(itemsUi) { item ->
                                    when (item) {
                                        is ScheduleItemUi.SingleLesson -> {
                                            val lesson = item.lesson
                                            val isActive = isToday && activeLessonNum == lesson.num
                                            val changes = currentSchedule?.getChangesForLesson(dayIdx, lesson.num) ?: emptyList()
                                            val lNotes = filterNotesForLesson(lesson.num)
                                            LessonCard(
                                                lesson = lesson,
                                                change = changes.firstOrNull(),
                                                changes = changes,
                                                isActiveNow = isActive,
                                                isTeacherMode = isTeacherMode,
                                                notes = lNotes,
                                                onToggleNoteCompleted = { notesRepository?.toggleCompleted(it) },
                                                onTeacherClassClick = onTeacherClassClick,
                                                onRoomClick = onRoomClick,
                                                onClick = if (notesRepository != null && !isTeacherMode) {
                                                    {
                                                        val isMiddle = Regex("^[5-7][а-яА-Яa-zA-Z].*").matches(targetName.trim())
                                                        val effSubj = NotesTimeHelper.getEffectiveLessonSubject(
                                                            schedule = currentSchedule,
                                                            dateIso = selectedDateIso,
                                                            lessonNum = lesson.num,
                                                            userSubgroup = userSubgroup,
                                                            userTechSubgroup = userTechSubgroup,
                                                            isMiddleSchool = isMiddle
                                                        )
                                                        dialogTargetDayIndex = dayIdx
                                                        dialogDefaultType = NoteType.HOMEWORK
                                                        dialogDefaultLessonNum = lesson.num
                                                        dialogDefaultSubject = effSubj
                                                        showNoteDialog = true
                                                    }
                                                } else null
                                            )
                                        }
                                        is ScheduleItemUi.CombinedPair -> {
                                            val changes1 = currentSchedule?.getChangesForLesson(dayIdx, item.firstLesson.num) ?: emptyList()
                                            val changes2 = currentSchedule?.getChangesForLesson(dayIdx, item.secondLesson.num) ?: emptyList()
                                            val isPairActive = isToday && (activePairNum == item.pairNum || activeLessonNum == item.firstLesson.num || activeLessonNum == item.secondLesson.num)
                                            val isL1Active = isToday && (activeLessonNum == item.firstLesson.num)
                                            val isL2Active = isToday && (activeLessonNum == item.secondLesson.num)
                                            val pNotes = filterNotesForLesson(item.firstLesson.num) + filterNotesForLesson(item.secondLesson.num)
                                            PairCard(
                                                pairUi = item,
                                                changesL1 = changes1,
                                                changesL2 = changes2,
                                                isActiveNow = isPairActive,
                                                isActiveL1 = isL1Active,
                                                isActiveL2 = isL2Active,
                                                isTeacherMode = isTeacherMode,
                                                notes = pNotes,
                                                onToggleNoteCompleted = { notesRepository?.toggleCompleted(it) },
                                                onTeacherClassClick = onTeacherClassClick,
                                                onRoomClick = onRoomClick,
                                                onLessonClick = if (notesRepository != null && !isTeacherMode) {
                                                    { lsnNum, _ ->
                                                        val isMiddle = Regex("^[5-7][а-яА-Яa-zA-Z].*").matches(targetName.trim())
                                                        val effSubj = NotesTimeHelper.getEffectiveLessonSubject(
                                                            schedule = currentSchedule,
                                                            dateIso = selectedDateIso,
                                                            lessonNum = lsnNum,
                                                            userSubgroup = userSubgroup,
                                                            userTechSubgroup = userTechSubgroup,
                                                            isMiddleSchool = isMiddle
                                                        )
                                                        dialogTargetDayIndex = dayIdx
                                                        dialogDefaultType = NoteType.HOMEWORK
                                                        dialogDefaultLessonNum = lsnNum
                                                        dialogDefaultSubject = effSubj
                                                        showNoteDialog = true
                                                    }
                                                } else null,
                                                onClick = if (notesRepository != null && !isTeacherMode) {
                                                    {
                                                        val isMiddle = Regex("^[5-7][а-яА-Яa-zA-Z].*").matches(targetName.trim())
                                                        val effSubj = NotesTimeHelper.getEffectiveLessonSubject(
                                                            schedule = currentSchedule,
                                                            dateIso = selectedDateIso,
                                                            lessonNum = item.firstLesson.num,
                                                            userSubgroup = userSubgroup,
                                                            userTechSubgroup = userTechSubgroup,
                                                            isMiddleSchool = isMiddle
                                                        )
                                                        dialogTargetDayIndex = dayIdx
                                                        dialogDefaultType = NoteType.HOMEWORK
                                                        dialogDefaultLessonNum = item.firstLesson.num
                                                        dialogDefaultSubject = effSubj
                                                        showNoteDialog = true
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                }

                                // Render any notes for windows or free periods (Point 12)
                                items(orphanLessonNotes, key = { "orphan_${it.id}" }) { orphanNote ->
                                    val bell = BellSchedule.BELLS.firstOrNull { it.num == orphanNote.lessonNum }
                                    val timeStr = bell?.timeRange ?: ""
                                    StandaloneLessonNoteCard(
                                        note = orphanNote,
                                        timeRange = timeStr,
                                        onToggleCompleted = { notesRepository?.toggleCompleted(orphanNote.id) }
                                    )
                                }

                                if (afterNote != null) {
                                    item {
                                        SlotNoteCard(
                                            note = afterNote,
                                            title = "После уроков",
                                            onToggleCompleted = { notesRepository?.toggleCompleted(afterNote.id) }
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }

                            // Static copy button in top-right corner over schedule
                            val dayNames = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
                            val dayDateText = ScheduleFormatter.getDayDateGenitiveLabel(currentSchedule?.weekStart, dayIdx)
                            val fullDayTitle = "${dayNames.getOrElse(dayIdx) { "Понедельник" }}, $dayDateText"
                            IconButton(
                                onClick = {
                                    val textToCopy = ScheduleFormatter.formatScheduleForCopy(
                                        dayTitle = fullDayTitle,
                                        lessons = lessons,
                                        scheduleResponse = currentSchedule,
                                        dayIndex = dayIdx,
                                        isTeacherMode = isTeacherMode,
                                        targetName = targetName,
                                        showPairsSetting = showPairsSchedule,
                                        notesForDay = notesForDay,
                                        includeHomework = copyIncludeHomework,
                                        includeNotes = copyIncludeNotes
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

                            // Pencil icon below copy icon (visible only when isFavorite and lessons today are not ended)
                            if (canShowPencilButton) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 6.dp, top = 40.dp)
                                ) {
                                    IconButton(
                                        onClick = { noteMenuExpanded = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Заметки",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = noteMenuExpanded,
                                        onDismissRequest = { noteMenuExpanded = false },
                                        modifier = Modifier.background(colorScheme.surfaceContainerHigh)
                                    ) {
                                        if (!isTeacherMode) {
                                            DropdownMenuItem(
                                                text = { Text("Записать ДЗ", style = MaterialTheme.typography.bodyMedium) },
                                                onClick = {
                                                    noteMenuExpanded = false
                                                    dialogTargetDayIndex = dayIdx
                                                    dialogDefaultType = NoteType.HOMEWORK
                                                    dialogDefaultLessonNum = null
                                                    dialogDefaultSubject = null
                                                    showNoteDialog = true
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Своя заметка", style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                noteMenuExpanded = false
                                                dialogTargetDayIndex = dayIdx
                                                dialogDefaultType = NoteType.NOTE
                                                dialogDefaultLessonNum = null
                                                dialogDefaultSubject = null
                                                showNoteDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Управление заметками", style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                noteMenuExpanded = false
                                                onNavigateToNotes(selectedDateIso)
                                            }
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

    LaunchedEffect(currentSchedule) {
        if (currentSchedule != null && notesRepository != null) {
            val isMiddle = Regex("^[5-7][а-яА-Яa-zA-Z].*").matches(targetName.trim())
            NotesTimeHelper.checkAndRelocateCancelledHomework(
                notesRepository = notesRepository,
                schedule = currentSchedule,
                userSubgroup = userSubgroup,
                userTechSubgroup = userTechSubgroup,
                isMiddleSchool = isMiddle
            )
            NotesTimeHelper.resolvePendingTransfers(
                notesRepository = notesRepository,
                schedule = currentSchedule,
                userSubgroup = userSubgroup,
                userTechSubgroup = userTechSubgroup,
                isMiddleSchool = isMiddle
            )
        }
    }

    if (showNoteDialog) {
        val currentDayIdx = dialogTargetDayIndex.coerceIn(0, 5)
        val targetDateIso = NotesTimeHelper.getDateIsoForDayIndex(currentSchedule?.weekStart, currentDayIdx)
        val isMiddle = Regex("^[5-7][а-яА-Яa-zA-Z].*").matches(targetName.trim())
        val available = NotesTimeHelper.getAvailableLessonsForDate(
            schedule = currentSchedule,
            dateIso = targetDateIso,
            userSubgroup = userSubgroup,
            userTechSubgroup = userTechSubgroup,
            isMiddleSchool = isMiddle
        )

        NoteEditDialog(
            defaultType = dialogDefaultType,
            targetName = targetName,
            isTeacherMode = isTeacherMode,
            defaultDateIso = targetDateIso,
            defaultLessonNum = dialogDefaultLessonNum,
            defaultSubject = dialogDefaultSubject,
            availableLessons = available,
            userSubgroup = userSubgroup,
            userTechSubgroup = userTechSubgroup,
            isMiddleSchool = isMiddle,
            scheduleResponse = currentSchedule,
            onSave = { newNote ->
                notesRepository?.addNote(newNote)
                showNoteDialog = false
                Toast.makeText(context, "Запись сохранена", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showNoteDialog = false }
        )
    }
}
