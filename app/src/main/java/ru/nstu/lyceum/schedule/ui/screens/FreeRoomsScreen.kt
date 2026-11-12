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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
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
import ru.nstu.lyceum.schedule.data.local.FreeAvailabilityEngine
import ru.nstu.lyceum.schedule.domain.BellState
import ru.nstu.lyceum.schedule.domain.RealtimeSyncManager
import ru.nstu.lyceum.schedule.ui.theme.NikaBorder
import ru.nstu.lyceum.schedule.ui.theme.NikaLsnTime
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject
import ru.nstu.lyceum.schedule.ui.theme.NikaTeacher

@Composable
fun FreeRoomsScreen(
    slotOccupancy: FreeAvailabilityEngine.SlotOccupancy?,
    selectedDayIndex: Int,
    selectedLessonNum: Int,
    realtimeState: RealtimeSyncManager.RealtimeState,
    adminPassword: String,
    isLoading: Boolean,
    onDaySelected: (Int) -> Unit,
    onLessonNumSelected: (Int) -> Unit,
    onAdminPasswordChanged: (String) -> Unit
) {
    var mode by remember { mutableStateOf("rooms") } // "rooms" or "teachers"
    var teacherSearch by remember { mutableStateOf("") }
    val dayNames = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")

    Column(modifier = Modifier.fillMaxSize()) {

        // Mode Switcher: Кабинеты / Учителя
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8F1FA))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(1.dp, NikaBorder, RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .clickable { mode = "rooms" }
                        .background(if (mode == "rooms") NikaSubject else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Свободные кабинеты",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (mode == "rooms") Color.White else NikaSubject
                    )
                }

                Box(
                    modifier = Modifier
                        .clickable { mode = "teachers" }
                        .background(if (mode == "teachers") NikaTeacher else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Свободные учителя",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (mode == "teachers") Color.White else NikaTeacher
                    )
                }
            }
        }

        // Slot Picker: Days (Пн..Сб)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб").forEachIndexed { idx, shortName ->
                val isSelected = idx == selectedDayIndex
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NikaSubject else Color(0xFFF1F5F9))
                        .border(1.dp, if (isSelected) NikaSubject else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                        .clickable { onDaySelected(idx) }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = shortName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF334155)
                    )
                }
            }
        }

        // Lesson slot selector (1..8 урок)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val activeNum = (realtimeState.bellState as? BellState.InLesson)?.num

            for (num in 1..8) {
                val isSelected = num == selectedLessonNum
                val isRealtimeActive = selectedDayIndex == realtimeState.dayIndex && num == activeNum

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isSelected -> NikaLsnTime
                                isRealtimeActive -> Color(0xFFDCFCE7)
                                else -> Color.White
                            }
                        )
                        .border(
                            1.dp,
                            if (isSelected) NikaLsnTime else Color(0xFFCBD5E1),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onLessonNumSelected(num) }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$num ур.",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || isRealtimeActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF1E293B)
                    )
                }
            }
        }

        // Slot Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2EBF4))
                .border(1.dp, NikaBorder)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dayName = dayNames.getOrElse(selectedDayIndex) { "Понедельник" }
            val time = FreeAvailabilityEngine.getLessonTime(selectedLessonNum)

            Text(
                text = "$dayName, $selectedLessonNum урок ($time)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NikaSubject
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Пароль",
                    tint = Color(0xFF15803D),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Пароль: $adminPassword",
                    fontSize = 10.sp,
                    color = Color(0xFF15803D)
                )
            }
        }

        if (isLoading || slotOccupancy == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NikaSubject)
            }
        } else {
            if (mode == "rooms") {
                // Free Rooms List
                val freeRooms = slotOccupancy.freeRooms
                val busyRooms = slotOccupancy.busyRooms

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Свободно кабинетов: ${freeRooms.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NikaLsnTime
                            )
                            Text(
                                text = "Занято: ${busyRooms.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }

                    // Free Rooms Badges Grid
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Доступные свободные кабинеты:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (freeRooms.isEmpty()) "Все кабинеты заняты" else freeRooms.joinToString("  •  "),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF14532D),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Занятые кабинеты и уроки:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(busyRooms.entries.toList().sortedBy { it.key }) { (room, occupancy) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFEE2E2), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = room,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = occupancy,
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                maxLines = 1
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            } else {
                // Free Teachers List
                val freeTeachers = slotOccupancy.freeTeachers.filter {
                    it.contains(teacherSearch, ignoreCase = true)
                }

                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    OutlinedTextField(
                        value = teacherSearch,
                        onValueChange = { teacherSearch = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        placeholder = { Text("Поиск свободного учителя...") },
                        singleLine = true
                    )

                    Text(
                        text = "Свободно учителей в этот слот: ${slotOccupancy.freeTeachers.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NikaTeacher,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(freeTeachers) { teacher ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Свободен",
                                    tint = NikaLsnTime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = teacher,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }
}
