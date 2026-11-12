package ru.nstu.lyceum.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.CalendarEvent
import ru.nstu.lyceum.schedule.data.api.EventCategory
import ru.nstu.lyceum.schedule.data.api.ScheduleResponse
import ru.nstu.lyceum.schedule.ui.theme.EventBlue
import ru.nstu.lyceum.schedule.ui.theme.EventCyan
import ru.nstu.lyceum.schedule.ui.theme.EventGreen
import ru.nstu.lyceum.schedule.ui.theme.EventOrange
import ru.nstu.lyceum.schedule.ui.theme.EventPurple
import ru.nstu.lyceum.schedule.ui.theme.EventRed
import ru.nstu.lyceum.schedule.ui.theme.EventYellow
import ru.nstu.lyceum.schedule.ui.theme.NikaBorder
import ru.nstu.lyceum.schedule.ui.theme.NikaCalChange
import ru.nstu.lyceum.schedule.ui.theme.NikaCalExists
import ru.nstu.lyceum.schedule.ui.theme.NikaCalTodayBorder
import ru.nstu.lyceum.schedule.ui.theme.NikaSaturday
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject
import ru.nstu.lyceum.schedule.ui.theme.NikaSunday
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    currentSchedule: ScheduleResponse?,
    events: List<CalendarEvent>,
    categories: List<EventCategory>,
    onDayClick: (Int) -> Unit
) {
    var calendarMonthOffset by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedDateString by remember { mutableStateOf<String?>(null) }

    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    val todayMonth = cal.get(Calendar.MONTH)
    val todayYear = cal.get(Calendar.YEAR)

    cal.add(Calendar.MONTH, calendarMonthOffset)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val currentMonthName = SimpleDateFormat("LLLL yyyy", Locale("ru")).format(cal.time)
        .replaceFirstChar { it.titlecase(Locale("ru")) }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // 1st day of month day of week (Monday = 1, ..., Sunday = 7 in Russian calendar)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0 = Mon, 6 = Sun
    val displayedMonth = cal.get(Calendar.MONTH)
    val displayedYear = cal.get(Calendar.YEAR)

    Column(modifier = Modifier.fillMaxSize()) {

        // Month Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2EBF4))
                .border(1.dp, NikaBorder)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { calendarMonthOffset-- },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Предыдущий месяц",
                    tint = NikaSubject
                )
            }

            Text(
                text = currentMonthName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NikaSubject
            )

            IconButton(
                onClick = { calendarMonthOffset++ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Следующий месяц",
                    tint = NikaSubject
                )
            }
        }

        // Calendar Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(6.dp)
        ) {
            // Days of week header
            val weekHeaders = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekHeaders.forEachIndexed { i, h ->
                    Text(
                        text = h,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (i) {
                            5 -> NikaSaturday
                            6 -> NikaSunday
                            else -> Color(0xFF475569)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar cells: 6 rows of 7 days
            val totalCells = 42
            for (row in 0 until 6) {
                val rowStart = row * 7
                if (rowStart >= firstDayOfWeek + daysInMonth) break

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOfWeek + 1

                        if (dayNumber in 1..daysInMonth) {
                            val isToday = dayNumber == todayDay && displayedMonth == todayMonth && displayedYear == todayYear
                            val isSaturday = col == 5
                            val isSunday = col == 6

                            // Date string format YYYY-MM-DD
                            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", displayedYear, displayedMonth + 1, dayNumber)
                            val hasEvents = events.any { it.date == dateStr }
                            val isSelected = selectedDateString == dateStr

                            // Check if changes exist in schedule for this day
                            val hasChanges = currentSchedule?.hasChanges == true

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .padding(1.5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            isSelected -> Color(0xFFBFDBFE)
                                            isSunday -> Color(0xFFFEE2E2)
                                            isSaturday -> Color(0xFFDCFCE7)
                                            hasChanges -> NikaCalChange
                                            else -> NikaCalExists
                                        }
                                    )
                                    .border(
                                        width = if (isToday) 2.dp else 0.5.dp,
                                        color = if (isToday) NikaCalTodayBorder else Color(0xFFAACCFF),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        selectedDateString = if (selectedDateString == dateStr) null else dateStr
                                        if (col in 0..5) {
                                            onDayClick(col) // Jump to schedule day
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isSunday -> NikaSunday
                                            isSaturday -> NikaSaturday
                                            else -> Color(0xFF1E293B)
                                        }
                                    )

                                    if (hasEvents) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(NikaSunday)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1.2f))
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Все события (${events.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NikaSubject,
                    selectedLabelColor = Color.White
                )
            )

            categories.forEach { cat ->
                val isSelected = selectedCategory == cat.color
                val count = events.count { it.color == cat.color }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = if (isSelected) null else cat.color },
                    label = { Text("${cat.icon} ${cat.title} ($count)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getCategoryColor(cat.color),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Events List
        val filteredEvents = events.filter { event ->
            (selectedCategory == null || event.color == selectedCategory) &&
            (selectedDateString == null || event.date == selectedDateString)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            item {
                Text(
                    text = if (selectedDateString != null) "События на $selectedDateString" else "События лицея",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NikaSubject,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (filteredEvents.isEmpty()) {
                item {
                    Text(
                        text = "В выбранной категории событий не найдено",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(filteredEvents) { ev ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(ev.color))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ev.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "${ev.date} ${if (!ev.sourceLabel.isNullOrBlank()) "• ${ev.sourceLabel}" else ""}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

fun getCategoryColor(colorName: String?): Color {
    return when (colorName?.lowercase()) {
        "blue" -> EventBlue
        "green" -> EventGreen
        "red" -> EventRed
        "orange" -> EventOrange
        "purple" -> EventPurple
        "yellow" -> EventYellow
        "cyan" -> EventCyan
        else -> EventBlue
    }
}
