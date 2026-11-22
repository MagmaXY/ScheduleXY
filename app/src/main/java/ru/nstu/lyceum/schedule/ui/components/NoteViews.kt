package ru.nstu.lyceum.schedule.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.local.NoteType
import ru.nstu.lyceum.schedule.data.local.ScheduleNote
import ru.nstu.lyceum.schedule.ui.util.MarkdownRenderer

@Composable
fun LessonNotesSection(
    notes: List<ScheduleNote>,
    onToggleCompleted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) return

    // Order: HOMEWORK first, then NOTE
    val sortedNotes = notes.sortedBy { if (it.type == NoteType.HOMEWORK) 0 else 1 }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 42.dp, end = 46.dp, top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        sortedNotes.forEach { note ->
            val prefix = if (note.type == NoteType.HOMEWORK) "ДЗ: " else "Заметка: "

            val parsedText = MarkdownRenderer.parseMarkdown(note.text)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCompleted(note.id) },
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    ),
                    color = if (note.isCompleted) colorScheme.outlineVariant else colorScheme.onSurfaceVariant
                )
                Text(
                    text = parsedText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp,
                        textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (note.isCompleted) colorScheme.outlineVariant else colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SlotNoteCard(
    note: ScheduleNote,
    title: String,
    onToggleCompleted: (String) -> Unit,
    onEditClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val parsedText = MarkdownRenderer.parseMarkdown(note.text)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 46.dp, top = 8.dp, bottom = 8.dp)
                .clickable { onToggleCompleted(note.id) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label centered in the 76dp space reserved for number + time
            Box(
                modifier = Modifier.width(76.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        lineHeight = 13.sp
                    ),
                    color = colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Note text in the content space, gray
            Text(
                text = parsedText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (note.isCompleted) colorScheme.outlineVariant else colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            if (onEditClick != null) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)
    }
}

@Composable
fun StandaloneLessonNoteCard(
    note: ScheduleNote,
    timeRange: String = "",
    onToggleCompleted: (String) -> Unit,
    onEditClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val nika = ru.nstu.lyceum.schedule.ui.theme.NikaTheme.colors
    val parsedText = MarkdownRenderer.parseMarkdown(note.text)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .clickable { onToggleCompleted(note.id) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lesson Number
            Text(
                text = (note.lessonNum ?: 1).toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = nika.lsnNum,
                modifier = Modifier.width(22.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Time
            Column(
                modifier = Modifier.width(48.dp),
                horizontalAlignment = Alignment.Start
            ) {
                val times = timeRange.split("–", "-")
                Text(
                    text = times.getOrNull(0)?.trim() ?: timeRange,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = nika.lsnTime
                )
                Text(
                    text = times.getOrNull(1)?.trim() ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = nika.lsnTime
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                if (note.type == NoteType.HOMEWORK && !note.subject.isNullOrBlank()) {
                    Text(
                        text = note.subject,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = nika.subject
                    )
                }

                Row(verticalAlignment = Alignment.Top) {
                    val prefix = if (note.type == NoteType.HOMEWORK) "ДЗ: " else "Заметка: "
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = if (note.isCompleted) colorScheme.outlineVariant else colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = parsedText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp,
                            textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (note.isCompleted) colorScheme.outlineVariant else colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (onEditClick != null) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)
    }
}
