package ru.nstu.lyceum.schedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.domain.BellState
import ru.nstu.lyceum.schedule.domain.RealtimeSyncManager
import ru.nstu.lyceum.schedule.ui.theme.NikaBorderLight
import ru.nstu.lyceum.schedule.ui.theme.NikaHeaderEnd
import ru.nstu.lyceum.schedule.ui.theme.NikaHeaderStart
import ru.nstu.lyceum.schedule.ui.theme.NikaLsnTime
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject
import ru.nstu.lyceum.schedule.ui.theme.NikaTimeAccent
import ru.nstu.lyceum.schedule.ui.theme.NikaTimeBg

@Composable
fun HeaderView(
    realtimeState: RealtimeSyncManager.RealtimeState,
    isLoading: Boolean,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NikaHeaderStart, NikaHeaderEnd)
                )
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Инженерный лицей НГТУ",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NikaSubject,
                    lineHeight = 20.sp
                )
                Text(
                    text = "г. Новосибирск • Официальное расписание",
                    fontSize = 12.sp,
                    color = Color(0xFF4A5568),
                    lineHeight = 16.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(2.dp),
                        strokeWidth = 2.dp,
                        color = NikaSubject
                    )
                } else {
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = NikaSubject,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = NikaSubject,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Realtime Bar (authentic rasp time container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NikaTimeBg, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${realtimeState.dateString} • ${realtimeState.timeString}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2D3748)
                    )

                    val bellText = realtimeState.bellState.displayText
                    val isBreak = realtimeState.bellState is BellState.Break
                    val isLesson = realtimeState.bellState is BellState.InLesson

                    Text(
                        text = bellText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isLesson -> NikaLsnTime
                            isBreak -> NikaTimeAccent
                            else -> Color(0xFF718096)
                        }
                    )
                }
            }
        }
    }
}
