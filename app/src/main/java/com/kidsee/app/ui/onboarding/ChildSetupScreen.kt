package com.kidsee.app.ui.onboarding

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.core.ui.R
import com.kidsee.app.service.KidSeeDeviceAdmin
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeePillButton
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeWarmWhite

@Composable
fun ChildSetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current

    val isOverlayGranted = Settings.canDrawOverlays(context)
    val isAdminGranted = KidSeeDeviceAdmin.isAdminActive(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = KidSeeTypography.headlineLarge.copy(color = WazeBlue)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.setup_subtitle),
            style = KidSeeTypography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Accessibility Service
        PermissionStepCard(
            title = stringResource(R.string.perm_accessibility),
            description = stringResource(R.string.perm_accessibility_desc),
            icon = Icons.Default.Accessibility,
            isGranted = false,
            onAction = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Notification Listener
        PermissionStepCard(
            title = stringResource(R.string.perm_notification),
            description = stringResource(R.string.perm_notification_desc),
            icon = Icons.Default.NotificationsActive,
            isGranted = false,
            onAction = {
                context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Overlay (Display over other apps)
        PermissionStepCard(
            title = stringResource(R.string.perm_overlay),
            description = stringResource(R.string.perm_overlay_desc),
            icon = Icons.Default.Layers,
            isGranted = isOverlayGranted,
            onAction = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Device Admin
        PermissionStepCard(
            title = stringResource(R.string.perm_device_admin),
            description = stringResource(R.string.perm_device_admin_desc),
            icon = Icons.Default.AdminPanelSettings,
            isGranted = isAdminGranted,
            onAction = {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        KidSeeDeviceAdmin.getComponentName(context)
                    )
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "KidSee needs Device Admin to lock screen on parent demand or schedule"
                    )
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Usage Access
        PermissionStepCard(
            title = stringResource(R.string.perm_usage),
            description = stringResource(R.string.perm_usage_desc),
            icon = Icons.Default.QueryStats,
            isGranted = false,
            onAction = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        )

        Spacer(modifier = Modifier.height(30.dp))

        KidSeePillButton(
            text = stringResource(R.string.btn_finish_setup),
            onClick = onSetupComplete,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionStepCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onAction: () -> Unit
) {
    KidSeeCard(
        onClick = if (!isGranted) onAction else null,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isGranted) WazeGreen.copy(alpha = 0.15f) else WazeBlue.copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) WazeGreen else WazeBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = KidSeeTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = description,
                    style = KidSeeTypography.bodyMedium.copy(fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = WazeGreen,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = WazeBlue
                ) {
                    Text(
                        text = stringResource(R.string.perm_status_needed),
                        style = KidSeeTypography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
