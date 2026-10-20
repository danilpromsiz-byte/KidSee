package com.kidsee.app.state

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.kidsee.app.service.KidSeeDeviceAdmin
import com.kidsee.app.service.KidSeeMediaMonitorService
import com.kidsee.app.service.KidSeeOverlayLockService
import com.kidsee.app.service.ScreenCaptureService
import com.kidsee.app.service.VoiceMessageManager
import com.kidsee.core.data.local.KidSeePreferences
import com.kidsee.core.data.repository.KidSeeRepository
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.domain.model.CommandType
import com.kidsee.core.domain.model.DeviceCommand
import com.kidsee.core.domain.model.DeviceStatus
import com.kidsee.core.domain.model.DeviceType
import com.kidsee.core.domain.model.UserRole
import com.kidsee.core.network.FirebaseClient
import com.kidsee.core.network.lan.LanDiscoveryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar

class KidSeeStateManager(
    private val context: Context,
    val lanDiscoveryManager: LanDiscoveryManager = LanDiscoveryManager(context),
    val repository: KidSeeRepository = KidSeeRepository(
        KidSeePreferences(context),
        FirebaseClient(),
        lanDiscoveryManager
    )
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val voiceMessageManager = VoiceMessageManager(context)

    private var heartbeatJob: Job? = null
    private var commandJob: Job? = null
    private var voiceJob: Job? = null
    private var scheduleJob: Job? = null

    private var currentPackage: String? = null
    private var currentAppLabel: String? = null
    private var isVideo: Boolean = false
    private var isScreenLocked: Boolean = false

    private val _currentDeviceState = MutableStateFlow<ChildDevice?>(null)
    val currentDeviceState: StateFlow<ChildDevice?> = _currentDeviceState.asStateFlow()

    var onStreamRequested: (() -> Unit)? = null

    fun init() {
        scope.launch {
            val role = repository.userRoleFlow.firstOrNull()
            if (role == UserRole.CHILD) {
                startChildServices()
            }
        }
    }

    fun startChildServices() {
        stopChildServices()

        // Synchronous initial state to avoid null state in server / broadcast
        val syncDeviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: ("dev_" + Build.MODEL.hashCode().toString())

        val syncInitial = ChildDevice(
            id = syncDeviceId,
            familyId = "KID-7777",
            name = Build.MODEL,
            deviceType = detectDeviceType(),
            status = DeviceStatus.ONLINE,
            batteryPercent = getBatteryInfo().first,
            isCharging = getBatteryInfo().second,
            lastSeenTimestamp = System.currentTimeMillis()
        )
        _currentDeviceState.value = syncInitial

        // 0. Start LAN embedded server & broadcasting of child state
        lanDiscoveryManager.setFrameProvider { ScreenCaptureService.latestJpegFrame }
        lanDiscoveryManager.startBroadcasting { _currentDeviceState.value }

        scope.launch {
            val deviceId = repository.deviceIdFlow.first()
            val familyId = repository.familyIdFlow.first()
            val deviceType = detectDeviceType()
            val batteryInfo = getBatteryInfo()
            val initialDevice = ChildDevice(
                id = deviceId,
                familyId = familyId,
                name = Build.MODEL,
                deviceType = deviceType,
                status = if (isScreenLocked) DeviceStatus.LOCKED else DeviceStatus.ONLINE,
                currentPackage = currentPackage,
                currentAppLabel = currentAppLabel,
                isVideoPlaying = isVideo,
                isAudioPlaying = false,
                currentVolume = 50,
                isScreenLocked = isScreenLocked,
                lastSeenTimestamp = System.currentTimeMillis(),
                batteryPercent = batteryInfo.first,
                isCharging = batteryInfo.second
            )
            _currentDeviceState.value = initialDevice
            lanDiscoveryManager.registerNsdService(initialDevice)
            lanDiscoveryManager.broadcastDevice(initialDevice)
        }

        // 1. Start Heartbeat Loop
        heartbeatJob = scope.launch {
            val deviceId = repository.deviceIdFlow.first()
            val deviceType = detectDeviceType()

            while (isActive) {
                val familyId = repository.familyIdFlow.first()
                val batteryInfo = getBatteryInfo()
                val mediaService = KidSeeMediaMonitorService.instance
                val volume = mediaService?.getCurrentVolumePercent() ?: 50
                val isAudio = mediaService?.isMediaPlaying() ?: false

                val device = ChildDevice(
                    id = deviceId,
                    familyId = familyId,
                    name = Build.MODEL,
                    deviceType = deviceType,
                    status = if (isScreenLocked) DeviceStatus.LOCKED else DeviceStatus.ONLINE,
                    currentPackage = currentPackage,
                    currentAppLabel = currentAppLabel,
                    isVideoPlaying = isVideo,
                    isAudioPlaying = isAudio,
                    currentVolume = volume,
                    isScreenLocked = isScreenLocked,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    batteryPercent = batteryInfo.first,
                    isCharging = batteryInfo.second
                )

                _currentDeviceState.value = device
                repository.updateDeviceStatus(device)
                delay(5_000L) // 5s heartbeat
            }
        }

        // 2. Start Command Listener
        commandJob = scope.launch {
            val deviceId = repository.deviceIdFlow.first()
            repository.observeIncomingCommands(deviceId).collect { command ->
                executeCommand(command)
            }
        }

        // 3. Start Voice Message Listener
        voiceJob = scope.launch {
            val deviceId = repository.deviceIdFlow.first()
            repository.observeVoiceMessages(deviceId).collect { msg ->
                voiceMessageManager.playVoiceMessage(msg)
            }
        }

        // 4. Start Schedule Check Loop
        scheduleJob = scope.launch {
            val deviceId = repository.deviceIdFlow.first()
            while (isActive) {
                checkSchedule(deviceId)
                delay(60_000L) // Check every minute
            }
        }
    }

    fun stopChildServices() {
        heartbeatJob?.cancel()
        commandJob?.cancel()
        voiceJob?.cancel()
        scheduleJob?.cancel()
        lanDiscoveryManager.stop()
    }

    suspend fun onChildActivityChanged(packageName: String, appLabel: String, isVideo: Boolean) {
        this.currentPackage = packageName
        this.currentAppLabel = appLabel
        this.isVideo = isVideo

        val current = _currentDeviceState.value ?: return
        val updated = current.copy(
            currentPackage = packageName,
            currentAppLabel = appLabel,
            isVideoPlaying = isVideo
        )
        _currentDeviceState.value = updated
        repository.updateDeviceStatus(updated)
    }

    private fun executeCommand(command: DeviceCommand) {
        Timber.d("Executing command on child: ${command.type}")
        val mediaService = KidSeeMediaMonitorService.instance

        when (command.type) {
            CommandType.LOCK_SCREEN -> {
                isScreenLocked = true
                KidSeeOverlayLockService.show(context)
                KidSeeDeviceAdmin.lockNow(context)
                mediaService?.pauseMedia()
            }
            CommandType.UNLOCK_SCREEN -> {
                isScreenLocked = false
                KidSeeOverlayLockService.hide(context)
            }
            CommandType.MEDIA_PLAY -> mediaService?.playMedia()
            CommandType.MEDIA_PAUSE -> mediaService?.pauseMedia()
            CommandType.MEDIA_NEXT -> mediaService?.skipNext()
            CommandType.MEDIA_PREV -> mediaService?.skipPrevious()
            CommandType.VOLUME_SET -> {
                val level = command.payload?.toIntOrNull() ?: 50
                mediaService?.setVolume(level)
            }
            CommandType.VOLUME_UP -> mediaService?.changeVolumeBy(1)
            CommandType.VOLUME_DOWN -> mediaService?.changeVolumeBy(-1)
            CommandType.START_STREAM -> {
                onStreamRequested?.invoke()
            }
            CommandType.STOP_STREAM -> {
                ScreenCaptureService.stop(context)
            }
            CommandType.SET_FAMILY -> {
                val newFamilyId = command.payload?.trim() ?: ""
                if (newFamilyId.isNotBlank()) {
                    scope.launch {
                        repository.saveFamilyId(newFamilyId)
                        val current = _currentDeviceState.value
                        if (current != null) {
                            val updated = current.copy(familyId = newFamilyId)
                            _currentDeviceState.value = updated
                            repository.updateDeviceStatus(updated)
                            lanDiscoveryManager.registerNsdService(updated)
                            lanDiscoveryManager.broadcastDevice(updated)
                        }
                        Timber.d("Child family ID updated to $newFamilyId")
                    }
                }
            }
            CommandType.CUSTOM -> {
                val payload = command.payload
                if (payload != null && payload.startsWith("SET_FAMILY:")) {
                    val newFamilyId = payload.removePrefix("SET_FAMILY:").trim()
                    if (newFamilyId.isNotBlank()) {
                        scope.launch {
                            repository.saveFamilyId(newFamilyId)
                            val current = _currentDeviceState.value
                            if (current != null) {
                                val updated = current.copy(familyId = newFamilyId)
                                _currentDeviceState.value = updated
                                repository.updateDeviceStatus(updated)
                                lanDiscoveryManager.registerNsdService(updated)
                                lanDiscoveryManager.broadcastDevice(updated)
                            }
                            Timber.d("Child family ID updated to $newFamilyId")
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private suspend fun checkSchedule(deviceId: String) {
        val rules = repository.observeSchedules(deviceId).first()
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        val dayOfWeekAdjusted = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1 // 1=Mon...7=Sun
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val shouldLock = rules.any { it.isActiveAt(dayOfWeekAdjusted, hour, minute) }
        if (shouldLock && !isScreenLocked) {
            isScreenLocked = true
            KidSeeOverlayLockService.show(context)
        } else if (!shouldLock && isScreenLocked) {
            isScreenLocked = false
            KidSeeOverlayLockService.hide(context)
        }
    }

    private fun detectDeviceType(): DeviceType {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        return when (uiModeManager.currentModeType) {
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION -> DeviceType.TV
            else -> {
                val metrics = context.resources.displayMetrics
                val widthInches = metrics.widthPixels / metrics.xdpi
                val heightInches = metrics.heightPixels / metrics.ydpi
                val diagonal = Math.hypot(widthInches.toDouble(), heightInches.toDouble())
                if (diagonal >= 7.0) DeviceType.TABLET else DeviceType.PHONE
            }
        }
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        return Pair(percent, isCharging)
    }
}
