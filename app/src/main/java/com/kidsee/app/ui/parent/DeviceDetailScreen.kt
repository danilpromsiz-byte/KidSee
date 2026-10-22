package com.kidsee.app.ui.parent

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.network.lan.LanDiscoveryManager
import com.kidsee.core.ui.R
import com.kidsee.core.ui.components.KidSeeCard
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.components.KidSeePillButton
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeBorder
import com.kidsee.core.ui.theme.WazeGreen
import com.kidsee.core.ui.theme.WazeGreenLight
import com.kidsee.core.ui.theme.WazeOrange
import com.kidsee.core.ui.theme.WazePurple
import com.kidsee.core.ui.theme.WazeRed
import com.kidsee.core.ui.theme.WazeRedLight
import com.kidsee.core.ui.theme.WazeTextPrimary
import com.kidsee.core.ui.theme.WazeTextSecondary
import com.kidsee.core.ui.theme.WazeWarmWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun DeviceDetailScreen(
    device: ChildDevice,
    lanDiscoveryManager: LanDiscoveryManager? = null,
    onRequestStream: () -> Unit = {},
    onReconnect: () -> Unit = {},
    onBackClick: () -> Unit,
    onPipClick: () -> Unit,
    onLockToggle: () -> Unit,
    onMediaPlay: () -> Unit,
    onMediaPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onPushToTalkStart: () -> Unit,
    onPushToTalkStop: () -> Unit
) {
    var lastNonZeroVolume by remember { mutableStateOf(if (device.currentVolume > 0) device.currentVolume else 50) }
    if (device.currentVolume > 0) {
        lastNonZeroVolume = device.currentVolume
    }
    val isAudioMuted = device.currentVolume == 0
    var isTalkingLive by remember { mutableStateOf(false) }
    var volumeSlider by remember(device.currentVolume) { mutableFloatStateOf(device.currentVolume.toFloat()) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Intercept hardware/system back button to exit fullscreen or return to devices list
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBackClick()
        }
    }

    // Real-time streaming loop for child screen frames over Wi-Fi
    LaunchedEffect(device.id, lanDiscoveryManager) {
        while (isActive) {
            var receivedFrames = false
            try {
                lanDiscoveryManager?.streamScreenFrames(device.id)?.collect { frameBytes ->
                    if (frameBytes.isNotEmpty()) {
                        val bmp = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                        if (bmp != null) {
                            currentBitmap = bmp
                            receivedFrames = true
                        }
                    }
                }
            } catch (_: Exception) {}

            // Fallback: poll a single frame if stream endpoint was not supported or disconnected
            if (!receivedFrames && isActive) {
                val frameBytes = withContext(Dispatchers.IO) {
                    lanDiscoveryManager?.fetchScreenFrame(device.id)
                }
                if (frameBytes != null && frameBytes.isNotEmpty()) {
                    val bmp = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                    if (bmp != null) {
                        currentBitmap = bmp
                    }
                }
            }

            if (!device.isOnline) {
                currentBitmap = null
            }
            delay(500)
        }
    }

    // Fullscreen Mode
    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val bmp = currentBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.screen_mirror_content_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    KidSeeMascot(size = 64.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(if (device.isOnline) R.string.screen_stream_not_active else R.string.screen_stream_offline),
                        color = Color.White,
                        style = KidSeeTypography.titleMedium
                    )
                }
            }

            // Exit Fullscreen Button in top-left
            IconButton(
                onClick = { isFullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = stringResource(R.string.btn_exit_fullscreen),
                    tint = Color.White
                )
            }
        }
        return
    }

    val isVideoActive = device.isVideoPlaying ||
            (device.currentPackage?.contains("youtube", ignoreCase = true) == true) ||
            (device.currentPackage?.contains("video", ignoreCase = true) == true) ||
            (device.currentPackage?.contains("rutube", ignoreCase = true) == true) ||
            (device.currentPackage?.contains("twitch", ignoreCase = true) == true) ||
            !device.currentMediaTitle.isNullOrEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WazeWarmWhite)
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        // Compact Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name.ifEmpty { stringResource(R.string.device_detail_default_name) },
                    style = KidSeeTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = device.currentAppLabel ?: stringResource(R.string.app_idle),
                    style = KidSeeTypography.bodySmall.copy(color = WazeTextSecondary),
                    maxLines = 1
                )
            }

            // Reconnect Device Button
            IconButton(onClick = onReconnect) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.btn_reconnect),
                    tint = WazeBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            // PiP Button
            IconButton(onClick = onPipClick) {
                Icon(
                    imageVector = Icons.Default.PictureInPicture,
                    contentDescription = stringResource(R.string.pip_button),
                    tint = WazeBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Screen Video View (Live Screen Stream Canvas)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val bmp = currentBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.screen_mirror_content_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Inactive stream placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(14.dp)
                ) {
                    KidSeeMascot(size = 48.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (device.isOnline) R.string.screen_stream_not_active else R.string.screen_stream_offline
                        ),
                        color = Color.White,
                        style = KidSeeTypography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.screen_stream_prompt_desc),
                        color = Color.White.copy(alpha = 0.7f),
                        style = KidSeeTypography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    if (device.isOnline) {
                        Spacer(modifier = Modifier.height(8.dp))
                        KidSeePillButton(
                            text = stringResource(R.string.btn_request_stream),
                            onClick = onRequestStream,
                            backgroundColor = WazeBlue,
                            height = 34.dp
                        )
                    }
                }
            }

            // Live streaming red badge in top-left
            if (currentBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = WazeRed,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.screen_stream_active_badge),
                            style = KidSeeTypography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Live Audio indicator pill in top-right
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isAudioMuted) Color.Black.copy(alpha = 0.6f) else WazeGreen.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(if (isAudioMuted) R.string.audio_muted else R.string.audio_on),
                        style = KidSeeTypography.labelSmall.copy(color = Color.White)
                    )
                }
            }

            // Fullscreen Button in bottom-right
            IconButton(
                onClick = { isFullscreen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = stringResource(R.string.btn_fullscreen),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Screen Lock Action Card (Text is cleanly placed UNDER the button)
        KidSeeCard(
            backgroundColor = if (device.isScreenLocked) WazeGreenLight else WazeRedLight,
            borderColor = if (device.isScreenLocked) WazeGreen else WazeRed,
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KidSeePillButton(
                    text = stringResource(
                        if (device.isScreenLocked) R.string.btn_unlock else R.string.btn_lock
                    ),
                    onClick = onLockToggle,
                    backgroundColor = if (device.isScreenLocked) WazeGreen else WazeRed,
                    icon = {
                        Icon(
                            imageVector = if (device.isScreenLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    height = 42.dp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(
                        if (device.isScreenLocked) R.string.device_locked_title else R.string.block_screen_now_title
                    ),
                    style = KidSeeTypography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (device.isScreenLocked) WazeGreen else WazeRed
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Audio & Push-To-Talk Control Bar (Unified single voice button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mute / Unmute Sound (Sends instant volume 0 or restores previous level)
            KidSeePillButton(
                text = stringResource(if (isAudioMuted) R.string.btn_hear_sound else R.string.btn_mute_sound),
                onClick = {
                    if (isAudioMuted) {
                        val restore = if (lastNonZeroVolume > 0) lastNonZeroVolume else 50
                        volumeSlider = restore.toFloat()
                        onVolumeChanged(restore)
                    } else {
                        lastNonZeroVolume = if (device.currentVolume > 0) device.currentVolume else 50
                        volumeSlider = 0f
                        onVolumeChanged(0)
                    }
                },
                backgroundColor = if (isAudioMuted) WazeBorder else WazeBlue,
                contentColor = if (isAudioMuted) WazeTextPrimary else Color.White,
                icon = {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f),
                height = 46.dp
            )

            // Push-to-Talk (Hold to speak live to child)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
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
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(if (isTalkingLive) R.string.status_recording_voice else R.string.btn_hold_to_talk),
                        style = KidSeeTypography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Video Notification Banner (ONLY shown when video is detected)
        if (isVideoActive) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = WazeBlue.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, WazeBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = WazeBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.status_video_playing,
                            device.currentMediaTitle ?: device.currentAppLabel ?: ""
                        ),
                        style = KidSeeTypography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = WazeBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Video Playback Controls Card (Prev, Pause, Play, Next)
            KidSeeCard(elevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.media_controls_title),
                        style = KidSeeTypography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onMediaPrev,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F4F8))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = stringResource(R.string.btn_prev),
                                tint = WazeBlue
                            )
                        }

                        IconButton(
                            onClick = onMediaPause,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(WazeBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = stringResource(R.string.btn_pause),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = onMediaPlay,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(WazeGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.btn_play),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = onMediaNext,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F4F8))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.btn_next),
                                tint = WazeBlue
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Volume Control Card
        KidSeeCard(elevation = 2.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val next = (volumeSlider - 10).coerceAtLeast(0f)
                            volumeSlider = next
                            onVolumeChanged(next.toInt())
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = null,
                            tint = WazeTextSecondary
                        )
                    }

                    Slider(
                        value = volumeSlider,
                        onValueChange = {
                            volumeSlider = it
                        },
                        onValueChangeFinished = {
                            onVolumeChanged(volumeSlider.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = WazeBlue,
                            activeTrackColor = WazeBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            val next = (volumeSlider + 10).coerceAtMost(100f)
                            volumeSlider = next
                            onVolumeChanged(next.toInt())
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = WazeTextSecondary
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.volume_label, volumeSlider.toInt()),
                    style = KidSeeTypography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
