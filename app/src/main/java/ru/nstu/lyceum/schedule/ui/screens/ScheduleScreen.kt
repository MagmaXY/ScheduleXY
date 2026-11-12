package ru.nstu.lyceum.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import ru.nstu.lyceum.schedule.domain.BellState
import ru.nstu.lyceum.schedule.domain.RealtimeSyncManager
import ru.nstu.lyceum.schedule.ui.components.DaySelector
import ru.nstu.lyceum.schedule.ui.components.LessonCard
import ru.nstu.lyceum.schedule.ui.theme.NikaBorder
import ru.nstu.lyceum.schedule.ui.theme.NikaCalExists
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject
import ru.nstu.lyceum.schedule.ui.theme.NikaTeacher

@Composable
fun ScheduleScreen(
    currentSchedule: ScheduleResponse?,
    classesList: List<ClassItem>,
    teachersList: List<TeacherItem>,
    targetType: String,
    targetName: String,
    isFavorite: Boolean,
    selectedDayIndex: Int,
    realtimeState: RealtimeSyncManager.RealtimeState,
    isLoading: Boolean,
    onTargetTypeChanged: (String) -> Unit,
    onTargetNameChanged: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDaySelected: (Int) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }

    val isTeacherMode = targetType == PreferencesRepository.TYPE_TEACHER

    Column(modifier = Modifier.fillMaxSize()) {

        // Mode switchers: КЛАССЫ / УЧИТЕЛЯ & Target Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8F1FA))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type toggle buttons
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(1.dp, NikaBorder, RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onTargetTypeChanged(PreferencesRepository.TYPE_CLASS) }
                        .background(if (!isTeacherMode) NikaSubject else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Классы",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isTeacherMode) Color.White else NikaSubject
                    )
                }

                Box(
                    modifier = Modifier
                        .clickable { onTargetTypeChanged(PreferencesRepository.TYPE_TEACHER) }
                        .background(if (isTeacherMode) NikaTeacher else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Учителя",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTeacherMode) Color.White else NikaTeacher
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Current Target Selector Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(1.dp, NikaBorder, RoundedCornerShape(6.dp))
                    .clickable { dropdownExpanded = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = targetName.ifEmpty { if (isTeacherMode) "Выберите учителя" else "Выберите класс" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Выбрать",
                        tint = NikaSubject
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = {
                        dropdownExpanded = false
                        filterQuery = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(360.dp)
                ) {
                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        placeholder = { Text("Поиск...") },
                        singleLine = true
                    )

                    val items = if (isTeacherMode) {
                        teachersList.map { it.name }.filter { it.contains(filterQuery, ignoreCase = true) }
                    } else {
                        classesList.map { it.name }.filter { it.contains(filterQuery, ignoreCase = true) }
                    }

                    items.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name, fontWeight = if (name == targetName) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                onTargetNameChanged(name)
                                dropdownExpanded = false
                                filterQuery = ""
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Favorite Button ("Это мой класс" / "В избранное")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isFavorite) Color(0xFFFFF9C4) else Color.White)
                    .border(1.dp, if (isFavorite) Color(0xFFFBC02D) else NikaBorder, RoundedCornerShape(6.dp))
                    .clickable { onToggleFavorite() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Избранное",
                    tint = if (isFavorite) Color(0xFFF57F17) else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Day Selector Bar (< Пн..Сб >)
        DaySelector(
            selectedDayIndex = selectedDayIndex,
            currentDayIndex = realtimeState.dayIndex,
            onDaySelected = onDaySelected
        )

        // Lessons List Content
        if (isLoading && currentSchedule == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NikaSubject)
            }
        } else {
            val daySchedule = currentSchedule?.days?.firstOrNull { it.dayIndex == selectedDayIndex }
            val lessons = daySchedule?.lessons ?: emptyList()

            if (lessons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "В этот день занятий нет",
                        fontSize = 15.sp,
                        color = Color(0xFF718096),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val isToday = selectedDayIndex == realtimeState.dayIndex
                val activeLsnNum = (realtimeState.bellState as? BellState.InLesson)?.num

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    items(lessons) { lesson ->
                        val isActive = isToday && activeLsnNum == lesson.num
                        LessonCard(
                            lesson = lesson,
                            isActiveNow = isActive,
                            isTeacherMode = isTeacherMode
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
