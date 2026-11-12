package ru.nstu.lyceum.schedule.data.repository

import kotlinx.coroutines.Dispatchers
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
            val response = if (type == PreferencesRepository.TYPE_TEACHER) {
                api.getTeacherSchedule(name, week)
            } else {
                api.getClassSchedule(name, week)
            }
            cache.saveSchedule(type, name, week, response)
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

    suspend fun getArchiveWeeks(): ArchiveResponse = withContext(Dispatchers.IO) {
        try {
            api.getArchiveWeeks()
        } catch (e: Exception) {
            ArchiveResponse(weeks = listOf("2026-10-26", "2026-10-19", "2026-10-12"))
        }
    }

    suspend fun getEvents(): EventsResponse = withContext(Dispatchers.IO) {
        try {
            val res = api.getEvents()
            if (res.items.isNotEmpty()) {
                cache.saveEvents(res)
            }
            res
        } catch (e: Exception) {
            cache.getEvents() ?: EventsResponse()
        }
    }

    suspend fun getFreeRooms(dayIndex: Int, lessonNum: Int): FreeRoomsResponse = withContext(Dispatchers.IO) {
        try {
            // First attempt with admin password
            api.getFreeRooms(prefs.adminPassword)
        } catch (e: Exception) {
            // Seamlessly fall back to local calculation engine
            val allClassSchedules = cache.getAllCachedClassSchedules(0).toMutableList()
            if (allClassSchedules.isEmpty()) {
                // Fetch a subset or base classes to compute if cache is empty
                val classes = getClasses().items.take(15)
                for (cls in classes) {
                    try {
                        val s = api.getClassSchedule(cls.name, 0)
                        cache.saveSchedule(PreferencesRepository.TYPE_CLASS, cls.name, 0, s)
                        allClassSchedules.add(s)
                    } catch (_: Exception) {}
                }
            }
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
            val allClassSchedules = cache.getAllCachedClassSchedules(0)
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
