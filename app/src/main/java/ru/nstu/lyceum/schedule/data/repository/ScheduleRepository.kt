package ru.nstu.lyceum.schedule.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import ru.nstu.lyceum.schedule.data.api.ArchiveResponse
import ru.nstu.lyceum.schedule.data.api.ClassesResponse
import ru.nstu.lyceum.schedule.data.api.EventsResponse
import ru.nstu.lyceum.schedule.data.api.FreeRoomsResponse
import ru.nstu.lyceum.schedule.data.api.FreeTeachersResponse
import ru.nstu.lyceum.schedule.data.api.LyceumApiService
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.TeachersResponse
import ru.nstu.lyceum.schedule.data.local.CacheManager
import ru.nstu.lyceum.schedule.data.local.FreeAvailabilityEngine

class ScheduleRepository(
    private val api: LyceumApiService,
    private val cache: CacheManager,
    private val prefs: PreferencesRepository
) {

    suspend fun getClasses(): ClassesResponse = withContext(Dispatchers.IO) {
        try {
            val response = api.getClasses()
            if (response.items.isNotEmpty()) {
                cache.saveClasses(response)
            }
            response
        } catch (e: Exception) {
            cache.getClasses() ?: ClassesResponse()
        }
    }

    suspend fun getTeachers(): TeachersResponse = withContext(Dispatchers.IO) {
        try {
            val response = api.getTeachers()
            if (response.items.isNotEmpty()) {
                cache.saveTeachers(response)
            }
            response
        } catch (e: Exception) {
            cache.getTeachers() ?: TeachersResponse()
        }
    }

    suspend fun getSchedule(type: String, name: String, week: Int = 0): Result<ScheduleResponse> = withContext(Dispatchers.IO) {
        try {
            var response = if (type == PreferencesRepository.TYPE_TEACHER) {
                api.getTeacherSchedule(name, week)
            } else {
                api.getClassSchedule(name, week)
            }

            if (type == PreferencesRepository.TYPE_TEACHER) {
                response = enrichTeacherScheduleWithGroupLabels(name, week, response)
            }

            cache.saveSchedule(type, name, week, response)
            if (week == 0) {
                // Silently update changes in cache for all other cached schedules in background
                cache.updateCachedScheduleChanges(response.exportDate, response.changes)

                // Check notifications for both favorite class and favorite teacher
                try {
                    ru.nstu.lyceum.schedule.notifications.ScheduleNotificationManager.checkForScheduleUpdates(
                        context = ru.nstu.lyceum.schedule.LyceumApp.instance,
                        newSchedule = response
                    )
                } catch (_: Exception) {}
            }
            Result.success(response)
        } catch (e: Exception) {
            val cached = cache.getSchedule(type, name, week)
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Enriches teacher schedule subgroups with subgroup labels (e.g. "гр. 1", "гр. 2")
     * fetched from the corresponding class schedules.
     */
    private suspend fun enrichTeacherScheduleWithGroupLabels(
        teacherName: String,
        week: Int,
        teacherSchedule: ScheduleResponse
    ): ScheduleResponse = withContext(Dispatchers.IO) {
        try {
            // Find all distinct classes mentioned in this teacher's schedule
            val classNames = mutableSetOf<String>()
            teacherSchedule.days.forEach { day ->
                day.lessons.forEach { lesson ->
                    lesson.subgroups.forEach { sg ->
                        val c = sg.className?.trim()
                        if (!c.isNullOrBlank()) {
                            classNames.add(c)
                        }
                    }
                }
            }

            if (classNames.isEmpty()) return@withContext teacherSchedule

            // Fetch class schedules (from cache or API)
            val classSchedules = mutableMapOf<String, ScheduleResponse>()
            val classesToFetch = mutableListOf<String>()

            for (c in classNames) {
                val cached = cache.getSchedule(PreferencesRepository.TYPE_CLASS, c, week)
                if (cached != null) {
                    classSchedules[c] = cached
                } else {
                    classesToFetch.add(c)
                }
            }

            if (classesToFetch.isNotEmpty()) {
                val dispatcher = Dispatchers.IO.limitedParallelism(6)
                val deferreds = classesToFetch.map { c ->
                    async(dispatcher) {
                        try {
                            val sched = api.getClassSchedule(c, week)
                            cache.saveSchedule(PreferencesRepository.TYPE_CLASS, c, week, sched)
                            c to sched
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                deferreds.awaitAll().filterNotNull().forEach { (c, sched) ->
                    classSchedules[c] = sched
                }
            }

            val cleanTeacher = teacherName.replace("\\s+".toRegex(), "").lowercase()

            val newDays = teacherSchedule.days.map { day ->
                val newLessons = day.lessons.map { lesson ->
                    val newSubgroups = lesson.subgroups.map { sg ->
                        val cName = sg.className?.trim()
                        if (!cName.isNullOrBlank() && sg.label.isNullOrBlank()) {
                            val classSched = classSchedules[cName]
                            val matchingClassDay = classSched?.days?.firstOrNull { it.dayIndex == day.dayIndex }
                            val matchingClassLesson = matchingClassDay?.lessons?.firstOrNull { it.num == lesson.num }
                            val matchingSg = matchingClassLesson?.subgroups?.firstOrNull { csg ->
                                val cTeacher = (csg.teacher ?: "").replace("\\s+".toRegex(), "").lowercase()
                                cTeacher == cleanTeacher || (cTeacher.isNotEmpty() && cleanTeacher.contains(cTeacher)) || (cleanTeacher.isNotEmpty() && cTeacher.contains(cleanTeacher))
                            }
                            if (matchingSg != null && !matchingSg.label.isNullOrBlank()) {
                                sg.copy(label = matchingSg.label)
                            } else {
                                sg
                            }
                        } else {
                            sg
                        }
                    }
                    lesson.copy(subgroups = newSubgroups)
                }
                day.copy(lessons = newLessons)
            }

            teacherSchedule.copy(days = newDays)
        } catch (_: Exception) {
            teacherSchedule
        }
    }

    /**
     * Silently fetches schedule for any target and updates its cache in the background without triggering notifications.
     */
    suspend fun fetchAndCacheDirectly(type: String, name: String, week: Int = 0): ScheduleResponse? = withContext(Dispatchers.IO) {
        try {
            var response = if (type == PreferencesRepository.TYPE_TEACHER) {
                api.getTeacherSchedule(name, week)
            } else {
                api.getClassSchedule(name, week)
            }
            if (type == PreferencesRepository.TYPE_TEACHER) {
                response = enrichTeacherScheduleWithGroupLabels(name, week, response)
            }
            cache.saveSchedule(type, name, week, response)
            response
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getArchiveWeeks(): ArchiveResponse = withContext(Dispatchers.IO) {
        try {
            val response = api.getArchiveWeeks()
            if (response.weeks.isNotEmpty()) {
                cache.saveArchiveWeeks(response)
            }
            response
        } catch (e: Exception) {
            cache.getArchiveWeeks() ?: ArchiveResponse()
        }
    }

    suspend fun getEvents(): EventsResponse = withContext(Dispatchers.IO) {
        try {
            val response = api.getEvents()
            if (response.items.isNotEmpty()) {
                cache.saveEvents(response)
            }
            response
        } catch (e: Exception) {
            cache.getEvents() ?: EventsResponse()
        }
    }

    fun getCachedSchedule(type: String, name: String, week: Int = 0): ScheduleResponse? {
        return cache.getSchedule(type, name, week)
    }

    fun getCachedAllClassSchedules(): List<ScheduleResponse> {
        return cache.getAllCachedClassSchedules(0)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cache.clearCache()
    }

    suspend fun prefetchAllClasses(force: Boolean = false): List<ScheduleResponse> = withContext(Dispatchers.IO) {
        val cached = cache.getAllCachedClassSchedules(0)
        if (!force && cached.size >= 35) {
            return@withContext cached
        }

        val classes = try {
            api.getClasses().items
        } catch (e: Exception) {
            cache.getClasses()?.items ?: emptyList()
        }
        if (classes.isEmpty()) return@withContext cached

        val dispatcher = Dispatchers.IO.limitedParallelism(10)
        val deferreds = classes.map { cls ->
            async(dispatcher) {
                try {
                    val s = api.getClassSchedule(cls.name, 0)
                    cache.saveSchedule(PreferencesRepository.TYPE_CLASS, cls.name, 0, s)
                    s
                } catch (e: Exception) {
                    cache.getSchedule(PreferencesRepository.TYPE_CLASS, cls.name, 0)
                }
            }
        }
        val results = deferreds.awaitAll().filterNotNull()
        if (results.isNotEmpty()) results else cached
    }

    suspend fun getFreeRooms(dayIndex: Int, lessonNum: Int): FreeRoomsResponse = withContext(Dispatchers.IO) {
        try {
            // First attempt with admin password
            api.getFreeRooms(prefs.adminPassword)
        } catch (e: Exception) {
            // Seamlessly fall back to local calculation engine using all 39 class schedules
            val allClassSchedules = prefetchAllClasses()
            FreeAvailabilityEngine.generateFullLocalFreeRooms(allClassSchedules)
        }
    }

    suspend fun getFreeTeachers(dayIndex: Int, lessonNum: Int): FreeTeachersResponse = withContext(Dispatchers.IO) {
        try {
            // First attempt with admin password
            api.getFreeTeachers(prefs.adminPassword)
        } catch (e: Exception) {
            // Fallback to local calculation engine
            val teachers = getTeachers().items
            val allClassSchedules = prefetchAllClasses()
            FreeAvailabilityEngine.generateFullLocalFreeTeachers(allClassSchedules, teachers)
        }
    }

    suspend fun checkAdminPassword(password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = api.checkAdminPassword(password)
            res.ok
        } catch (e: Exception) {
            false
        }
    }

    suspend fun refreshServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = api.refreshSchedule()
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
