package ru.nstu.lyceum.schedule.domain

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class RealtimeSyncManager(
    private val scope: CoroutineScope,
    private val context: Context? = null
) {

    data class RealtimeState(
        val dayIndex: Int,
        val bellState: BellState
    ) {
        val timeString: String get() = ""
        val dateString: String get() = ""
        val shortDate: String get() = ""
        val timestamp: Long get() = 0L
    }

    private var syncJob: Job? = null
    private val _state = MutableStateFlow(computeCurrentState())
    val state: StateFlow<RealtimeState> = _state.asStateFlow()

    init {
        startSyncLoop()
    }

    fun computeCurrentState(): RealtimeState {
        val cal = Calendar.getInstance()
        val isVacation = context?.let { VacationChecker.isVacation(it, cal) } ?: false
        val prefs = context?.let { ru.nstu.lyceum.schedule.data.repository.PreferencesRepository(it) }
        val isPairsMode = prefs?.statusBarPairsMode == true
        val bell = if (isPairsMode) {
            PairSchedule.getCurrentPairState(cal, isVacation)
        } else {
            BellSchedule.getCurrentBellState(cal, isVacation)
        }
        val dayIdx = BellSchedule.getScheduleDayIndex(cal)

        return RealtimeState(
            dayIndex = dayIdx,
            bellState = bell
        )
    }

    fun forceRefresh() {
        _state.value = computeCurrentState()
    }

    fun startSyncLoop() {
        if (syncJob?.isActive == true) return
        _state.value = computeCurrentState()
        syncJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(15000) // update every 15 seconds (energy efficient, no battery drain)
                _state.value = computeCurrentState()
            }
        }
    }

    fun stopSyncLoop() {
        syncJob?.cancel()
        syncJob = null
    }
}
