package ru.nstu.lyceum.schedule.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import ru.nstu.lyceum.schedule.R
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.data.repository.PreferencesRepository
import ru.nstu.lyceum.schedule.data.update.ReleaseInfo

@Composable
fun SettingsDialog(
    currentAdminPassword: String,
    isAdmin: Boolean = false,
    initialThemePreset: String = PreferencesRepository.THEME_CLASSIC,
    onThemePresetChanged: (String) -> Unit = {},
    initialShowPairs: Boolean,
    initialStatusBarPairs: Boolean,
    initialOngoingStatusNotification: Boolean = false,
    initialOverlaySwipe: Boolean,
    favoriteClass: String?,
    initialSubgroup: Int,
    initialTechSubgroup: Int,
    initialCopyHw: Boolean,
    initialCopyNotes: Boolean,
    isMagmaUnlocked: Boolean = false,
    initialUseMagmaLogo: Boolean = false,
    onMagmaLogoChanged: (Boolean) -> Unit = {},
    onSettingsSaved: (
        password: String,
        showPairs: Boolean,
        statusBarPairs: Boolean,
        ongoingStatusNotification: Boolean,
        overlaySwipe: Boolean,
        subgroup: Int,
        techSubgroup: Int,
        copyHw: Boolean,
        copyNotes: Boolean
    ) -> Unit,
    onForceServerRefresh: () -> Unit,
    onExportNotes: () -> Unit = {},
    onImportNotes: () -> Unit = {},
    isYandexConnected: Boolean = false,
    yandexUserLogin: String? = null,
    yandexLastSyncTime: Long = 0L,
    isYandexSyncing: Boolean = false,
    onYandexLogin: () -> Unit = {},
    onYandexLogout: () -> Unit = {},
    onYandexSyncManual: () -> Unit = {},
    currentAppVersion: String = "1.0.0",
    currentApkSha256: String? = null,
    availableUpdate: ReleaseInfo? = null,
    isUpdateDismissed: Boolean = false,
    initialAutoUpdateEnabled: Boolean = true,
    onAutoUpdateChanged: (Boolean) -> Unit = {},
    lastUpdateCheckTime: Long = 0L,
    isCheckingUpdate: Boolean = false,
    onCheckUpdateManual: () -> Unit = {},
    onInstallUpdateClick: (ReleaseInfo) -> Unit = {},
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedThemePreset by remember { mutableStateOf(initialThemePreset) }
    var passwordInput by remember { mutableStateOf(currentAdminPassword) }
    var showPairs by remember { mutableStateOf(initialShowPairs) }
    var statusBarPairs by remember { mutableStateOf(initialStatusBarPairs) }
    var ongoingStatusNotification by remember { mutableStateOf(initialOngoingStatusNotification) }
    var overlaySwipe by remember { mutableStateOf(initialOverlaySwipe) }
    var autoUpdateEnabled by remember { mutableStateOf(initialAutoUpdateEnabled) }

    var studentSubgroup by remember { mutableIntStateOf(initialSubgroup) }
    var studentTechSubgroup by remember { mutableIntStateOf(initialTechSubgroup) }
    var copyHw by remember { mutableStateOf(initialCopyHw) }
    var copyNotes by remember { mutableStateOf(initialCopyNotes) }
    var useMagmaLogo by remember { mutableStateOf(initialUseMagmaLogo) }

    val isMiddleSchool = remember(favoriteClass) {
        if (favoriteClass.isNullOrBlank()) false
        else Regex("^[5-7][а-яА-Яa-zA-Z].*").matches(favoriteClass.trim())
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        titleContentColor = colorScheme.onSurface,
        textContentColor = colorScheme.onSurfaceVariant,
        title = {
            Text(
                "Настройки",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Theme Selection Section
                Text(
                    text = "Тема оформления",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemeOptionCard(
                            title = "Лицей",
                            color = Color(0xFF003882),
                            isSelected = selectedThemePreset == PreferencesRepository.THEME_CLASSIC,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedThemePreset = PreferencesRepository.THEME_CLASSIC
                            onThemePresetChanged(PreferencesRepository.THEME_CLASSIC)
                        }

                        ThemeOptionCard(
                            title = "Шишки",
                            color = Color(0xFF006C4C),
                            isSelected = selectedThemePreset == PreferencesRepository.THEME_EMERALD,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedThemePreset = PreferencesRepository.THEME_EMERALD
                            onThemePresetChanged(PreferencesRepository.THEME_EMERALD)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemeOptionCard(
                            title = "Material You",
                            color = Color(0xFF6750A4),
                            isSelected = selectedThemePreset == PreferencesRepository.THEME_DYNAMIC,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedThemePreset = PreferencesRepository.THEME_DYNAMIC
                            onThemePresetChanged(PreferencesRepository.THEME_DYNAMIC)
                        }

                        ThemeOptionCard(
                            title = "Амолед",
                            color = Color(0xFF000000),
                            borderColor = Color(0xFF71717A),
                            isSelected = selectedThemePreset == PreferencesRepository.THEME_OLED,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedThemePreset = PreferencesRepository.THEME_OLED
                            onThemePresetChanged(PreferencesRepository.THEME_OLED)
                        }
                    }
                }

                if (isMagmaUnlocked || isAdmin) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Логотип приложения",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    useMagmaLogo = false
                                    onMagmaLogoChanged(false)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!useMagmaLogo) colorScheme.primaryContainer else colorScheme.surface,
                            border = BorderStroke(
                                width = if (!useMagmaLogo) 2.dp else 1.dp,
                                color = if (!useMagmaLogo) colorScheme.primary else colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_lyceum_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Лицей",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = if (!useMagmaLogo) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    useMagmaLogo = true
                                    onMagmaLogoChanged(true)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (useMagmaLogo) colorScheme.primaryContainer else colorScheme.surface,
                            border = BorderStroke(
                                width = if (useMagmaLogo) 2.dp else 1.dp,
                                color = if (useMagmaLogo) colorScheme.primary else colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_magma_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "MagmaXY",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = if (useMagmaLogo) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Pairs schedule switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Отображать расписание парами",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = showPairs,
                        onCheckedChange = { showPairs = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status bar pairs switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Статус-бар парами",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = statusBarPairs,
                        onCheckedChange = { statusBarPairs = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ongoing Status notification switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Статус-бар уведомление",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = ongoingStatusNotification,
                        onCheckedChange = { ongoingStatusNotification = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Schedule flipping animation switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Анимация свайпа",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = overlaySwipe,
                        onCheckedChange = { overlaySwipe = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primaryContainer
                        )
                    )
                }

                // Copying options
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Включать ДЗ при копировании",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = copyHw,
                        onCheckedChange = { copyHw = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Включать заметки при копировании",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = copyNotes,
                        onCheckedChange = { copyNotes = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primaryContainer
                        )
                    )
                }

                // Subgroup section (moved below "Включать заметки при копировании")
                if (!favoriteClass.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Моя подгруппа ($favoriteClass)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2).forEach { grp ->
                                val isSel = studentSubgroup == grp
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) colorScheme.primary else colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { studentSubgroup = grp }
                                ) {
                                    Text(
                                        text = "$grp гр",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSel) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isMiddleSchool) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Подгруппа по технологии",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1, 2, 3).forEach { grp ->
                                    val isSel = studentTechSubgroup == grp
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) colorScheme.primary else colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { studentTechSubgroup = grp }
                                    ) {
                                        Text(
                                            text = "$grp",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSel) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isMagmaUnlocked || isAdmin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Пароль администратора:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Введите пароль...", style = MaterialTheme.typography.bodyMedium, color = colorScheme.outline) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            focusedContainerColor = colorScheme.surface,
                            unfocusedContainerColor = colorScheme.surface,
                            cursorColor = colorScheme.primary,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // --- Синхронизация с Яндекс Диском ---
                Text(
                    text = "Синхронизация заметок и ДЗ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Яндекс Диск",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurface
                                )
                                val statusText = if (isYandexConnected) {
                                    val loginStr = if (!yandexUserLogin.isNullOrBlank()) " ($yandexUserLogin)" else ""
                                    "Подключено$loginStr"
                                } else {
                                    "Не подключено"
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isYandexConnected) colorScheme.primary else colorScheme.outline
                                )
                            }

                            if (isYandexConnected) {
                                OutlinedButton(
                                    onClick = onYandexLogout,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Выйти", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Button(
                                    onClick = onYandexLogin,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Войти через Яндекс", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        if (isYandexConnected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.35f))
                            Spacer(modifier = Modifier.height(8.dp))

                            val lastSyncStr = if (yandexLastSyncTime > 0) {
                                val sdf = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
                                "Синхронизировано: ${sdf.format(java.util.Date(yandexLastSyncTime))}"
                            } else {
                                "Синхронизация ещё не выполнялась"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = lastSyncStr,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = colorScheme.outline,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedButton(
                                    onClick = onYandexSyncManual,
                                    enabled = !isYandexSyncing,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isYandexSyncing) "Синхронизация..." else "Синхронизировать",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Updates section
                Text(
                    text = "Обновления",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.5.dp, colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val shortHash = currentApkSha256?.take(8) ?: "—"
                        Text(
                            text = "Текущая версия: $currentAppVersion (sha256: $shortHash…)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colorScheme.onSurface
                        )
                        val lastCheckStr = if (lastUpdateCheckTime > 0) {
                            val sdf = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
                            "Проверено: ${sdf.format(java.util.Date(lastUpdateCheckTime))}"
                        } else {
                            "Проверка ещё не выполнялась"
                        }
                        Text(
                            text = lastCheckStr,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = colorScheme.outline
                        )

                        if (availableUpdate != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(0.5.dp, colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Доступно: ${availableUpdate.tagName.ifBlank { availableUpdate.releaseName }}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onSurface
                                        )
                                        if (isUpdateDismissed) {
                                            Text(
                                                text = "Напоминание скрыто",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onInstallUpdateClick(availableUpdate) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.primary,
                                            contentColor = colorScheme.onPrimary
                                        )
                                    ) {
                                        Text(
                                            text = "Установить",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Автопроверка обновлений",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Проверять последний релиз раз в 24 часа",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoUpdateEnabled,
                                onCheckedChange = {
                                    autoUpdateEnabled = it
                                    onAutoUpdateChanged(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colorScheme.onPrimary,
                                    checkedTrackColor = colorScheme.primary,
                                    uncheckedThumbColor = colorScheme.outline,
                                    uncheckedTrackColor = colorScheme.surfaceVariant
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onCheckUpdateManual,
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isCheckingUpdate) "Проверка обновлений..." else "Проверить обновления",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onForceServerRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Обновить расписание на сервере",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportNotes,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Экспорт заметок",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onImportNotes,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Импорт заметок",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSettingsSaved(
                        passwordInput.trim(),
                        showPairs,
                        statusBarPairs,
                        ongoingStatusNotification,
                        overlaySwipe,
                        studentSubgroup,
                        studentTechSubgroup,
                        copyHw,
                        copyNotes
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text("Сохранить", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Закрыть",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun ThemeOptionCard(
    title: String,
    color: Color,
    borderColor: Color? = null,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.5f) else colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (borderColor != null) Modifier.border(1.dp, borderColor, CircleShape)
                        else Modifier
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
