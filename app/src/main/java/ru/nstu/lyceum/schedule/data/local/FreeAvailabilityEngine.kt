package ru.nstu.lyceum.schedule.data.local

import ru.nstu.lyceum.schedule.data.api.FreeRoomsResponse
import ru.nstu.lyceum.schedule.data.api.FreeTeachersResponse
import ru.nstu.lyceum.schedule.data.api.RoomSlotAvailability
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.TeacherItem

object FreeAvailabilityEngine {

    // Master inventory of all lyceum rooms across floors and wings
    val ALL_KNOWN_ROOMS = listOf(
        "101", "103", "105", "105(Н)", "108", "109", "110", "113", "116", "128",
        "201", "202", "203", "204", "205", "206", "207", "208", "209", "210", "211", "212", "213", "214", "226", "230", "232",
        "301", "302", "303", "304", "305", "306", "307", "308", "309", "310", "311", "312", "313", "314",
        "1к404", "2к502", "Акт. зал", "Спортзал", "Малый зал"
    )

    data class SlotOccupancy(
        val dayIndex: Int,
        val dayName: String,
        val lessonNum: Int,
        val time: String,
        val freeRooms: List<String>,
        val busyRooms: Map<String, String>, // room -> occupancy info (e.g. "10-1: Физика")
        val freeTeachers: List<String>,
        val busyTeachers: Map<String, String> // teacher -> occupancy info (e.g. "10-1: Физика")
    )

    /**
     * Computes free and busy rooms & teachers locally from available class schedules.
     */
    fun computeSlotAvailability(
        allClassSchedules: List<ScheduleResponse>,
        allTeachers: List<TeacherItem>,
        dayIndex: Int,
        lessonNum: Int,
        dayName: String = getDayName(dayIndex),
        lessonTime: String = getLessonTime(lessonNum)
    ): SlotOccupancy {
        val busyRooms = mutableMapOf<String, String>()
        val busyTeachers = mutableMapOf<String, String>()

        // Discover any custom rooms from schedules
        val discoveredRooms = mutableSetOf<String>().apply { addAll(ALL_KNOWN_ROOMS) }

        for (schedule in allClassSchedules) {
            val className = schedule.className ?: continue
            val day = schedule.days.firstOrNull { it.dayIndex == dayIndex } ?: continue
            val lesson = day.lessons.firstOrNull { it.num == lessonNum } ?: continue

            // Check if there is an active change for this class, day and lesson
            val change = schedule.getChangeForLesson(dayIndex, lessonNum)
            if (change != null) {
                if (change.isCancelledLesson) {
                    // Cancelled lesson - class is free, no room or teacher occupied
                    continue
                }
                val repSubj = change.subject?.takeIf { it.isNotBlank() }?.let { ru.nstu.lyceum.schedule.data.api.normalizeSubject(it) } ?: "Занятие"
                val repRoom = change.room?.trim()
                val repTeacher = change.teacher?.trim()

                if (!repRoom.isNullOrBlank()) {
                    discoveredRooms.add(repRoom)
                    val info = "$className: $repSubj (замена)"
                    busyRooms[repRoom] = if (busyRooms.containsKey(repRoom)) "${busyRooms[repRoom]}, $info" else info
                }
                if (!repTeacher.isNullOrBlank()) {
                    val info = "$className: $repSubj ${if (!repRoom.isNullOrBlank()) "($repRoom)" else ""} (замена)".trim()
                    busyTeachers[repTeacher] = if (busyTeachers.containsKey(repTeacher)) "${busyTeachers[repTeacher]}, $info" else info
                }
                continue
            }

            for (subgroup in lesson.subgroups) {
                val subject = subgroup.subject?.let { ru.nstu.lyceum.schedule.data.api.normalizeSubject(it) } ?: "Занятие"
                val room = subgroup.room?.trim()
                if (!room.isNullOrBlank()) {
                    discoveredRooms.add(room)
                    val info = "$className: $subject"
                    busyRooms[room] = if (busyRooms.containsKey(room)) "${busyRooms[room]}, $info" else info
                }

                val teacher = subgroup.teacher?.trim()
                if (!teacher.isNullOrBlank()) {
                    val info = "$className: $subject ${if (!room.isNullOrBlank()) "($room)" else ""}".trim()
                    busyTeachers[teacher] = if (busyTeachers.containsKey(teacher)) "${busyTeachers[teacher]}, $info" else info
                }
            }
        }

        val freeRooms = discoveredRooms.filter { it !in busyRooms.keys }.sorted()
        val allTeacherNames = allTeachers.map { it.name }.distinct()
        val freeTeachers = allTeacherNames.filter { it !in busyTeachers.keys }.sorted()

        return SlotOccupancy(
            dayIndex = dayIndex,
            dayName = dayName,
            lessonNum = lessonNum,
            time = lessonTime,
            freeRooms = freeRooms,
            busyRooms = busyRooms,
            freeTeachers = freeTeachers,
            busyTeachers = busyTeachers
        )
    }

    /**
     * Converts locally computed data to API-compatible format
     */
    fun generateFullLocalFreeRooms(
        allClassSchedules: List<ScheduleResponse>
    ): FreeRoomsResponse {
        val roomsMap = mutableMapOf<String, RoomSlotAvailability>()
        val days = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
        val lessonTimes = mutableMapOf<String, String>()

        for (num in 1..12) {
            lessonTimes[num.toString()] = getLessonTime(num)
        }

        for (dIdx in 0..5) {
            val dName = days[dIdx]
            for (num in 1..12) {
                val slot = computeSlotAvailability(allClassSchedules, emptyList(), dIdx, num)
                val key = "$dName|$num"
                roomsMap[key] = RoomSlotAvailability(
                    busy = slot.busyRooms.keys.toList().sorted(),
                    free = slot.freeRooms
                )
            }
        }

        return FreeRoomsResponse(
            rooms = roomsMap,
            lessonTimes = lessonTimes,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Converts locally computed data to API-compatible format
     */
    fun generateFullLocalFreeTeachers(
        allClassSchedules: List<ScheduleResponse>,
        allTeachers: List<TeacherItem>
    ): FreeTeachersResponse {
        val teachersMap = mutableMapOf<String, List<String>>()
        val days = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
        val lessonTimes = mutableMapOf<String, String>()

        for (num in 1..12) {
            lessonTimes[num.toString()] = getLessonTime(num)
        }

        for (dIdx in 0..5) {
            val dName = days[dIdx]
            for (num in 1..12) {
                val slot = computeSlotAvailability(allClassSchedules, allTeachers, dIdx, num)
                val key = "$dName|$num"
                teachersMap[key] = slot.freeTeachers
            }
        }

        return FreeTeachersResponse(
            teachers = teachersMap,
            lessonTimes = lessonTimes,
            totalTeachers = allTeachers.size,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun getDayName(dayIndex: Int): String {
        return when (dayIndex) {
            0 -> "Понедельник"
            1 -> "Вторник"
            2 -> "Среда"
            3 -> "Четверг"
            4 -> "Пятница"
            5 -> "Суббота"
            else -> "Воскресенье"
        }
    }

    fun getLessonTime(lessonNum: Int): String {
        return when (lessonNum) {
            1 -> "8:15–9:00"
            2 -> "9:10–9:55"
            3 -> "10:15–11:00"
            4 -> "11:15–12:00"
            5 -> "12:10–12:55"
            6 -> "13:05–13:50"
            7 -> "14:00–14:40"
            8 -> "14:55–15:35"
            9 -> "15:50–16:30"
            10 -> "16:40–17:20"
            11 -> "17:30–18:10"
            12 -> "18:20–19:00"
            else -> ""
        }
    }
}
