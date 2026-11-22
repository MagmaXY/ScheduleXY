package ru.nstu.lyceum.schedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import ru.nstu.lyceum.schedule.R
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.domain.BellState
import ru.nstu.lyceum.schedule.domain.RealtimeSyncManager
import ru.nstu.lyceum.schedule.ui.theme.LyceumBlue
import ru.nstu.lyceum.schedule.ui.theme.LyceumGold
import ru.nstu.lyceum.schedule.ui.theme.LyceumGreen

@Composable
fun HeaderView(
    realtimeState: RealtimeSyncManager.RealtimeState,
    isLoading: Boolean,
    isDarkTheme: Boolean = false,
    isPairsStatus: Boolean = false,
    useMagmaLogo: Boolean = false,
    onLogoEasterEggTriggered: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInfoClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Main Top Bar: Lyceum Logo & School Title (Left) + Actions (Right: Info, Theme, Settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Logo & School Title
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    var logoTapCount by remember { mutableIntStateOf(0) }
                    var lastLogoTapTime by remember { mutableLongStateOf(0L) }

                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                val now = System.currentTimeMillis()
                                if (now - lastLogoTapTime > 2000L) {
                                    logoTapCount = 1
                                } else {
                                    logoTapCount++
                                }
                                lastLogoTapTime = now

                                if (logoTapCount in 4..6) {
                                    val remaining = 7 - logoTapCount
                                    Toast.makeText(context, "Осталось тапов: $remaining", Toast.LENGTH_SHORT).show()
                                } else if (logoTapCount >= 7) {
                                    logoTapCount = 0
                                    onLogoEasterEggTriggered()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = if (useMagmaLogo) R.drawable.ic_magma_logo else R.drawable.ic_lyceum_logo),
                            contentDescription = if (useMagmaLogo) "Логотип MagmaXY" else "Логотип лицея",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Инженерный лицей НГТУ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Right: Action Buttons (Info `(i)`, Theme, Settings)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "О приложении и сервисы",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Светлая тема" else "Тёмная тема",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Realtime Live Bell Ticker Card with Embedded Progress Bar (only during active lessons/breaks)
            val bellState = realtimeState.bellState
            val bellText = bellState.getDisplayText(isPairsStatus)
            val isLesson = bellState is BellState.InLesson
            val isBreak = bellState is BellState.Break
            val isBeforeSchool = bellState is BellState.BeforeSchool
            val progress = bellState.currentProgress

            // Only show the status card if school is actively in progress:
            // (Strictly between 8:00 AM and end of 12th lesson at 19:00, during BeforeSchool, InLesson or Break).
            // Outside of active school hours (before 8:00 AM, after 12th lesson, weekends, vacations), card is completely hidden.
            val curMinutes = remember(realtimeState) {
                val c = java.util.Calendar.getInstance()
                c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
            }
            val maxMinutes = if (isPairsStatus) 15 * 60 + 30 else 19 * 60
            val isTimeInRange = curMinutes in (8 * 60) until maxMinutes
            val isSchoolActive = isTimeInRange && (isLesson || isBreak || isBeforeSchool) &&
                bellState !is BellState.AfterSchool && bellState !is BellState.Weekend && bellState !is BellState.Vacation

            if (isSchoolActive) {
                Spacer(modifier = Modifier.height(6.dp))

                val statusAccentColor = when {
                    isLesson -> colorScheme.secondary // Lyceum Emerald Green
                    isBreak -> colorScheme.tertiary    // Lyceum Amber Gold
                    isBeforeSchool -> colorScheme.primary
                    else -> colorScheme.outline
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceContainerHigh)
                        .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bellText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = when {
                                    isLesson -> colorScheme.secondary
                                    isBreak -> colorScheme.tertiary
                                    isBeforeSchool -> colorScheme.primary
                                    else -> colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // If progress exists, show percent badge
                            if (progress != null && progress in 0.01f..0.99f) {
                                val percent = (progress * 100).toInt()
                                Text(
                                    text = "$percent%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = statusAccentColor
                                )
                            }
                        }

                        // Sleek Organic Progress Bar for Active Lesson / Break
                        if (progress != null && (isLesson || isBreak)) {
                            Spacer(modifier = Modifier.height(5.dp))
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress.coerceIn(0f, 1f),
                                label = "bellProgress"
                            )
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = statusAccentColor,
                                trackColor = colorScheme.surfaceContainerHighest
                            )
                        }
                    }
                }
            }

            // Indeterminate sleek network sync progress bar right under header
            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(2.dp)
                        .clip(CircleShape),
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}
