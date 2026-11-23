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

        // Helper to extract effective subject and room for a single lesson accounting for replacements and cancellations
        fun getEffectiveLessonInfo(lesson: ru.nstu.lyceum.schedule.data.api.Lesson): Pair<String, String>? {
            val changes = cachedSchedule?.getChangesForLesson(dayIdx, lesson.num) ?: emptyList()

            // Check total cancellation
            val isTotalCancelled = when {
                changes.isEmpty() -> false
                lesson.subgroups.isEmpty() -> changes.any { it.isCancelledLesson }
                lesson.subgroups.size <= 1 -> changes.any { it.isCancelledLesson }
                else -> {
                    val hasGeneralCancel = changes.any {
                        it.isCancelledLesson && it.group.isNullOrBlank() && it.subgroup.isNullOrBlank() && it.label.isNullOrBlank()
                    }
                    if (hasGeneralCancel) true
                    else {
                        lesson.subgroups.all { sg ->
                            val c = ru.nstu.lyceum.schedule.ui.components.findChangeForSubgroup(sg, changes, lesson.subgroups)
                            c?.isCancelledLesson == true
                        }
                    }
                }
            }
            if (isTotalCancelled) {
                return Pair("Урок отменен", "")
            }

            val isTech = lesson.subgroups.any {
                it.subject?.contains("технолог", ignoreCase = true) == true
            } && isMiddleSchool
            val targetGroup = if (isTech) userTechSubgroup else userSubgroup

            val userSg = if (lesson.subgroups.size > 1) {
                lesson.subgroups.firstOrNull { sg ->
                    val digit = sg.label?.filter { it.isDigit() }?.toIntOrNull()
                    digit == targetGroup
                } ?: lesson.subgroups.firstOrNull()
            } else {
                lesson.subgroups.firstOrNull()
            }

            if (userSg != null) {
                val sgChange = ru.nstu.lyceum.schedule.ui.components.findChangeForSubgroup(userSg, changes, lesson.subgroups)
                if (sgChange != null) {
                    if (sgChange.isCancelledLesson) {
                        return Pair("Урок отменен", "")
                    }
                    val subj = if (!sgChange.subject.isNullOrBlank() && sgChange.subject != "F" && sgChange.subject != "$" && !sgChange.subject.startsWith("$")) {
                        ru.nstu.lyceum.schedule.data.api.normalizeSubject(sgChange.subject)
                    } else {
                        ru.nstu.lyceum.schedule.data.api.normalizeSubject(userSg.subject)
                    }
                    val room = if (!sgChange.room.isNullOrBlank()) {
                        sgChange.room.trim().removePrefix("(").removeSuffix(")")
                    } else {
                        userSg.room?.trim()?.removePrefix("(")?.removeSuffix(")") ?: ""
                    }
                    return Pair(subj.ifEmpty { "Урок" }, room)
                }

                val subj = ru.nstu.lyceum.schedule.data.api.normalizeSubject(userSg.subject).ifEmpty { "Урок" }
                val room = userSg.room?.trim()?.removePrefix("(")?.removeSuffix(")") ?: ""
                return Pair(subj, room)
            }

            // Fallback for lessons without subgroups
            val rep = changes.firstOrNull { !it.isCancelledLesson }
            val subj = rep?.subject?.takeIf { it.isNotBlank() && it != "F" && it != "$" && !it.startsWith("$") }
                ?: lesson.displaySubject
            val room = rep?.room?.takeIf { it.isNotBlank() } ?: lesson.displayRoom
            return Pair(
                ru.nstu.lyceum.schedule.data.api.normalizeSubject(subj).ifEmpty { "Урок" },
                room.trim().removePrefix("(").removeSuffix(")")
            )
        }

        fun getUiItemInfo(item: ScheduleItemUi): Pair<String, String>? {
            return when (item) {
                is ScheduleItemUi.SingleLesson -> getEffectiveLessonInfo(item.lesson)
                is ScheduleItemUi.CombinedPair -> {
                    // For pairs, if during first lesson, show first; if during second or break, show appropriate
                    val firstInfo = getEffectiveLessonInfo(item.firstLesson)
                    val secondInfo = getEffectiveLessonInfo(item.secondLesson)
                    val curLsnNum = when (bellState) {
                        is BellState.InLesson -> if (isPairsMode) {
                            if (curTotalMinutes >= 90) item.secondLesson.num else item.firstLesson.num
                        } else bellState.num
                        else -> item.firstLesson.num
                    }
                    if (curLsnNum == item.secondLesson.num && secondInfo != null) secondInfo else firstInfo
                }
                else -> null
            }
        }

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
                val info = firstItem?.let { getUiItemInfo(it) }
                val lsnName = info?.first ?: "Урок"
                val lsnRoom = info?.second ?: ""
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

                val info = activeItem?.let { getUiItemInfo(it) }
                val lsnName = info?.first ?: if (isPairsMode) "Пара" else "Урок"
                val lsnRoom = info?.second ?: ""
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

                val info = nextItem?.let { getUiItemInfo(it) }
                val lsnName = info?.first
                val lsnRoom = info?.second

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
