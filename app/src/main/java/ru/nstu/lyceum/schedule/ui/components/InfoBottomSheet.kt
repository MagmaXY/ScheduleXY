package ru.nstu.lyceum.schedule.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nstu.lyceum.schedule.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun openLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyLink(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            Text(
                text = "О приложении и сервисы",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 1. Developer and Project Section
            Text(
                text = "Разработчик",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.6.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.developer_avatar),
                            contentDescription = "Дима Филиппов",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Дима Филиппов",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp
                                ),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Л10-3 (2026–2027 уч. год)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // GitHub Link Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { openLink("https://github.com/MagmaXY/ScheduleXY") },
                        color = colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_brand_github),
                                    contentDescription = "GitHub",
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "GitHub: MagmaXY/ScheduleXY",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { copyLink("GitHub Репозиторий", "https://github.com/MagmaXY/ScheduleXY") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Скопировать",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1.1 System Administrator Section
            Text(
                text = "Системный администратор",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.6.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.sysadmin_avatar),
                            contentDescription = "Муль Павел Фридрихович",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Муль Павел Фридрихович",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp
                                ),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Учитель информатики",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // GitHub Link Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { openLink("https://github.com/paulmy") },
                        color = colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_brand_github),
                                    contentDescription = "GitHub",
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "GitHub: paulmy",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { copyLink("GitHub Системного администратора", "https://github.com/paulmy") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Скопировать",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Lyceum Services Section
            Text(
                text = "Сервисы лицея",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LinkItemCard(
                title = "Основной сайт лицея",
                subtitle = "lyceum.nstu.ru",
                url = "https://lyceum.nstu.ru/",
                iconContent = {
                    LyceumLogoTwoToneIcon(
                        modifier = Modifier.size(20.dp),
                        primaryColor = colorScheme.primary,
                        secondaryColor = colorScheme.primary.copy(alpha = 0.45f)
                    )
                },
                onOpen = ::openLink,
                onCopy = ::copyLink
            )

            LinkItemCard(
                title = "СДО Лицея",
                subtitle = "lyceum.nstu.ru/sdo",
                url = "https://lyceum.nstu.ru/sdo",
                iconContent = {
                    SdoLogoTwoToneIcon(
                        modifier = Modifier.size(20.dp),
                        primaryColor = colorScheme.primary,
                        secondaryColor = colorScheme.primary.copy(alpha = 0.45f)
                    )
                },
                onOpen = ::openLink,
                onCopy = ::copyLink
            )

            LinkItemCard(
                title = "Веб-расписание",
                subtitle = "lyceum.nstu.ru/rasp/schedule.html",
                url = "https://lyceum.nstu.ru/rasp/schedule.html",
                iconContent = {
                    NikaSoftLogoTwoToneIcon(
                        modifier = Modifier.size(20.dp),
                        primaryColor = colorScheme.primary,
                        secondaryColor = colorScheme.primary.copy(alpha = 0.35f)
                    )
                },
                onOpen = ::openLink,
                onCopy = ::copyLink
            )

            LinkItemCard(
                title = "Информационная панель",
                subtitle = "lyceum.nstu.ru/info",
                url = "https://lyceum.nstu.ru/info",
                iconContent = {
                    DashboardLogoTwoToneIcon(
                        modifier = Modifier.size(20.dp),
                        primaryColor = colorScheme.primary,
                        secondaryColor = colorScheme.primary.copy(alpha = 0.40f)
                    )
                },
                onOpen = ::openLink,
                onCopy = ::copyLink
            )

            LinkItemCard(
                title = "Дневник НСО",
                subtitle = "school.nso.ru/journal-app",
                url = "https://school.nso.ru/journal-app",
                iconContent = {
                    NsoCoatOfArmsTwoToneIcon(
                        modifier = Modifier.size(20.dp),
                        primaryColor = colorScheme.primary,
                        secondaryColor = colorScheme.primary.copy(alpha = 0.40f)
                    )
                },
                onOpen = ::openLink,
                onCopy = ::copyLink
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(14.dp))

            // 3. Social Media Section
            Text(
                text = "Социальные сети",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LinkItemCard(
                title = "ВКонтакте",
                subtitle = "vk.com/il_nstu",
                url = "https://vk.com/il_nstu",
                iconRes = R.drawable.ic_brand_vk,
                iconColor = colorScheme.primary,
                onOpen = ::openLink,
                onCopy = ::copyLink
            )

            LinkItemCard(
                title = "Telegram",
                subtitle = "t.me/il_ngtu",
                url = "https://t.me/il_ngtu",
                iconRes = R.drawable.ic_brand_telegram,
                iconColor = colorScheme.primary,
                onOpen = ::openLink,
                onCopy = ::copyLink
            )

            LinkItemCard(
                title = "МАХ",
                subtitle = "max.ru/id5404140563_gos",
                url = "https://max.ru/id5404140563_gos",
                iconRes = R.drawable.ic_brand_max,
                iconColor = colorScheme.primary,
                onOpen = ::openLink,
                onCopy = ::copyLink
            )

            LinkItemCard(
                title = "YouTube (архивный)",
                subtitle = "youtube.com/c/ИнженерныйлицейНГТУ",
                url = "https://www.youtube.com/c/ИнженерныйлицейНГТУ",
                iconRes = R.drawable.ic_brand_youtube,
                iconColor = colorScheme.primary,
                onOpen = ::openLink,
                onCopy = ::copyLink
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LinkItemCard(
    title: String,
    subtitle: String,
    url: String,
    iconVector: ImageVector? = null,
    iconRes: Int? = null,
    iconColor: Color? = null,
    iconContent: (@Composable () -> Unit)? = null,
    onOpen: (String) -> Unit,
    onCopy: (String, String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 0.5.dp,
            brush = androidx.compose.ui.graphics.SolidColor(colorScheme.outlineVariant)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen(url) }
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (iconContent != null) {
                    iconContent()
                } else if (iconRes != null) {
                    if (iconColor == Color.Unspecified) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = iconColor ?: colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                } else if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconColor ?: colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp
                    ),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { onCopy(title, url) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Скопировать",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
