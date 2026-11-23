package ru.nstu.lyceum.schedule.data.local

import com.google.gson.annotations.SerializedName
import java.util.UUID

enum class NoteType {
    @SerializedName("homework")
    HOMEWORK,
    @SerializedName("note")
    NOTE
}

enum class SlotType {
    @SerializedName("before_lessons")
    BEFORE_LESSONS,
    @SerializedName("lesson")
    LESSON,
    @SerializedName("after_lessons")
    AFTER_LESSONS,
    @SerializedName("day_note")
    DAY_NOTE
}

data class ScheduleNote(
    val id: String = UUID.randomUUID().toString(),
    val targetName: String,
    val type: NoteType = NoteType.HOMEWORK,
    val subject: String? = null,
    val subgroup: Int? = null, // null = all, 1, 2, 3
    val date: String, // "YYYY-MM-DD"
    val slotType: SlotType = SlotType.LESSON,
    val lessonNum: Int? = null,
    val text: String,
    val isCompleted: Boolean = false,
    val transferPending: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean? = null,
    val deletedAt: Long? = null
) {
    val isDeleted: Boolean
        get() = deleted == true || deletedAt != null
}
