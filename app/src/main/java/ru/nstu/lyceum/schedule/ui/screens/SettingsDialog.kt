package ru.nstu.lyceum.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.domain.BellSchedule
import ru.nstu.lyceum.schedule.ui.theme.NikaLsnTime
import ru.nstu.lyceum.schedule.ui.theme.NikaSubject

@Composable
fun SettingsDialog(
    currentAdminPassword: String,
    onAdminPasswordSaved: (String) -> Unit,
    onForceServerRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var passwordInput by remember { mutableStateOf(currentAdminPassword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Настройки и звонки",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NikaSubject
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                // Bell timetable section
                Text(
                    text = "Расписание звонков лицея:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    BellSchedule.BELLS.forEach { bell ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${bell.num} урок",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = bell.timeRange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NikaLsnTime
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Admin password section
                Text(
                    text = "Пароль администратора (X-Admin-Password):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("licey2026") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Server cache refresh button
                OutlinedButton(
                    onClick = onForceServerRefresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сбросить кэш на сервере (/api/schedule/refresh)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdminPasswordSaved(passwordInput.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NikaSubject)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
