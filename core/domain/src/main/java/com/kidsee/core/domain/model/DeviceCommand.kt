package com.kidsee.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CommandType {
    LOCK_SCREEN,
    UNLOCK_SCREEN,
    MEDIA_PLAY,
    MEDIA_PAUSE,
    MEDIA_NEXT,
    MEDIA_PREV,
    VOLUME_SET,
    VOLUME_UP,
    VOLUME_DOWN,
    START_STREAM,
    STOP_STREAM,
    PLAY_VOICE_MESSAGE,
    SET_FAMILY,
    SET_PIN,
    CUSTOM
}

@Serializable
enum class CommandStatus {
    PENDING,
    EXECUTED,
    FAILED
}

@Serializable
data class DeviceCommand(
    val id: String = "",
    val targetDeviceId: String = "",
    val type: CommandType = CommandType.LOCK_SCREEN,
    val payload: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: CommandStatus = CommandStatus.PENDING
)

@Serializable
data class VoiceMessage(
    val id: String = "",
    val targetDeviceId: String = "",
    val senderName: String = "",
    val audioUrl: String = "",
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isPlayed: Boolean = false
)
