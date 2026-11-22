package ru.nstu.lyceum.schedule.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.data.api.ScheduleChangeItem
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.ui.AppIconManager
import ru.nstu.lyceum.schedule.ui.MainActivity
import java.util.Calendar

object ScheduleNotificationManager {

    const val CHANNEL_ID = "schedule_updates_channel"
    private const val CHANNEL_NAME = "Изменения в расписании"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления об отменах уроков, переменах кабинетов и публикации нового расписания"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        targetType: String,
        targetName: String,
        dayIndex: Int
    ) {
        val prefs = PreferencesRepository(context)
        if (!prefs.notificationsEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        createNotificationChannel(context)

        val intent = AppIconManager.createAppIntent(context).apply {
            putExtra(MainActivity.EXTRA_TAB_INDEX, 0)
            putExtra(MainActivity.EXTRA_DAY_INDEX, dayIndex)
            putExtra(MainActivity.EXTRA_TARGET_TYPE, targetType)
            putExtra(MainActivity.EXTRA_TARGET_NAME, targetName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconRes = if (prefs.useMagmaLogo) R.drawable.ic_magma_logo else R.drawable.ic_lyceum_logo
        val smallIconRes = if (prefs.useMagmaLogo) R.drawable.ic_notification_magma else R.drawable.ic_notification

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, largeIconRes)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .apply {
                if (largeIcon != null) setLargeIcon(largeIcon)
            }
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, builder.build())
    }

    fun getActiveFavorites(prefs: PreferencesRepository): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val favClass = prefs.favoriteClass
        if (!favClass.isNullOrBlank()) {
            list.add(PreferencesRepository.TYPE_CLASS to favClass)
        }
        val favTeacher = prefs.favoriteTeacher
        if (!favTeacher.isNullOrBlank()) {
            list.add(PreferencesRepository.TYPE_TEACHER to favTeacher)
        }
        return list
    }

    fun checkForScheduleUpdates(
        context: Context,
        newSchedule: ScheduleResponse,
        targetType: String? = null,
        targetName: String? = null
    ) {
        val prefs = PreferencesRepository(context)
        if (!prefs.notificationsEnabled) return

        val activeFavorites = if (!targetType.isNullOrBlank() && !targetName.isNullOrBlank()) {
            val favClass = prefs.favoriteClass
            val favTeacher = prefs.favoriteTeacher
            val isFav = (targetType == PreferencesRepository.TYPE_CLASS && !favClass.isNullOrBlank() && targetName.equals(favClass, ignoreCase = true)) ||
                    (targetType == PreferencesRepository.TYPE_TEACHER && !favTeacher.isNullOrBlank() && targetName.equals(favTeacher, ignoreCase = true))
            if (!isFav) return
            listOf(targetType to targetName)
        } else {
            getActiveFavorites(prefs)
        }

        if (activeFavorites.isEmpty()) return

        val weekStart = newSchedule.weekStart
        val exportDate = newSchedule.exportDate

        val lastWeek = prefs.lastNotifiedWeekStart
        val knownTargets = prefs.lastNotifiedTargets.toMutableSet()
        val knownKeys = prefs.lastNotifiedChangeKeys.toMutableSet()

        // 1. Initial global seed if never checked before
        if (lastWeek == null) {
            prefs.lastNotifiedWeekStart = weekStart
            prefs.lastNotifiedExportDate = exportDate
            for ((type, name) in activeFavorites) {
                knownTargets.add("$type:$name")
                val changes = extractMatchingChanges(newSchedule, type, name)
                for (c in changes) {
                    knownKeys.add("$type|$name|${c.uniqueKey}")
                }
            }
            prefs.lastNotifiedTargets = knownTargets
            prefs.lastNotifiedChangeKeys = knownKeys
            return
        }

        // 2. Check for New Week Schedule publication (e.g. Saturday evening)
        val isNewWeek = !weekStart.isNullOrBlank() && weekStart != lastWeek
        if (isNewWeek) {
            prefs.lastNotifiedWeekStart = weekStart
            prefs.lastNotifiedExportDate = exportDate
            knownKeys.clear()

            val weekRange = formatWeekRange(weekStart)
            for ((type, name) in activeFavorites) {
                val notifId = if (type == PreferencesRepository.TYPE_TEACHER) 1002 else 1001
                sendNotification(
                    context = context,
                    notificationId = notifId,
                    title = "Новое расписание: $name",
                    message = "Выгружено расписание на неделю $weekRange",
                    targetType = type,
                    targetName = name,
                    dayIndex = 0 // Monday
                )
            }
        }

        // 3. Check for mid-week changes for each active favorite
        val app = context.applicationContext as? ru.nstu.lyceum.schedule.LyceumApp
        val notesRepo = app?.notesRepository

        for ((type, name) in activeFavorites) {
            val targetKey = "$type:$name"
            val matchingChanges = extractMatchingChanges(newSchedule, type, name)

            val isMiddle = prefs.isMiddleSchoolClass(name)
            val userSg = prefs.studentSubgroup
            val userTechSg = prefs.studentTechSubgroup

            val relocatedNotes = if (notesRepo != null) {
                ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper.checkAndRelocateCancelledHomework(
                    notesRepository = notesRepo,
                    schedule = newSchedule,
                    userSubgroup = userSg,
                    userTechSubgroup = userTechSg,
                    isMiddleSchool = isMiddle
                )
            } else emptyList()

            if (notesRepo != null) {
                ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper.resolvePendingTransfers(
                    notesRepository = notesRepo,
                    schedule = newSchedule,
                    userSubgroup = userSg,
                    userTechSubgroup = userTechSg,
                    isMiddleSchool = isMiddle
                )
            }

            // If this target was just recently added to favorites, seed its current changes
            if (targetKey !in knownTargets) {
                knownTargets.add(targetKey)
                for (c in matchingChanges) {
                    knownKeys.add("$type|$name|${c.uniqueKey}")
                }
                continue
            }

            if (matchingChanges.isNotEmpty()) {
                val newChanges = matchingChanges.filter { "$type|$name|${it.uniqueKey}" !in knownKeys }

                if (newChanges.isNotEmpty()) {
                    for ((idx, change) in newChanges.withIndex()) {
                        val dayIdx = parseDayIndex(change.item.dateStr)
                        val dayName = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
                            .getOrElse(dayIdx) { "" }

                        val lsnNum = change.item.lessonNum?.let { "$it урок" } ?: "Урок"
                        val rawSubj = change.item.subject?.takeIf {
                            it != "F" && it != "$" && !it.startsWith("$") && !it.equals("Урок отменен", ignoreCase = true)
                        } ?: ""
                        val subjectWithParens = if (rawSubj.isNotBlank()) " ($rawSubj)" else ""

                        val details = resolveChangeDetails(
                            changeItem = change.item,
                            type = type,
                            targetName = name,
                            dayIdx = dayIdx,
                            newSchedule = newSchedule,
                            prefs = prefs
                        )

                        val notifText = "$dayName, $lsnNum$subjectWithParens: $details"
                        val baseId = if (type == PreferencesRepository.TYPE_TEACHER) 3000 else 2000
                        val notifId = baseId + dayIdx * 20 + (change.item.lessonNum ?: (idx % 20))

                        sendNotification(
                            context = context,
                            notificationId = notifId,
                            title = "Изменение в расписании: $name",
                            message = notifText,
                            targetType = type,
                            targetName = name,
                            dayIndex = dayIdx
                        )

                        knownKeys.add("$type|$name|${change.uniqueKey}")
                    }
                }
            }
        }

        prefs.lastNotifiedTargets = knownTargets
        prefs.lastNotifiedChangeKeys = knownKeys
    }

    private data class TrackedChange(
        val uniqueKey: String,
        val item: ScheduleChangeItem
    )

    private fun extractMatchingChanges(
        schedule: ScheduleResponse,
        targetType: String,
        targetName: String
    ): List<TrackedChange> {
        val map = if (targetType == PreferencesRepository.TYPE_TEACHER) {
            schedule.changes?.teacherChanges
        } else {
            schedule.changes?.classChanges
        } ?: return emptyList()

        val normalizedTarget = targetName.trim().replace("\\s+".toRegex(), " ")
        val results = mutableListOf<TrackedChange>()

        for ((key, item) in map) {
            val parts = key.split("|")
            val keyTarget = parts.getOrNull(0)?.trim()?.replace("\\s+".toRegex(), " ")

            val matches = keyTarget?.equals(normalizedTarget, ignoreCase = true) == true ||
                item.className?.trim()?.equals(normalizedTarget, ignoreCase = true) == true ||
                item.teacher?.trim()?.equals(normalizedTarget, ignoreCase = true) == true

            if (matches) {
                val uniqueKey = "${item.dateStr}|${item.lessonNum}|${item.isCancelledLesson}|${item.subject}|${item.room}|${item.teacher}"
                results.add(TrackedChange(uniqueKey, item))
            }
        }
        return results
    }

    private fun resolveChangeDetails(
        changeItem: ScheduleChangeItem,
        type: String,
        targetName: String,
        dayIdx: Int,
        newSchedule: ScheduleResponse,
        prefs: PreferencesRepository
    ): String {
        val userSg = prefs.studentSubgroup
        val userTechSg = prefs.studentTechnologySubgroup
        val isMiddleSchool = prefs.isMiddleSchoolClass(targetName)

        val dayLessons = newSchedule.days?.firstOrNull { it.dayIndex == dayIdx }?.lessons
            ?: newSchedule.days?.getOrNull(dayIdx)?.lessons
        val baseLesson = dayLessons?.firstOrNull { it.num == changeItem.lessonNum }
        val subgroups = baseLesson?.subgroups ?: emptyList()

        if (changeItem.isCancelledLesson) {
            val forClass = if (type == PreferencesRepository.TYPE_TEACHER && !changeItem.className.isNullOrBlank()) {
                "${changeItem.className}: "
            } else ""

            if (type == PreferencesRepository.TYPE_CLASS && subgroups.size > 1) {
                // Find subgroup targeted by this cancellation
                val cSgDigit = (changeItem.group ?: changeItem.subgroup ?: changeItem.label)?.filter { it.isDigit() }
                val targetSg = subgroups.firstOrNull { sg ->
                    val sgNum = sg.label?.filter { it.isDigit() }
                    (!cSgDigit.isNullOrBlank() && sgNum == cSgDigit) ||
                    (!changeItem.teacher.isNullOrBlank() && sg.teacher?.trim()?.equals(changeItem.teacher?.trim(), ignoreCase = true) == true) ||
                    (!changeItem.room.isNullOrBlank() && sg.room?.trim()?.equals(changeItem.room?.trim(), ignoreCase = true) == true)
                }

                if (targetSg != null) {
                    val sgNum = targetSg.label?.filter { it.isDigit() }
                    val sgLabel = if (!sgNum.isNullOrBlank()) "$sgNum подгруппа" else (targetSg.label ?: "подгруппа")
                    val otherSg = subgroups.firstOrNull { it != targetSg }
                    val otherNum = otherSg?.label?.filter { it.isDigit() }
                    val otherLabel = if (!otherNum.isNullOrBlank()) "$otherNum подгруппы" else "другой подгруппы"

                    val isTech = targetSg.subject?.lowercase()?.contains("технолог") == true
                    val activeUserSg = if (isTech && isMiddleSchool) userTechSg else userSg

                    return when {
                        activeUserSg > 0 && sgNum == activeUserSg.toString() -> {
                            "[Ваша $sgLabel] Урок отменен"
                        }
                        activeUserSg > 0 && !sgNum.isNullOrBlank() && sgNum != activeUserSg.toString() -> {
                            "Отмена у $sgLabel (у вашей подгруппы урок остался!)"
                        }
                        else -> {
                            "$sgLabel — отменен (у $otherLabel остался)"
                        }
                    }
                }
            }
            return "${forClass}урок отменен"
        }

        // Handle replacements / room moves for subgroups
        val sgPrefix = if (type == PreferencesRepository.TYPE_CLASS && subgroups.size > 1) {
            val cSgDigit = (changeItem.group ?: changeItem.subgroup ?: changeItem.label)?.filter { it.isDigit() }
            val matchedSg = subgroups.firstOrNull { sg ->
                val sgNum = sg.label?.filter { it.isDigit() }
                !cSgDigit.isNullOrBlank() && sgNum == cSgDigit
            }
            val num = matchedSg?.label?.filter { it.isDigit() } ?: cSgDigit
            if (!num.isNullOrBlank()) "$num подгруппа: " else ""
        } else ""

        return when {
            !changeItem.room.isNullOrBlank() && !changeItem.teacher.isNullOrBlank() && type == PreferencesRepository.TYPE_CLASS ->
                "${sgPrefix}каб. ${changeItem.room} (${changeItem.teacher})"
            !changeItem.room.isNullOrBlank() && !changeItem.className.isNullOrBlank() && type == PreferencesRepository.TYPE_TEACHER ->
                "${changeItem.className}, каб. ${changeItem.room}"
            !changeItem.room.isNullOrBlank() -> "${sgPrefix}каб. ${changeItem.room}"
            !changeItem.teacher.isNullOrBlank() -> "${sgPrefix}замена: ${changeItem.teacher}"
            else -> "${sgPrefix}изменение расписания"
        }
    }

    private fun parseDayIndex(dateStr: String?): Int {
        if (dateStr.isNullOrBlank()) return 0
        try {
            if (dateStr.contains(".")) {
                val parts = dateStr.split(".")
                if (parts.size == 3) {
                    val cal = Calendar.getInstance().apply {
                        set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                    }
                    val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0..6
                    return dow.coerceIn(0, 5)
                }
            } else if (dateStr.contains("-")) {
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    val cal = Calendar.getInstance().apply {
                        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    }
                    val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0..6
                    return dow.coerceIn(0, 5)
                }
            }
        } catch (_: Exception) {}
        return 0
    }

    private fun formatWeekRange(weekStart: String): String {
        try {
            val parts = weekStart.split("-")
            if (parts.size == 3) {
                val cal = Calendar.getInstance().apply {
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                }
                val startDay = cal.get(Calendar.DAY_OF_MONTH)
                val startMonth = cal.get(Calendar.MONTH)
                cal.add(Calendar.DAY_OF_MONTH, 5)
                val endDay = cal.get(Calendar.DAY_OF_MONTH)
                val endMonth = cal.get(Calendar.MONTH)

                val months = arrayOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
                return if (startMonth == endMonth) {
                    "$startDay–$endDay ${months.getOrElse(startMonth) { "" }}"
                } else {
                    "$startDay ${months.getOrElse(startMonth) { "" }} – $endDay ${months.getOrElse(endMonth) { "" }}"
                }
            }
        } catch (_: Exception) {}
        return weekStart
    }
}
