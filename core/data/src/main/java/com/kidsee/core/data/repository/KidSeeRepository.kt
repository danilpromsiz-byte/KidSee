package com.kidsee.core.data.repository

import com.kidsee.core.data.local.KidSeePreferences
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.domain.model.DeviceCommand
import com.kidsee.core.domain.model.ScheduleRule
import com.kidsee.core.domain.model.UserRole
import com.kidsee.core.domain.model.VoiceMessage
import com.kidsee.core.network.FirebaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class KidSeeRepository(
    private val preferences: KidSeePreferences,
    private val firebaseClient: FirebaseClient
) {
    constructor(preferences: KidSeePreferences) : this(preferences, FirebaseClient())

    val userRoleFlow = preferences.roleFlow
    val familyIdFlow = preferences.familyIdFlow
    val deviceIdFlow = preferences.deviceIdFlow
    val isSetupCompleteFlow = preferences.isSetupCompleteFlow

    suspend fun saveRole(role: UserRole) {
        preferences.setUserRole(role)
    }

    suspend fun saveFamilyId(familyId: String) {
        preferences.setFamilyId(familyId)
    }

    suspend fun saveDeviceId(deviceId: String) {
        preferences.setDeviceId(deviceId)
    }

    suspend fun completeSetup() {
        preferences.setSetupComplete(true)
    }

    // --- Devices ---

    suspend fun updateDeviceStatus(device: ChildDevice) {
        firebaseClient.updateDeviceStatus(device)
    }

    fun observeFamilyDevices(familyId: String): Flow<List<ChildDevice>> {
        return firebaseClient.observeFamilyDevices(familyId)
    }

    fun observeSingleDevice(familyId: String, deviceId: String): Flow<ChildDevice?> {
        return firebaseClient.observeSingleDevice(familyId, deviceId)
    }

    // --- Commands ---

    suspend fun sendCommand(command: DeviceCommand) {
        firebaseClient.sendCommand(command)
    }

    fun observeIncomingCommands(deviceId: String): Flow<DeviceCommand> {
        return firebaseClient.observeIncomingCommands(deviceId)
    }

    // --- Voice Messages ---

    suspend fun sendVoiceMessage(message: VoiceMessage) {
        firebaseClient.sendVoiceMessage(message)
    }

    fun observeVoiceMessages(deviceId: String): Flow<VoiceMessage> {
        return firebaseClient.observeVoiceMessages(deviceId)
    }

    // --- Scheduling ---

    suspend fun saveScheduleRule(rule: ScheduleRule) {
        firebaseClient.saveScheduleRule(rule)
    }

    fun observeSchedules(deviceId: String): Flow<List<ScheduleRule>> {
        return firebaseClient.observeSchedules(deviceId)
    }
}
