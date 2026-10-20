package com.kidsee.app

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import com.kidsee.app.ui.child.ChildActiveScreen
import com.kidsee.app.ui.components.UpdateDialog
import com.kidsee.app.ui.onboarding.ChildSetupScreen
import com.kidsee.app.ui.onboarding.OnboardingScreen
import com.kidsee.app.ui.parent.DeviceDetailScreen
import com.kidsee.app.ui.parent.ParentDashboardScreen
import com.kidsee.app.util.AppUpdateManager
import com.kidsee.app.util.UpdateState
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.domain.model.CommandType
import com.kidsee.core.domain.model.DeviceCommand
import com.kidsee.core.domain.model.UserRole
import com.kidsee.core.ui.theme.KidSeeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.kidsee.app.PIP_PLAY_PAUSE"
        const val ACTION_PIP_LOCK = "com.kidsee.app.PIP_LOCK"
    }

    private var isInPipMode by mutableStateOf(false)
    private var selectedDevice by mutableStateOf<ChildDevice?>(null)
    private val appUpdateManager by lazy { AppUpdateManager(this) }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = selectedDevice ?: return
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> {
                    val cmdType = if (device.isVideoPlaying) CommandType.MEDIA_PAUSE else CommandType.MEDIA_PLAY
                    sendCommand(device.id, cmdType)
                }
                ACTION_PIP_LOCK -> {
                    val cmdType = if (device.isScreenLocked) CommandType.UNLOCK_SCREEN else CommandType.LOCK_SCREEN
                    sendCommand(device.id, cmdType)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_LOCK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipReceiver, filter)
        }

        setContent {
            KidSeeTheme {
                KidSeeMainContent()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pipReceiver)
    }

    @Composable
    private fun KidSeeMainContent() {
        val app = KidSeeApp.instance
        val repository = app.stateManager.repository
        val scope = rememberCoroutineScope()

        val roleState by repository.userRoleFlow.collectAsState(initial = null)
        val isSetupComplete by repository.isSetupCompleteFlow.collectAsState(initial = false)
        val childState by app.stateManager.currentDeviceState.collectAsState()
        val updateState by appUpdateManager.updateState.collectAsState()
        var showUpdateDialog by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            appUpdateManager.checkForUpdates()
        }

        if (showUpdateDialog && (updateState is UpdateState.UpdateAvailable || updateState is UpdateState.Downloading)) {
            UpdateDialog(
                updateState = updateState,
                onDismiss = { showUpdateDialog = false },
                onUpdateClick = { releaseInfo ->
                    scope.launch {
                        appUpdateManager.downloadAndInstall(releaseInfo)
                    }
                }
            )
        }

        val familyDevicesFlow = remember {
            MutableStateFlow<List<ChildDevice>>(
                listOf(
                    ChildDevice(
                        id = "child_phone_1",
                        name = "Alex's Tablet",
                        deviceType = com.kidsee.core.domain.model.DeviceType.TABLET,
                        status = com.kidsee.core.domain.model.DeviceStatus.ONLINE,
                        currentAppLabel = "YouTube Kids",
                        isVideoPlaying = true,
                        currentVolume = 70,
                        batteryPercent = 84,
                        lastSeenTimestamp = System.currentTimeMillis()
                    ),
                    ChildDevice(
                        id = "child_tv_1",
                        name = "Kids Room TV",
                        deviceType = com.kidsee.core.domain.model.DeviceType.TV,
                        status = com.kidsee.core.domain.model.DeviceStatus.ONLINE,
                        currentAppLabel = "Netflix",
                        isVideoPlaying = true,
                        currentVolume = 45,
                        batteryPercent = 100,
                        lastSeenTimestamp = System.currentTimeMillis()
                    )
                )
            )
        }

        val isTv = isAndroidTv()

        if (roleState == null) {
            OnboardingScreen(
                onRoleSelected = { role ->
                    scope.launch {
                        repository.saveRole(role)
                        if (role == UserRole.CHILD) {
                            app.stateManager.startChildServices()
                        }
                    }
                },
                onLanguageChanged = { langCode ->
                    val appLocale = LocaleListCompat.forLanguageTags(langCode)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            )
        } else if (roleState == UserRole.CHILD) {
            if (!isSetupComplete) {
                ChildSetupScreen(
                    onSetupComplete = {
                        scope.launch { repository.completeSetup() }
                    }
                )
            } else {
                ChildActiveScreen(device = childState)
            }
        } else {
            // Parent Mode
            val activeDevice = selectedDevice
            if (activeDevice != null) {
                DeviceDetailScreen(
                    device = activeDevice,
                    onBackClick = { selectedDevice = null },
                    onPipClick = { enterPictureInPicture() },
                    onLockToggle = {
                        val cmdType = if (activeDevice.isScreenLocked) CommandType.UNLOCK_SCREEN else CommandType.LOCK_SCREEN
                        sendCommand(activeDevice.id, cmdType)
                        selectedDevice = activeDevice.copy(isScreenLocked = !activeDevice.isScreenLocked)
                    },
                    onMediaPlay = { sendCommand(activeDevice.id, CommandType.MEDIA_PLAY) },
                    onMediaPause = { sendCommand(activeDevice.id, CommandType.MEDIA_PAUSE) },
                    onMediaNext = { sendCommand(activeDevice.id, CommandType.MEDIA_NEXT) },
                    onMediaPrev = { sendCommand(activeDevice.id, CommandType.MEDIA_PREV) },
                    onVolumeChanged = { vol ->
                        sendCommand(activeDevice.id, CommandType.VOLUME_SET, vol.toString())
                    },
                    onPushToTalkStart = {
                        Timber.d("Push-to-Talk started for ${activeDevice.id}")
                    },
                    onPushToTalkStop = {
                        Timber.d("Push-to-Talk stopped for ${activeDevice.id}")
                    },
                    onSendVoiceNote = {
                        Timber.d("Send voice note requested for ${activeDevice.id}")
                    }
                )
            } else {
                ParentDashboardScreen(
                    devicesFlow = familyDevicesFlow,
                    onDeviceClick = { device -> selectedDevice = device },
                    onDevicePipClick = { device ->
                        selectedDevice = device
                        enterPictureInPicture()
                    },
                    onLockToggle = { device ->
                        val cmdType = if (device.isScreenLocked) CommandType.UNLOCK_SCREEN else CommandType.LOCK_SCREEN
                        sendCommand(device.id, cmdType)
                    },
                    onAddDeviceClick = {
                        Timber.d("Add device clicked")
                    },
                    isTv = isTv
                )
            }
        }
    }

    private fun sendCommand(deviceId: String, type: CommandType, payload: String? = null) {
        val app = KidSeeApp.instance
        val command = DeviceCommand(
            targetDeviceId = deviceId,
            type = type,
            payload = payload
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            app.stateManager.repository.sendCommand(command)
        }
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playPauseIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_PIP_PLAY_PAUSE), PendingIntent.FLAG_IMMUTABLE
            )
            val lockIntent = PendingIntent.getBroadcast(
                this, 1, Intent(ACTION_PIP_LOCK), PendingIntent.FLAG_IMMUTABLE
            )

            val actions = listOf(
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                    "Pause", "Pause child video", playPauseIntent
                ),
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_lock_lock),
                    "Lock", "Lock child screen", lockIntent
                )
            )

            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(actions)
                .build()

            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    // Android TV Remote Control / D-pad Key Handling
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val device = selectedDevice
        if (device != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    sendCommand(device.id, CommandType.MEDIA_PAUSE)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    sendCommand(device.id, CommandType.MEDIA_PLAY)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    sendCommand(device.id, CommandType.MEDIA_NEXT)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    sendCommand(device.id, CommandType.MEDIA_PREV)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_DPAD_UP -> {
                    sendCommand(device.id, CommandType.VOLUME_UP)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    sendCommand(device.id, CommandType.VOLUME_DOWN)
                    return true
                }
                KeyEvent.KEYCODE_PROG_RED -> {
                    val cmdType = if (device.isScreenLocked) CommandType.UNLOCK_SCREEN else CommandType.LOCK_SCREEN
                    sendCommand(device.id, cmdType)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isAndroidTv(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
