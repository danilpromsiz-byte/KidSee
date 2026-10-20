package com.kidsee.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.app.util.UpdateState
import com.kidsee.core.network.updater.ReleaseInfo
import com.kidsee.core.ui.R
import com.kidsee.core.ui.components.KidSeePillButton
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onUpdateClick: (ReleaseInfo) -> Unit
) {
    val releaseInfo = when (updateState) {
        is UpdateState.UpdateAvailable -> updateState.releaseInfo
        else -> null
    }

    if (releaseInfo == null && updateState !is UpdateState.Downloading) return

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = WazeCardBg,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = WazeBlue,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.update_available_title),
                    style = KidSeeTypography.headlineMedium.copy(
                        color = WazeBlue,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (releaseInfo != null) {
                    Text(
                        text = releaseInfo.tagName,
                        style = KidSeeTypography.titleMedium.copy(
                            color = WazeGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    if (releaseInfo.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = releaseInfo.releaseNotes,
                            style = KidSeeTypography.bodyMedium.copy(
                                color = WazeTextSecondary
                            ),
                            maxLines = 6
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (updateState) {
                    is UpdateState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { updateState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = WazeBlue,
                                trackColor = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.downloading_progress, updateState.progressPercent),
                                style = KidSeeTypography.bodySmall.copy(color = WazeTextSecondary)
                            )
                        }
                    }

                    is UpdateState.UpdateAvailable -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = stringResource(R.string.btn_later),
                                    color = WazeTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            KidSeePillButton(
                                text = stringResource(R.string.btn_update_now),
                                onClick = { onUpdateClick(updateState.releaseInfo) }
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}
