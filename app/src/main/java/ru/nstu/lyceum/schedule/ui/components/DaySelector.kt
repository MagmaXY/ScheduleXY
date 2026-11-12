package ru.nstu.lyceum.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.ui.theme.NikaBorder
import ru.nstu.lyceum.schedule.ui.theme.NikaCalExists
import ru.nstu.lyceum.schedule.ui.theme.NikaSaturday
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject

@Composable
fun DaySelector(
    selectedDayIndex: Int,
    currentDayIndex: Int,
    onDaySelected: (Int) -> Unit
) {
    val dayNames = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
    val shortDayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE2EBF4))
            .border(width = 1.dp, color = NikaBorder)
    ) {
        // Main Nav Bar with < Day >
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val prev = if (selectedDayIndex > 0) selectedDayIndex - 1 else 5
                    onDaySelected(prev)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Предыдущий день",
                    tint = NikaSubject
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dayNames.getOrElse(selectedDayIndex) { "Понедельник" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedDayIndex == 5) NikaSaturday else NikaSubject
                )
                if (selectedDayIndex == currentDayIndex) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(NikaCalExists, RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF8888CC), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Сегодня",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NikaSubject
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    val next = if (selectedDayIndex < 5) selectedDayIndex + 1 else 0
                    onDaySelected(next)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Следующий день",
                    tint = NikaSubject
                )
            }
        }

        // Quick Day Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            shortDayNames.forEachIndexed { index, name ->
                val isSelected = index == selectedDayIndex
                val isToday = index == currentDayIndex

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isSelected -> NikaSubject
                                isToday -> NikaCalExists
                                else -> Color.White.copy(alpha = 0.7f)
                            }
                        )
                        .border(
                            width = if (isToday && !isSelected) 1.5.dp else 1.dp,
                            color = if (isToday && !isSelected) Color(0xFF8888CC) else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onDaySelected(index) }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> Color.White
                            index == 5 -> NikaSaturday
                            else -> Color(0xFF2D3748)
                        }
                    )
                }
            }
        }
    }
}
