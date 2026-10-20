package com.kidsee.app.ui.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.app.KidSeeApp
import com.kidsee.core.ui.R
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.components.KidSeeStatusBadge
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeGreenLight
import com.kidsee.core.ui.theme.WazeOrange
import com.kidsee.core.ui.theme.WazeOrangeLight
import com.kidsee.core.ui.theme.WazeTextSecondary
import com.kidsee.core.ui.theme.WazeWarmWhite

@Composable
fun ChildActiveScreen(
    device: ChildDevice?,
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Parent Settings",
                    tint = WazeTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        KidSeeMascot(size = 120.dp, isHappy = true)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.child_active_title),
            style = KidSeeTypography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                color = WazeBlue
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.child_active_desc),
            style = KidSeeTypography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        val isDefaultFamily = device?.familyId.isNullOrEmpty() || device?.familyId == "KID-7777"
        val localIp = remember { KidSeeApp.instance.stateManager.lanDiscoveryManager.getPrimaryLocalIp() }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KidSeeStatusBadge(
                statusText = stringResource(R.string.status_online),
                backgroundColor = WazeGreenLight,
                textColor = WazeGreen
            )

            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isDefaultFamily) WazeOrangeLight else WazeBlue.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (isDefaultFamily) {
                        stringResource(R.string.child_ready_connect)
                    } else {
                        stringResource(R.string.family_code_desc, device?.familyId ?: "")
                    },
                    style = KidSeeTypography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDefaultFamily) WazeOrange else WazeBlue
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        if (localIp != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = Color(0xFFF1EFEA)
            ) {
                Text(
                    text = stringResource(R.string.wifi_ip_badge, localIp),
                    style = KidSeeTypography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = WazeBlue
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isDefaultFamily) {
            Text(
                text = stringResource(R.string.child_waiting_parent),
                style = KidSeeTypography.bodySmall.copy(color = WazeTextSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Protection summary card
        KidSeeCard(elevation = 3.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = WazeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.active_protections),
                        style = KidSeeTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                ProtectionItem(title = stringResource(R.string.prot_screen_mon))
                ProtectionItem(title = stringResource(R.string.prot_schedules))
                ProtectionItem(title = stringResource(R.string.prot_voice_notes))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProtectionItem(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = WazeGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = KidSeeTypography.bodyMedium
        )
    }
}
