package com.kidsee.app.ui.components

import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.kidsee.app.util.AppUpdateManager
import com.kidsee.app.util.UpdateState
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeePillButton
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentVersion: String,
    updateState: UpdateState,
    familyId: String = "KID-7777",
    currentLanguage: String = "ru",
    onCheckUpdate: () -> Unit,
    onChangePin: () -> Unit,
    onSwitchRole: () -> Unit,
    onUpdateFamilyId: (String) -> Unit = {},
    onLanguageChanged: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedLang by remember { mutableStateOf(currentLanguage) }
    var showEditFamilyDialog by remember { mutableStateOf(false) }

    if (showEditFamilyDialog) {
        var tempCode by remember { mutableStateOf(familyId) }
        AlertDialog(
            onDismissRequest = { showEditFamilyDialog = false },
            title = {
                Text(
                    text = "Код семьи",
                    style = KidSeeTypography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Задайте общий код для синхронизации устройств:",
                        style = KidSeeTypography.bodyMedium.copy(color = WazeTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempCode,
                        onValueChange = { tempCode = it.trim().uppercase() },
                        singleLine = true,
                        placeholder = { Text("Например: SEMYA-1 или 7777") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempCode.isNotBlank()) {
                        onUpdateFamilyId(tempCode)
                        showEditFamilyDialog = false
                    }
                }) {
                    Text("Сохранить", fontWeight = FontWeight.Bold, color = WazeBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditFamilyDialog = false }) {
                    Text("Отмена", color = WazeTextSecondary)
                }
            }
        )
    }

    val languages = listOf(
        "ru" to "Русский",
        "uk" to "Українська",
        "en" to "English",
        "es" to "Español",
        "fr" to "Français"
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = WazeCardBg,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = WazeBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = WazeBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Настройки",
                                style = KidSeeTypography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = WazeTextPrimary
                                )
                            )
                            Text(
                                text = "KidSee v$currentVersion",
                                style = KidSeeTypography.labelSmall.copy(color = WazeTextSecondary)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = WazeTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Check for Updates Button
                SettingsActionCard(
                    icon = Icons.Default.SystemUpdate,
                    title = "Проверить обновления",
                    subtitle = when (updateState) {
                        is UpdateState.Checking -> "Проверка сервера..."
                        is UpdateState.UpdateAvailable -> "🔥 Доступна новая версия!"
                        is UpdateState.UpToDate -> "Установлена актуальная версия"
                        else -> "Поиск новой версии на GitHub"
                    },
                    iconTint = WazeBlue,
                    trailing = {
                        if (updateState is UpdateState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = WazeBlue,
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    onClick = onCheckUpdate
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Change Parent PIN
                SettingsActionCard(
                    icon = Icons.Default.Password,
                    title = "PIN-код родителя",
                    subtitle = "Защита настроек и режима от детей",
                    iconTint = WazeGreen,
                    onClick = onChangePin
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Family Code
                SettingsActionCard(
                    icon = Icons.Default.Group,
                    title = "Код семьи",
                    subtitle = "Текущий код: $familyId (нажмите для изменения)",
                    iconTint = WazeBlue,
                    onClick = { showEditFamilyDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Switch Role (Parent <-> Child)
                SettingsActionCard(
                    icon = Icons.Default.SwapHoriz,
                    title = "Сменить режим",
                    subtitle = "Переключить между Родителем и Ребенком",
                    iconTint = Color(0xFFFF9800),
                    onClick = onSwitchRole
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Language Selector
                Text(
                    text = "Язык приложения",
                    style = KidSeeTypography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = WazeTextSecondary
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for ((code, label) in languages) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedLang == code) WazeBlue else Color(0xFFF3F1EA),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedLang = code
                                    onLanguageChanged(code)
                                }
                        ) {
                            Text(
                                text = code.uppercase(),
                                style = KidSeeTypography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedLang == code) Color.White else WazeTextPrimary
                                ),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    KidSeeCard(
        onClick = onClick,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = KidSeeTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = KidSeeTypography.bodySmall.copy(
                        color = WazeTextSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
    }
}
