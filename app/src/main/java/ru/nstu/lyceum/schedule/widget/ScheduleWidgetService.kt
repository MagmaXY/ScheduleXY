package ru.nstu.lyceum.schedule.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ru.nstu.lyceum.schedule.LyceumApp
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.data.api.Lesson
import ru.nstu.lyceum.schedule.domain.BellSchedule
import java.util.Calendar

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ScheduleRemoteViewsFactory(applicationContext)
    }
}

class ScheduleRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var lessons: List<Lesson> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val app = context.applicationContext as? LyceumApp ?: LyceumApp.instance
        val prefs = app.preferencesRepository
        val cache = app.cacheManager

        val targetType = prefs.targetType
        val targetName = prefs.targetName

        val cal = Calendar.getInstance()
        val currentDayIdx = BellSchedule.getScheduleDayIndex(cal)
        val offset = prefs.widgetDayOffset
        // Calculate day index within 0..5 (Mon..Sat)
        var targetDayIdx = (currentDayIdx + offset) % 6
        if (targetDayIdx < 0) targetDayIdx += 6

        val schedule = cache.getSchedule(targetType, targetName, 0)
        lessons = schedule?.days?.firstOrNull { it.dayIndex == targetDayIdx }?.lessons ?: emptyList()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = lessons.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= lessons.size) return RemoteViews(context.packageName, R.layout.widget_item_lesson)

        val lesson = lessons[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item_lesson)

        views.setTextViewText(R.id.item_lsn_num, lesson.num.toString())
        views.setTextViewText(R.id.item_lsn_time, lesson.time.split("–").firstOrNull()?.trim() ?: lesson.time)
        views.setTextViewText(R.id.item_lsn_subject, lesson.displaySubject)
        views.setTextViewText(R.id.item_lsn_sub, lesson.displayTeacherOrClass)

        val room = lesson.displayRoom
        if (room.isNotBlank()) {
            views.setTextViewText(R.id.item_lsn_room, room)
            views.setViewVisibility(R.id.item_lsn_room, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.item_lsn_room, android.view.View.GONE)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
