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

fun normalizeSubject(name: String?): String {
    if (name.isNullOrBlank()) return ""
    val trimmed = name.trim()
    val subjectStr = if (trimmed.equals("М", ignoreCase = true) ||
        trimmed.equals("М.", ignoreCase = true) ||
        trimmed.equals("M", ignoreCase = true) ||
        trimmed.equals("M.", ignoreCase = true)
    ) {
        "Методический час"
    } else {
        trimmed
    }
    return subjectStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("ru")) else it.toString() }
}

data class Lesson(
    @SerializedName("num") val num: Int,
    @SerializedName("time") val time: String,
    @SerializedName("subgroups") val subgroups: List<Subgroup> = emptyList()
) {
    val displaySubject: String
        get() {
            val raw = subgroups.firstOrNull { !it.subject.isNullOrBlank() }?.subject ?: "—"
            return if (raw == "—") raw else normalizeSubject(raw)
        }

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

data class ScheduleChangeItem(
    @SerializedName("dateStr") val dateStr: String? = null,
    @SerializedName("lessonNum") val lessonNum: Int? = null,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("teacher") val teacher: String? = null,
    @SerializedName("room") val room: String? = null,
    @SerializedName("className") val className: String? = null,
    @SerializedName("group") val group: String? = null,
    @SerializedName("subgroup") val subgroup: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("isCancelled") val isCancelled: Boolean = false,
    @SerializedName("hasChange") val hasChange: Boolean = false
) {
    val isCancelledLesson: Boolean
        get() = isCancelled || subject == "F" || subject == "$" || subject?.startsWith("$") == true || subject?.equals("Урок отменен", ignoreCase = true) == true
}

data class ScheduleChanges(
    @SerializedName("class") val classChanges: Map<String, ScheduleChangeItem>? = emptyMap(),
    @SerializedName("teacher") val teacherChanges: Map<String, ScheduleChangeItem>? = emptyMap()
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
    @SerializedName("changes") val changes: ScheduleChanges? = null,
    @SerializedName("hasChanges") val hasChanges: Boolean = false,
    @SerializedName("changedMonths") val changedMonths: List<Int>? = emptyList(),
    @SerializedName("days") val days: List<DaySchedule> = emptyList()
) {
    val title: String
        get() = className ?: teacherName ?: "Расписание"

    val isTeacher: Boolean
        get() = !teacherName.isNullOrBlank()

    fun getChangesForLesson(dayIndex: Int, lessonNum: Int): List<ScheduleChangeItem> {
        val target = className ?: teacherName ?: return emptyList()
        val ws = weekStart?.let {
            try { java.time.LocalDate.parse(it) } catch (_: Exception) { null }
        } ?: return emptyList()
        val lessonDate = ws.plusDays(dayIndex.toLong())
        val dateStr = String.format(java.util.Locale.US, "%02d.%02d.%04d", lessonDate.dayOfMonth, lessonDate.monthValue, lessonDate.year)
        val normalizedTarget = target.trim().replace("\\s+".toRegex(), " ")

        val map = if (isTeacher) changes?.teacherChanges else changes?.classChanges
        if (map.isNullOrEmpty()) return emptyList()

        val results = mutableListOf<ScheduleChangeItem>()
        for ((key, item) in map) {
            val parts = key.split("|")
            val keyTarget = parts.getOrNull(0)?.trim()?.replace("\\s+".toRegex(), " ")
            val keyDate = parts.getOrNull(1)?.trim()
            val keyNum = parts.getOrNull(2)?.trim()?.toIntOrNull()
            val keyGrp = parts.getOrNull(3)?.trim()

            val matchesTarget = keyTarget?.equals(normalizedTarget, ignoreCase = true) == true ||
                item.className?.trim()?.equals(normalizedTarget, ignoreCase = true) == true ||
                item.teacher?.trim()?.equals(normalizedTarget, ignoreCase = true) == true

            val matchesDate = keyDate == dateStr || item.dateStr == dateStr
            val matchesNum = keyNum == lessonNum || item.lessonNum == lessonNum

            if (matchesTarget && matchesDate && matchesNum) {
                val enriched = if (!keyGrp.isNullOrBlank() && item.group.isNullOrBlank()) {
                    item.copy(group = keyGrp)
                } else item
                results.add(enriched)
            }
        }
        return results
    }

    fun getChangesForDateAndLesson(dateIso: String, lessonNum: Int): List<ScheduleChangeItem> {
        val target = className ?: teacherName ?: return emptyList()
        val parts = dateIso.split("-")
        val dateStr = if (parts.size == 3) {
            try {
                String.format(java.util.Locale.US, "%02d.%02d.%04d", parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            } catch (_: Exception) { "" }
        } else ""

        val normalizedTarget = target.trim().replace("\\s+".toRegex(), " ")
        val map = if (isTeacher) changes?.teacherChanges else changes?.classChanges
        if (map.isNullOrEmpty()) return emptyList()

        val results = mutableListOf<ScheduleChangeItem>()
        for ((key, item) in map) {
            val kParts = key.split("|")
            val keyTarget = kParts.getOrNull(0)?.trim()?.replace("\\s+".toRegex(), " ")
            val keyDate = kParts.getOrNull(1)?.trim()
            val keyNum = kParts.getOrNull(2)?.trim()?.toIntOrNull()
            val keyGrp = kParts.getOrNull(3)?.trim()

            val matchesTarget = keyTarget?.equals(normalizedTarget, ignoreCase = true) == true ||
                item.className?.trim()?.equals(normalizedTarget, ignoreCase = true) == true ||
                item.teacher?.trim()?.equals(normalizedTarget, ignoreCase = true) == true

            val matchesDate = (dateStr.isNotEmpty() && keyDate == dateStr) || (dateStr.isNotEmpty() && item.dateStr == dateStr)
            val matchesNum = keyNum == lessonNum || item.lessonNum == lessonNum

            if (matchesTarget && matchesDate && matchesNum) {
                val enriched = if (!keyGrp.isNullOrBlank() && item.group.isNullOrBlank()) {
                    item.copy(group = keyGrp)
                } else item
                results.add(enriched)
            }
        }
        return results
    }

    fun getChangeForLesson(dayIndex: Int, lessonNum: Int): ScheduleChangeItem? {
        return getChangesForLesson(dayIndex, lessonNum).firstOrNull()
    }
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
