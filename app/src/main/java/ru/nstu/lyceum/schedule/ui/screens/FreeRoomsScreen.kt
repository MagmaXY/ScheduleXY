package ru.nstu.lyceum.schedule.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.local.FreeAvailabilityEngine
import ru.nstu.lyceum.schedule.domain.RealtimeSyncManager
import ru.nstu.lyceum.schedule.ui.util.SmartSearchEngine

@Composable
fun FreeRoomsScreen(
    slotOccupancy: FreeAvailabilityEngine.SlotOccupancy?,
    selectedDayIndex: Int,
    selectedLessonNum: Int,
    realtimeState: RealtimeSyncManager.RealtimeState,
    adminPassword: String,
    isLoading: Boolean,
    overlaySwipeAnimation: Boolean = true,
    onDaySelected: (Int) -> Unit,
    onLessonNumSelected: (Int) -> Unit,
    onAdminPasswordChanged: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var totalDragX by remember { mutableFloatStateOf(0f) }

    var selectedMode by remember { mutableStateOf("rooms") } // "rooms" or "teachers"
    var teacherSearch by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    val isTeachersUnlocked = adminPassword == "licey2026"

    LaunchedEffect(isTeachersUnlocked) {
        if (!isTeachersUnlocked) {
            selectedMode = "rooms"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .pointerInput(selectedLessonNum) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        if (totalDragX < -60f) {
                            // Swipe left -> next lesson (cyclic)
                            val next = if (selectedLessonNum < 12) selectedLessonNum + 1 else 1
                            onLessonNumSelected(next)
                        } else if (totalDragX > 60f) {
                            // Swipe right -> previous lesson (cyclic)
                            val prev = if (selectedLessonNum > 1) selectedLessonNum - 1 else 12
                            onLessonNumSelected(prev)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    }
                )
            }
    ) {
        // Top Bar: Mode Switcher (only if unlocked) + Days + Lesson Slots
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Mode Switcher: Кабинеты / Учителя (отображается ТОЛЬКО после ввода пароля)
                if (isTeachersUnlocked) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isRooms = selectedMode == "rooms"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clickable { selectedMode = "rooms" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Кабинеты",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isRooms) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isRooms) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        val isTeachers = selectedMode == "teachers"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clickable { selectedMode = "teachers" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Учителя",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isTeachers) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isTeachers) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Slot Picker: Days (Пн..Сб)
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
                        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб").forEachIndexed { idx, shortName ->
                            val isSelected = idx == selectedDayIndex
                            val isToday = idx == realtimeState.dayIndex

                            val textColor = when {
                                isToday -> colorScheme.primary
                                isSelected -> colorScheme.onSurface
                                else -> colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onDaySelected(idx) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shortName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    ),
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                // Lesson slot selector (1..12)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .background(colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..12).forEach { num ->
                            val isSelected = num == selectedLessonNum
                            val isCurrentLesson = (realtimeState.bellState.activeLessonNum == num) && (selectedDayIndex == realtimeState.dayIndex)

                            val textColor = when {
                                isCurrentLesson -> colorScheme.primary
                                isSelected -> colorScheme.onSurface
                                else -> colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onLessonNumSelected(num) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$num",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected || isCurrentLesson) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.5.sp
                                    ),
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)

        if (isLoading && slotOccupancy == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        } else if (slotOccupancy != null) {
            AnimatedContent(
                targetState = Triple(selectedMode, selectedLessonNum, selectedDayIndex),
                transitionSpec = {
                    if (overlaySwipeAnimation) {
                        val forward = when {
                            initialState.second == 12 && targetState.second == 1 -> true
                            initialState.second == 1 && targetState.second == 12 -> false
                            else -> targetState.second > initialState.second || targetState.third > initialState.third
                        }
                        if (forward) {
                            (slideInHorizontally(
                                animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                            ) { it } + fadeIn(tween(150)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                                    ) { -it } + fadeOut(tween(150))
                                )
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                            ) { -it } + fadeIn(tween(150)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                                    ) { it } + fadeOut(tween(150))
                                )
                        }
                    } else {
                        EnterTransition.None.togetherWith(ExitTransition.None)
                    }
                },
                label = "free_rooms_teachers_slide_content"
            ) { (mode, _, _) ->
                if (mode == "rooms") {
                    // Free Rooms View
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
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Свободно: ${freeRooms.size}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.secondary
                                )
                                Text(
                                    text = "Занято: ${busyRooms.size}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.error
                                )
                            }
                        }

                        // Free Rooms Badges Card
                        item {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 0.7.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(colorScheme.secondary.copy(alpha = 0.4f))
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Свободные кабинеты:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (freeRooms.isEmpty()) "Все кабинеты заняты" else freeRooms.joinToString("  •  "),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Занятые кабинеты:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.error,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }

                        items(busyRooms.entries.toList().sortedBy { it.key }) { (room, occupancy) ->
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 0.6.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outlineVariant)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colorScheme.errorContainer.copy(alpha = 0.5f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = room,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.error
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = occupancy,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                } else {
                    // Free Teachers View
                    if (!isTeachersUnlocked) {
                        // Password Lock View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 0.7.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outlineVariant)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Доступ к разделу учителей",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Для просмотра свободных преподавателей введите пароль администратора:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = {
                                            passwordInput = it
                                            passwordError = false
                                        },
                                        placeholder = { Text("Введите пароль...", style = MaterialTheme.typography.bodyMedium) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        isError = passwordError,
                                        supportingText = if (passwordError) {
                                            { Text("Неверный пароль", color = colorScheme.error) }
                                        } else null,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = colorScheme.onSurface,
                                            unfocusedTextColor = colorScheme.onSurface,
                                            focusedBorderColor = colorScheme.primary,
                                            unfocusedBorderColor = colorScheme.outlineVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            if (passwordInput.trim() == "licey2026") {
                                                onAdminPasswordChanged("licey2026")
                                                passwordError = false
                                            } else {
                                                passwordError = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.primary,
                                            contentColor = colorScheme.onPrimary
                                        )
                                    ) {
                                        Text("Разблокировать", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Unlocked Free Teachers List
                        val allFree = slotOccupancy.freeTeachers
                        val busyTeachers = slotOccupancy.busyTeachers

                        val filteredFree = remember(allFree, teacherSearch) {
                            val q = teacherSearch.trim()
                            if (q.isEmpty()) allFree
                            else {
                                allFree.filter { teacher ->
                                    SmartSearchEngine.evaluateScore(q, title = teacher, content = null) > 0
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            item {
                                OutlinedTextField(
                                    value = teacherSearch,
                                    onValueChange = { teacherSearch = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 4.dp),
                                    placeholder = {
                                        Text(
                                            text = "Поиск свободного учителя...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Поиск",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (teacherSearch.isNotEmpty()) {
                                            IconButton(
                                                onClick = { teacherSearch = "" },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Очистить",
                                                    tint = colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = colorScheme.onSurface,
                                        unfocusedTextColor = colorScheme.onSurface,
                                        focusedContainerColor = colorScheme.surfaceContainerLow,
                                        unfocusedContainerColor = colorScheme.surfaceContainerLow,
                                        cursorColor = colorScheme.primary,
                                        focusedBorderColor = colorScheme.primary,
                                        unfocusedBorderColor = colorScheme.outlineVariant
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Свободно учителей: ${allFree.size}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.primary
                                    )
                                    Text(
                                        text = "Занято: ${busyTeachers.size}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.error
                                    )
                                }
                            }

                            if (filteredFree.isEmpty()) {
                                item {
                                    Text(
                                        text = if (teacherSearch.isNotBlank()) "Учитель не найден" else "В этот слот нет свободных учителей",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(filteredFree) { teacher ->
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            width = 0.6.dp,
                                            brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outlineVariant)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Свободен",
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = teacher,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            if (busyTeachers.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Занятые учителя (${busyTeachers.size}):",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                    )
                                }

                                items(busyTeachers.entries.toList().sortedBy { it.key }) { (teacher, info) ->
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            width = 0.6.dp,
                                            brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outlineVariant)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colorScheme.errorContainer.copy(alpha = 0.5f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = teacher,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = colorScheme.error
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = info,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
