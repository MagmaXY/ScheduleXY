package ru.nstu.lyceum.schedule.data.api

import com.google.gson.annotations.SerializedName

data class ClassItem(
    @SerializedName("name") val name: String
)

data class ClassesResponse(
    @SerializedName("items") val items: List<ClassItem> = emptyList()
)

data class TeacherItem(
    @SerializedName("name") val name: String
)

data class TeachersResponse(
    @SerializedName("items") val items: List<TeacherItem> = emptyList()
)

data class Subgroup(
    @SerializedName("label") val label: String? = "",
    @SerializedName("subject") val subject: String? = "",
    @SerializedName("teacher") val teacher: String? = "",
    @SerializedName("room") val room: String? = "",
    @SerializedName("className") val className: String? = ""
)

data class Lesson(
    @SerializedName("num") val num: Int,
    @SerializedName("time") val time: String,
    @SerializedName("subgroups") val subgroups: List<Subgroup> = emptyList()
) {
    val displaySubject: String
        get() = subgroups.firstOrNull { !it.subject.isNullOrBlank() }?.subject ?: "—"

    val displayRoom: String
        get() = subgroups.mapNotNull { it.room?.takeIf { r -> r.isNotBlank() } }.distinct().joinToString(", ")

    val displayTeacherOrClass: String
        get() {
            val teachers = subgroups.mapNotNull { it.teacher?.takeIf { t -> t.isNotBlank() } }.distinct()
            if (teachers.isNotEmpty()) return teachers.joinToString(", ")
            val classes = subgroups.mapNotNull { it.className?.takeIf { c -> c.isNotBlank() } }.distinct()
            if (classes.isNotEmpty()) return classes.joinToString(", ")
            return ""
        }
}

data class DaySchedule(
    @SerializedName("dayIndex") val dayIndex: Int,
    @SerializedName("dayName") val dayName: String,
    @SerializedName("lessons") val lessons: List<Lesson> = emptyList()
)

data class ScheduleResponse(
    @SerializedName("className") val className: String? = null,
    @SerializedName("classId") val classId: String? = null,
    @SerializedName("teacherName") val teacherName: String? = null,
    @SerializedName("teacherId") val teacherId: String? = null,
    @SerializedName("weekStart") val weekStart: String? = null,
    @SerializedName("periodStart") val periodStart: String? = null,
    @SerializedName("periodEnd") val periodEnd: String? = null,
    @SerializedName("exportDate") val exportDate: String? = null,
    @SerializedName("hasChanges") val hasChanges: Boolean = false,
    @SerializedName("changedMonths") val changedMonths: List<Int>? = emptyList(),
    @SerializedName("days") val days: List<DaySchedule> = emptyList()
) {
    val title: String
        get() = className ?: teacherName ?: "Расписание"

    val isTeacher: Boolean
        get() = !teacherName.isNullOrBlank()
}

data class RoomSlotAvailability(
    @SerializedName("busy") val busy: List<String> = emptyList(),
    @SerializedName("free") val free: List<String> = emptyList()
)

data class FreeRoomsResponse(
    @SerializedName("rooms") val rooms: Map<String, RoomSlotAvailability> = emptyMap(),
    @SerializedName("lessonTimes") val lessonTimes: Map<String, String> = emptyMap(),
    @SerializedName("updatedAt") val updatedAt: Long? = null
)

data class FreeTeachersResponse(
    @SerializedName("teachers") val teachers: Map<String, List<String>> = emptyMap(),
    @SerializedName("lessonTimes") val lessonTimes: Map<String, String> = emptyMap(),
    @SerializedName("totalTeachers") val totalTeachers: Int? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null
)

data class ArchiveResponse(
    @SerializedName("weeks") val weeks: List<String> = emptyList()
)

data class CalendarEvent(
    @SerializedName("date") val date: String,
    @SerializedName("ts") val ts: Long? = null,
    @SerializedName("title") val title: String,
    @SerializedName("color") val color: String? = "blue",
    @SerializedName("source") val source: String? = null,
    @SerializedName("sourceLabel") val sourceLabel: String? = null
)

data class EventCategory(
    @SerializedName("color") val color: String,
    @SerializedName("title") val title: String,
    @SerializedName("icon") val icon: String
)

data class EventsResponse(
    @SerializedName("items") val items: List<CalendarEvent> = emptyList(),
    @SerializedName("categories") val categories: List<EventCategory> = emptyList(),
    @SerializedName("updatedAt") val updatedAt: Long? = null
)

data class AdminCheckResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("required") val required: Boolean = true
)
