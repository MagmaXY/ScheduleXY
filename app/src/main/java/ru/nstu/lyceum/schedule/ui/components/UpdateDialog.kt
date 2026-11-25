package ru.nstu.lyceum.schedule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.update.ReleaseInfo
import ru.nstu.lyceum.schedule.ui.util.MarkdownRenderer
import java.util.Locale

@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    currentSha256: String?,
    isDownloading: Boolean,
    downloadProgress: Float, // 0..1 or -1f indeterminate
    downloadError: String?,
    onInstallClick: () -> Unit,
    onLaterClick: () -> Unit,
    onDeclineReleaseClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sizeMb = if (release.apkSize > 0) {
        String.format(Locale.US, "%.1f МБ", release.apkSize / (1024f * 1024f))
    } else null

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onLaterClick() },
        shape = RoundedCornerShape(16.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        title = {
            Column {
                Text(
                    text = "Доступно обновление",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colorScheme.primaryContainer
                    ) {
                        Text(
                            text = release.tagName.ifBlank { release.releaseName },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (sizeMb != null) {
                        Text(
                            text = sizeMb,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (release.hasExactSha256) {
                    val curShort = currentSha256?.take(8) ?: "—"
                    val relShort = release.sha256.take(8)
                    Text(
                        text = "SHA-256: $curShort… → $relShort…",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (release.changelog.isNotBlank()) {
                    Text(
                        text = "Что нового:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = MarkdownRenderer.parseMarkdown(release.changelog),
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Вышла новая сборка приложения в GitHub Releases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (downloadProgress >= 0f) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = colorScheme.primary,
                            trackColor = colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Загрузка: ${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = colorScheme.primary,
                            trackColor = colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Загрузка обновления...",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!downloadError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = downloadError,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onInstallClick,
                enabled = !isDownloading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isDownloading) "Загрузка..." else "Установить",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onDeclineReleaseClick,
                    enabled = !isDownloading,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Отказаться",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.error
                    )
                }
                TextButton(
                    onClick = onLaterClick,
                    enabled = !isDownloading,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Позже",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
