package ru.nstu.lyceum.schedule.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NotesRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val gson = Gson()
    private val notesFile = File(context.filesDir, "lyceum_notes.json")
    // In-memory list containing all records including soft-deleted ones (for sync)
    private var allInternalNotes: MutableList<ScheduleNote> = mutableListOf()
    // Flow exposed to UI (excludes deleted notes)
    private val _notes = MutableStateFlow<List<ScheduleNote>>(emptyList())
    val notes: StateFlow<List<ScheduleNote>> = _notes.asStateFlow()

    var onNotesChangedListener: (() -> Unit)? = null

    init {
        loadNotes()
    }

    private fun loadNotes() {
        scope.launch(Dispatchers.IO) {
            val list = try {
                if (notesFile.exists()) {
                    val json = notesFile.readText()
                    val type = object : TypeToken<List<ScheduleNote>>() {}.type
                    gson.fromJson<List<ScheduleNote>>(json, type) ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
            allInternalNotes = list.toMutableList()
            _notes.value = list.filter { !it.isDeleted }
        }
    }

    private fun saveNotes(newInternalList: List<ScheduleNote>) {
        allInternalNotes = newInternalList.toMutableList()
        _notes.value = newInternalList.filter { !it.isDeleted }
        scope.launch(Dispatchers.IO) {
            try {
                val json = gson.toJson(newInternalList)
                notesFile.writeText(json)
            } catch (_: Exception) {}
            onNotesChangedListener?.invoke()
        }
    }

    fun getAllNotesIncludingDeleted(): List<ScheduleNote> {
        return allInternalNotes.toList()
    }

    fun applyMergedNotes(merged: List<ScheduleNote>) {
        saveNotes(merged)
    }

    fun addNote(note: ScheduleNote) {
        val current = allInternalNotes.toMutableList()
        val now = System.currentTimeMillis()
        val updatedNote = note.copy(
            updatedAt = now,
            deleted = false,
            deletedAt = null
        )
        current.add(updatedNote)
        saveNotes(current)
    }

    fun updateNote(updated: ScheduleNote) {
        val current = allInternalNotes.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        val now = System.currentTimeMillis()
        val noteToSave = updated.copy(updatedAt = now, deleted = false, deletedAt = null)
        if (index >= 0) {
            current[index] = noteToSave
        } else {
            current.add(noteToSave)
        }
        saveNotes(current)
    }

    fun deleteNote(id: String) {
        val current = allInternalNotes.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        val now = System.currentTimeMillis()
        if (index >= 0) {
            current[index] = current[index].copy(deleted = true, deletedAt = now, updatedAt = now)
            saveNotes(current)
        }
    }

    fun toggleCompleted(id: String) {
        val current = allInternalNotes.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            val now = System.currentTimeMillis()
            current[index] = old.copy(isCompleted = !old.isCompleted, updatedAt = now)
            saveNotes(current)
        }
    }

    fun setCompleted(id: String, completed: Boolean) {
        val current = allInternalNotes.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            val now = System.currentTimeMillis()
            current[index] = old.copy(isCompleted = completed, updatedAt = now)
            saveNotes(current)
        }
    }

    fun transferHomework(id: String, targetDate: String, targetLessonNum: Int?) {
        val current = allInternalNotes.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            val now = System.currentTimeMillis()
            current[index] = old.copy(
                date = targetDate,
                lessonNum = targetLessonNum ?: old.lessonNum,
                isCompleted = false,
                transferPending = false,
                updatedAt = now
            )
            saveNotes(current)
        }
    }

    fun markTransferPending(id: String) {
        val current = allInternalNotes.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            val now = System.currentTimeMillis()
            current[index] = old.copy(
                transferPending = true,
                isCompleted = false,
                updatedAt = now
            )
            saveNotes(current)
        }
    }

    fun updateNotes(updatedNotes: List<ScheduleNote>) {
        if (updatedNotes.isEmpty()) return
        val current = allInternalNotes.toMutableList()
        var changed = false
        val now = System.currentTimeMillis()
        for (up in updatedNotes) {
            val idx = current.indexOfFirst { it.id == up.id }
            val noteWithTime = up.copy(updatedAt = now)
            if (idx >= 0 && current[idx] != noteWithTime) {
                current[idx] = noteWithTime
                changed = true
            }
        }
        if (changed) {
            saveNotes(current)
        }
    }

    fun getNotesForDay(targetName: String, date: String): List<ScheduleNote> {
        return _notes.value.filter { it.targetName == targetName && it.date == date }
    }

    fun getNotesForLesson(targetName: String, date: String, lessonNum: Int): List<ScheduleNote> {
        return _notes.value.filter {
            it.targetName == targetName && it.date == date &&
            it.slotType == SlotType.LESSON && it.lessonNum == lessonNum
        }
    }

    fun getBeforeLessonsNote(targetName: String, date: String): ScheduleNote? {
        return _notes.value.firstOrNull {
            it.targetName == targetName && it.date == date && it.slotType == SlotType.BEFORE_LESSONS
        }
    }

    fun getAfterLessonsNote(targetName: String, date: String): ScheduleNote? {
        return _notes.value.firstOrNull {
            it.targetName == targetName && it.date == date && it.slotType == SlotType.AFTER_LESSONS
        }
    }

    fun getDebts(targetName: String, todayIso: String): List<ScheduleNote> {
        return _notes.value.filter {
            it.targetName == targetName &&
            it.type == NoteType.HOMEWORK &&
            !it.isCompleted &&
            it.date < todayIso
        }.sortedBy { it.date }
    }

    fun cleanupOldCompleted(olderThanDays: Int = 30) {
        val threshold = System.currentTimeMillis() - (olderThanDays.toLong() * 24 * 60 * 60 * 1000L)
        val current = _notes.value.filter { note ->
            !(note.isCompleted && note.createdAt < threshold)
        }
        saveNotes(current)
    }

    fun exportNotesJson(): String {
        return gson.toJson(_notes.value)
    }

    fun importNotesJson(json: String): Int {
        return try {
            val type = object : TypeToken<List<ScheduleNote>>() {}.type
            val list = gson.fromJson<List<ScheduleNote>>(json, type) ?: return -1
            saveNotes(list)
            list.size
        } catch (_: Exception) {
            -1
        }
    }
}
