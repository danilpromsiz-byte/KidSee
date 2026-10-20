package com.kidsee.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChildDevice(
    val id: String = "",
    val familyId: String = "",
    val name: String = "",
    val deviceType: DeviceType = DeviceType.PHONE,
    val status: DeviceStatus = DeviceStatus.OFFLINE,
    val currentPackage: String? = null,
    val currentAppLabel: String? = null,
    val currentMediaTitle: String? = null,
    val isVideoPlaying: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val currentVolume: Int = 50,
    val maxVolume: Int = 100,
    val isScreenLocked: Boolean = false,
    val lastSeenTimestamp: Long = 0L,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val thumbnailBase64: String? = null,
    val isWebRtcStreamActive: Boolean = false
) {
    val isOnline: Boolean
        get() = (System.currentTimeMillis() - lastSeenTimestamp) < 30_000L
}
