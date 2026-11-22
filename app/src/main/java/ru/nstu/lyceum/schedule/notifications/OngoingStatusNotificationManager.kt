package ru.nstu.lyceum.schedule.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ru.nstu.lyceum.schedule.LyceumApp
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.domain.BellState
import ru.nstu.lyceum.schedule.domain.PairSchedule
import ru.nstu.lyceum.schedule.domain.VacationChecker
import ru.nstu.lyceum.schedule.ui.AppIconManager
import ru.nstu.lyceum.schedule.ui.MainActivity
import ru.nstu.lyceum.schedule.ui.components.ScheduleItemUi
import ru.nstu.lyceum.schedule.ui.components.SchedulePairMerger
import java.util.Calendar

object OngoingStatusNotificationManager {

    const val CHANNEL_ID = "status_bar_ongoing_channel"
    private const val CHANNEL_NAME = "Статус текущего урока в шторке"
    private const val NOTIFICATION_ID = 20261

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Постоянная строка текущего урока и времени до звонка в шторке уведомлений"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun cancel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    /**
     * Updates ongoing status bar notification.
     * Enforces the exact same time segment as percentage status bar:
     * - Does NOT run before 8:00 AM
     * - Does NOT run after lessons end (no homework notifications)
     * - Respects the user's specific subgroup (studentSubgroup, studentTechnologySubgroup)
     * - Supports pairs mode (statusBarPairsMode and showPairsSchedule)
     */
    fun update(context: Context) {
        val prefs = PreferencesRepository(context)
        if (!prefs.ongoingStatusNotificationEnabled) {
            cancel(context)
            return
        }

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) {
            cancel(context)
            return
        }

        if (VacationChecker.isVacation(context, cal)) {
            cancel(context)
            return
        }

        val curHour = cal.get(Calendar.HOUR_OF_DAY)
        val curMinute = cal.get(Calendar.MINUTE)
        val curTotalMinutes = curHour * 60 + curMinute

        val isPairsMode = prefs.statusBarPairsMode
        val maxTotalMinutes = if (isPairsMode) 15 * 60 + 30 else 19 * 60

        // Rule 1: Do not work before 08:00 AM or after lessons end (15:30 for 4th pair, 19:00 for 12th lesson)
        if (curTotalMinutes < 8 * 60 || curTotalMinutes >= maxTotalMinutes) {
            cancel(context)
            return
        }

        val isVacation = false
        val bellState = if (isPairsMode) {
            PairSchedule.getCurrentPairState(cal, isVacation)
        } else {
            BellSchedule.getCurrentBellState(cal, isVacation)
        }

        // Rule 2: After lessons end, do not show and do not notify about homework
        if (bellState is BellState.AfterSchool || bellState is BellState.Weekend || bellState is BellState.Vacation) {
            cancel(context)
            return
        }

        // Get user's active lessons for today matching subgroup
        val app = context.applicationContext as? LyceumApp
        val targetType = prefs.targetType
        val targetName = prefs.targetName
        val cachedSchedule = app?.cacheManager?.getSchedule(targetType, targetName, 0)
        val dayIdx = BellSchedule.getScheduleDayIndex(cal)

        val dayLessons = cachedSchedule?.days?.firstOrNull { it.dayIndex == dayIdx }?.lessons
            ?: cachedSchedule?.days?.getOrNull(dayIdx)?.lessons
            ?: emptyList()

        val isMiddleSchool = prefs.isMiddleSchoolClass(targetName)
        val userSubgroup = prefs.studentSubgroup
        val userTechSubgroup = prefs.studentTechnologySubgroup

        // Filter by user's subgroup
        val filteredLessons = dayLessons.filter { lsn ->
            val isTech = lsn.subgroups.any { it.subject?.lowercase()?.contains("технолог") == true }
            val activeSubgroups = lsn.subgroups.filter { sg ->
                if (sg.label.isNullOrBlank()) true
                else {
                    val s = sg.subject?.lowercase() ?: ""
                    if (s.contains("технолог") && isMiddleSchool) {
                        sg.label.contains(userTechSubgroup.toString())
                    } else {
                        sg.label.contains(userSubgroup.toString())
                    }
                }
            }
            activeSubgroups.isNotEmpty() || lsn.subgroups.isEmpty()
        }

        if (filteredLessons.isEmpty()) {
            cancel(context)
            return
        }

        val processedItems = if (prefs.showPairsSchedule) {
            SchedulePairMerger.processLessons(
                lessons = filteredLessons,
                isTeacherMode = targetType == PreferencesRepository.TYPE_TEACHER,
                targetName = targetName,
                showPairsSetting = true
            )
        } else {
            filteredLessons.map { ScheduleItemUi.SingleLesson(it) }
        }

        var title = ""
        var text = ""
        var progressPercent: Int? = null

        when (bellState) {
            is BellState.BeforeSchool -> {
                val firstItem = processedItems.firstOrNull()
                val lsnName = when (firstItem) {
                    is ScheduleItemUi.SingleLesson -> firstItem.lesson.displaySubject
                    is ScheduleItemUi.CombinedPair -> firstItem.firstLesson.displaySubject
                    else -> "Урок"
                }
                val lsnRoom = when (firstItem) {
                    is ScheduleItemUi.SingleLesson -> firstItem.lesson.displayRoom
                    is ScheduleItemUi.CombinedPair -> firstItem.firstLesson.displayRoom
                    else -> ""
                }
                val roomStr = if (lsnRoom.isNotBlank()) " • каб. $lsnRoom" else ""
                val prefix = if (isPairsMode) "До 1 пары" else "До 1 урока"

                title = "$prefix: ${bellState.minutesUntil} мин"
                text = "1-й: $lsnName$roomStr (в ${bellState.firstLessonStart})"
            }

            is BellState.InLesson -> {
                val num = bellState.num
                val matchingLessonNums = if (isPairsMode) setOf(num * 2 - 1, num * 2) else setOf(num)
                val activeItem = processedItems.firstOrNull { item ->
                    when (item) {
                        is ScheduleItemUi.SingleLesson -> item.lesson.num in matchingLessonNums
                        is ScheduleItemUi.CombinedPair -> item.firstLesson.num in matchingLessonNums || item.secondLesson.num in matchingLessonNums
                        else -> false
                    }
                }

                val lsnName = when (activeItem) {
                    is ScheduleItemUi.SingleLesson -> activeItem.lesson.displaySubject
                    is ScheduleItemUi.CombinedPair -> activeItem.firstLesson.displaySubject
                    else -> if (isPairsMode) "Пара" else "Урок"
                }
                val lsnRoom = when (activeItem) {
                    is ScheduleItemUi.SingleLesson -> activeItem.lesson.displayRoom
                    is ScheduleItemUi.CombinedPair -> activeItem.firstLesson.displayRoom
                    else -> ""
                }
                val roomStr = if (lsnRoom.isNotBlank()) " • каб. $lsnRoom" else ""
                val label = if (isPairsMode) "$num пара" else "$num урок"

                title = "[$label] $lsnName$roomStr"
                text = "До звонка: ${bellState.minutesLeft} мин"
                progressPercent = (bellState.progress * 100).toInt().coerceIn(0, 100)
            }

            is BellState.Break -> {
                val nextNum = bellState.nextNum
                val nextMatchingNums = if (isPairsMode) setOf(nextNum * 2 - 1, nextNum * 2) else setOf(nextNum)
                val nextItem = processedItems.firstOrNull { item ->
                    when (item) {
                        is ScheduleItemUi.SingleLesson -> item.lesson.num in nextMatchingNums
                        is ScheduleItemUi.CombinedPair -> item.firstLesson.num in nextMatchingNums || item.secondLesson.num in nextMatchingNums
                        else -> false
                    }
                }

                val lsnName = when (nextItem) {
                    is ScheduleItemUi.SingleLesson -> nextItem.lesson.displaySubject
                    is ScheduleItemUi.CombinedPair -> nextItem.firstLesson.displaySubject
                    else -> null
                }
                val lsnRoom = when (nextItem) {
                    is ScheduleItemUi.SingleLesson -> nextItem.lesson.displayRoom
                    is ScheduleItemUi.CombinedPair -> nextItem.firstLesson.displayRoom
                    else -> null
                }

                val nextInfo = if (!lsnName.isNullOrBlank()) {
                    val roomStr = if (!lsnRoom.isNullOrBlank()) " • каб. $lsnRoom" else ""
                    "След: $lsnName$roomStr"
                } else {
                    val nextLabel = if (isPairsMode) "$nextNum пары" else "$nextNum урока"
                    "До $nextLabel"
                }

                title = "[Перемена] $nextInfo"
                text = "Осталось: ${bellState.minutesLeft} мин (до ${bellState.nextStart})"
                progressPercent = (bellState.progress * 100).toInt().coerceIn(0, 100)
            }

            else -> {
                cancel(context)
                return
            }
        }

        val intent = AppIconManager.createAppIntent(context).apply {
            putExtra(MainActivity.EXTRA_TAB_INDEX, 0)
            putExtra(MainActivity.EXTRA_DAY_INDEX, dayIdx)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = if (prefs.useMagmaLogo) R.drawable.ic_notification_magma else R.drawable.ic_notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progressPercent != null && progressPercent in 0..100) {
            builder.setProgress(100, progressPercent, false)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createNotificationChannel(context)
        notificationManager?.notify(NOTIFICATION_ID, builder.build())
    }
}
