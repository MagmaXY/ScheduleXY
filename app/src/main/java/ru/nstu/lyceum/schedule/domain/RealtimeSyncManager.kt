package ru.nstu.lyceum.schedule.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RealtimeSyncManager(private val scope: CoroutineScope) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("ru"))
    private val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ru"))
    private val shortDateFormat = SimpleDateFormat("d MMMM", Locale("ru"))

    data class RealtimeState(
        val timeString: String,
        val dateString: String,
        val shortDate: String,
        val dayIndex: Int,
        val bellState: BellState,
        val timestamp: Long
    )

    private val _state = MutableStateFlow(computeCurrentState())
    val state: StateFlow<RealtimeState> = _state.asStateFlow()

    init {
        startSyncLoop()
    }

    private fun computeCurrentState(): RealtimeState {
        val cal = Calendar.getInstance()
        val bell = BellSchedule.getCurrentBellState(cal)
        val dayIdx = BellSchedule.getScheduleDayIndex(cal)

        val dateCap = dateFormat.format(cal.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString()
        }

        return RealtimeState(
            timeString = timeFormat.format(cal.time),
            dateString = dateCap,
            shortDate = shortDateFormat.format(cal.time),
            dayIndex = dayIdx,
            bellState = bell,
            timestamp = cal.timeInMillis
        )
    }

    private fun startSyncLoop() {
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                _state.value = computeCurrentState()
                delay(1000) // update every second for live countdown
            }
        }
    }
}
