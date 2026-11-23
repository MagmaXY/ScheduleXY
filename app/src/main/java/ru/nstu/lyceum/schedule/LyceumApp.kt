package ru.nstu.lyceum.schedule

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.nstu.lyceum.schedule.data.api.LyceumApiService
import ru.nstu.lyceum.schedule.data.local.CacheManager
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.data.repository.ScheduleRepository
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.domain.BellState
import ru.nstu.lyceum.schedule.domain.RealtimeSyncManager
import ru.nstu.lyceum.schedule.domain.VacationChecker
import ru.nstu.lyceum.schedule.notifications.ScheduleNotificationManager
import ru.nstu.lyceum.schedule.notifications.ScheduleSyncWorker
import ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider
import java.util.Calendar

class LyceumApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var apiService: LyceumApiService
        private set
    lateinit var cacheManager: CacheManager
        private set
    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var scheduleRepository: ScheduleRepository
        private set
    lateinit var realtimeSyncManager: RealtimeSyncManager
        private set
    lateinit var notesRepository: ru.nstu.lyceum.schedule.data.local.NotesRepository
        private set
    lateinit var yandexDiskSyncService: ru.nstu.lyceum.schedule.data.sync.YandexDiskSyncService
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        apiService = LyceumApiService.create()
        cacheManager = CacheManager(this)
        preferencesRepository = PreferencesRepository(this)
        scheduleRepository = ScheduleRepository(apiService, cacheManager, preferencesRepository)
        realtimeSyncManager = RealtimeSyncManager(applicationScope, this)
        notesRepository = ru.nstu.lyceum.schedule.data.local.NotesRepository(this, applicationScope)
        yandexDiskSyncService = ru.nstu.lyceum.schedule.data.sync.YandexDiskSyncService(this, preferencesRepository, notesRepository)

        // Trigger auto-sync when notes change if authorized
        notesRepository.onNotesChangedListener = {
            // Update widget immediately when notes change locally
            ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider.notifyWidgetUpdate(this@LyceumApp)

            if (yandexDiskSyncService.isAuthorized()) {
                applicationScope.launch {
                    try {
                        val res = yandexDiskSyncService.sync()
                        if (res is ru.nstu.lyceum.schedule.data.sync.SyncResult.Success) {
                            ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider.notifyWidgetUpdate(this@LyceumApp)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Auto-sync notes on app start if user is connected
        if (yandexDiskSyncService.isAuthorized()) {
            applicationScope.launch {
                try {
                    val res = yandexDiskSyncService.sync()
                    if (res is ru.nstu.lyceum.schedule.data.sync.SyncResult.Success) {
                        ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider.notifyWidgetUpdate(this@LyceumApp)
                    }
                } catch (_: Exception) {}
            }
        }

        ScheduleNotificationManager.createNotificationChannel(this)
        ru.nstu.lyceum.schedule.notifications.OngoingStatusNotificationManager.createNotificationChannel(this)
        ru.nstu.lyceum.schedule.notifications.OngoingStatusNotificationManager.update(this)
        ScheduleSyncWorker.schedulePeriodicSync(this)
        ru.nstu.lyceum.schedule.ui.AppShortcutManager.updateShortcuts(this, preferencesRepository.adminPassword == "licey2026")

        // Automatically update widget bell status every minute while screen is on and school is active
        var widgetScreenTimerJob: Job? = null

        fun isWidgetBellActive(): Boolean {
            val cal = Calendar.getInstance()
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val prefs = PreferencesRepository(this)
            val maxMinutes = if (prefs.statusBarPairsMode) 15 * 60 + 30 else 19 * 60
            if (currentMinutes < 8 * 60 || currentMinutes >= maxMinutes) return false
            val isVacation = VacationChecker.isVacation(this, cal)
            val bellState = if (prefs.statusBarPairsMode) {
                ru.nstu.lyceum.schedule.domain.PairSchedule.getCurrentPairState(cal, isVacation)
            } else {
                BellSchedule.getCurrentBellState(cal, isVacation)
            }
            return bellState is BellState.InLesson || bellState is BellState.Break || bellState is BellState.BeforeSchool
        }

        fun startWidgetTimer() {
            if (widgetScreenTimerJob?.isActive == true) return
            widgetScreenTimerJob = applicationScope.launch {
                while (isActive) {
                    if (isWidgetBellActive()) {
                        ScheduleWidgetProvider.notifyWidgetUpdate(this@LyceumApp)
                        ru.nstu.lyceum.schedule.notifications.OngoingStatusNotificationManager.update(this@LyceumApp)
                    } else {
                        ru.nstu.lyceum.schedule.notifications.OngoingStatusNotificationManager.cancel(this@LyceumApp)
                    }

                    // Synchronize with the exact start of the next system minute (:00 seconds)
                    val now = System.currentTimeMillis()
                    val delayMs = 60000L - (now % 60000L)
                    delay(if (delayMs > 50L) delayMs else 60000L)
                }
            }
        }

        fun stopWidgetTimer() {
            widgetScreenTimerJob?.cancel()
            widgetScreenTimerJob = null
        }

        // Check initial screen state
        val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        if (powerManager?.isInteractive == true) {
            startWidgetTimer()
        }

        // Automatically rewind widget schedule to today (or closest Monday) and start/stop 1-min ticker on screen state
        val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                        val prefs = PreferencesRepository(context)
                        prefs.widgetDayIndex = BellSchedule.getDefaultScheduleDayIndex()
                        ScheduleWidgetProvider.notifyWidgetUpdate(context)
                        startWidgetTimer()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        stopWidgetTimer()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        try {
            ContextCompat.registerReceiver(
                this,
                screenReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {
            try {
                registerReceiver(screenReceiver, filter)
            } catch (_: Exception) {}
        }
    }

    companion object {
        lateinit var instance: LyceumApp
            private set
    }
}
