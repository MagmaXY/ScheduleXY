package ru.nstu.lyceum.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.ClassItem
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.data.api.TeacherItem
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.ui.components.DaySelector
import ru.nstu.lyceum.schedule.ui.components.LessonCard
import ru.nstu.lyceum.schedule.ui.theme.NikaBorder
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject

@Composable
fun ArchiveScreen(
    archiveSchedule: ScheduleResponse?,
    classesList: List<ClassItem>,
    teachersList: List<TeacherItem>,
    targetType: String,
    targetName: String,
    selectedWeek: Int, // 0, -1, -2
    selectedDayIndex: Int,
    isLoading: Boolean,
    onWeekChanged: (Int) -> Unit,
    onTargetNameChanged: (String) -> Unit,
    onDaySelected: (Int) -> Unit
) {
    var weekDropdownExpanded by remember { mutableStateOf(false) }

    val weekOptions = listOf(
        Pair(0, "Текущая неделя (0)"),
        Pair(-1, "1 неделя назад (-1)"),
        Pair(-2, "2 недели назад (-2)")
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // Archive Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF7ED))
                .border(1.dp, Color(0xFFFDBA74))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Архив",
                tint = Color(0xFFEA580C)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Архив расписания",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9A3412)
                )

                val periodText = if (archiveSchedule?.periodStart != null && archiveSchedule.periodEnd != null) {
                    "${archiveSchedule.periodStart} – ${archiveSchedule.periodEnd}"
                } else {
                    weekOptions.firstOrNull { it.first == selectedWeek }?.second ?: "Неделя $selectedWeek"
                }

                Text(
                    text = "Период: $periodText",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC2410C)
                )
            }

            // Dropdown to pick -1 or -2 weeks
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(6.dp))
                    .clickable { weekDropdownExpanded = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (selectedWeek == 0) "Текущая" else "$selectedWeek нед.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Выбрать неделю",
                        tint = Color(0xFFEA580C)
                    )
                }

                DropdownMenu(
                    expanded = weekDropdownExpanded,
                    onDismissRequest = { weekDropdownExpanded = false }
                ) {
                    weekOptions.forEach { (weekVal, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontWeight = if (weekVal == selectedWeek) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                onWeekChanged(weekVal)
                                weekDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Day Selector for the Archive Week
        DaySelector(
            selectedDayIndex = selectedDayIndex,
            currentDayIndex = -1, // No "today" badge in archive
            onDaySelected = onDaySelected
        )

        // Archive Lessons List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NikaSubject)
            }
        } else {
            val daySchedule = archiveSchedule?.days?.firstOrNull { it.dayIndex == selectedDayIndex }
            val lessons = daySchedule?.lessons ?: emptyList()

            if (lessons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "В архиве на этот день уроков не найдено",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    items(lessons) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            isActiveNow = false,
                            isTeacherMode = targetType == PreferencesRepository.TYPE_TEACHER
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}
