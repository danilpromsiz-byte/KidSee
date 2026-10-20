package com.kidsee.app.ui.parent

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kidsee.core.ui.R
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.components.KidSeePillButton
import com.kidsee.core.ui.components.KidSeeStatusBadge
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeBorder
import com.kidsee.core.ui.theme.WazeCardBg
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeGreenLight
import com.kidsee.core.ui.theme.WazeOrange
import com.kidsee.core.ui.theme.WazeOrangeLight
import com.kidsee.core.ui.theme.WazePurple
import com.kidsee.core.ui.theme.WazeRed
import com.kidsee.core.ui.theme.WazeRedLight
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary
import com.kidsee.core.ui.theme.WazeWarmWhite
import org.webrtc.SurfaceViewRenderer

@Composable
fun DeviceDetailScreen(
    device: ChildDevice,
    onBackClick: () -> Unit,
    onPipClick: () -> Unit,
    onLockToggle: () -> Unit,
    onMediaPlay: () -> Unit,
    onMediaPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onPushToTalkStart: () -> Unit,
    onPushToTalkStop: () -> Unit,
    onSendVoiceNote: () -> Unit
) {
    var isAudioMuted by remember { mutableStateOf(false) }
    var isTalkingLive by remember { mutableStateOf(false) }
    var volumeSlider by remember(device.currentVolume) { mutableFloatStateOf(device.currentVolume.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name.ifEmpty { "Child Device" },
                    style = KidSeeTypography.titleLarge
                )
                Text(
                    text = device.currentAppLabel ?: stringResource(R.string.app_idle),
                    style = KidSeeTypography.bodyMedium
                )
            }

            // PiP Button
            IconButton(onClick = onPipClick) {
                Icon(
                    imageVector = Icons.Default.PictureInPicture,
                    contentDescription = "PiP",
                    tint = WazeBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Screen Video View (WebRTC Live Stream Canvas)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay indicator if stream is starting or idle
            if (!device.isVideoPlaying && !device.isWebRtcStreamActive) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    KidSeeMascot(size = 64.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (device.isOnline) "Ready to Stream" else "Device Offline",
                        color = Color.White,
                        style = KidSeeTypography.titleMedium
                    )
                }
            }

            // Live Audio indicator pill
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isAudioMuted) Color.Black.copy(alpha = 0.6f) else WazeGreen.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAudioMuted) "Muted" else "Audio ON",
                        style = KidSeeTypography.labelSmall.copy(color = Color.White)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio & Push-To-Talk Control Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mute / Unmute Sound
            KidSeePillButton(
                text = if (isAudioMuted) "Hear Sound" else "Mute Sound",
                onClick = { isAudioMuted = !isAudioMuted },
                backgroundColor = if (isAudioMuted) WazeBorder else WazeBlue,
                contentColor = if (isAudioMuted) WazeTextPrimary else Color.White,
                icon = {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.weight(1f),
                height = 50.dp
            )

            // Push-to-Talk (Hold to speak live to child)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (isTalkingLive) WazeGreen else WazePurple)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isTalkingLive = true
                                onPushToTalkStart()
                                tryAwaitRelease()
                                isTalkingLive = false
                                onPushToTalkStop()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTalkingLive) "Speaking…" else "Hold to Talk",
                        style = KidSeeTypography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Media Controls Card (Play, Pause, Skip, Prev, Volume)
        KidSeeCard(elevation = 3.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Video & Media Controls",
                    style = KidSeeTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Playback Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onMediaPrev,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F4F8))
                    ) {
                        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev", tint = WazeBlue)
                    }

                    IconButton(
                        onClick = onMediaPause,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(WazeBlue)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    IconButton(
                        onClick = onMediaPlay,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(WazeGreen)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    IconButton(
                        onClick = onMediaNext,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F4F8))
                    ) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next", tint = WazeBlue)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Volume Control Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.VolumeDown, contentDescription = null, tint = WazeTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = volumeSlider,
                        onValueChange = {
                            volumeSlider = it
                            onVolumeChanged(it.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = WazeBlue,
                            activeTrackColor = WazeBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = WazeTextSecondary)
                }

                Text(
                    text = stringResource(R.string.volume_label, volumeSlider.toInt()),
                    style = KidSeeTypography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Screen Lock Action Card
        KidSeeCard(
            backgroundColor = if (device.isScreenLocked) WazeGreenLight else WazeRedLight,
            borderColor = if (device.isScreenLocked) WazeGreen else WazeRed,
            elevation = 3.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (device.isScreenLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (device.isScreenLocked) WazeGreen else WazeRed,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (device.isScreenLocked) "Device is Locked" else "Block Screen Now",
                        style = KidSeeTypography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (device.isScreenLocked) WazeGreen else WazeRed
                        )
                    )
                    Text(
                        text = if (device.isScreenLocked) "Tap to unlock child screen" else "Instantly blocks access & shows break screen",
                        style = KidSeeTypography.bodyMedium.copy(fontSize = 12.sp)
                    )
                }

                KidSeePillButton(
                    text = if (device.isScreenLocked) "Unlock" else "Lock",
                    onClick = onLockToggle,
                    backgroundColor = if (device.isScreenLocked) WazeGreen else WazeRed,
                    height = 44.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Send Voice Note Button
        KidSeePillButton(
            text = stringResource(R.string.send_voice_note),
            onClick = onSendVoiceNote,
            backgroundColor = WazeOrange,
            icon = {
                Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            height = 54.dp
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
