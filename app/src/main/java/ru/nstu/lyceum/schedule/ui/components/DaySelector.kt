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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.ui.theme.LyceumGreen

@Composable
fun DaySelector(
    selectedDayIndex: Int,
    currentDayIndex: Int,
    dateLabel: String? = null,
    onDaySelected: (Int) -> Unit,
    onDateClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val dayNames = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
    val shortDayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Nav Bar with < Day >
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val prev = if (selectedDayIndex > 0) selectedDayIndex - 1 else 5
                        onDaySelected(prev)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Предыдущий день",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (onDateClick != null) Modifier.clickable(onClick = onDateClick) else Modifier
                ) {
                    val baseDayName = dayNames.getOrElse(selectedDayIndex) { "Понедельник" }
                    val resolvedDateLabel = if (!dateLabel.isNullOrBlank()) {
                        dateLabel
                    } else {
                        try {
                            val cal = java.util.Calendar.getInstance()
                            val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
                            cal.add(java.util.Calendar.DAY_OF_MONTH, -dayOfWeek + selectedDayIndex)
                            java.text.SimpleDateFormat("d MMMM", java.util.Locale("ru")).format(cal.time)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    val title = if (!resolvedDateLabel.isNullOrBlank()) "$baseDayName, $resolvedDateLabel" else baseDayName
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = if (selectedDayIndex == 5) colorScheme.secondary else colorScheme.onSurface
                    )

                    if (selectedDayIndex == currentDayIndex) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colorScheme.secondaryContainer)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Сегодня",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        val next = if (selectedDayIndex < 5) selectedDayIndex + 1 else 0
                        onDaySelected(next)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Следующий день",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quick Day Selector (styled like bottom nav bar / segmented control)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .background(colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    shortDayNames.forEachIndexed { index, name ->
                        val isSelected = index == selectedDayIndex
                        val isToday = index == currentDayIndex

                        val textColor = when {
                            isToday -> colorScheme.primary // Синий цвет сегодняшнего дня недели (всегда подсвечивается синим)
                            isSelected -> colorScheme.onSurface // Выбранный день (например, завтрашний) — обычный активный цвет, без синей подсветки
                            else -> colorScheme.onSurfaceVariant // Неактивный день недели
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDaySelected(index) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else (if (isToday) FontWeight.SemiBold else FontWeight.Normal),
                                    fontSize = 12.sp
                                ),
                                color = textColor
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = colorScheme.outlineVariant
            )
        }
    }
}
