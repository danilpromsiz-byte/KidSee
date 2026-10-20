package com.kidsee.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.core.ui.R
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.components.KidSeePillButton
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeBorder
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceDialog(
    familyId: String,
    onUpdateFamilyId: (String) -> Unit,
    onRescan: () -> Unit,
    onProbeIp: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    if (showEditDialog) {
        var tempCode by remember { mutableStateOf(familyId) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.family_code_title),
                    style = KidSeeTypography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.add_device_family_desc),
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
                        showEditDialog = false
                        Toast.makeText(context, context.getString(R.string.family_code_updated_toast, tempCode), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold, color = WazeBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = WazeTextSecondary)
                }
            }
        )
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = WazeCardBg,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = WazeBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = WazeBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.add_device_title),
                                style = KidSeeTypography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = WazeTextPrimary
                                )
                            )
                            Text(
                                text = stringResource(R.string.add_device_subtitle),
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

                Spacer(modifier = Modifier.height(18.dp))

                KidSeeMascot(size = 76.dp)

                Spacer(modifier = Modifier.height(14.dp))

                // Family Code Box
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, WazeBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.family_code_label),
                                style = KidSeeTypography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = WazeTextSecondary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = familyId.ifEmpty { "KID-7777" },
                                style = KidSeeTypography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = WazeBlue,
                                    fontSize = 20.sp
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Edit Code Button
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF1EFEA),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { showEditDialog = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit code",
                                        tint = WazeTextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Copy Code Button
                            Surface(
                                shape = CircleShape,
                                color = WazeBlue.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("KidSee Family Code", familyId.ifEmpty { "KID-7777" })
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, context.getString(R.string.code_copied_toast), Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        tint = WazeBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Steps Instructions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StepInstructionRow(
                        stepNum = "1",
                        text = stringResource(R.string.add_device_step1)
                    )
                    StepInstructionRow(
                        stepNum = "2",
                        text = stringResource(R.string.add_device_step2)
                    )
                    StepInstructionRow(
                        stepNum = "3",
                        text = stringResource(R.string.add_device_step3)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Direct IP connect card (fallback for strict routers / AP isolation)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, WazeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.add_device_by_ip),
                            style = KidSeeTypography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = WazeTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = manualIp,
                                onValueChange = { manualIp = it.trim() },
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.ip_placeholder), fontSize = 13.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            KidSeePillButton(
                                text = stringResource(R.string.btn_find),
                                onClick = {
                                    if (manualIp.isNotBlank()) {
                                        onProbeIp?.invoke(manualIp)
                                        Toast.makeText(context, context.getString(R.string.direct_ip_probe_toast, manualIp), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                height = 48.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wi-Fi Discovery Status Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WazeGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = WazeGreen,
                            modifier = Modifier
                                .size(20.dp)
                                .alpha(alphaAnim)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.searching_wifi_network),
                            style = KidSeeTypography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = WazeGreen,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rescan Button
                KidSeePillButton(
                    text = stringResource(R.string.btn_rescan),
                    onClick = {
                        onRescan()
                        Toast.makeText(context, context.getString(R.string.searching_wifi_network), Toast.LENGTH_SHORT).show()
                    },
                    backgroundColor = Color.White,
                    contentColor = WazeBlue,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                KidSeePillButton(
                    text = stringResource(R.string.btn_done),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StepInstructionRow(stepNum: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = WazeBlue,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNum,
                    style = KidSeeTypography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = KidSeeTypography.bodyMedium.copy(
                color = WazeTextPrimary,
                fontSize = 13.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
