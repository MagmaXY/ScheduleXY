package ru.nstu.lyceum.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nstu.lyceum.schedule.LyceumApp
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.data.local.CacheManager
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.domain.BellState
import ru.nstu.lyceum.schedule.domain.VacationChecker
import ru.nstu.lyceum.schedule.ui.AppIconManager
import ru.nstu.lyceum.schedule.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val prefs = PreferencesRepository(context)
        val defaultDay = BellSchedule.getDefaultScheduleDayIndex()

        when (intent.action) {
            WidgetActions.ACTION_PREV_DAY -> {
                val cur = if (prefs.widgetDayIndex in 0..5) prefs.widgetDayIndex else defaultDay
                prefs.widgetDayIndex = if (cur > 0) cur - 1 else 5
                notifyWidgetUpdate(context)
            }
            WidgetActions.ACTION_NEXT_DAY -> {
                val cur = if (prefs.widgetDayIndex in 0..5) prefs.widgetDayIndex else defaultDay
                prefs.widgetDayIndex = if (cur < 5) cur + 1 else 0
                notifyWidgetUpdate(context)
            }
            WidgetActions.ACTION_COPY_SCHEDULE -> {
                val dayIdx = intent.getIntExtra(WidgetActions.EXTRA_DAY_INDEX, -1).let {
                    if (it in 0..5) it else (if (prefs.widgetDayIndex in 0..5) prefs.widgetDayIndex else defaultDay)
                }
                val cache = CacheManager(context)
                val sched = cache.getSchedule(prefs.targetType, prefs.targetName, 0)
                val daySchedule = sched?.days?.firstOrNull { it.dayIndex == dayIdx }
                val lessons = daySchedule?.lessons ?: emptyList()

                if (lessons.isNotEmpty()) {
                    val dateCal = Calendar.getInstance()
                    val weekStart = sched?.weekStart
                    if (!weekStart.isNullOrBlank()) {
                        try {
                            val parts = weekStart.split("-")
                            if (parts.size == 3) {
                                dateCal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                dateCal.add(Calendar.DAY_OF_MONTH, dayIdx)
                            }
                        } catch (_: Exception) {}
                    } else {
                        val daysFromMonday = (dateCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                        dateCal.add(Calendar.DAY_OF_MONTH, -daysFromMonday + dayIdx)
                    }
                    val dayNames = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
                    val dayFormat = SimpleDateFormat("d MMMM", Locale("ru"))
                    val fullDayTitle = "${dayNames.getOrElse(dayIdx) { "Понедельник" }}, ${dayFormat.format(dateCal.time)}"

                    val textToCopy = ru.nstu.lyceum.schedule.ui.util.ScheduleFormatter.formatScheduleForCopy(
                        dayTitle = fullDayTitle,
                        lessons = lessons,
                        scheduleResponse = sched,
                        dayIndex = dayIdx,
                        isTeacherMode = prefs.targetType == PreferencesRepository.TYPE_TEACHER,
                        targetName = prefs.targetName,
                        showPairsSetting = prefs.showPairsSchedule
                    )

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Расписание", textToCopy)
                    clipboard?.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "Расписание скопировано", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "В этот день уроков нет", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            WidgetActions.ACTION_TOGGLE_TYPE -> {
                // Toggle between Class and Teacher mode using independent favorites
                if (prefs.targetType == PreferencesRepository.TYPE_CLASS) {
                    prefs.targetType = PreferencesRepository.TYPE_TEACHER
                    prefs.targetName = prefs.favoriteTeacher ?: PreferencesRepository.DEFAULT_TEACHER
                } else {
                    prefs.targetType = PreferencesRepository.TYPE_CLASS
                    prefs.targetName = prefs.favoriteClass ?: PreferencesRepository.DEFAULT_CLASS
                }
                notifyWidgetUpdate(context)
            }
            WidgetActions.ACTION_REFRESH -> {
                notifyWidgetUpdate(context)
                // Asynchronously fetch latest schedule from server when user explicitly clicks refresh
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as? LyceumApp
                        app?.scheduleRepository?.getSchedule(prefs.targetType, prefs.targetName, 0)
                        withContext(Dispatchers.Main) {
                            notifyWidgetUpdate(context)
                        }
                    } catch (_: Exception) {}
                }
            }
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_SCREEN_ON -> {
                // Rewind widget schedule to today (or closest Monday on weekend/vacation) using local cache
                prefs.widgetDayIndex = BellSchedule.getDefaultScheduleDayIndex()
                notifyWidgetUpdate(context)
            }
        }
    }

    companion object {
        fun notifyWidgetUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, ScheduleWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(component)

                for (id in ids) {
                    updateAppWidget(context, appWidgetManager, id)
                }
                appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_lesson_list)
            } catch (_: Exception) {}
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = PreferencesRepository(context)
            val isDark = prefs.isDarkMode
            val themePreset = prefs.themePreset

            val views = RemoteViews(context.packageName, R.layout.widget_schedule_2x2)

            val dynamicPrimary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themePreset == PreferencesRepository.THEME_DYNAMIC) {
                context.getColor(if (isDark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600)
            } else null

            // Theme styling
            val textBlackColor = when {
                themePreset == PreferencesRepository.THEME_OLED && isDark -> Color.parseColor("#EDEDED")
                themePreset == PreferencesRepository.THEME_EMERALD && isDark -> Color.parseColor("#DEE4DF")
                isDark -> Color.parseColor("#F1F5F9")
                themePreset == PreferencesRepository.THEME_EMERALD -> Color.parseColor("#171D1A")
                themePreset == PreferencesRepository.THEME_OLED -> Color.parseColor("#18181B")
                else -> Color.parseColor("#0F172A")
            }

            val textDarkGrayColor = when {
                themePreset == PreferencesRepository.THEME_OLED && isDark -> Color.parseColor("#A1A1AA")
                themePreset == PreferencesRepository.THEME_EMERALD && isDark -> Color.parseColor("#89938E")
                isDark -> Color.parseColor("#94A3B8")
                themePreset == PreferencesRepository.THEME_EMERALD -> Color.parseColor("#4D6357")
                themePreset == PreferencesRepository.THEME_OLED -> Color.parseColor("#71717A")
                else -> Color.parseColor("#475569")
            }

            val saturdayColor = when {
                dynamicPrimary != null -> dynamicPrimary
                themePreset == PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#6EDBB0") else Color.parseColor("#006C4C")
                themePreset == PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#FAFAFA") else Color.parseColor("#18181B")
                else -> if (isDark) Color.parseColor("#34D399") else Color.parseColor("#059669")
            }

            val dividerColor = when {
                themePreset == PreferencesRepository.THEME_OLED && isDark -> Color.parseColor("#27272A")
                themePreset == PreferencesRepository.THEME_EMERALD && isDark -> Color.parseColor("#1C2722")
                isDark -> Color.parseColor("#334155")
                themePreset == PreferencesRepository.THEME_EMERALD -> Color.parseColor("#BFC9C3")
                themePreset == PreferencesRepository.THEME_OLED -> Color.parseColor("#E4E4E7")
                else -> Color.parseColor("#E2E8F0")
            }

            val bgResource = when {
                themePreset == PreferencesRepository.THEME_OLED && isDark -> R.drawable.widget_background_oled
                themePreset == PreferencesRepository.THEME_EMERALD && isDark -> R.drawable.widget_background_emerald_dark
                themePreset == PreferencesRepository.THEME_EMERALD -> R.drawable.widget_background_emerald
                isDark -> R.drawable.widget_background_dark
                else -> R.drawable.widget_background
            }

            views.setInt(R.id.widget_container, "setBackgroundResource", bgResource)
            views.setInt(R.id.widget_icon_target, "setColorFilter", dynamicPrimary ?: textDarkGrayColor)
            views.setTextColor(R.id.widget_text_target, textBlackColor)
            views.setInt(R.id.widget_btn_prev, "setColorFilter", textDarkGrayColor)
            views.setInt(R.id.widget_btn_next, "setColorFilter", textDarkGrayColor)
            views.setInt(R.id.widget_btn_copy, "setColorFilter", textDarkGrayColor)
            views.setInt(R.id.widget_divider, "setBackgroundColor", dividerColor)
            views.setTextColor(R.id.widget_empty_text, if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#AAAAAA"))

            // 1. Target Name & Type with vector icon (no OS color tinting) and class/teacher name
            val isTeacher = prefs.targetType == PreferencesRepository.TYPE_TEACHER
            views.setImageViewResource(
                R.id.widget_icon_target,
                if (isTeacher) R.drawable.ic_widget_person else R.drawable.ic_widget_group
            )
            views.setTextViewText(R.id.widget_text_target, prefs.targetName)

            // 2. Real-time Bell Status: compact minutes left with appropriate color (strictly 8:00 AM to 19:00 during active school hours)
            val cal = Calendar.getInstance()
            val isVacation = VacationChecker.isVacation(context, cal)
            val bellState = if (prefs.statusBarPairsMode) {
                ru.nstu.lyceum.schedule.domain.PairSchedule.getCurrentPairState(cal, isVacation)
            } else {
                BellSchedule.getCurrentBellState(cal, isVacation)
            }
            val curMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val maxMinutes = if (prefs.statusBarPairsMode) 15 * 60 + 30 else 19 * 60
            val isTimeInRange = curMinutes in (8 * 60) until maxMinutes
            val isSchoolActive = isTimeInRange &&
                (bellState is BellState.InLesson || bellState is BellState.Break || bellState is BellState.BeforeSchool) &&
                bellState !is BellState.AfterSchool && bellState !is BellState.Weekend && bellState !is BellState.Vacation

            if (isSchoolActive) {
                val beforeSchoolColor = when {
                    dynamicPrimary != null -> dynamicPrimary
                    themePreset == PreferencesRepository.THEME_EMERALD -> if (isDark) Color.parseColor("#6EDBB0") else Color.parseColor("#006C4C")
                    themePreset == PreferencesRepository.THEME_OLED -> if (isDark) Color.parseColor("#FAFAFA") else Color.parseColor("#18181B")
                    else -> if (isDark) Color.parseColor("#9999FF") else Color.parseColor("#222288")
                }
                val bellColor = when (bellState) {
                    is BellState.InLesson -> if (isDark) Color.parseColor("#34D399") else Color.parseColor("#059669") // Green
                    is BellState.Break -> if (isDark) Color.parseColor("#FBBF24") else Color.parseColor("#D97706")    // Amber
                    is BellState.BeforeSchool -> beforeSchoolColor
                    else -> textDarkGrayColor
                }
                views.setTextViewText(R.id.widget_text_bell, bellState.widgetText)
                views.setTextColor(R.id.widget_text_bell, bellColor)
                views.setViewVisibility(R.id.widget_text_bell, View.VISIBLE)
            } else {
                views.setTextViewText(R.id.widget_text_bell, "")
                views.setViewVisibility(R.id.widget_text_bell, View.GONE)
            }

            // 3. Day and Date: defaults to Monday on Saturday evening (>= 15:00) and Sunday
            val defaultDay = BellSchedule.getDefaultScheduleDayIndex()
            val selectedDayIdx = if (prefs.widgetDayIndex in 0..5) prefs.widgetDayIndex else defaultDay

            val cache = CacheManager(context)
            val sched = cache.getSchedule(prefs.targetType, prefs.targetName, 0)
            val dateCal = Calendar.getInstance()
            val weekStart = sched?.weekStart
            if (!weekStart.isNullOrBlank()) {
                try {
                    val parts = weekStart.split("-")
                    if (parts.size == 3) {
                        dateCal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                        dateCal.add(Calendar.DAY_OF_MONTH, selectedDayIdx)
                    }
                } catch (_: Exception) {}
            } else {
                val daysFromMonday = (dateCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                dateCal.add(Calendar.DAY_OF_MONTH, -daysFromMonday + selectedDayIdx)
            }
            val dayFormat = SimpleDateFormat("EE, d MMM", Locale("ru"))
            val formattedDate = dayFormat.format(dateCal.time).replaceFirstChar { it.titlecase(Locale("ru")) }
            views.setTextViewText(R.id.widget_text_day, formattedDate)
            val dayTextColor = if (selectedDayIdx == 5) saturdayColor else textBlackColor
            views.setTextColor(R.id.widget_text_day, dayTextColor)

            // Click on widget date opens Calendar at this specific date
            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dateCal.time)
            val calendarIntent = AppIconManager.createAppIntent(context).apply {
                putExtra(MainActivity.EXTRA_TAB_INDEX, 1)
                putExtra(MainActivity.EXTRA_DATE, isoDateFormat)
            }
            val calendarPendingIntent = PendingIntent.getActivity(
                context,
                105,
                calendarIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_text_day, calendarPendingIntent)

            // 4. RemoteViews Service Intent for the ListView
            val serviceIntent = Intent(context, ScheduleWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_lesson_list, serviceIntent)
            views.setEmptyView(R.id.widget_lesson_list, R.id.widget_empty_text)

            // 5. Pending Intents for Navigation & Action buttons
            val prevIntent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = WidgetActions.ACTION_PREV_DAY
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                PendingIntent.getBroadcast(context, 101, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val nextIntent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = WidgetActions.ACTION_NEXT_DAY
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                PendingIntent.getBroadcast(context, 102, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val copyIntent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = WidgetActions.ACTION_COPY_SCHEDULE
                putExtra(WidgetActions.EXTRA_DAY_INDEX, selectedDayIdx)
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_copy,
                PendingIntent.getBroadcast(context, 104, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val switchIntent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = WidgetActions.ACTION_TOGGLE_TYPE
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_switch,
                PendingIntent.getBroadcast(context, 103, switchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Open App on widget click to the exact day currently viewed on the widget
            val openAppIntent = AppIconManager.createAppIntent(context).apply {
                putExtra(MainActivity.EXTRA_TAB_INDEX, 0)
                putExtra(MainActivity.EXTRA_DAY_INDEX, selectedDayIdx)
                putExtra(MainActivity.EXTRA_TARGET_TYPE, prefs.targetType)
                putExtra(MainActivity.EXTRA_TARGET_NAME, prefs.targetName)
                data = Uri.parse("schedulewidget://open/$appWidgetId/$selectedDayIdx/${System.currentTimeMillis()}")
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                1000 + appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_text_day, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_text_bell, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_text, openAppPendingIntent)
            views.setPendingIntentTemplate(R.id.widget_lesson_list, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
