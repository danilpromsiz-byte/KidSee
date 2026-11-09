package com.kidsee.app.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.app.util.UpdateState
import com.kidsee.core.ui.R
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeOrange
import com.kidsee.core.ui.theme.WazeRed
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentVersion: String,
    updateState: UpdateState,
    familyId: String = "KID-7777",
    currentLanguage: String = "ru",
    isBiometricSupported: Boolean = false,
    isBiometricEnabled: Boolean = true,
    lockType: String = "PIN",
    parentPattern: String = "",
    onToggleBiometric: (Boolean) -> Unit = {},
    onLockTypeChanged: (String) -> Unit = {},
    onSavePattern: (String) -> Unit = {},
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
    var showSetPatternDialog by remember { mutableStateOf(false) }

    if (showEditFamilyDialog) {
        var tempCode by remember { mutableStateOf(familyId) }
        AlertDialog(
            onDismissRequest = { showEditFamilyDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.family_code_title),
                    style = KidSeeTypography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.family_code_dialog_desc),
                        style = KidSeeTypography.bodyMedium.copy(color = WazeTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempCode,
                        onValueChange = { tempCode = it.trim().uppercase() },
                        singleLine = true,
                        placeholder = { Text("SEMYA-1 / 7777") },
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
                    Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold, color = WazeBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditFamilyDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = WazeTextSecondary)
                }
            }
        )
    }

    if (showSetPatternDialog) {
        var patternError by remember { mutableStateOf(false) }
        var patternSuccess by remember { mutableStateOf(false) }

        BasicAlertDialog(onDismissRequest = { showSetPatternDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = WazeCardBg,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    KidSeeMascot(size = 64.dp, isHappy = !patternError)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.pattern_setup_title),
                        style = KidSeeTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (patternError) {
                            stringResource(R.string.pattern_too_short)
                        } else {
                            stringResource(R.string.pattern_setup_desc)
                        },
                        style = KidSeeTypography.bodySmall.copy(
                            color = if (patternError) WazeRed else WazeTextSecondary
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PatternLockView(
                        modifier = Modifier.size(260.dp),
                        isError = patternError,
                        isSuccess = patternSuccess,
                        onPatternEntered = { pattern ->
                            if (pattern.length < 4) {
                                patternError = true
                                scope.launch {
                                    delay(900)
                                    patternError = false
                                }
                            } else {
                                patternSuccess = true
                                onSavePattern(pattern)
                                onLockTypeChanged("PATTERN")
                                Toast.makeText(context, context.getString(R.string.pattern_saved_toast), Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    delay(300)
                                    showSetPatternDialog = false
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { showSetPatternDialog = false }) {
                        Text(stringResource(R.string.btn_cancel), color = WazeTextSecondary)
                    }
                }
            }
        }
    }

    val languages = listOf(
        "ru" to "Русский",
        "uk" to "Українська",
        "en" to "English",
        "es" to "Español",
        "fr" to "Français"
    )

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = WazeCardBg,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
                                text = stringResource(R.string.settings_title),
                                style = KidSeeTypography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = WazeTextPrimary
                                )
                            )
                            Text(
                                text = "KidScreen v$currentVersion",
                                style = KidSeeTypography.labelSmall.copy(color = WazeTextSecondary)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = WazeTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Diagnostics & Health Card
                SettingsActionCard(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.system_diagnostics_title),
                    subtitle = stringResource(R.string.diag_service_active),
                    iconTint = WazeGreen,
                    trailing = {
                        Text(
                            text = "OK",
                            style = KidSeeTypography.labelSmall.copy(
                                color = WazeGreen,
                                fontWeight = FontWeight.Black
                            )
                        )
                    },
                    onClick = {
                        Toast.makeText(
                            context,
                            "${context.getString(R.string.diag_service_active)}\n${context.getString(R.string.diag_network_ok)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Check for Updates Card (all updates inside Settings)
                SettingsActionCard(
                    icon = Icons.Default.SystemUpdate,
                    title = stringResource(R.string.check_updates_title),
                    subtitle = when (updateState) {
                        is UpdateState.Checking -> stringResource(R.string.update_checking)
                        is UpdateState.UpdateAvailable -> stringResource(R.string.update_available_badge)
                        is UpdateState.UpToDate -> stringResource(R.string.update_up_to_date)
                        else -> stringResource(R.string.check_updates_desc)
                    },
                    iconTint = WazeBlue,
                    trailing = {
                        if (updateState is UpdateState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = WazeBlue,
                                strokeWidth = 2.dp
                            )
                        } else if (updateState is UpdateState.UpdateAvailable) {
                            Surface(
                                shape = CircleShape,
                                color = WazeGreen,
                                modifier = Modifier.size(10.dp)
                            ) {}
                        }
                    },
                    onClick = onCheckUpdate
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Biometric Unlock Switch (if supported by hardware)
                if (isBiometricSupported) {
                    SettingsActionCard(
                        icon = Icons.Default.Fingerprint,
                        title = stringResource(R.string.biometric_settings_title),
                        subtitle = stringResource(R.string.biometric_settings_desc),
                        iconTint = WazeBlue,
                        trailing = {
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = onToggleBiometric,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = WazeBlue
                                )
                            )
                        },
                        onClick = { onToggleBiometric(!isBiometricEnabled) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 4. Change PIN
                SettingsActionCard(
                    icon = Icons.Default.Password,
                    title = stringResource(R.string.parent_pin_title),
                    subtitle = stringResource(R.string.parent_pin_desc),
                    iconTint = WazeGreen,
                    onClick = onChangePin
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Pattern Lock Setup
                SettingsActionCard(
                    icon = Icons.Default.GridOn,
                    title = stringResource(R.string.lock_type_pattern),
                    subtitle = if (parentPattern.isNotBlank()) {
                        stringResource(R.string.pattern_saved_toast)
                    } else {
                        stringResource(R.string.pattern_setup_desc)
                    },
                    iconTint = WazeOrange,
                    trailing = {
                        if (lockType == "PATTERN") {
                            Text(
                                text = "ON",
                                style = KidSeeTypography.labelSmall.copy(
                                    color = WazeOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    },
                    onClick = { showSetPatternDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. Default Lock Method Selector (PIN vs Pattern)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.lock_type_title),
                        style = KidSeeTypography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = WazeTextSecondary
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (lockType == "PIN") WazeBlue else Color(0xFFF3F1EA),
                            modifier = Modifier.clickable { onLockTypeChanged("PIN") }
                        ) {
                            Text(
                                text = stringResource(R.string.lock_type_pin),
                                style = KidSeeTypography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (lockType == "PIN") Color.White else WazeTextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (lockType == "PATTERN") WazeBlue else Color(0xFFF3F1EA),
                            modifier = Modifier.clickable {
                                if (parentPattern.isBlank()) {
                                    showSetPatternDialog = true
                                } else {
                                    onLockTypeChanged("PATTERN")
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.lock_type_pattern),
                                style = KidSeeTypography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (lockType == "PATTERN") Color.White else WazeTextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 7. Family Code
                SettingsActionCard(
                    icon = Icons.Default.Group,
                    title = stringResource(R.string.family_code_title),
                    subtitle = stringResource(R.string.family_code_desc, familyId),
                    iconTint = WazeBlue,
                    onClick = { showEditFamilyDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 8. Switch Role (Parent <-> Child)
                SettingsActionCard(
                    icon = Icons.Default.SwapHoriz,
                    title = stringResource(R.string.switch_role_title),
                    subtitle = stringResource(R.string.switch_role_desc),
                    iconTint = Color(0xFFFF9800),
                    onClick = onSwitchRole
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 9. Language Selector
                Text(
                    text = stringResource(R.string.app_language_title),
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
                    for ((code, _) in languages) {
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
