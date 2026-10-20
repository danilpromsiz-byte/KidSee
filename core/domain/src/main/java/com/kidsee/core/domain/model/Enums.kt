package com.kidsee.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    PARENT,
    CHILD
}

@Serializable
enum class DeviceType {
    PHONE,
    TABLET,
    TV
}

@Serializable
enum class DeviceStatus {
    ONLINE,
    OFFLINE,
    STREAMING,
    LOCKED
}
