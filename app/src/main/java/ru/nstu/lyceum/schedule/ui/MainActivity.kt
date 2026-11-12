package ru.nstu.lyceum.schedule.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.nstu.lyceum.schedule.LyceumApp
import ru.nstu.lyceum.schedule.data.api.CalendarEvent
import ru.nstu.lyceum.schedule.data.api.ClassItem
import ru.nstu.lyceum.schedule.data.api.EventCategory
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.TeacherItem
import ru.nstu.lyceum.schedule.data.local.FreeAvailabilityEngine
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.ui.components.HeaderView
import ru.nstu.lyceum.schedule.ui.screens.ArchiveScreen
import ru.nstu.lyceum.schedule.ui.screens.CalendarScreen
import ru.nstu.lyceum.schedule.ui.screens.FreeRoomsScreen
import ru.nstu.lyceum.schedule.ui.screens.ScheduleScreen
import ru.nstu.lyceum.schedule.ui.screens.SettingsDialog
import ru.nstu.lyceum.schedule.ui.theme.NikaBorder
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject
import ru.nstu.lyceum.schedule.ui.theme.ScheduleXYTheme
import ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider
import java.util.Calendar

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LyceumApp
        val repo = app.scheduleRepository
        val prefs = app.preferencesRepository
        val realtime = app.realtimeSyncManager

        setContent {
            ScheduleXYTheme {
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }
                val realtimeState by realtime.state.collectAsState()

                var currentTab by remember { mutableIntStateOf(0) }
                var showSettingsDialog by remember { mutableStateOf(false) }

                var targetType by remember { mutableStateOf(prefs.targetType) }
                var targetName by remember { mutableStateOf(prefs.targetName) }
                var isFavorite by remember { mutableStateOf(prefs.isFavorite(targetType, targetName)) }

                val initialDay = BellSchedule.getScheduleDayIndex(Calendar.getInstance()).coerceIn(0, 5)
                var selectedDayIndex by remember { mutableIntStateOf(initialDay) }

                var classesList by remember { mutableStateOf<List<ClassItem>>(emptyList()) }
                var teachersList by remember { mutableStateOf<List<TeacherItem>>(emptyList()) }
                var currentSchedule by remember { mutableStateOf<ScheduleResponse?>(null) }
                var archiveSchedule by remember { mutableStateOf<ScheduleResponse?>(null) }
                var eventsList by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
                var eventCategories by remember { mutableStateOf<List<EventCategory>>(emptyList()) }

                var selectedArchiveWeek by remember { mutableIntStateOf(-1) }
                var archiveDayIndex by remember { mutableIntStateOf(0) }

                var freeDayIndex by remember { mutableIntStateOf(initialDay) }
                var freeLessonNum by remember { mutableIntStateOf(1) }
                var freeOccupancy by remember { mutableStateOf<FreeAvailabilityEngine.SlotOccupancy?>(null) }

                var isLoading by remember { mutableStateOf(false) }

                // Load initial classes and teachers
                LaunchedEffect(Unit) {
                    isLoading = true
                    try {
                        val cls = repo.getClasses()
                        classesList = cls.items

                        val tch = repo.getTeachers()
                        teachersList = tch.items

                        // If favorite is set, load favorite first
                        if (prefs.favoriteTargetName != null && prefs.favoriteTargetType != null) {
                            targetType = prefs.favoriteTargetType!!
                            targetName = prefs.favoriteTargetName!!
                            isFavorite = true
                        }

                        // Load schedule for target
                        val res = repo.getSchedule(targetType, targetName, 0)
                        currentSchedule = res.getOrNull()

                        // Load events
                        val ev = repo.getEvents()
                        eventsList = ev.items
                        eventCategories = ev.categories
                    } finally {
                        isLoading = false
                    }
                }

                // Reload schedule on target change
                fun loadCurrentSchedule(type: String, name: String) {
                    scope.launch {
                        isLoading = true
                        prefs.targetType = type
                        prefs.targetName = name
                        targetType = type
                        targetName = name
                        isFavorite = prefs.isFavorite(type, name)
                        val res = repo.getSchedule(type, name, 0)
                        currentSchedule = res.getOrNull()
                        isLoading = false
                        // Notify widget to update with new target
                        ScheduleWidgetProvider.notifyWidgetUpdate(this@MainActivity)
                    }
                }

                // Load archive on demand
                fun loadArchive(week: Int) {
                    scope.launch {
                        isLoading = true
                        selectedArchiveWeek = week
                        val res = repo.getSchedule(targetType, targetName, week)
                        archiveSchedule = res.getOrNull()
                        isLoading = false
                    }
                }

                // Load free rooms and teachers
                fun loadFree(dayIdx: Int, lsnNum: Int) {
                    scope.launch {
                        isLoading = true
                        freeDayIndex = dayIdx
                        freeLessonNum = lsnNum
                        val allClassScheds = app.cacheManager.getAllCachedClassSchedules(0)
                        freeOccupancy = FreeAvailabilityEngine.computeSlotAvailability(
                            allClassSchedules = allClassScheds,
                            allTeachers = teachersList,
                            dayIndex = dayIdx,
                            lessonNum = lsnNum
                        )
                        isLoading = false
                    }
                }

                LaunchedEffect(currentTab) {
                    if (currentTab == 2 && freeOccupancy == null) {
                        loadFree(freeDayIndex, freeLessonNum)
                    } else if (currentTab == 3 && archiveSchedule == null) {
                        loadArchive(selectedArchiveWeek)
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            contentColor = NikaSubject
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.Schedule, contentDescription = "Расписание") },
                                label = { Text("Расписание", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NikaSubject,
                                    selectedTextColor = NikaSubject,
                                    indicatorColor = Color(0xFFD7EBFA)
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Календарь") },
                                label = { Text("Календарь", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NikaSubject,
                                    selectedTextColor = NikaSubject,
                                    indicatorColor = Color(0xFFD7EBFA)
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Default.MeetingRoom, contentDescription = "Свободные") },
                                label = { Text("Свободные", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NikaSubject,
                                    selectedTextColor = NikaSubject,
                                    indicatorColor = Color(0xFFD7EBFA)
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.Archive, contentDescription = "Архив") },
                                label = { Text("Архив", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NikaSubject,
                                    selectedTextColor = NikaSubject,
                                    indicatorColor = Color(0xFFD7EBFA)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Top School Header
                        HeaderView(
                            realtimeState = realtimeState,
                            isLoading = isLoading,
                            onRefreshClick = {
                                loadCurrentSchedule(targetType, targetName)
                            },
                            onSettingsClick = {
                                showSettingsDialog = true
                            }
                        )

                        // Tab Content
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentTab) {
                                0 -> ScheduleScreen(
                                    currentSchedule = currentSchedule,
                                    classesList = classesList,
                                    teachersList = teachersList,
                                    targetType = targetType,
                                    targetName = targetName,
                                    isFavorite = isFavorite,
                                    selectedDayIndex = selectedDayIndex,
                                    realtimeState = realtimeState,
                                    isLoading = isLoading,
                                    onTargetTypeChanged = { newType ->
                                        val defaultName = if (newType == PreferencesRepository.TYPE_TEACHER) {
                                            teachersList.firstOrNull()?.name ?: "Муль П.Ф."
                                        } else {
                                            classesList.firstOrNull()?.name ?: "10-1"
                                        }
                                        loadCurrentSchedule(newType, defaultName)
                                    },
                                    onTargetNameChanged = { newName ->
                                        loadCurrentSchedule(targetType, newName)
                                    },
                                    onToggleFavorite = {
                                        prefs.toggleFavorite(targetType, targetName)
                                        isFavorite = prefs.isFavorite(targetType, targetName)
                                        val msg = if (isFavorite) "Установлено основное расписание" else "Удалено из избранного"
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    onDaySelected = { selectedDayIndex = it }
                                )

                                1 -> CalendarScreen(
                                    currentSchedule = currentSchedule,
                                    events = eventsList,
                                    categories = eventCategories,
                                    onDayClick = { dayIndex ->
                                        selectedDayIndex = dayIndex
                                        currentTab = 0
                                    }
                                )

                                2 -> FreeRoomsScreen(
                                    slotOccupancy = freeOccupancy,
                                    selectedDayIndex = freeDayIndex,
                                    selectedLessonNum = freeLessonNum,
                                    realtimeState = realtimeState,
                                    adminPassword = prefs.adminPassword,
                                    isLoading = isLoading,
                                    onDaySelected = { loadFree(it, freeLessonNum) },
                                    onLessonNumSelected = { loadFree(freeDayIndex, it) },
                                    onAdminPasswordChanged = { prefs.adminPassword = it }
                                )

                                3 -> ArchiveScreen(
                                    archiveSchedule = archiveSchedule,
                                    classesList = classesList,
                                    teachersList = teachersList,
                                    targetType = targetType,
                                    targetName = targetName,
                                    selectedWeek = selectedArchiveWeek,
                                    selectedDayIndex = archiveDayIndex,
                                    isLoading = isLoading,
                                    onWeekChanged = { loadArchive(it) },
                                    onTargetNameChanged = { loadCurrentSchedule(targetType, it) },
                                    onDaySelected = { archiveDayIndex = it }
                                )
                            }
                        }
                    }

                    if (showSettingsDialog) {
                        SettingsDialog(
                            currentAdminPassword = prefs.adminPassword,
                            onAdminPasswordSaved = {
                                prefs.adminPassword = it
                                Toast.makeText(this@MainActivity, "Пароль сохранён", Toast.LENGTH_SHORT).show()
                            },
                            onForceServerRefresh = {
                                scope.launch {
                                    val ok = repo.refreshServer()
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (ok) "Запрос на обновление отправлен" else "Ошибка обновления на сервере",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onDismiss = { showSettingsDialog = false }
                        )
                    }
                }
            }
        }
    }
}
