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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.ClassItem
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.TeacherItem
import ru.nstu.lyceum.schedule.data.local.NoteType
import ru.nstu.lyceum.schedule.data.local.NotesRepository
import ru.nstu.lyceum.schedule.data.local.ScheduleNote
import ru.nstu.lyceum.schedule.data.local.SlotType
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.ui.components.NoteEditDialog
import ru.nstu.lyceum.schedule.ui.util.MarkdownRenderer
import ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper

@Composable
fun NotesScreen(
    notesRepository: NotesRepository,
    classesList: List<ClassItem> = emptyList(),
    teachersList: List<TeacherItem> = emptyList(),
    targetType: String = PreferencesRepository.TYPE_CLASS,
    targetName: String = "",
    isFavorite: Boolean = false,
    favoriteClass: String? = null,
    favoriteTeacher: String? = null,
    classTeachers: List<String> = emptyList(),
    teacherClasses: List<String> = emptyList(),
    onTargetTypeChanged: (String) -> Unit = {},
    onTargetNameChanged: (String) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    targetDateFocus: String? = null,
    scheduleResponse: ScheduleResponse?,
    userSubgroup: Int = 1,
    userTechSubgroup: Int = 1,
    isMiddleSchool: Boolean = false,
    overlaySwipeAnimation: Boolean = true
) {
    val isTeacherMode = targetType == PreferencesRepository.TYPE_TEACHER
    var dropdownExpanded by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }

    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val allNotes by notesRepository.notes.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, HW, NOTES, DEBTS
    var noteToEdit by remember { mutableStateOf<ScheduleNote?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createDialogType by remember(isTeacherMode) { mutableStateOf(if (isTeacherMode) NoteType.NOTE else NoteType.HOMEWORK) }

    val todayIso = remember { NotesTimeHelper.getTodayIso() }

    // Filter by target class / teacher
    val currentTargetNotes = remember(allNotes, targetName) {
        allNotes.filter { it.targetName == targetName }
    }

    val debts = remember(currentTargetNotes, todayIso) {
        currentTargetNotes.filter {
            it.type == NoteType.HOMEWORK && !it.isCompleted && (it.date < todayIso || it.transferPending)
        }.sortedBy { it.date }
    }

    val displayedNotes = remember(currentTargetNotes, selectedFilter, todayIso) {
        when (selectedFilter) {
            "HW" -> currentTargetNotes.filter { it.type == NoteType.HOMEWORK && (it.date >= todayIso || it.transferPending) }
                .sortedWith(compareBy({ if (it.transferPending) "9999-99-99" else it.date }, { it.lessonNum ?: 99 }))
            "NOTES" -> currentTargetNotes.filter { it.type == NoteType.NOTE && it.date >= todayIso }
                .sortedWith(compareBy({ it.date }, { it.slotType.ordinal }, { it.lessonNum ?: 99 }))
            "DEBTS" -> debts
            else -> currentTargetNotes.filter { it.date >= todayIso || it.transferPending }
                .sortedWith(compareBy({ if (it.transferPending) "9999-99-99" else it.date }, { it.type.ordinal }, { it.lessonNum ?: 99 }))
        }
    }

    LaunchedEffect(scheduleResponse) {
        if (scheduleResponse != null) {
            NotesTimeHelper.checkAndRelocateCancelledHomework(
                notesRepository = notesRepository,
                schedule = scheduleResponse,
                userSubgroup = userSubgroup,
                userTechSubgroup = userTechSubgroup,
                isMiddleSchool = isMiddleSchool
            )
            NotesTimeHelper.resolvePendingTransfers(
                notesRepository = notesRepository,
                schedule = scheduleResponse,
                userSubgroup = userSubgroup,
                userTechSubgroup = userTechSubgroup,
                isMiddleSchool = isMiddleSchool
            )
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to targetDateFocus if provided
    LaunchedEffect(targetDateFocus) {
        if (!targetDateFocus.isNullOrBlank()) {
            val idx = displayedNotes.indexOfFirst { it.date == targetDateFocus }
            if (idx >= 0) {
                listState.animateScrollToItem(idx)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header: Target Selector (Classes, Teachers, Dropdown, Favorite Star) & Filter Chips
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top row with Class/Teacher toggle and Selector Box with Star Favorite Button
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

                    // Filters row (horizontal scrollable list) and "Очистить сделанные"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" },
                                label = { Text("Все") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primaryContainer,
                                    selectedLabelColor = colorScheme.onPrimaryContainer
                                )
                            )

                            if (!isTeacherMode) {
                                FilterChip(
                                    selected = selectedFilter == "HW",
                                    onClick = { selectedFilter = "HW" },
                                    label = { Text("ДЗ") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colorScheme.primaryContainer,
                                        selectedLabelColor = colorScheme.onPrimaryContainer
                                    )
                                )
                            }

                            FilterChip(
                                selected = selectedFilter == "NOTES",
                                onClick = { selectedFilter = "NOTES" },
                                label = { Text("Заметки") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primaryContainer,
                                    selectedLabelColor = colorScheme.onPrimaryContainer
                                )
                            )

                            if (!isTeacherMode) {
                                FilterChip(
                                    selected = selectedFilter == "DEBTS",
                                    onClick = { selectedFilter = "DEBTS" },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Долги")
                                            if (debts.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = debts.size.toString(),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = colorScheme.onError
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colorScheme.errorContainer,
                                        selectedLabelColor = colorScheme.onErrorContainer
                                    )
                                )
                            }
                        }

                        val completedCount = currentTargetNotes.count { it.isCompleted }
                        if (completedCount > 0) {
                            Text(
                                text = "Очистить сделанные",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        notesRepository.cleanupOldCompleted(olderThanDays = 0)
                                        Toast.makeText(context, "Выполненные записи удалены", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)

            val filterTabs = remember(isTeacherMode) {
                if (isTeacherMode) listOf("ALL", "NOTES") else listOf("ALL", "HW", "NOTES", "DEBTS")
            }
            var totalDragX by remember { mutableFloatStateOf(0f) }

            // Side slides container with horizontal swipe detection and flip/slide animation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedFilter, isTeacherMode) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onDragEnd = {
                                val currentIdx = filterTabs.indexOf(selectedFilter)
                                if (currentIdx != -1 && filterTabs.isNotEmpty()) {
                                    if (totalDragX < -60f) {
                                        // Swipe left -> next slide (cyclic)
                                        val nextIdx = (currentIdx + 1) % filterTabs.size
                                        selectedFilter = filterTabs[nextIdx]
                                    } else if (totalDragX > 60f) {
                                        // Swipe right -> previous slide (cyclic)
                                        val prevIdx = if (currentIdx - 1 < 0) filterTabs.size - 1 else currentIdx - 1
                                        selectedFilter = filterTabs[prevIdx]
                                    }
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDragX += dragAmount
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = selectedFilter,
                    transitionSpec = {
                        if (overlaySwipeAnimation) {
                            val targetIdx = filterTabs.indexOf(targetState)
                            val initialIdx = filterTabs.indexOf(initialState)
                            val forward = when {
                                initialIdx == filterTabs.size - 1 && targetIdx == 0 -> true
                                initialIdx == 0 && targetIdx == filterTabs.size - 1 -> false
                                else -> targetIdx > initialIdx
                            }
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
                    label = "notes_slide_content"
                ) { currentFilter ->
                    val filterNotes = remember(currentTargetNotes, currentFilter, todayIso) {
                        when (currentFilter) {
                            "HW" -> currentTargetNotes.filter { it.type == NoteType.HOMEWORK && it.date >= todayIso }
                                .sortedWith(compareBy({ it.date }, { it.lessonNum ?: 99 }))
                            "NOTES" -> currentTargetNotes.filter { it.type == NoteType.NOTE && it.date >= todayIso }
                                .sortedWith(compareBy({ it.date }, { it.slotType.ordinal }, { it.lessonNum ?: 99 }))
                            "DEBTS" -> debts
                            else -> currentTargetNotes.filter { it.date >= todayIso }
                                .sortedWith(compareBy({ it.date }, { it.type.ordinal }, { it.lessonNum ?: 99 }))
                        }
                    }

                    if (filterNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (currentFilter) {
                                    "DEBTS" -> "Долгов нет! Все задания выполнены."
                                    "HW" -> "Нет актуальных домашних заданий."
                                    "NOTES" -> "Нет заметок."
                                    else -> "Записей пока нет. Нажмите +, чтобы добавить!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filterNotes, key = { it.id }) { note ->
                                val isDebt = note.type == NoteType.HOMEWORK && !note.isCompleted && note.date < todayIso

                                NoteItemCard(
                                    note = note,
                                    isDebt = isDebt,
                                    onToggleCompleted = { notesRepository.toggleCompleted(note.id) },
                                    onEdit = { noteToEdit = note },
                                    onDelete = {
                                        notesRepository.deleteNote(note.id)
                                        Toast.makeText(context, "Запись удалена", Toast.LENGTH_SHORT).show()
                                    },
                                    onTransferToNextLesson = {
                                        val subj = note.subject
                                        if (!subj.isNullOrBlank()) {
                                            val nextInWeek = NotesTimeHelper.findNextLessonInCurrentWeek(
                                                schedule = scheduleResponse,
                                                subject = subj,
                                                targetSubgroup = note.subgroup,
                                                fromDateIso = todayIso,
                                                afterLessonNum = if (note.date == todayIso) note.lessonNum else null,
                                                userTechSubgroup = userTechSubgroup,
                                                isMiddleSchool = isMiddleSchool
                                            )
                                            if (nextInWeek != null) {
                                                notesRepository.transferHomework(
                                                    id = note.id,
                                                    targetDate = nextInWeek.first,
                                                    targetLessonNum = nextInWeek.second
                                                )
                                                val dateLabel = NotesTimeHelper.formatDateRussian(nextInWeek.first)
                                                Toast.makeText(context, "Перенесено на $dateLabel (${nextInWeek.second} урок)", Toast.LENGTH_SHORT).show()
                                            } else {
                                                notesRepository.markTransferPending(note.id)
                                                Toast.makeText(
                                                    context,
                                                    "Перенесено на след. неделю (будет привязано к ближайшему уроку)",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(72.dp))
                            }
                        }
                    }
                }
            }
        }

        // FAB to Add Note
        FloatingActionButton(
            onClick = {
                createDialogType = when {
                    isTeacherMode -> NoteType.NOTE
                    selectedFilter == "NOTES" -> NoteType.NOTE
                    else -> NoteType.HOMEWORK
                }
                showCreateDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить запись")
        }
    }

    // Dialog for creating
    if (showCreateDialog) {
        NoteEditDialog(
            defaultType = createDialogType,
            targetName = targetName,
            isTeacherMode = isTeacherMode,
            defaultDateIso = targetDateFocus ?: todayIso,
            userSubgroup = userSubgroup,
            userTechSubgroup = userTechSubgroup,
            isMiddleSchool = isMiddleSchool,
            scheduleResponse = scheduleResponse,
            onSave = { newNote ->
                notesRepository.addNote(newNote)
                showCreateDialog = false
                Toast.makeText(context, "Запись сохранена", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Dialog for editing
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
            scheduleResponse = scheduleResponse,
            onSave = { updatedNote ->
                notesRepository.updateNote(updatedNote)
                noteToEdit = null
                Toast.makeText(context, "Запись обновлена", Toast.LENGTH_SHORT).show()
            },
            onDelete = { noteId ->
                notesRepository.deleteNote(noteId)
                noteToEdit = null
                Toast.makeText(context, "Запись удалена", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { noteToEdit = null }
        )
    }
}

@Composable
private fun NoteItemCard(
    note: ScheduleNote,
    isDebt: Boolean,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTransferToNextLesson: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isCompleted = note.isCompleted

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isDebt -> colorScheme.errorContainer.copy(alpha = 0.2f)
            isCompleted -> colorScheme.surfaceContainerLow
            else -> colorScheme.surfaceContainerHigh
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Header Row: Type / Slot info + Date badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val typeLabel = when {
                        note.type == NoteType.HOMEWORK -> note.subject ?: "ДЗ"
                        note.slotType == SlotType.BEFORE_LESSONS -> "До уроков"
                        note.slotType == SlotType.AFTER_LESSONS -> "После уроков"
                        note.slotType == SlotType.LESSON -> "Урок ${note.lessonNum ?: ""}"
                        else -> "Заметка"
                    }

                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            isDebt -> colorScheme.error
                            isCompleted -> colorScheme.outlineVariant
                            else -> colorScheme.onSurface
                        }
                    )

                    if (note.slotType == SlotType.LESSON && note.type == NoteType.HOMEWORK && note.lessonNum != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${note.lessonNum} урок)",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Date or Transfer Badge
                if (note.transferPending) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "Перенос на след. неделю",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = NotesTimeHelper.formatDateWithDay(note.date),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDebt) colorScheme.error else colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Text with markdown
            val parsedText = MarkdownRenderer.parseMarkdown(note.text)
            Text(
                text = parsedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 17.sp,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (isCompleted) colorScheme.outlineVariant else colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completed Checkbox / Status
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCompleted) colorScheme.primaryContainer else colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onToggleCompleted() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isCompleted) colorScheme.onPrimaryContainer else colorScheme.outline,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCompleted) "Сдано" else "Не сдано",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isCompleted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDebt && !note.transferPending) {
                        // Transfer to next lesson button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clickable { onTransferToNextLesson() }
                                .padding(end = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Update,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "На след. урок",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
