package ru.nstu.lyceum.schedule.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.LyceumApp
import ru.nstu.lyceum.schedule.data.api.CalendarEvent
import ru.nstu.lyceum.schedule.data.api.ClassItem
import ru.nstu.lyceum.schedule.data.api.EventCategory
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.TeacherItem
import ru.nstu.lyceum.schedule.data.local.FreeAvailabilityEngine
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.domain.BellState
import ru.nstu.lyceum.schedule.domain.VacationChecker
import ru.nstu.lyceum.schedule.ui.components.HeaderView
import ru.nstu.lyceum.schedule.ui.components.InfoBottomSheet
import ru.nstu.lyceum.schedule.ui.screens.ArchiveScreen
import ru.nstu.lyceum.schedule.ui.screens.CalendarScreen
import ru.nstu.lyceum.schedule.ui.screens.FreeRoomsScreen
import ru.nstu.lyceum.schedule.ui.screens.NotesScreen
import ru.nstu.lyceum.schedule.ui.screens.ScheduleScreen
import ru.nstu.lyceum.schedule.ui.screens.SettingsDialog
import ru.nstu.lyceum.schedule.ui.theme.NikaBorder
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject
import ru.nstu.lyceum.schedule.ui.theme.NikaTheme
import ru.nstu.lyceum.schedule.ui.theme.ScheduleXYTheme
import ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled */ }

    private var pendingNotificationDayIndex by mutableStateOf<Int?>(null)
    private var pendingNotificationTargetType by mutableStateOf<String?>(null)
    private var pendingNotificationTargetName by mutableStateOf<String?>(null)
    private var pendingNotificationTab by mutableStateOf<Int?>(null)
    private var pendingNotificationCalendarDate by mutableStateOf<String?>(null)

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        // Check if intent is Yandex OAuth deep link: ru.nstu.lyceum.schedule://oauth#access_token=...
        val dataUri = intent.data
        if (dataUri != null && dataUri.scheme == "ru.nstu.lyceum.schedule" && dataUri.host == "oauth") {
            handleYandexOAuthResponse(dataUri)
            return
        }

        val tab = intent.getIntExtra(EXTRA_TAB_INDEX, -1)
        val day = intent.getIntExtra(EXTRA_DAY_INDEX, -1)
        val type = intent.getStringExtra(EXTRA_TARGET_TYPE)
        val name = intent.getStringExtra(EXTRA_TARGET_NAME)
        val date = intent.getStringExtra(EXTRA_DATE)

        if (tab >= 0) pendingNotificationTab = tab
        if (day >= 0) pendingNotificationDayIndex = day
        if (!type.isNullOrBlank()) pendingNotificationTargetType = type
        if (!name.isNullOrBlank()) pendingNotificationTargetName = name
        if (!date.isNullOrBlank()) pendingNotificationCalendarDate = date
    }

    private fun handleYandexOAuthResponse(uri: Uri) {
        val app = application as LyceumApp
        val fragment = uri.fragment ?: ""
        var token: String? = null

        // Format: access_token=AQAAAA...&token_type=bearer&expires_in=...
        val params = fragment.split("&")
        for (p in params) {
            val parts = p.split("=")
            if (parts.size == 2 && parts[0] == "access_token") {
                token = parts[1]
                break
            }
        }
        if (token.isNullOrBlank()) {
            token = uri.getQueryParameter("access_token")
        }

        if (!token.isNullOrBlank()) {
            app.yandexDiskSyncService.saveToken(token)
            Toast.makeText(this, "Авторизация в Яндекс успешна!", Toast.LENGTH_SHORT).show()
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
            scope.launch {
                app.yandexDiskSyncService.fetchUserInfo()
                val res = app.yandexDiskSyncService.sync()
                if (res is ru.nstu.lyceum.schedule.data.sync.SyncResult.Success) {
                    ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider.notifyWidgetUpdate(this@MainActivity)
                    Toast.makeText(this@MainActivity, res.message, Toast.LENGTH_SHORT).show()
                } else if (res is ru.nstu.lyceum.schedule.data.sync.SyncResult.Error) {
                    Toast.makeText(this@MainActivity, res.error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        checkNotificationPermission()

        val app = application as LyceumApp
        val repo = app.scheduleRepository
        val prefs = app.preferencesRepository
        val realtime = app.realtimeSyncManager

        // Set native window colors immediately on startup to prevent white flash in dark mode
        val isDark = prefs.isDarkMode
        val nativeBg = if (isDark) android.graphics.Color.parseColor("#121212") else android.graphics.Color.parseColor("#F8FAFC")
        window.decorView.setBackgroundColor(nativeBg)
        window.statusBarColor = nativeBg
        window.navigationBarColor = if (isDark) android.graphics.Color.parseColor("#1E1E2E") else android.graphics.Color.parseColor("#FFFFFF")

        setContent {
            var isDarkMode by remember { mutableStateOf(prefs.isDarkMode) }
            var themePreset by remember { mutableStateOf(prefs.themePreset) }

            ScheduleXYTheme(darkTheme = isDarkMode, themePreset = themePreset) {
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }
                val realtimeState by realtime.state.collectAsState()

                var showSplashScreen by remember { mutableStateOf(true) }
                var splashAnimated by remember { mutableStateOf(false) }

                val splashLogoScale by animateFloatAsState(
                    targetValue = if (splashAnimated) 1.0f else 0.65f,
                    animationSpec = tween(durationMillis = 450, easing = CubicBezierEasing(0.18f, 0.9f, 0.32f, 1.2f)),
                    label = "splash_scale"
                )
                val splashLogoAlpha by animateFloatAsState(
                    targetValue = if (splashAnimated) 1.0f else 0.0f,
                    animationSpec = tween(durationMillis = 350, easing = LinearEasing),
                    label = "splash_alpha"
                )

                LaunchedEffect(Unit) {
                    splashAnimated = true
                }

                var currentTab by rememberSaveable { mutableIntStateOf(pendingNotificationTab ?: 0) }
                val syncState by app.yandexDiskSyncService.syncState.collectAsState()
                var isYandexConnected by remember(prefs.yandexOAuthToken) { mutableStateOf(!prefs.yandexOAuthToken.isNullOrBlank()) }
                var yandexUserLogin by remember(prefs.yandexUserLogin) { mutableStateOf(prefs.yandexUserLogin) }
                var yandexLastSyncTime by remember(prefs.yandexLastSyncTime) { mutableStateOf(prefs.yandexLastSyncTime) }

                LaunchedEffect(syncState) {
                    isYandexConnected = !prefs.yandexOAuthToken.isNullOrBlank()
                    yandexUserLogin = prefs.yandexUserLogin
                    yandexLastSyncTime = prefs.yandexLastSyncTime
                }

                LaunchedEffect(currentTab) {
                    if (currentTab == 2 && app.yandexDiskSyncService.isAuthorized()) {
                        // Background sync when opening Notes tab
                        launch {
                            try {
                                val res = app.yandexDiskSyncService.sync()
                                if (res is ru.nstu.lyceum.schedule.data.sync.SyncResult.Success) {
                                    ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider.notifyWidgetUpdate(this@MainActivity)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
                var showSettingsDialog by remember { mutableStateOf(false) }
                var showInfoBottomSheet by remember { mutableStateOf(false) }
                var statusBarPairsMode by remember { mutableStateOf(prefs.statusBarPairsMode) }
                var showPairsSchedule by remember { mutableStateOf(prefs.showPairsSchedule) }
                var overlaySwipeAnimation by remember { mutableStateOf(prefs.overlaySwipeAnimation) }
                var copyIncludeHomework by remember { mutableStateOf(prefs.copyIncludeHomework) }
                var copyIncludeNotes by remember { mutableStateOf(prefs.copyIncludeNotes) }
                var studentSubgroup by remember { mutableIntStateOf(prefs.studentSubgroup) }
                var studentTechSubgroup by remember { mutableIntStateOf(prefs.studentTechnologySubgroup) }
                var notesTargetDateFocus by remember { mutableStateOf<String?>(null) }
                var calendarTargetDateFocus by remember { mutableStateOf<String?>(null) }

                var targetType by remember { mutableStateOf(prefs.targetType) }
                var targetName by remember {
                    val defaultName = if (prefs.targetType == PreferencesRepository.TYPE_TEACHER) {
                        prefs.favoriteTeacher ?: PreferencesRepository.DEFAULT_TEACHER
                    } else {
                        prefs.favoriteClass ?: PreferencesRepository.DEFAULT_CLASS
                    }
                    mutableStateOf(defaultName)
                }
                var isFavorite by remember { mutableStateOf(prefs.isFavorite(targetType, targetName)) }

                val defaultDayIndex = BellSchedule.getDefaultScheduleDayIndex(Calendar.getInstance())
                var selectedDayIndex by rememberSaveable { mutableIntStateOf(pendingNotificationDayIndex ?: defaultDayIndex) }

                var classesList by remember { mutableStateOf<List<ClassItem>>(emptyList()) }
                var teachersList by remember { mutableStateOf<List<TeacherItem>>(emptyList()) }
                var currentSchedule by remember { mutableStateOf<ScheduleResponse?>(null) }
                var archiveSchedule by remember { mutableStateOf<ScheduleResponse?>(null) }
                var archiveWeeksList by remember { mutableStateOf<List<String>>(emptyList()) }
                var eventsList by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
                var eventCategories by remember { mutableStateOf<List<EventCategory>>(emptyList()) }

                var selectedArchiveWeek by rememberSaveable { mutableIntStateOf(-1) }
                var archiveDayIndex by rememberSaveable { mutableIntStateOf(defaultDayIndex) }

                val isVacation = VacationChecker.isVacation(this@MainActivity)
                val currentLessonBell = BellSchedule.getCurrentBellState(Calendar.getInstance(), isVacation)
                val initialActiveLsn = when (currentLessonBell) {
                    is BellState.InLesson -> currentLessonBell.num
                    is BellState.Break -> currentLessonBell.nextNum
                    else -> 1
                }.coerceIn(1, 12)
                val initialFreeDay = if (realtime.state.value.dayIndex in 0..5 &&
                    currentLessonBell !is BellState.Weekend &&
                    currentLessonBell !is BellState.Vacation) {
                    realtime.state.value.dayIndex
                } else {
                    defaultDayIndex
                }
                var freeDayIndex by remember { mutableIntStateOf(initialFreeDay) }
                var freeLessonNum by remember { mutableIntStateOf(initialActiveLsn) }
                var freeOccupancy by remember { mutableStateOf<FreeAvailabilityEngine.SlotOccupancy?>(null) }

                val favClassSched = remember(prefs.favoriteClass, currentSchedule) {
                    val fc = prefs.favoriteClass
                    if (fc.isNullOrBlank()) null
                    else if (targetType == PreferencesRepository.TYPE_CLASS && targetName == fc) {
                        currentSchedule
                    } else {
                        app.cacheManager.getSchedule(PreferencesRepository.TYPE_CLASS, fc, 0)
                    }
                }
                val classTeachers = remember(favClassSched) {
                    favClassSched?.days?.flatMap { it.lessons }
                        ?.flatMap { it.subgroups }
                        ?.mapNotNull { it.teacher?.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.distinct()
                        ?.sorted() ?: emptyList()
                }

                val favTeacherSched = remember(prefs.favoriteTeacher, currentSchedule) {
                    val ft = prefs.favoriteTeacher
                    if (ft.isNullOrBlank()) null
                    else if (targetType == PreferencesRepository.TYPE_TEACHER && targetName == ft) {
                        currentSchedule
                    } else {
                        app.cacheManager.getSchedule(PreferencesRepository.TYPE_TEACHER, ft, 0)
                    }
                }
                val teacherClasses = remember(favTeacherSched) {
                    favTeacherSched?.days?.flatMap { it.lessons }
                        ?.flatMap { it.subgroups }
                        ?.mapNotNull { it.className?.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.distinct()
                        ?.sorted() ?: emptyList()
                }

                var isLoading by remember { mutableStateOf(false) }
                var isAdminAuthenticated by remember { mutableStateOf(false) }
                var useMagmaLogo by remember { mutableStateOf(prefs.useMagmaLogo) }
                var isMagmaUnlocked by remember { mutableStateOf(prefs.isMagmaUnlocked) }

                LaunchedEffect(isLoading, currentSchedule) {
                    if (!isLoading && currentSchedule != null) {
                        delay(450)
                        showSplashScreen = false
                    }
                }

                LaunchedEffect(Unit) {
                    delay(1200) // Fallback safety timer
                    showSplashScreen = false
                }

                fun checkAdminAccess(pass: String) {
                    if (pass == "licey2026") {
                        isAdminAuthenticated = true
                        return
                    }
                    if (pass.isBlank()) {
                        isAdminAuthenticated = false
                        return
                    }
                    scope.launch {
                        val valid = repo.checkAdminPassword(pass)
                        isAdminAuthenticated = valid
                    }
                }

                LaunchedEffect(prefs.adminPassword) {
                    checkAdminAccess(prefs.adminPassword)
                }

                LaunchedEffect(isAdminAuthenticated) {
                    AppShortcutManager.updateShortcuts(this@MainActivity, isAdminAuthenticated)
                }

                // Reload current schedule
                fun loadCurrentSchedule(type: String, name: String) {
                    prefs.targetType = type
                    prefs.targetName = name
                    targetType = type
                    targetName = name
                    isFavorite = prefs.isFavorite(type, name)
                    scope.launch {
                        isLoading = true
                        // Instantly show cached schedule if background sync already updated it on disk
                        val cached = repo.getCachedSchedule(type, name, 0)
                        if (cached != null) {
                            currentSchedule = cached
                        }
                        val res = repo.getSchedule(type, name, 0)
                        val fresh = res.getOrNull()
                        if (fresh != null) {
                            currentSchedule = fresh
                        }
                        isLoading = false
                        // Notify widget of target/schedule update
                        ScheduleWidgetProvider.notifyWidgetUpdate(this@MainActivity)
                    }
                }

                LaunchedEffect(pendingNotificationTab, pendingNotificationDayIndex, pendingNotificationTargetType, pendingNotificationTargetName, pendingNotificationCalendarDate) {
                    val tab = pendingNotificationTab
                    val day = pendingNotificationDayIndex
                    val type = pendingNotificationTargetType
                    val name = pendingNotificationTargetName
                    val calDate = pendingNotificationCalendarDate

                    if (tab != null) {
                        currentTab = tab
                        pendingNotificationTab = null
                    }
                    if (calDate != null) {
                        calendarTargetDateFocus = calDate
                        pendingNotificationCalendarDate = null
                    }
                    if (type != null && name != null) {
                        loadCurrentSchedule(type, name)
                        pendingNotificationTargetType = null
                        pendingNotificationTargetName = null
                    }
                    if (day != null) {
                        selectedDayIndex = day
                        pendingNotificationDayIndex = null
                    }
                }

                // Load archive
                fun loadArchive(week: Int) {
                    selectedArchiveWeek = week
                    val cached = repo.getCachedSchedule(targetType, targetName, week)
                    archiveSchedule = cached
                    scope.launch {
                        if (cached == null) isLoading = true
                        val res = repo.getSchedule(targetType, targetName, week)
                        archiveSchedule = res.getOrNull() ?: cached
                        isLoading = false
                    }
                }

                var cachedAllClassSchedules by remember { mutableStateOf<List<ScheduleResponse>?>(null) }
                var isFreeRoomsLoading by remember { mutableStateOf(false) }

                // Load free rooms with all 39 class schedules
                fun loadFree(dayIdx: Int, lsnNum: Int) {
                    freeDayIndex = dayIdx
                    freeLessonNum = lsnNum

                    val inMem = cachedAllClassSchedules
                    if (inMem != null && inMem.isNotEmpty()) {
                        freeOccupancy = FreeAvailabilityEngine.computeSlotAvailability(
                            allClassSchedules = inMem,
                            allTeachers = teachersList,
                            dayIndex = dayIdx,
                            lessonNum = lsnNum
                        )
                    } else {
                        scope.launch {
                            isFreeRoomsLoading = true
                            val allClassScheds = repo.prefetchAllClasses()
                            cachedAllClassSchedules = allClassScheds
                            freeOccupancy = FreeAvailabilityEngine.computeSlotAvailability(
                                allClassSchedules = allClassScheds,
                                allTeachers = teachersList,
                                dayIndex = dayIdx,
                                lessonNum = lsnNum
                            )
                            isFreeRoomsLoading = false
                        }
                    }
                }

                val exportNotesLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            val json = app.notesRepository.exportNotesJson()
                            contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(json.toByteArray(Charsets.UTF_8))
                            }
                            Toast.makeText(this@MainActivity, "Заметки успешно экспортированы", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val importNotesLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            val json = contentResolver.openInputStream(uri)?.use { input ->
                                input.bufferedReader(Charsets.UTF_8).readText()
                            } ?: ""
                            val count = app.notesRepository.importNotesJson(json)
                            if (count >= 0) {
                                Toast.makeText(this@MainActivity, "Импортировано заметок: $count", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Ошибка формата файла заметок", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Ошибка импорта: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                fun resolveOppositeTarget(clickedName: String): Pair<String, String> {
                    val trimmed = clickedName.trim()
                    return if (targetType == PreferencesRepository.TYPE_TEACHER) {
                        val cleanClass = classesList.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.name
                            ?: trimmed
                        PreferencesRepository.TYPE_CLASS to cleanClass
                    } else {
                        val cleanTeacher = teachersList.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.name
                            ?: teachersList.firstOrNull { it.name.contains(trimmed, ignoreCase = true) || trimmed.contains(it.name, ignoreCase = true) }?.name
                            ?: trimmed
                        PreferencesRepository.TYPE_TEACHER to cleanTeacher
                    }
                }

                val onTeacherClassClick: (String) -> Unit = { clickedName ->
                    val (newType, cleanName) = resolveOppositeTarget(clickedName)
                    loadCurrentSchedule(newType, cleanName)
                }

                val onArchiveTeacherClassClick: (String) -> Unit = { clickedName ->
                    val (newType, cleanName) = resolveOppositeTarget(clickedName)
                    loadCurrentSchedule(newType, cleanName)
                    loadArchive(selectedArchiveWeek)
                }

                val onRoomClick: (String, Int) -> Unit = { _, lessonNum ->
                    val day = if (selectedDayIndex in 0..5) selectedDayIndex else defaultDayIndex
                    freeDayIndex = day
                    freeLessonNum = lessonNum
                    loadFree(day, lessonNum)
                    currentTab = 3
                }

                val onTableRoomClick: (Int, String, Int) -> Unit = { dayIdx, _, lessonNum ->
                    val day = dayIdx.coerceIn(0, 5)
                    freeDayIndex = day
                    freeLessonNum = lessonNum
                    loadFree(day, lessonNum)
                    currentTab = 3
                }

                // Lifecycle management: run ticker ONLY while app is in foreground
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> {
                                realtime.startSyncLoop()
                                val cached = repo.getCachedSchedule(targetType, targetName, 0)
                                if (cached != null && cached.exportDate != currentSchedule?.exportDate) {
                                    currentSchedule = cached
                                }
                                loadCurrentSchedule(targetType, targetName)
                            }
                            Lifecycle.Event.ON_PAUSE,
                            Lifecycle.Event.ON_STOP -> {
                                realtime.stopSyncLoop()
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        realtime.stopSyncLoop()
                    }
                }

                // Initial loading: fetch fresh data using persistent cache (energy-efficient)
                LaunchedEffect(Unit) {
                    isLoading = true
                    try {
                        val cls = repo.getClasses()
                        classesList = cls.items

                        val tch = repo.getTeachers()
                        teachersList = tch.items

                        // Pick favorite target
                        if (targetType == PreferencesRepository.TYPE_TEACHER) {
                            targetName = prefs.favoriteTeacher ?: PreferencesRepository.DEFAULT_TEACHER
                        } else {
                            targetName = prefs.favoriteClass ?: PreferencesRepository.DEFAULT_CLASS
                        }
                        isFavorite = prefs.isFavorite(targetType, targetName)

                        val res = repo.getSchedule(targetType, targetName, 0)
                        currentSchedule = res.getOrNull()

                        val ev = repo.getEvents()
                        eventsList = ev.items
                        eventCategories = ev.categories

                        val arch = repo.getArchiveWeeks()
                        archiveWeeksList = arch.weeks
                    } finally {
                        isLoading = false
                    }
                }

                LaunchedEffect(currentTab) {
                    if (currentTab == 1) {
                        if (archiveWeeksList.isEmpty()) {
                            scope.launch {
                                val arch = repo.getArchiveWeeks()
                                archiveWeeksList = arch.weeks
                            }
                        }
                    }
                    if (currentTab == 3) {
                        val isVac = VacationChecker.isVacation(this@MainActivity)
                        val lsnBell = BellSchedule.getCurrentBellState(Calendar.getInstance(), isVac)
                        val activeLsn = when (lsnBell) {
                            is BellState.InLesson -> lsnBell.num
                            is BellState.Break -> lsnBell.nextNum
                            else -> 1
                        }.coerceIn(1, 12)
                        val todayHasLessons = lsnBell !is BellState.Weekend &&
                                              lsnBell !is BellState.Vacation &&
                                              realtimeState.dayIndex in 0..5
                        freeDayIndex = if (todayHasLessons) realtimeState.dayIndex else defaultDayIndex
                        freeLessonNum = activeLsn
                        loadFree(freeDayIndex, freeLessonNum)
                    }
                    if (currentTab == 4) {
                        if (archiveWeeksList.isEmpty()) {
                            scope.launch {
                                val arch = repo.getArchiveWeeks()
                                archiveWeeksList = arch.weeks
                            }
                        }
                        if (archiveSchedule == null) {
                            loadArchive(selectedArchiveWeek)
                        }
                    }
                }

                LaunchedEffect(currentSchedule) {
                    val sched = currentSchedule
                    if (sched != null) {
                        val isMiddle = prefs.isMiddleSchoolClass(sched.title)
                        ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper.checkAndRelocateCancelledHomework(
                            notesRepository = app.notesRepository,
                            schedule = sched,
                            userSubgroup = studentSubgroup,
                            userTechSubgroup = studentTechSubgroup,
                            isMiddleSchool = isMiddle
                        )
                        ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper.resolvePendingTransfers(
                            notesRepository = app.notesRepository,
                            schedule = sched,
                            userSubgroup = studentSubgroup,
                            userTechSubgroup = studentTechSubgroup,
                            isMiddleSchool = isMiddle
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        HeaderView(
                            realtimeState = realtimeState,
                            isLoading = isLoading,
                            isDarkTheme = isDarkMode,
                            isPairsStatus = statusBarPairsMode,
                            useMagmaLogo = useMagmaLogo,
                            onLogoEasterEggTriggered = {
                                prefs.isMagmaUnlocked = true
                                isMagmaUnlocked = true
                                showSettingsDialog = true
                                Toast.makeText(this@MainActivity, "Секретные настройки открыты", Toast.LENGTH_SHORT).show()
                            },
                            onToggleTheme = {
                                isDarkMode = !isDarkMode
                                prefs.isDarkMode = isDarkMode
                                ScheduleWidgetProvider.notifyWidgetUpdate(this@MainActivity)
                            },
                            onSettingsClick = { showSettingsDialog = true },
                            onInfoClick = { showInfoBottomSheet = true },
                            onRefreshClick = {
                                loadCurrentSchedule(targetType, targetName)
                                Toast.makeText(this@MainActivity, "Обновление...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {
                            val items = listOf(
                                Triple(0, "Расписание", Icons.Default.Schedule),
                                Triple(1, "Календарь", Icons.Default.CalendarMonth),
                                Triple(2, "Заметки", Icons.AutoMirrored.Outlined.MenuBook),
                                Triple(3, "Кабинеты", Icons.Default.MeetingRoom),
                                Triple(4, "Архив", Icons.Default.Archive)
                            )

                            items.forEach { (index, title, icon) ->
                                val selected = currentTab == index
                                val iconTint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = iconTint
                                        )
                                    },
                                    label = null,
                                    alwaysShowLabel = false,
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
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
                                    showPairsSchedule = showPairsSchedule,
                                    statusBarPairsMode = statusBarPairsMode,
                                    overlaySwipeAnimation = overlaySwipeAnimation,
                                    notesRepository = app.notesRepository,
                                    copyIncludeHomework = copyIncludeHomework,
                                    copyIncludeNotes = copyIncludeNotes,
                                    userSubgroup = studentSubgroup,
                                    userTechSubgroup = studentTechSubgroup,
                                    favoriteClass = prefs.favoriteClass,
                                    favoriteTeacher = prefs.favoriteTeacher,
                                    classTeachers = classTeachers,
                                    teacherClasses = teacherClasses,
                                    onTargetTypeChanged = { newType ->
                                        val newName = if (newType == PreferencesRepository.TYPE_TEACHER) {
                                            prefs.favoriteTeacher ?: PreferencesRepository.DEFAULT_TEACHER
                                        } else {
                                            prefs.favoriteClass ?: PreferencesRepository.DEFAULT_CLASS
                                        }
                                        loadCurrentSchedule(newType, newName)
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
                                    onDaySelected = { selectedDayIndex = it },
                                    onTeacherClassClick = onTeacherClassClick,
                                    onRoomClick = onRoomClick,
                                    onTableRoomClick = onTableRoomClick,
                                    onNavigateToNotes = { dateIso ->
                                        notesTargetDateFocus = dateIso
                                        currentTab = 2
                                    },
                                    onNavigateToCalendar = { dateIso ->
                                        calendarTargetDateFocus = dateIso
                                        currentTab = 1
                                    }
                                )

                                1 -> CalendarScreen(
                                    currentSchedule = currentSchedule,
                                    archiveWeeks = archiveWeeksList,
                                    events = eventsList,
                                    categories = eventCategories,
                                    notesRepository = app.notesRepository,
                                    targetName = targetName,
                                    isTeacherMode = targetType == PreferencesRepository.TYPE_TEACHER,
                                    userSubgroup = studentSubgroup,
                                    userTechSubgroup = studentTechSubgroup,
                                    isMiddleSchool = prefs.isMiddleSchoolClass(targetName),
                                    targetDateFocus = calendarTargetDateFocus,
                                    overlaySwipeAnimation = overlaySwipeAnimation,
                                    onNavigateToSchedule = { weekOffset, dayIndex ->
                                        if (weekOffset == 0) {
                                            selectedDayIndex = dayIndex
                                            currentTab = 0
                                        } else {
                                            selectedArchiveWeek = weekOffset
                                            archiveDayIndex = dayIndex
                                            loadArchive(weekOffset)
                                            currentTab = 4
                                        }
                                    }
                                )

                                2 -> NotesScreen(
                                    notesRepository = app.notesRepository,
                                    classesList = classesList,
                                    teachersList = teachersList,
                                    targetType = targetType,
                                    targetName = targetName,
                                    isFavorite = isFavorite,
                                    favoriteClass = prefs.favoriteClass,
                                    favoriteTeacher = prefs.favoriteTeacher,
                                    classTeachers = classTeachers,
                                    teacherClasses = teacherClasses,
                                    onTargetTypeChanged = { newType ->
                                        val newName = if (newType == PreferencesRepository.TYPE_TEACHER) {
                                            prefs.favoriteTeacher ?: PreferencesRepository.DEFAULT_TEACHER
                                        } else {
                                            prefs.favoriteClass ?: PreferencesRepository.DEFAULT_CLASS
                                        }
                                        loadCurrentSchedule(newType, newName)
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
                                    targetDateFocus = notesTargetDateFocus,
                                    scheduleResponse = currentSchedule,
                                    userSubgroup = studentSubgroup,
                                    userTechSubgroup = studentTechSubgroup,
                                    isMiddleSchool = prefs.isMiddleSchoolClass(targetName),
                                    overlaySwipeAnimation = overlaySwipeAnimation
                                )

                                3 -> FreeRoomsScreen(
                                    slotOccupancy = freeOccupancy,
                                    selectedDayIndex = freeDayIndex,
                                    selectedLessonNum = freeLessonNum,
                                    realtimeState = realtimeState,
                                    adminPassword = prefs.adminPassword,
                                    isLoading = isFreeRoomsLoading,
                                    overlaySwipeAnimation = overlaySwipeAnimation,
                                    onDaySelected = { loadFree(it, freeLessonNum) },
                                    onLessonNumSelected = { loadFree(freeDayIndex, it) },
                                    onAdminPasswordChanged = { prefs.adminPassword = it }
                                )

                                4 -> ArchiveScreen(
                                    archiveSchedule = archiveSchedule,
                                    archiveWeeks = archiveWeeksList,
                                    classesList = classesList,
                                    teachersList = teachersList,
                                    targetType = targetType,
                                    targetName = targetName,
                                    selectedWeek = selectedArchiveWeek,
                                    selectedDayIndex = archiveDayIndex,
                                    isLoading = isLoading,
                                    showPairsSchedule = showPairsSchedule,
                                    overlaySwipeAnimation = overlaySwipeAnimation,
                                    favoriteClass = prefs.favoriteClass,
                                    favoriteTeacher = prefs.favoriteTeacher,
                                    classTeachers = classTeachers,
                                    teacherClasses = teacherClasses,
                                    onWeekChanged = { loadArchive(it) },
                                    onTargetTypeChanged = { newType ->
                                        val newName = if (newType == PreferencesRepository.TYPE_TEACHER) {
                                            prefs.favoriteTeacher ?: PreferencesRepository.DEFAULT_TEACHER
                                        } else {
                                            prefs.favoriteClass ?: PreferencesRepository.DEFAULT_CLASS
                                        }
                                        loadCurrentSchedule(newType, newName)
                                        loadArchive(selectedArchiveWeek)
                                    },
                                    onTargetNameChanged = { newName ->
                                        loadCurrentSchedule(targetType, newName)
                                        loadArchive(selectedArchiveWeek)
                                    },
                                    onDaySelected = { archiveDayIndex = it },
                                    onTeacherClassClick = onArchiveTeacherClassClick,
                                    onNavigateToCalendar = { dateIso ->
                                        calendarTargetDateFocus = dateIso
                                        currentTab = 1
                                    }
                                )
                            }
                        }
                    }

                    if (showSettingsDialog) {
                        SettingsDialog(
                            currentAdminPassword = prefs.adminPassword,
                            isAdmin = isAdminAuthenticated || prefs.adminPassword == "licey2026",
                            initialThemePreset = themePreset,
                            onThemePresetChanged = { newPreset ->
                                themePreset = newPreset
                                prefs.themePreset = newPreset
                                ScheduleWidgetProvider.notifyWidgetUpdate(this@MainActivity)
                            },
                            initialShowPairs = prefs.showPairsSchedule,
                            initialStatusBarPairs = prefs.statusBarPairsMode,
                            initialOngoingStatusNotification = prefs.ongoingStatusNotificationEnabled,
                            initialOverlaySwipe = prefs.overlaySwipeAnimation,
                            favoriteClass = prefs.favoriteClass,
                            initialSubgroup = studentSubgroup,
                            initialTechSubgroup = studentTechSubgroup,
                            initialCopyHw = copyIncludeHomework,
                            initialCopyNotes = copyIncludeNotes,
                            isMagmaUnlocked = isMagmaUnlocked,
                            initialUseMagmaLogo = useMagmaLogo,
                            onMagmaLogoChanged = { newMagma ->
                                useMagmaLogo = newMagma
                                prefs.useMagmaLogo = newMagma
                                AppIconManager.setMagmaIcon(this@MainActivity, newMagma)
                                ru.nstu.lyceum.schedule.notifications.OngoingStatusNotificationManager.update(this@MainActivity)
                                ScheduleWidgetProvider.notifyWidgetUpdate(this@MainActivity)
                                ru.nstu.lyceum.schedule.tiles.ScheduleTileService.updateTile(this@MainActivity)
                                Toast.makeText(
                                    this@MainActivity,
                                    if (newMagma) "Логотип MagmaXY установлен!" else "Возвращен официальный логотип Лицея",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onSettingsSaved = { pwd, pairs, statusPairs, ongoingStatus, overlaySwipe, subgrp, techSubgrp, copyHw, copyNotes ->
                                prefs.adminPassword = pwd
                                prefs.showPairsSchedule = pairs
                                prefs.statusBarPairsMode = statusPairs
                                prefs.ongoingStatusNotificationEnabled = ongoingStatus
                                prefs.overlaySwipeAnimation = overlaySwipe
                                prefs.studentSubgroup = subgrp
                                prefs.studentTechnologySubgroup = techSubgrp
                                prefs.copyIncludeHomework = copyHw
                                prefs.copyIncludeNotes = copyNotes

                                showPairsSchedule = pairs
                                statusBarPairsMode = statusPairs
                                overlaySwipeAnimation = overlaySwipe
                                studentSubgroup = subgrp
                                studentTechSubgroup = techSubgrp
                                copyIncludeHomework = copyHw
                                copyIncludeNotes = copyNotes

                                checkAdminAccess(pwd)
                                realtime.forceRefresh()
                                ru.nstu.lyceum.schedule.notifications.OngoingStatusNotificationManager.update(this@MainActivity)
                                ScheduleWidgetProvider.notifyWidgetUpdate(this@MainActivity)
                                ru.nstu.lyceum.schedule.tiles.ScheduleTileService.updateTile(this@MainActivity)
                                Toast.makeText(this@MainActivity, "Настройки сохранены", Toast.LENGTH_SHORT).show()
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
                            onExportNotes = {
                                exportNotesLauncher.launch("lyceum_notes_backup.json")
                            },
                            onImportNotes = {
                                importNotesLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            isYandexConnected = isYandexConnected,
                            yandexUserLogin = yandexUserLogin,
                            yandexLastSyncTime = yandexLastSyncTime,
                            isYandexSyncing = syncState is ru.nstu.lyceum.schedule.data.sync.SyncResult.InProgress,
                            onYandexLogin = {
                                ru.nstu.lyceum.schedule.data.sync.YandexDiskSyncService.openAuthInCustomTabs(this@MainActivity)
                            },
                            onYandexLogout = {
                                app.yandexDiskSyncService.logout()
                                isYandexConnected = false
                                yandexUserLogin = null
                                yandexLastSyncTime = 0L
                                Toast.makeText(this@MainActivity, "Вы вышли из Яндекс Диска", Toast.LENGTH_SHORT).show()
                            },
                            onYandexSyncManual = {
                                scope.launch {
                                    val res = app.yandexDiskSyncService.sync()
                                    if (res is ru.nstu.lyceum.schedule.data.sync.SyncResult.Success) {
                                        ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider.notifyWidgetUpdate(this@MainActivity)
                                        Toast.makeText(this@MainActivity, res.message, Toast.LENGTH_SHORT).show()
                                    } else if (res is ru.nstu.lyceum.schedule.data.sync.SyncResult.Error) {
                                        Toast.makeText(this@MainActivity, res.error, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onDismiss = { showSettingsDialog = false }
                        )
                    }

                    if (showInfoBottomSheet) {
                        InfoBottomSheet(
                            onDismissRequest = { showInfoBottomSheet = false }
                        )
                    }

                    // Telegram-style startup splash screen with theme-colored solid background and scaling logo
                    AnimatedVisibility(
                        visible = showSplashScreen,
                        enter = EnterTransition.None,
                        exit = fadeOut(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = if (useMagmaLogo) R.drawable.ic_magma_logo else R.drawable.ic_lyceum_logo),
                                contentDescription = "Загрузка...",
                                modifier = Modifier
                                    .size(96.dp)
                                    .graphicsLayer(
                                        scaleX = splashLogoScale,
                                        scaleY = splashLogoScale,
                                        alpha = splashLogoAlpha
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

    companion object {
        const val EXTRA_TAB_INDEX = "EXTRA_TAB_INDEX"
        const val EXTRA_DAY_INDEX = "EXTRA_DAY_INDEX"
        const val EXTRA_TARGET_TYPE = "EXTRA_TARGET_TYPE"
        const val EXTRA_TARGET_NAME = "EXTRA_TARGET_NAME"
        const val EXTRA_DATE = "EXTRA_DATE"
    }
}
