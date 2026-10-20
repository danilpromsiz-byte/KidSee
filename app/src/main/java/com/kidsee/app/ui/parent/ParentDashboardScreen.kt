package com.kidsee.app.ui.parent

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.core.ui.R
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.domain.model.DeviceType
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.components.KidSeePillButton
import com.kidsee.core.ui.components.KidSeeStatusBadge
import com.kidsee.core.ui.components.TvFocusableCard
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeGreenLight
import com.kidsee.core.ui.theme.WazeOrange
import com.kidsee.core.ui.theme.WazeOrangeLight
import com.kidsee.core.ui.theme.WazeRed
import com.kidsee.core.ui.theme.WazeRedLight
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary
import com.kidsee.core.ui.theme.WazeWarmWhite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ParentDashboardScreen(
    devicesFlow: Flow<List<ChildDevice>>,
    onDeviceClick: (ChildDevice) -> Unit,
    onDevicePipClick: (ChildDevice) -> Unit,
    onLockToggle: (ChildDevice) -> Unit,
    onAddDeviceClick: () -> Unit,
    onCheckUpdateClick: () -> Unit = {},
    onRescanClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    currentFamilyId: String = "KID-7777",
    onAdoptDevice: ((ChildDevice) -> Unit)? = null,
    isTv: Boolean = false
) {
    val devices by devicesFlow.collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Waze Style Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KidSeeMascot(size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = KidSeeTypography.headlineLarge.copy(
                            color = WazeBlue,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = stringResource(R.string.dashboard_subtitle),
                        style = KidSeeTypography.bodyMedium
                    )
                }

                // Rescan Wi-Fi Devices Button
                IconButton(onClick = onRescanClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan devices",
                        tint = WazeBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Check for Updates Button
                IconButton(onClick = onCheckUpdateClick) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Check for updates",
                        tint = WazeBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Lock Parent Mode Button
                IconButton(onClick = onLockClick) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Parent Controls",
                        tint = WazeTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Settings Button
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = WazeTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (devices.isEmpty()) {
                // Empty state
                EmptyDevicesView(
                    onAddDeviceClick = onAddDeviceClick,
                    onRescanClick = onRescanClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(devices, key = { it.id }) { device ->
                        if (isTv) {
                            TvFocusableCard(onClick = { onDeviceClick(device) }) {
                                DeviceCardContent(
                                    device = device,
                                    currentFamilyId = currentFamilyId,
                                    onAdoptClick = onAdoptDevice?.let { { it(device) } },
                                    onPipClick = { onDevicePipClick(device) },
                                    onLockClick = { onLockToggle(device) }
                                )
                            }
                        } else {
                            KidSeeCard(
                                elevation = 4.dp,
                                onClick = { onDeviceClick(device) }
                            ) {
                                DeviceCardContent(
                                    device = device,
                                    currentFamilyId = currentFamilyId,
                                    onAdoptClick = onAdoptDevice?.let { { it(device) } },
                                    onPipClick = { onDevicePipClick(device) },
                                    onLockClick = { onLockToggle(device) }
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        KidSeePillButton(
                            text = stringResource(R.string.connect_another_device),
                            onClick = onAddDeviceClick,
                            backgroundColor = Color.White,
                            contentColor = WazeBlue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCardContent(
    device: ChildDevice,
    currentFamilyId: String,
    onAdoptClick: (() -> Unit)?,
    onPipClick: () -> Unit,
    onLockClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device Type Icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WazeBlue.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (device.deviceType) {
                        DeviceType.TV -> Icons.Default.Tv
                        DeviceType.TABLET -> Icons.Default.Tablet
                        DeviceType.PHONE -> Icons.Default.Smartphone
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = WazeBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name.ifEmpty { "Child Device" },
                    style = KidSeeTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = WazeTextSecondary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${device.batteryPercent}%",
                        style = KidSeeTypography.labelSmall
                    )
                }
            }

            // Status Badge
            if (device.isScreenLocked) {
                KidSeeStatusBadge(
                    statusText = stringResource(R.string.status_locked),
                    backgroundColor = WazeRedLight,
                    textColor = WazeRed
                )
            } else if (device.isVideoPlaying) {
                KidSeeStatusBadge(
                    statusText = stringResource(R.string.watching_video),
                    backgroundColor = WazeOrangeLight,
                    textColor = WazeOrange
                )
            } else if (device.isOnline) {
                KidSeeStatusBadge(
                    statusText = stringResource(R.string.status_online),
                    backgroundColor = WazeGreenLight,
                    textColor = WazeGreen
                )
            } else {
                KidSeeStatusBadge(
                    statusText = stringResource(R.string.status_offline),
                    backgroundColor = Color(0xFFF0F0F0),
                    textColor = Color(0xFF888888)
                )
            }
        }

        // New / Unpaired LAN Device Banner with 1-click Link button
        if (device.familyId != currentFamilyId) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WazeOrangeLight, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.found_on_wifi),
                    style = KidSeeTypography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = WazeOrange
                    )
                )
                if (onAdoptClick != null) {
                    TextButton(
                        onClick = onAdoptClick,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.link_to_family),
                            style = KidSeeTypography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = WazeBlue
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Activity Bar: App & Media Title
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF5F4EE),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (device.isVideoPlaying) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = WazeOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = device.currentAppLabel ?: stringResource(R.string.app_idle),
                    style = KidSeeTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = WazeTextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Controls: PiP & Lock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PiP Button
            IconButton(onClick = onPipClick) {
                Icon(
                    imageVector = Icons.Default.PictureInPicture,
                    contentDescription = "PiP",
                    tint = WazeBlue
                )
            }

            // Lock / Unlock Button
            IconButton(onClick = onLockClick) {
                Icon(
                    imageVector = if (device.isScreenLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = if (device.isScreenLocked) WazeGreen else WazeRed
                )
            }
        }
    }
}

@Composable
private fun EmptyDevicesView(
    onAddDeviceClick: () -> Unit,
    onRescanClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        KidSeeMascot(size = 90.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.no_devices_title),
            style = KidSeeTypography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_devices_desc),
            style = KidSeeTypography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        KidSeePillButton(
            text = stringResource(R.string.btn_add_device),
            onClick = onAddDeviceClick,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onRescanClick) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = WazeBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.btn_rescan),
                style = KidSeeTypography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WazeBlue
                )
            )
        }
    }
}
