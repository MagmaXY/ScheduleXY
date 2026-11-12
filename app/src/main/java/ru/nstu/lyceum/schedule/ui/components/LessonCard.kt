package ru.nstu.lyceum.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.api.Lesson
import ru.nstu.lyceum.schedule.ui.theme.NikaBorderLight
import ru.nstu.lyceum.schedule.ui.theme.NikaCalExists
import ru.nstu.lyceum.schedule.ui.theme.NikaCancel
import ru.nstu.lyceum.schedule.ui.theme.NikaChange
import ru.nstu.lyceum.schedule.ui.theme.NikaGroup
import ru.nstu.lyceum.schedule.ui.theme.NikaLsnNum
import ru.nstu.lyceum.schedule.ui.theme.NikaLsnTime
import ru.nstu.lyceum.schedule.ui.theme.NikaRoom
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject
import ru.nstu.lyceum.schedule.ui.theme.NikaTeacher

@Composable
fun LessonCard(
    lesson: Lesson,
    isActiveNow: Boolean = false,
    isTeacherMode: Boolean = false
) {
    val cardBg = if (isActiveNow) NikaCalExists.copy(alpha = 0.5f) else Color.White
    val borderColor = if (isActiveNow) Color(0xFF3B82F6) else NikaBorderLight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .background(cardBg, RoundedCornerShape(8.dp))
            .border(
                width = if (isActiveNow) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lesson Number Column
            Column(
                modifier = Modifier.width(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = lesson.num.toString(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NikaLsnNum
                )
                if (isActiveNow) {
                    Text(
                        text = "идёт",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = NikaLsnTime
                    )
                }
            }

            // Lesson Time Column
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.Start
            ) {
                val times = lesson.time.split("–", "-")
                Text(
                    text = times.getOrNull(0)?.trim() ?: lesson.time,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NikaLsnTime
                )
                Text(
                    text = times.getOrNull(1)?.trim() ?: "",
                    fontSize = 10.sp,
                    color = NikaLsnTime.copy(alpha = 0.8f)
                )
            }

            // Subgroups Content Column
            Column(modifier = Modifier.weight(1f)) {
                if (lesson.subgroups.isEmpty()) {
                    Text(
                        text = "—",
                        fontSize = 14.sp,
                        color = NikaLsnNum
                    )
                } else {
                    lesson.subgroups.forEachIndexed { index, sg ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!sg.label.isNullOrBlank()) {
                                        Text(
                                            text = "[${sg.label}] ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = NikaGroup
                                        )
                                    }

                                    Text(
                                        text = sg.subject ?: "Без названия",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NikaSubject
                                    )
                                }

                                if (isTeacherMode) {
                                    if (!sg.className.isNullOrBlank()) {
                                        Text(
                                            text = "Класс: ${sg.className}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = NikaGroup
                                        )
                                    }
                                } else {
                                    if (!sg.teacher.isNullOrBlank()) {
                                        Text(
                                            text = sg.teacher,
                                            fontSize = 11.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = NikaTeacher
                                        )
                                    }
                                }
                            }

                            // Room Badge
                            if (!sg.room.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFEBF3FC), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFFC4DBF6), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = sg.room,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NikaRoom
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
