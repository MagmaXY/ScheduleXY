package ru.nstu.lyceum.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.ui.theme.LyceumGreen

object DayCycleHelper {
    fun cycleOrder(showAllOption: Boolean): List<Int> =
        if (showAllOption) listOf(6, 0, 1, 2, 3, 4, 5) else listOf(0, 1, 2, 3, 4, 5)

    fun nextDay(current: Int, showAllOption: Boolean): Int {
        val order = cycleOrder(showAllOption)
        val idx = order.indexOf(current).let { if (it == -1) 0 else it }
        return order[(idx + 1) % order.size]
    }

    fun prevDay(current: Int, showAllOption: Boolean): Int {
        val order = cycleOrder(showAllOption)
        val idx = order.indexOf(current).let { if (it == -1) 0 else it }
        return order[(idx - 1 + order.size) % order.size]
    }

    fun isForwardTransition(from: Int, to: Int, showAllOption: Boolean): Boolean {
        val order = cycleOrder(showAllOption)
        val fromPos = order.indexOf(from).let { if (it == -1) 0 else it }
        val toPos = order.indexOf(to).let { if (it == -1) 0 else it }
        val last = order.lastIndex
        return when {
            fromPos == 0 && toPos == last -> false
            fromPos == last && toPos == 0 -> true
            else -> toPos > fromPos
        }
    }
}

@Composable
fun DaySelector(
    selectedDayIndex: Int,
    currentDayIndex: Int,
    dateLabel: String? = null,
    showAllOption: Boolean = false,
    onDaySelected: (Int) -> Unit,
    onDateClick: (() -> Unit)? = null,
    trailingActionContent: (@Composable RowScope.() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val dayNames = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
    val shortDayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
    val isAllWeekSelected = showAllOption && selectedDayIndex == 6

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Nav Bar with < Day >
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = {
                        onDaySelected(DayCycleHelper.prevDay(selectedDayIndex, showAllOption))
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.CenterStart)
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
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = if (trailingActionContent != null) 88.dp else 38.dp)
                        .then(if (onDateClick != null) Modifier.clickable(onClick = onDateClick) else Modifier)
                ) {
                    val title = if (isAllWeekSelected) {
                        if (!dateLabel.isNullOrBlank()) "Вся неделя, $dateLabel" else "Вся неделя"
                    } else {
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
                        if (!resolvedDateLabel.isNullOrBlank()) "$baseDayName, $resolvedDateLabel" else baseDayName
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = if (!isAllWeekSelected && selectedDayIndex == 5) colorScheme.secondary else colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!isAllWeekSelected && selectedDayIndex == currentDayIndex) {
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

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trailingActionContent?.invoke(this)

                    IconButton(
                        onClick = {
                            onDaySelected(DayCycleHelper.nextDay(selectedDayIndex, showAllOption))
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
            }

            // Quick Day Selector (styled like bottom nav bar / segmented control)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .background(colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                val tabItems = if (showAllOption) {
                    listOf(6 to "Вся") + shortDayNames.mapIndexed { i, label -> i to label }
                } else {
                    shortDayNames.mapIndexed { i, label -> i to label }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabItems.forEach { (index, name) ->
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
