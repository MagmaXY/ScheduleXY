package ru.nstu.lyceum.schedule.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.nstu.lyceum.schedule.LyceumApp
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.widget.ScheduleWidgetProvider
import java.util.concurrent.TimeUnit

class ScheduleSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as? LyceumApp ?: return@withContext Result.success()
            val repo = app.scheduleRepository
            val prefs = app.preferencesRepository
            val cache = app.cacheManager

            val favClass = prefs.favoriteClass
            val favTeacher = prefs.favoriteTeacher

            val primaryType = prefs.targetType
            val primaryName = if (primaryType == PreferencesRepository.TYPE_TEACHER) {
                favTeacher ?: prefs.targetName.takeIf { it.isNotBlank() } ?: PreferencesRepository.DEFAULT_TEACHER
            } else {
                favClass ?: prefs.targetName.takeIf { it.isNotBlank() } ?: PreferencesRepository.DEFAULT_CLASS
            }

            val scheduleResult = repo.getSchedule(primaryType, primaryName, 0)
            val schedule = scheduleResult.getOrNull()

            // If user also has a favorite teacher/class not matching primaryType, make sure it is also fetched into cache
            if (!favTeacher.isNullOrBlank() && (primaryType != PreferencesRepository.TYPE_TEACHER || primaryName != favTeacher)) {
                try {
                    repo.fetchAndCacheDirectly(PreferencesRepository.TYPE_TEACHER, favTeacher, 0)
                } catch (_: Exception) {}
            }
            if (!favClass.isNullOrBlank() && (primaryType != PreferencesRepository.TYPE_CLASS || primaryName != favClass)) {
                try {
                    repo.fetchAndCacheDirectly(PreferencesRepository.TYPE_CLASS, favClass, 0)
                } catch (_: Exception) {}
            }

            if (schedule != null) {
                val isMiddle = prefs.isMiddleSchoolClass(schedule.title)
                ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper.checkAndRelocateCancelledHomework(
                    notesRepository = app.notesRepository,
                    schedule = schedule,
                    userSubgroup = prefs.studentSubgroup,
                    userTechSubgroup = prefs.studentTechSubgroup,
                    isMiddleSchool = isMiddle
                )
                ru.nstu.lyceum.schedule.ui.util.NotesTimeHelper.resolvePendingTransfers(
                    notesRepository = app.notesRepository,
                    schedule = schedule,
                    userSubgroup = prefs.studentSubgroup,
                    userTechSubgroup = prefs.studentTechSubgroup,
                    isMiddleSchool = isMiddle
                )

                // Checks for changes to BOTH favorite class and favorite teacher
                ScheduleNotificationManager.checkForScheduleUpdates(
                    context = applicationContext,
                    newSchedule = schedule
                )

                // Silently refresh cache in the background for any other classes currently in cache that had changes
                val changedClassNames = schedule.changes?.classChanges?.values
                    ?.mapNotNull { it.className }
                    ?.filter { !it.equals(favClass, ignoreCase = true) }
                    ?.toSet() ?: emptySet()

                for (otherCls in changedClassNames) {
                    if (cache.hasSchedule(PreferencesRepository.TYPE_CLASS, otherCls, 0)) {
                        try {
                            repo.fetchAndCacheDirectly(PreferencesRepository.TYPE_CLASS, otherCls, 0)
                        } catch (_: Exception) {}
                    }
                }

                ScheduleWidgetProvider.notifyWidgetUpdate(applicationContext)
            }

            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "schedule_sync_work"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ScheduleSyncWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
