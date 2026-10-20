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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.core.ui.R
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.components.KidSeeStatusBadge
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeGreenLight
import com.kidsee.core.ui.theme.WazeWarmWhite

@Composable
fun ChildActiveScreen(
    device: ChildDevice?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

        KidSeeStatusBadge(
            statusText = stringResource(R.string.status_online),
            backgroundColor = WazeGreenLight,
            textColor = WazeGreen
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                        text = "Active Protections",
                        style = KidSeeTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                ProtectionItem(title = "Safe Screen Monitoring")
                ProtectionItem(title = "Bedtime & Study Schedules")
                ProtectionItem(title = "Direct Parent Voice Notes")
            }
        }
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
