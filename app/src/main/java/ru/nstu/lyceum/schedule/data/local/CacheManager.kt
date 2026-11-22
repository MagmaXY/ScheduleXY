package ru.nstu.lyceum.schedule.data.local

import android.content.Context
import com.google.gson.Gson
import ru.nstu.lyceum.schedule.data.api.ArchiveResponse
import ru.nstu.lyceum.schedule.data.api.ClassesResponse
import ru.nstu.lyceum.schedule.data.api.EventsResponse
import ru.nstu.lyceum.schedule.data.api.ScheduleChanges
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.TeachersResponse
import java.io.File

class CacheManager(context: Context) {

    private val cacheDir = File(context.filesDir, "schedule_cache").apply { mkdirs() }
    private val gson = Gson()

    fun saveSchedule(type: String, name: String, week: Int, schedule: ScheduleResponse) {
        val file = File(cacheDir, "schedule_${type}_${safeFileName(name)}_week_$week.json")
        try {
            file.writeText(gson.toJson(schedule))
        } catch (_: Exception) {}
    }

    fun getSchedule(type: String, name: String, week: Int): ScheduleResponse? {
        val file = File(cacheDir, "schedule_${type}_${safeFileName(name)}_week_$week.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), ScheduleResponse::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun saveClasses(classes: ClassesResponse) {
        val file = File(cacheDir, "classes.json")
        try { file.writeText(gson.toJson(classes)) } catch (_: Exception) {}
    }

    fun getClasses(): ClassesResponse? {
        val file = File(cacheDir, "classes.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), ClassesResponse::class.java)
        } catch (_: Exception) { null }
    }

    fun saveTeachers(teachers: TeachersResponse) {
        val file = File(cacheDir, "teachers.json")
        try { file.writeText(gson.toJson(teachers)) } catch (_: Exception) {}
    }

    fun getTeachers(): TeachersResponse? {
        val file = File(cacheDir, "teachers.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), TeachersResponse::class.java)
        } catch (_: Exception) { null }
    }

    fun saveEvents(events: EventsResponse) {
        val file = File(cacheDir, "events.json")
        try { file.writeText(gson.toJson(events)) } catch (_: Exception) {}
    }

    fun getEvents(): EventsResponse? {
        val file = File(cacheDir, "events.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), EventsResponse::class.java)
        } catch (_: Exception) { null }
    }

    fun saveArchiveWeeks(archive: ArchiveResponse) {
        val file = File(cacheDir, "archive_weeks.json")
        try { file.writeText(gson.toJson(archive)) } catch (_: Exception) {}
    }

    fun getArchiveWeeks(): ArchiveResponse? {
        val file = File(cacheDir, "archive_weeks.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), ArchiveResponse::class.java)
        } catch (_: Exception) { null }
    }

    fun hasSchedule(type: String, name: String, week: Int = 0): Boolean {
        val file = File(cacheDir, "schedule_${type}_${safeFileName(name)}_week_$week.json")
        return file.exists()
    }

    /**
     * Silently updates the changes map and exportDate in all cached schedule files in the background.
     * This keeps the cache fresh for non-favorite classes without triggering push notifications.
     */
    fun updateCachedScheduleChanges(exportDate: String?, changes: ScheduleChanges?) {
        if (changes == null && exportDate == null) return
        try {
            val files = cacheDir.listFiles { _, name -> name.startsWith("schedule_") && name.endsWith("_week_0.json") }
            files?.forEach { file ->
                try {
                    val sched = gson.fromJson(file.readText(), ScheduleResponse::class.java) ?: return@forEach
                    val updated = sched.copy(
                        exportDate = exportDate ?: sched.exportDate,
                        changes = changes ?: sched.changes,
                        hasChanges = (changes?.classChanges?.isNotEmpty() == true || changes?.teacherChanges?.isNotEmpty() == true)
                    )
                    file.writeText(gson.toJson(updated))
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun getAllCachedClassSchedules(week: Int = 0): List<ScheduleResponse> {
        val list = mutableListOf<ScheduleResponse>()
        val files = cacheDir.listFiles { _, name -> name.startsWith("schedule_class_") && name.endsWith("_week_$week.json") }
        files?.forEach { file ->
            try {
                val sched = gson.fromJson(file.readText(), ScheduleResponse::class.java)
                if (sched != null) list.add(sched)
            } catch (_: Exception) {}
        }
        return list
    }

    fun clearScheduleAndEventsCache() {
        try {
            // Keep classes.json and teachers.json so UI never waits, clear dynamic schedules and events
            cacheDir.listFiles { _, name -> name.startsWith("schedule_") || name == "events.json" }?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    private fun safeFileName(name: String): String {
        return name.replace("/", "_").replace("\\", "_").replace(" ", "_")
    }
}
