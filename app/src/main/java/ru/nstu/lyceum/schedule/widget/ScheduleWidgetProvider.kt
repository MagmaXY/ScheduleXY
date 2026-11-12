package ru.nstu.lyceum.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import ru.nstu.lyceum.schedule.LyceumApp
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.data.local.FreeAvailabilityEngine
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.domain.BellSchedule
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
        val prefs = try {
            LyceumApp.instance.preferencesRepository
        } catch (_: Exception) {
            PreferencesRepository(context)
        }

        when (intent.action) {
            WidgetActions.ACTION_PREV_DAY -> {
                prefs.widgetDayOffset = prefs.widgetDayOffset - 1
                notifyWidgetUpdate(context)
            }
            WidgetActions.ACTION_NEXT_DAY -> {
                prefs.widgetDayOffset = prefs.widgetDayOffset + 1
                notifyWidgetUpdate(context)
            }
            WidgetActions.ACTION_TOGGLE_TYPE -> {
                // Toggle between Class and Teacher mode
                if (prefs.targetType == PreferencesRepository.TYPE_CLASS) {
                    prefs.targetType = PreferencesRepository.TYPE_TEACHER
                    prefs.targetName = prefs.favoriteTargetName?.takeIf { prefs.favoriteTargetType == PreferencesRepository.TYPE_TEACHER }
                        ?: "Муль П.Ф."
                } else {
                    prefs.targetType = PreferencesRepository.TYPE_CLASS
                    prefs.targetName = prefs.favoriteTargetName?.takeIf { prefs.favoriteTargetType == PreferencesRepository.TYPE_CLASS }
                        ?: "10-1"
                }
                notifyWidgetUpdate(context)
            }
            WidgetActions.ACTION_REFRESH -> {
                notifyWidgetUpdate(context)
            }
        }
    }

    companion object {
        fun notifyWidgetUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ScheduleWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)

            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_lesson_list)
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = try {
                LyceumApp.instance.preferencesRepository
            } catch (_: Exception) {
                PreferencesRepository(context)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_schedule_2x2)

            // 1. Target Name & Type
            val isTeacher = prefs.targetType == PreferencesRepository.TYPE_TEACHER
            val targetPrefix = if (isTeacher) "Уч: " else "Кл: "
            views.setTextViewText(R.id.widget_text_target, targetPrefix + prefs.targetName)

            // 2. Real-time Bell Status
            val cal = Calendar.getInstance()
            val bellState = BellSchedule.getCurrentBellState(cal)
            views.setTextViewText(R.id.widget_text_bell, bellState.compactText)

            // 3. Day and Date calculation with offset
            val offset = prefs.widgetDayOffset
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val dayFormat = SimpleDateFormat("EE, d MMM", Locale("ru"))
            val formattedDate = dayFormat.format(cal.time).replaceFirstChar { it.titlecase(Locale("ru")) }
            views.setTextViewText(R.id.widget_text_day, formattedDate)

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

            val switchIntent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = WidgetActions.ACTION_TOGGLE_TYPE
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_switch,
                PendingIntent.getBroadcast(context, 103, switchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Open App on widget click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_target_type", prefs.targetType)
                putExtra("extra_target_name", prefs.targetName)
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                104,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
